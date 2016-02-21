package org.jwatts.sudokusolver;

import org.jwatts.sudoku.Grid;

import rx.Observable;

public class SudokuSolverObservableFactory {
    public static Observable<Boolean> createSolverObservable(Grid grid) {
        return Observable.create(subscriber -> {
            boolean solved = grid.solve();
            subscriber.onNext(solved);
            subscriber.onCompleted();
        });
    }
}
