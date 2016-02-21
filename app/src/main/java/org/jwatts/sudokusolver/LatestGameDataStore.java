package org.jwatts.sudokusolver;

import android.content.Context;
import android.content.SharedPreferences;

import org.jwatts.sudoku.Grid;

public class LatestGameDataStore {
    private static final String LATEST_GAME_STORE_SHARED_PREFS = "latest_game_data";
    private static final String GRID_VALUES_KEY = "grid_values";

    private final SharedPreferences sharedPrefs;

    public LatestGameDataStore(Context context) {
        sharedPrefs = context.getSharedPreferences(LATEST_GAME_STORE_SHARED_PREFS, Context.MODE_PRIVATE);
    }

    public Grid getLatestSerializedGrid() {
        String serializedGridValues = sharedPrefs.getString(GRID_VALUES_KEY, null);
        if (serializedGridValues == null) {
            return null;
        }

        return Grid.fromSerializedString(serializedGridValues);
    }

    public void saveLatestGridValues(Grid grid) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(GRID_VALUES_KEY, grid.toSerializedString());
        editor.apply();
    }

    public void clear() {
        sharedPrefs.edit().clear().apply();
    }
}
