package it.undo.druscoe.pokertimer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlindLevelListView extends FrameLayout {
    private static final String TAG = "BlindLevelListView";
    private RecyclerView recyclerView;
    private UnifiedBlindAdapter adapter;
    private SettingsActivity.BlindLevel editingBlindLevel = new SettingsActivity.BlindLevel(200, 100, 15 * 60);
    private List<SettingsActivity.BlindLevel> blindLevels = new ArrayList<>();
    private List<SettingsActivity.BlindLevel> extrapolatedLevels = new ArrayList<>();

    public BlindLevelListView(Context context) {
        super(context);
        init(context);
    }
    public BlindLevelListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public BlindLevelListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    private void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null in init");
            return;
        }
        
        try {
            // Check if we're on a compatible API level
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                Log.w(TAG, "Running on very old Android version: " + Build.VERSION.SDK_INT);
            }
            
            LayoutInflater inflater = LayoutInflater.from(context);
            if (inflater == null) {
                Log.e(TAG, "LayoutInflater is null");
                return;
            }
            
            View inflatedView = inflater.inflate(R.layout.blind_level_list_view, this, true);
            if (inflatedView == null) {
                Log.e(TAG, "Failed to inflate layout");
                return;
            }
            
            recyclerView = findViewById(R.id.blind_levels_recycler);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                return;
            }
            
            // Use LinearLayoutManager with context check
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            if (layoutManager != null) {
                recyclerView.setLayoutManager(layoutManager);
            }
            
            adapter = new UnifiedBlindAdapter();
            recyclerView.setAdapter(adapter);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during initialization: " + e.getMessage(), e);
            // Try to create a minimal working state
            try {
                if (recyclerView == null) {
                    recyclerView = new RecyclerView(context);
                    addView(recyclerView);
                }
                if (adapter == null) {
                    adapter = new UnifiedBlindAdapter();
                }
                if (recyclerView != null && adapter != null) {
                    recyclerView.setAdapter(adapter);
                }
            } catch (Exception fallbackException) {
                Log.e(TAG, "Fallback initialization also failed: " + fallbackException.getMessage(), fallbackException);
            }
        }
    }
    
    public void setBlindLevels(List<SettingsActivity.BlindLevel> levels) {
        try {
            blindLevels.clear();
            if (levels != null) {
                blindLevels.addAll(levels);
            }
            editingBlindLevel = extrapolateNextLevel(blindLevels);
            calculateExtrapolated();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error setting blind levels: " + e.getMessage(), e);
        }
    }
    
    public List<SettingsActivity.BlindLevel> getBlindLevels() {
        return new ArrayList<>(blindLevels);
    }

    private static SettingsActivity.BlindLevel extrapolateNextLevel(List<SettingsActivity.BlindLevel> previousLevels) {
        SettingsActivity.BlindLevel newLevel = new SettingsActivity.BlindLevel(200, 100, 15 * 60);
        
        int size = previousLevels.size();
        if (size == 1) {
            SettingsActivity.BlindLevel last = previousLevels.get(size - 1);

            newLevel.small = last.small * 2;
            newLevel.big = newLevel.small * 2;
            newLevel.duration = last.duration;
        } else if (size > 1) {
            SettingsActivity.BlindLevel last = previousLevels.get(size - 1);
            SettingsActivity.BlindLevel secondLast = previousLevels.get(size - 2);

            newLevel.small = last.small + (last.small - secondLast.small);
            newLevel.big = newLevel.small * 2;
            newLevel.duration = last.duration;
        }
        return newLevel;
    }

    private void calculateExtrapolated() {
        List<SettingsActivity.BlindLevel> base = new ArrayList<>(blindLevels);
        base.add(editingBlindLevel);

        int count = Math.max(3, 12 - blindLevels.size());

        extrapolatedLevels.clear();
        for (int i = 0; i < count; i++) {
            SettingsActivity.BlindLevel next = extrapolateNextLevel(base);
            base.add(next);
            extrapolatedLevels.add(next);
        }
    }

    private boolean validateEditingBlind() {
        SettingsActivity.BlindLevel last = new SettingsActivity.BlindLevel(0, 0, 0);

        if (blindLevels.size() > 0)
            last = blindLevels.get(blindLevels.size() - 1);

        return editingBlindLevel.big > last.big 
            && editingBlindLevel.small > last.small
            && editingBlindLevel.big > editingBlindLevel.small
            && editingBlindLevel.duration >= 1;
    }

    private class UnifiedBlindAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_ACCEPTED = 0;
        private static final int VIEW_TYPE_EDIT = 1;
        private static final int VIEW_TYPE_EXTRAPOLATED = 2;

        @Override
        public int getItemViewType(int position) {
            if (position < 0)
                return VIEW_TYPE_ACCEPTED;
            else if (position < blindLevels.size())
                return VIEW_TYPE_ACCEPTED;
            else if (position == blindLevels.size())
                return VIEW_TYPE_EDIT;
            else
                return VIEW_TYPE_EXTRAPOLATED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_EDIT:
                    return createEditView(parent);
                case VIEW_TYPE_ACCEPTED:
                case VIEW_TYPE_EXTRAPOLATED:
                default:
                    return createBlindItemView(parent);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_EDIT:
                    if (holder instanceof EditViewHolder)
                        bindEditView((EditViewHolder) holder);
                    break;
                case VIEW_TYPE_ACCEPTED:
                    if (holder instanceof BlindViewHolder)
                        bindAcceptedBlind((BlindViewHolder) holder, position);
                    break;
                case VIEW_TYPE_EXTRAPOLATED:
                default:
                    if (holder instanceof BlindViewHolder)
                        bindExtrapolatedBlind((BlindViewHolder) holder, position);
                    break;
            }
        }

        private BlindViewHolder createBlindItemView(ViewGroup parent) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (inflater == null)
                throw new IllegalArgumentException("LayoutInflater is null");

            try {
                return new BlindViewHolder(inflater.inflate(R.layout.item_blind_level, parent, false));
            } catch (Exception e) {
                Log.e(TAG, "Failed to inflate item_blind_level: " + e.getMessage(), e);

                LinearLayout layout = new LinearLayout(context);
                layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(16, 8, 16, 8);
                
                TextView bigText = new TextView(context);
                bigText.setId(R.id.big_blind_text);
                bigText.setLayoutParams(new LinearLayout.LayoutParams(0, 
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                TextView smallText = new TextView(context);
                smallText.setId(R.id.small_blind_text);
                smallText.setLayoutParams(new LinearLayout.LayoutParams(0, 
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                TextView durationText = new TextView(context);
                durationText.setId(R.id.duration_text);
                durationText.setLayoutParams(new LinearLayout.LayoutParams(0, 
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                ImageButton deleteButton = new ImageButton(context);
                deleteButton.setId(R.id.delete_button);
                deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                deleteButton.setBackgroundResource(android.R.drawable.ic_menu_delete);
                
                layout.addView(bigText);
                layout.addView(smallText);
                layout.addView(durationText);
                layout.addView(deleteButton);
                
                return new BlindViewHolder(layout);
            }
        }

        private EditViewHolder createEditView(@NonNull ViewGroup parent) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (inflater == null)
                throw new IllegalArgumentException("LayoutInflater is null");

            try {
                return new EditViewHolder(inflater.inflate(R.layout.item_edit_blind, parent, false));
            } catch (Exception e) {
                Log.e(TAG, "Failed to inflate item_edit_blind: " + e.getMessage(), e);

                LinearLayout layout = new LinearLayout(context);
                layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(16, 8, 16, 8);
                
                EditText bigEdit = new EditText(context);
                bigEdit.setId(R.id.edit_big_blind);
                bigEdit.setHint("Big Blind");
                bigEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                bigEdit.setLayoutParams(new LinearLayout.LayoutParams(0, 
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                EditText smallEdit = new EditText(context);
                smallEdit.setId(R.id.edit_small_blind);
                smallEdit.setHint("Small Blind");
                smallEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                smallEdit.setLayoutParams(new LinearLayout.LayoutParams(0, 
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                EditText durationEdit = new EditText(context);
                durationEdit.setId(R.id.edit_duration);
                durationEdit.setHint("Duration (min)");
                durationEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                durationEdit.setLayoutParams(new LinearLayout.LayoutParams(0, 
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                Button addBlindButton = new Button(context);
                addBlindButton.setId(R.id.add_blind_button);
                addBlindButton.setText("Add");
                addBlindButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                
                layout.addView(bigEdit);
                layout.addView(smallEdit);
                layout.addView(durationEdit);
                layout.addView(addBlindButton);
                
                return new EditViewHolder(layout);
            }
        }

        private void bindAcceptedBlind(@NonNull BlindViewHolder holder, int position) {
            holder.setAcceptedBlindStyle();
            SettingsActivity.BlindLevel level = blindLevels.get(position);
            holder.setBlindData(level);
            holder.initHandlers();
        }

        private void bindExtrapolatedBlind(@NonNull BlindViewHolder holder, int position) {
            holder.setExtrapolatedBlindStyle();
            SettingsActivity.BlindLevel level = extrapolatedLevels.get(position - blindLevels.size() - 1);
            holder.setBlindData(level);
        }

        private void bindEditView(@NonNull EditViewHolder holder) {
            holder.initHandlers();
        }

        @Override
        public int getItemCount() {
            return blindLevels.size() + 1 + extrapolatedLevels.size(); // accepted + edit + extrapolated
        }

        class BlindViewHolder extends RecyclerView.ViewHolder {
            TextView bigBlindText, smallBlindText, durationText;
            ImageButton deleteButton;
            BlindViewHolder(View itemView) {
                super(itemView);
                try {
                    bigBlindText = itemView.findViewById(R.id.big_blind_text);
                    smallBlindText = itemView.findViewById(R.id.small_blind_text);
                    durationText = itemView.findViewById(R.id.duration_text);
                    deleteButton = itemView.findViewById(R.id.delete_button);
                } catch (Exception e) {
                    Log.e(TAG, "Error in BlindViewHolder constructor: " + e.getMessage(), e);
                }
            }

            public void setBlindData(@NonNull SettingsActivity.BlindLevel level) {
                bigBlindText.setText("Big: " + level.big);
                smallBlindText.setText("Small: " + level.small);

                int mins = level.duration / 60;
                durationText.setText(mins + " min");
            }

            public void setAcceptedBlindStyle() {
                itemView.setAlpha(1.0f);

                bigBlindText.setTypeface(null, android.graphics.Typeface.NORMAL);
                bigBlindText.setTextColor(getResources().getColor(R.color.colorOnSurface));

                smallBlindText.setTypeface(null, android.graphics.Typeface.NORMAL);
                smallBlindText.setTextColor(getResources().getColor(R.color.colorOnSurface));

                durationText.setTypeface(null, android.graphics.Typeface.NORMAL);
                durationText.setTextColor(getResources().getColor(R.color.colorOnSurface));

                deleteButton.setVisibility(View.VISIBLE);
            }

            public void initHandlers() {
                deleteButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition();
                        if (pos >= 0 && pos < blindLevels.size()) {
                            blindLevels.remove(pos);
                            calculateExtrapolated();

                            notifyItemRemoved(pos);
                            notifyItemRangeChanged(blindLevels.size() + 1, extrapolatedLevels.size());
                        }
                    }
                });
            }

            public void setExtrapolatedBlindStyle() {
                itemView.setAlpha(0.6f);
            
                bigBlindText.setTypeface(null, android.graphics.Typeface.ITALIC);
                bigBlindText.setTextColor(getResources().getColor(R.color.colorSecondary));

                smallBlindText.setTypeface(null, android.graphics.Typeface.ITALIC);
                smallBlindText.setTextColor(getResources().getColor(R.color.colorSecondary));

                durationText.setTypeface(null, android.graphics.Typeface.ITALIC);
                durationText.setTextColor(getResources().getColor(R.color.colorSecondary));

                deleteButton.setVisibility(View.GONE);
            }

        }

        class EditViewHolder extends RecyclerView.ViewHolder {
            EditText bigEdit, smallEdit, durationEdit;
            Button addBlindButton;

            EditViewHolder(View itemView) {
                super(itemView);

                bigEdit = itemView.findViewById(R.id.edit_big_blind);
                smallEdit = itemView.findViewById(R.id.edit_small_blind);
                durationEdit = itemView.findViewById(R.id.edit_duration);
                addBlindButton = itemView.findViewById(R.id.add_blind_button);

                setEditFieldDefaults(editingBlindLevel);
            }

            public void initHandlers()
            {
                TextWatcher watcher = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable e) {
                        editingBlindLevel = getEditFieldValues();
                        calculateExtrapolated();

                        addBlindButton.setEnabled(validateEditingBlind());

                        editingBlindLevel.big = editingBlindLevel.small *2;
                        setEditFieldDefaults(editingBlindLevel);
                        notifyItemRangeChanged(blindLevels.size() +1, extrapolatedLevels.size());
                    }
                };

                smallEdit.addTextChangedListener(watcher);
                bigEdit.addTextChangedListener(watcher);
                durationEdit.addTextChangedListener(watcher);
                
                addBlindButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        blindLevels.add(editingBlindLevel);
                        editingBlindLevel = extrapolatedLevels.get(0);
                        calculateExtrapolated();

                        setEditFieldDefaults(editingBlindLevel);
                        notifyDataSetChanged();
                    }
                });
            }

            private void updateIfChanged(@NotNull EditText editText, int value)
            {
                String current = editText.getText().toString();
                String replace = String.valueOf(value);
                if (!current.equals(replace))
                    editText.setText(replace);
            }

            public void setEditFieldDefaults(SettingsActivity.BlindLevel level) {
                updateIfChanged(bigEdit, level.big);
                updateIfChanged(smallEdit, level.small);
                updateIfChanged(durationEdit, level.duration / 60);
            }

            private int safeReadNumberFromEditText(EditText textField, int defaultValue)
            {
                try {
                    String stringValue = textField.getText().toString();
                    return TextUtils.isEmpty(stringValue) ? defaultValue : Integer.parseInt(stringValue);

                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid number format in edit fields: " + e.getMessage());
                    return defaultValue;
                } catch (Exception e) {
                    Log.e(TAG, "Error getting edit field values: " + e.getMessage(), e);
                    return defaultValue;
                }
            }

            public SettingsActivity.BlindLevel getEditFieldValues() {
                return new SettingsActivity.BlindLevel(
                    safeReadNumberFromEditText(bigEdit, 0),
                    safeReadNumberFromEditText(smallEdit, 0),
                    safeReadNumberFromEditText(durationEdit, 0) * 60
                );
            }

        }
    }
} 
