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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games_list);

        recyclerView = findViewById(R.id.games_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Stub: In-memory games list
        games = new ArrayList<>();
        games.add(new GameConfig("Home Game", 0));
        games.add(new GameConfig("Turbo", 1));
        games.add(new GameConfig("Deep Stack", 2));
        games.add(new GameConfig("Short Stack", 3));

        adapter = new GamesAdapter(this, games);
        recyclerView.setAdapter(adapter);
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
                context.startActivity(intent);
            });
            holder.playButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, BlindCountdownActivity.class);
                // TODO: Pass game config for playing
                context.startActivity(intent);
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
            GameViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.game_name_text);
                editButton = itemView.findViewById(R.id.edit_game_button);
                playButton = itemView.findViewById(R.id.play_game_button);
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
} 
