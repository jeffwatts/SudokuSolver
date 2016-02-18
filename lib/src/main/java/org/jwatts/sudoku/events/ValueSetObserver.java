package org.jwatts.sudoku.events;

import org.jwatts.sudoku.Square;

public interface ValueSetObserver {
    void notifyValueSet(Square square);
}
