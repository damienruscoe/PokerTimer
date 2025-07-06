package it.undo.druscoe.pokertimer;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.view.WindowManager;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.os.Build;

public class BlindCountdownActivity extends AppCompatActivity {
    private TextView countdownText;
    private TextView countdownReflection;
    private TextView bigBlindText;
    private TextView smallBlindText;
    private Button playPauseButton;
    private Button previousButton;
    private Button nextButton;
    private Button plusThirtyButton;
    private Button minusThirtyButton;
    private Button plusOneMinuteButton;
    private Button minusOneMinuteButton;
    private Button themeToggleButton;
    private Button settingsButton;
    private CountDownTimer timer;
    private boolean isRunning = false;
    private long timeLeftMillis;
    private int currentLevel = 0;

    private static final String PREFS_NAME = "poker_settings";
    private static final String KEY_BLIND_LEVELS = "blind_levels";
    private static final String KEY_WARNING_SOUND = "warning_sound_enabled";
    private static final String KEY_END_SOUND = "end_sound_enabled";

    private List<BlindLevel> blindLevels = new ArrayList<>();
    private boolean warningSoundEnabled = true;
    private boolean endSoundEnabled = true;

    private MediaPlayer warningPlayer;
    private MediaPlayer endPlayer;

    private static final int REQUEST_CODE_SETTINGS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply fallback theme for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            try {
                setTheme(R.style.Theme_PokerTime_Legacy);
            } catch (Exception e) {
                Log.e("BlindCountdown", "Failed to set fallback theme", e);
            }
        }
        
        try {
            setContentView(R.layout.activity_blind_countdown);
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to set content view", e);
            // Fallback to a simple layout if needed
            return;
        }

        loadSettings();

        // Initialize views with null checks
        try {
            countdownText = findViewById(R.id.countdown_text);
            countdownReflection = findViewById(R.id.countdown_reflection);
            bigBlindText = findViewById(R.id.big_blind_text);
            smallBlindText = findViewById(R.id.small_blind_text);
            playPauseButton = findViewById(R.id.play_pause_button);
            previousButton = findViewById(R.id.previous_button);
            nextButton = findViewById(R.id.next_button);
            plusThirtyButton = findViewById(R.id.plus_thirty_seconds_button);
            minusThirtyButton = findViewById(R.id.minus_thirty_seconds_button);
            plusOneMinuteButton = findViewById(R.id.plus_one_minute_button);
            minusOneMinuteButton = findViewById(R.id.minus_one_minute_button);
            themeToggleButton = findViewById(R.id.theme_toggle_button);
            settingsButton = findViewById(R.id.settings_button);
            
            // Log view initialization for debugging
            Log.d("BlindCountdown", "Views initialized - countdownText: " + (countdownText != null) + 
                  ", playPauseButton: " + (playPauseButton != null) + 
                  ", Android version: " + Build.VERSION.SDK_INT);
            
            // Handle reflection effect based on Android version
            if (countdownReflection != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    // Disable reflection effect on older Android versions
                    countdownReflection.setVisibility(View.GONE);
                } else {
                    // Apply rotation for newer Android versions
                    countdownReflection.setRotationX(180);
                }
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to find views", e);
            return;
        }

        // Handle Material Design compatibility for older Android versions
        handleMaterialDesignCompatibility();

        // Initialize MediaPlayer with error handling
        try {
            warningPlayer = MediaPlayer.create(this, R.raw.warning);
            endPlayer = MediaPlayer.create(this, R.raw.end);
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to create MediaPlayer", e);
            warningPlayer = null;
            endPlayer = null;
        }

        if (savedInstanceState != null) {
            try {
                timeLeftMillis = savedInstanceState.getLong("timeLeftMillis", blindLevels.get(0).duration * 1000L);
                isRunning = savedInstanceState.getBoolean("isRunning", false);
                currentLevel = savedInstanceState.getInt("currentLevel", 0);
                if (currentLevel < blindLevels.size()) {
                    int big = blindLevels.get(currentLevel).big;
                    int small = blindLevels.get(currentLevel).small;
                    if (bigBlindText != null) bigBlindText.setText(String.valueOf(big));
                    if (smallBlindText != null) smallBlindText.setText(String.valueOf(small));
                }
                updateCountdownText();
                if (previousButton != null) previousButton.setEnabled(currentLevel > 0);
                if (nextButton != null) nextButton.setEnabled(currentLevel < blindLevels.size() - 1);
                if (isRunning) {
                    startTimer();
                } else {
                    if (playPauseButton != null) playPauseButton.setText("Play");
                }
            } catch (Exception e) {
                Log.e("BlindCountdown", "Failed to restore state", e);
                setupLevel();
            }
        } else {
            setupLevel();
        }

        // Set default to dark mode only if not set
        try {
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            updateThemeToggleButton();
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to set theme", e);
        }

        // Set up click listeners with null checks
        if (themeToggleButton != null) {
            themeToggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        int mode = AppCompatDelegate.getDefaultNightMode();
                        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        }
                        recreate();
                    } catch (Exception e) {
                        Log.e("BlindCountdown", "Failed to toggle theme", e);
                    }
                }
            });
        }

        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isRunning) {
                        pauseTimer();
                    } else {
                        startTimer();
                    }
                }
            });
        }

        if (previousButton != null) {
            previousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLevel > 0) {
                        currentLevel--;
                        setupLevel();
                    }
                }
            });
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentLevel < blindLevels.size() - 1) {
                        currentLevel++;
                        setupLevel();
                    }
                }
            });
        }

        if (plusThirtyButton != null) {
            plusThirtyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeLeftMillis += 30000;
                    updateCountdownText();
                }
            });
        }

        if (minusThirtyButton != null) {
            minusThirtyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeLeftMillis = Math.max(0, timeLeftMillis - 30000);
                    updateCountdownText();
                }
            });
        }

        if (plusOneMinuteButton != null) {
            plusOneMinuteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeLeftMillis += 60000;
                    updateCountdownText();
                }
            });
        }

        if (minusOneMinuteButton != null) {
            minusOneMinuteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeLeftMillis = Math.max(0, timeLeftMillis - 60000);
                    updateCountdownText();
                }
            });
        }

        if (settingsButton != null) {
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(BlindCountdownActivity.this, SettingsActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                    } catch (Exception e) {
                        Log.e("BlindCountdown", "Failed to start settings activity", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putLong("timeLeftMillis", timeLeftMillis);
            outState.putBoolean("isRunning", isRunning);
            outState.putInt("currentLevel", currentLevel);
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to save state", e);
        }
    }

    private void loadSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            warningSoundEnabled = prefs.getBoolean(KEY_WARNING_SOUND, true);
            endSoundEnabled = prefs.getBoolean(KEY_END_SOUND, true);
            String json = prefs.getString(KEY_BLIND_LEVELS, null);
            blindLevels.clear();
            if (json != null) {
                try {
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        blindLevels.add(new BlindLevel(
                            obj.getInt("big"),
                            obj.getInt("small"),
                            obj.getInt("duration")
                        ));
                    }
                } catch (JSONException e) {
                    Log.e("BlindCountdown", "Failed to parse blind levels JSON", e);
                }
            } else {
                // Default levels if none saved
                blindLevels.add(new BlindLevel(200, 100, 60 * 15));
                blindLevels.add(new BlindLevel(400, 200, 60 * 15));
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to load settings", e);
            // Set default levels
            blindLevels.clear();
            blindLevels.add(new BlindLevel(200, 100, 60 * 15));
            blindLevels.add(new BlindLevel(400, 200, 60 * 15));
        }
    }

    static class BlindLevel {
        int big;
        int small;
        int duration;
        BlindLevel(int big, int small, int duration) {
            this.big = big;
            this.small = small;
            this.duration = duration;
        }
    }

    private void setupLevel() {
        try {
            if (blindLevels.isEmpty() || currentLevel >= blindLevels.size()) {
                Log.e("BlindCountdown", "Invalid blind level index");
                return;
            }
            
            int big = blindLevels.get(currentLevel).big;
            int small = blindLevels.get(currentLevel).small;
            int duration = blindLevels.get(currentLevel).duration;
            timeLeftMillis = duration * 1000L;
            updateCountdownText();
            if (bigBlindText != null) bigBlindText.setText(String.valueOf(big));
            if (smallBlindText != null) smallBlindText.setText(String.valueOf(small));
            if (playPauseButton != null) playPauseButton.setText("Play");
            isRunning = false;
            if (timer != null) timer.cancel();
            if (previousButton != null) previousButton.setEnabled(currentLevel > 0);
            if (nextButton != null) nextButton.setEnabled(currentLevel < blindLevels.size() - 1);
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to setup level", e);
        }
    }

    private void safePlay(MediaPlayer player) {
        try {
            if (player != null) {
                player.seekTo(0);
                player.start();
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to play sound", e);
        }
    }

    private void flashScreen(final int times) {
        // No flashing
    }

    private void startTimer() {
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            timer = new CountDownTimer(timeLeftMillis, 1000) {
                boolean warningPlayed = false;
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftMillis = millisUntilFinished;
                    updateCountdownText();
                    if (millisUntilFinished <= 60000 && !warningPlayed) {
                        if (warningSoundEnabled) safePlay(warningPlayer);
                        warningPlayed = true;
                    }
                }
                @Override
                public void onFinish() {
                    if (endSoundEnabled) safePlay(endPlayer);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    nextLevel();
                }
            };
            timer.start();
            isRunning = true;
            if (playPauseButton != null) playPauseButton.setText("Pause");
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to start timer", e);
        }
    }

    private void pauseTimer() {
        try {
            if (timer != null) timer.cancel();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            isRunning = false;
            if (playPauseButton != null) playPauseButton.setText("Play");
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to pause timer", e);
        }
    }

    private void nextLevel() {
        try {
            currentLevel++;
            if (currentLevel < blindLevels.size()) {
                setupLevel();
                startTimer();
            } else {
                if (countdownText != null) countdownText.setText("Game Over");
                if (countdownReflection != null) countdownReflection.setText("Game Over");
                if (bigBlindText != null) bigBlindText.setText("");
                if (smallBlindText != null) smallBlindText.setText("");
                if (playPauseButton != null) playPauseButton.setEnabled(false);
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to go to next level", e);
        }
    }

    private void updateCountdownText() {
        try {
            int minutes = (int) (timeLeftMillis / 1000) / 60;
            int seconds = (int) (timeLeftMillis / 1000) % 60;
            String timeFormatted = String.format("%02d:%02d", minutes, seconds);
            if (countdownText != null) countdownText.setText(timeFormatted);
            if (countdownReflection != null) {
                countdownReflection.setText(timeFormatted);
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to update countdown text", e);
        }
    }

    private void updateThemeToggleButton() {
        try {
            int mode = AppCompatDelegate.getDefaultNightMode();
            if (themeToggleButton != null) {
                if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
                    themeToggleButton.setText("☀");
                } else {
                    themeToggleButton.setText("☾");
                }
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to update theme toggle button", e);
        }
    }

    private void handleMaterialDesignCompatibility() {
        try {
            // On older Android versions, some Material Design features might not work properly
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // Disable some advanced Material Design features that might cause issues
                if (countdownReflection != null) {
                    countdownReflection.setVisibility(View.GONE);
                }
                
                // For API 19 and below, ensure basic button functionality
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    // Use basic Android colors for better compatibility
                    if (playPauseButton != null) {
                        playPauseButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                        playPauseButton.setTextColor(getResources().getColor(android.R.color.white));
                    }
                    
                    if (previousButton != null) {
                        previousButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        previousButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                    
                    if (nextButton != null) {
                        nextButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                    
                    if (plusThirtyButton != null) {
                        plusThirtyButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        plusThirtyButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                    
                    if (minusThirtyButton != null) {
                        minusThirtyButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        minusThirtyButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                    
                    if (plusOneMinuteButton != null) {
                        plusOneMinuteButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        plusOneMinuteButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                    
                    if (minusOneMinuteButton != null) {
                        minusOneMinuteButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        minusOneMinuteButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    }
                    
                    if (themeToggleButton != null) {
                        themeToggleButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        themeToggleButton.setTextColor(getResources().getColor(android.R.color.white));
                    }
                    
                    if (settingsButton != null) {
                        settingsButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        settingsButton.setTextColor(getResources().getColor(android.R.color.white));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to handle Material Design compatibility", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (timer != null) timer.cancel();
            if (warningPlayer != null) warningPlayer.release();
            if (endPlayer != null) endPlayer.release();
        } catch (Exception e) {
            Log.e("BlindCountdown", "Failed to cleanup resources", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTINGS) {
            try {
                loadSettings();
                currentLevel = 0;
                setupLevel();
            } catch (Exception e) {
                Log.e("BlindCountdown", "Failed to handle settings result", e);
            }
        }
    }
} 