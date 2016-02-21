package org.jwatts.sudokusolver;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import org.jwatts.sudoku.Grid;
import org.jwatts.sudoku.Square;
import org.jwatts.sudoku.events.ValueSetObserver;

import java.util.WeakHashMap;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class SolverSetupActivity extends Activity {
    private static final String TAG = "SUDOKU";
    private static final String SQUARE_TAG_BASE = "square_";

    private Button solveButton;
    private GridLayout gridLayout;
    private WeakHashMap<String, TextWatcher> textWatcherMap = new WeakHashMap<>();
    private final Grid sudokuGrid = new Grid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solver_setup);
        solveButton = (Button) findViewById(R.id.solve_button);
        solveButton.setEnabled(false);

        gridLayout = (GridLayout) findViewById(R.id.sudoku_grid);
        // track this separately just in case the child view count differs from the edit text count
        int editTextIndex = 0;
        int childViewCount = gridLayout.getChildCount();
        for (int i = 0; i < childViewCount; i++) {
            View v = gridLayout.getChildAt(i);
            if (v instanceof EditText) {
                EditText editText = (EditText) v;
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                final int squareIndex = editTextIndex;
                String tag = getTagForSquareIndex(squareIndex);
                editText.setTag(tag);
                TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        int rowIndex = getRowIndexFromSquareIndex(squareIndex);
                        int colIndex = getColIndexFromSquareIndex(squareIndex);
                        Square[][] squares = sudokuGrid.getSquares();
                        Square square = squares[rowIndex][colIndex];
                        int squareValue = 0;
                        if (!TextUtils.isEmpty(s)) {
                            squareValue = Integer.parseInt(s.toString());
                        }
                        square.setValue(squareValue);
                        solveButton.setEnabled(true);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                };
                editText.addTextChangedListener(textWatcher);
                textWatcherMap.put(tag, textWatcher);
                editTextIndex++;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_solver_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO make this useful
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void solvePuzzle(View solveButtonView) {
        Scheduler subscriptionScheduler = Schedulers.newThread();

        // Using a lambda inside create() confuses Android Studiohere  -- it loses the generic type
        // of Square -- so I'm using the anonymous inner class.
        Observable.create(new Observable.OnSubscribe<Square>() {
            @Override
            public void call(Subscriber<? super Square> subscriber) {
                ValueSetObserver valueSetObserver = subscriber::onNext;
                sudokuGrid.addValueSetObserver(valueSetObserver);
            }
        }).subscribeOn(subscriptionScheduler)
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::onSquareValueSet,
                        (e) -> {
                            Toast.makeText(this, "Oops! Encountered an error",
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error in observing square value changes", e);
                        });

        Observable<Boolean> solvingObservable = SudokuSolverObservableFactory.createSolverObservable(sudokuGrid);
        solvingObservable.subscribeOn(subscriptionScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(solved -> {
                    if (!solved) {
                        Toast.makeText(this, "Couldn't solve the puzzle. Maybe gimme a hint?",
                                Toast.LENGTH_LONG).show();
                    }
                });

        solveButton.setEnabled(false);
    }

    private static int getRowIndexFromSquareIndex(int squareIndex) {
        // squares 0-8 => row 0
        //        9-17 => row 1
        //       18-26 => row 2
        // ... etc
        int rowIndex = squareIndex / 9;
        Log.d(TAG, String.format(
                "Mapping squareIndex %d to row %d", squareIndex, rowIndex));
        return rowIndex;
    }

    private static int getColIndexFromSquareIndex(int squareIndex) {
        // squares 0, 9, 18, 27, 36, 45, 54, 63, 72 map to col 0
        // squares 1, 10, 19, 28, 37, 46, 55, 64, 73 map to col 1
        // ... etc
        int colIndex = squareIndex % 9;
        Log.d(TAG, String.format(
                "Mapping squareIndex %d to col %d", squareIndex, colIndex));
        return colIndex;
    }

    private static int getGridSquareIndexFromRolCol(int rowIndex, int colIndex) {
        int fromRow = rowIndex * 9;
        int gridSquareIndex = fromRow + colIndex;
        Log.d(TAG, String.format(
                "Mapping row %d col %d to squareIndex %d", rowIndex, colIndex, gridSquareIndex));
        return gridSquareIndex;
    }

    private static String getTagForSquareIndex(int squareIndex) {
        return SQUARE_TAG_BASE + squareIndex;
    }

    public void onSquareValueSet(Square square) {
        int gridSquareIndex = getGridSquareIndexFromRolCol(square.getRowIndex(), square.getColIndex());
        String tag = getTagForSquareIndex(gridSquareIndex);
        EditText squareEditText = (EditText) gridLayout.findViewWithTag(tag);
        TextWatcher watcher = textWatcherMap.get(tag);
        if (watcher != null) {
            squareEditText.removeTextChangedListener(watcher);
        }
        squareEditText.setText(String.format("%d", square.getValue()));
    }
}
