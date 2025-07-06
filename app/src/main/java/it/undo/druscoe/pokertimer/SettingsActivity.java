package it.undo.druscoe.pokertimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import it.undo.druscoe.pokertimer.BlindLevelListView;

public class SettingsActivity extends AppCompatActivity {
    private Button addLevelButton;
    private Switch warningSoundSwitch;
    private Switch endSoundSwitch;
    private Button saveButton;
    private BlindLevelListView blindLevelListView;

    private static final String PREFS_NAME = "poker_settings";
    private static final String KEY_BLIND_LEVELS = "blind_levels";
    private static final String KEY_WARNING_SOUND = "warning_sound_enabled";
    private static final String KEY_END_SOUND = "end_sound_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_settings);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to set content view", e);
            return;
        }

        try {
            blindLevelListView = findViewById(R.id.blind_level_list_view);
            warningSoundSwitch = findViewById(R.id.warning_sound_switch);
            endSoundSwitch = findViewById(R.id.end_sound_switch);
            saveButton = findViewById(R.id.save_button);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to find views", e);
            return;
        }

        if (saveButton != null) {
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (validateBlindLevels()) {
                            saveSettings();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Failed to save settings", e);
                    }
                }
            });
        }

        loadSettings();
    }

    private void loadSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            // Load sound settings
            boolean warningEnabled = prefs.getBoolean(KEY_WARNING_SOUND, true);
            boolean endEnabled = prefs.getBoolean(KEY_END_SOUND, true);
            if (warningSoundSwitch != null) warningSoundSwitch.setChecked(warningEnabled);
            if (endSoundSwitch != null) endSoundSwitch.setChecked(endEnabled);
            // Load blind levels
            String json = prefs.getString(KEY_BLIND_LEVELS, null);
            List<BlindLevel> loaded = new ArrayList<>();
            if (json != null) {
                try {
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        loaded.add(new BlindLevel(
                            obj.getInt("big"),
                            obj.getInt("small"),
                            obj.getInt("duration")
                        ));
                    }
                } catch (JSONException e) {
                    Log.e("SettingsActivity", "Failed to parse blind levels JSON", e);
                }
            } else {
                // Default levels if none saved
                loaded.add(new BlindLevel(200, 100, 60 * 15));
                loaded.add(new BlindLevel(400, 200, 60 * 15));
                loaded.add(new BlindLevel(600, 300, 60 * 15));
                loaded.add(new BlindLevel(800, 400, 60 * 15));
                loaded.add(new BlindLevel(1200, 600, 60 * 15));
                loaded.add(new BlindLevel(1600, 800, 60 * 15));
                loaded.add(new BlindLevel(2000, 1000, 60 * 15));
                loaded.add(new BlindLevel(4000, 2000, 60 * 15));
                loaded.add(new BlindLevel(8000, 4000, 60 * 15));
            }
            if (blindLevelListView != null) {
                blindLevelListView.setBlindLevels(loaded);
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to load settings", e);
        }
    }

    private void saveSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            // Save sound settings
            if (warningSoundSwitch != null) {
                editor.putBoolean(KEY_WARNING_SOUND, warningSoundSwitch.isChecked());
            }
            if (endSoundSwitch != null) {
                editor.putBoolean(KEY_END_SOUND, endSoundSwitch.isChecked());
            }
            // Save blind levels as JSON
            if (blindLevelListView != null) {
                JSONArray arr = new JSONArray();
                for (BlindLevel level : blindLevelListView.getBlindLevels()) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("big", level.big);
                        obj.put("small", level.small);
                        obj.put("duration", level.duration);
                        arr.put(obj);
                    } catch (JSONException e) {
                        Log.e("SettingsActivity", "Failed to create JSON object", e);
                    }
                }
                editor.putString(KEY_BLIND_LEVELS, arr.toString());
            }
            editor.apply();
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to save settings", e);
        }
    }

    private boolean validateBlindLevels() {
        try {
            if (blindLevelListView == null) {
                showErrorDialog("Blind level list view is not available");
                return false;
            }
            
            List<BlindLevel> levels = blindLevelListView.getBlindLevels();
            for (int i = 0; i < levels.size(); i++) {
                BlindLevel level = levels.get(i);
                if (level.big <= level.small) {
                    showErrorDialog("Big blind must be greater than small blind at level " + (i + 1));
                    return false;
                }
                if (level.duration < 60) {
                    showErrorDialog("Minimum duration is 1 minute at level " + (i + 1));
                    return false;
                }
                if (i > 0) {
                    BlindLevel prev = levels.get(i - 1);
                    if (level.big <= prev.big || level.small <= prev.small) {
                        showErrorDialog("Blinds must be in strictly ascending order at level " + (i + 1));
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to validate blind levels", e);
            showErrorDialog("Failed to validate blind levels");
            return false;
        }
    }

    private void showErrorDialog(String message) {
        try {
            new AlertDialog.Builder(this)
                .setTitle("Invalid Blind Levels")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
        } catch (Exception e) {
            Log.e("SettingsActivity", "Failed to show error dialog", e);
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
} 
