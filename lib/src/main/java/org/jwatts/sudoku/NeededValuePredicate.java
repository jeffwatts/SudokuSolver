package org.jwatts.sudoku;

import org.apache.commons.collections4.Predicate;

/**
 * Filters out integers that the row, column, or block already has set, altering the passed in set
 * of integers to include only values that this collection still needs for completeness.
 */
class NeededValuePredicate implements Predicate<Integer> {
    private Square[] squares;

    /**
     * @param squares should be a row, column, or block
     */
    public NeededValuePredicate(Square[] squares) {
        this.squares = squares;
    }

    @Override
    public boolean evaluate(Integer i) {
        for (Square s : squares) {
            if (i == s.getValue()) {
                return false;
            }
        }
        return true;
    }
}
