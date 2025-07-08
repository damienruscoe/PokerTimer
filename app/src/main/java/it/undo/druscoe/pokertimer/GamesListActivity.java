package it.undo.druscoe.pokertimer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GamesListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private GamesAdapter adapter;
    private List<GameConfig> games;
    private int[] cardColors = {
            R.color.game_card_1, R.color.game_card_2, R.color.game_card_3, R.color.game_card_4,
            R.color.game_card_5, R.color.game_card_6, R.color.game_card_7, R.color.game_card_8
    };
    private static final String PREFS_NAME = "poker_games";
    private static final String KEY_GAMES_LIST = "games_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games_list);

        recyclerView = findViewById(R.id.games_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load games from SharedPreferences
        games = loadGamesFromPrefs();
        if (games == null || games.isEmpty()) {
            games = new ArrayList<>();
            games.add(new GameConfig("Home Game", 0));
            games.add(new GameConfig("Turbo", 1));
            games.add(new GameConfig("Deep Stack", 2));
            games.add(new GameConfig("Short Stack", 3));
            // Save defaults to prefs
            saveGamesToPrefs();
        }

        adapter = new GamesAdapter(this, games);
        recyclerView.setAdapter(adapter);

        // Add Game button
        Button addGameButton = findViewById(R.id.add_game_button);
        if (addGameButton != null) {
            addGameButton.setOnClickListener(v -> {
                // Add a new game with a unique name and color
                int colorIdx = games.size() % cardColors.length;
                String name = "Game " + (games.size() + 1);
                games.add(new GameConfig(name, colorIdx));
                adapter.notifyItemInserted(games.size() - 1);
                saveGamesToPrefs();
            });
        }
    }

    private class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.GameViewHolder> {
        private Context context;
        private List<GameConfig> games;

        GamesAdapter(Context context, List<GameConfig> games) {
            this.context = context;
            this.games = games;
        }

        @NonNull
        @Override
        public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
            return new GameViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            GameConfig game = games.get(position);
            holder.nameText.setText(game.name);
            int colorRes = cardColors[position % cardColors.length];
            holder.itemView.setBackgroundResource(colorRes);
            holder.editButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, SettingsActivity.class);
                // Serialize blindLevels to JSON
                JSONArray arr = new JSONArray();
                for (SettingsActivity.BlindLevel level : game.blindLevels) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("big", level.big);
                        obj.put("small", level.small);
                        obj.put("duration", level.duration);
                        arr.put(obj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                intent.putExtra("EXTRA_BLIND_LEVELS_JSON", arr.toString());
                intent.putExtra("EXTRA_GAME_INDEX", holder.getAdapterPosition());
                ((AppCompatActivity) context).startActivityForResult(intent, 1001);
            });
            holder.playButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, BlindCountdownActivity.class);
                // TODO: Pass game config for playing
                context.startActivity(intent);
            });
            holder.deleteButton.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    games.remove(pos);
                    notifyItemRemoved(pos);
                    saveGamesToPrefs();
                }
            });
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        class GameViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            Button editButton;
            Button playButton;
            Button deleteButton;
            GameViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.game_name_text);
                editButton = itemView.findViewById(R.id.edit_game_button);
                playButton = itemView.findViewById(R.id.play_game_button);
                deleteButton = itemView.findViewById(R.id.delete_game_button);
            }
        }
    }

    // Simple model for a game configuration
    private static class GameConfig {
        String name;
        int colorIndex;
        List<SettingsActivity.BlindLevel> blindLevels;

        // Add more fields for blinds/duration as needed
        GameConfig(String name, int colorIndex) {
            this.name = name;
            this.colorIndex = colorIndex;
            this.blindLevels = new ArrayList<>();
        }
    }

    // Utility: Save games list to SharedPreferences as JSON
    private void saveGamesToPrefs() {
        try {
            JSONArray arr = new JSONArray();
            for (GameConfig game : games) {
                JSONObject obj = new JSONObject();
                obj.put("name", game.name);
                obj.put("colorIndex", game.colorIndex);
                // Serialize blindLevels
                JSONArray levelsArr = new JSONArray();
                for (SettingsActivity.BlindLevel level : game.blindLevels) {
                    JSONObject levelObj = new JSONObject();
                    levelObj.put("big", level.big);
                    levelObj.put("small", level.small);
                    levelObj.put("duration", level.duration);
                    levelsArr.put(levelObj);
                }
                obj.put("blindLevels", levelsArr);
                arr.put(obj);
            }
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_GAMES_LIST, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Utility: Load games list from SharedPreferences (JSON)
    private List<GameConfig> loadGamesFromPrefs() {
        List<GameConfig> loaded = new ArrayList<>();
        try {
            String json = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GAMES_LIST, null);
            if (json == null) return loaded;
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                int colorIndex = obj.getInt("colorIndex");
                GameConfig game = new GameConfig(name, colorIndex);
                // Blind levels
                if (obj.has("blindLevels")) {
                    JSONArray levelsArr = obj.getJSONArray("blindLevels");
                    for (int j = 0; j < levelsArr.length(); j++) {
                        JSONObject levelObj = levelsArr.getJSONObject(j);
                        int big = levelObj.getInt("big");
                        int small = levelObj.getInt("small");
                        int duration = levelObj.getInt("duration");
                        game.blindLevels.add(new SettingsActivity.BlindLevel(big, small, duration));
                    }
                }
                loaded.add(game);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loaded;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            int gameIdx = data.getIntExtra("EXTRA_GAME_INDEX", -1);
            String levelsJson = data.getStringExtra("EXTRA_BLIND_LEVELS_JSON");
            if (gameIdx >= 0 && gameIdx < games.size() && levelsJson != null) {
                try {
                    JSONArray arr = new JSONArray(levelsJson);
                    List<SettingsActivity.BlindLevel> newLevels = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        newLevels.add(new SettingsActivity.BlindLevel(
                            obj.getInt("big"),
                            obj.getInt("small"),
                            obj.getInt("duration")
                        ));
                    }
                    games.get(gameIdx).blindLevels = newLevels;
                    adapter.notifyItemChanged(gameIdx);
                    saveGamesToPrefs();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
} 
