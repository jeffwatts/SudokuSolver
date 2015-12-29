package org.jwatts.sudoku;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Square {
    private static final Logger sLogger = LoggerFactory.getLogger(Square.class);

    private final int rowIndex;
    private final int colIndex;
    private final Grid grid;
    private volatile int value;

    // true when the possible values from this square need recomputation because a square from its row, col, or block had a value set
    private volatile boolean isDirty = true;

    // cache of last computed values that this square could contain, if its value has not been set already
    private volatile Set<Integer> lastComputedPossibleValues;

    // n^2 squares per row
    private Square[] row;

    // n^2 squares per column
    private Square[] col;

    // n^2 squares per block
    private Square[] block;

    // Convenience collection for iterating over all squares in row, col, and block with no overlap
    // Not final so we can null out the reference when the value is set
    private final Set<Square> allAssociatedSquares = new HashSet<>();

    public Square(int rowIndex, int colIndex, Grid grid) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.grid = grid;
    }

    // Must be called after the whole grid is populated
    void lazyInitialize() {
        row = grid.getRow(rowIndex);
        col = grid.getColumn(colIndex);
        block = grid.getBlockSquares(rowIndex, colIndex);
        initAllAssociatedSquares();
    }

    private void initAllAssociatedSquares() {
        allAssociatedSquares.addAll(Arrays.asList(row));
        allAssociatedSquares.addAll(Arrays.asList(col));
        allAssociatedSquares.addAll(Arrays.asList(block));
        allAssociatedSquares.remove(this);
    }

    public boolean hasValue() {
        return value > 0;
    }

    public int getValue() {
        return value;
    }

    public void setDirty() {
        isDirty = true;
    }

    public void setValue(int value) {
        sLogger.debug("row {}, col {}; setting value {}", rowIndex, colIndex, value);
        this.value = value;

        for (Square s : allAssociatedSquares) {
            if (!s.hasValue()) {
                s.setDirty();
            }
        }

        // null out lastComputedPossibleValues as cleanup
        lastComputedPossibleValues = null;
    }

    public void attemptFindValue() {
        // value already populated, return
        if (hasValue()) {
            return;
        }

        Set<Integer> possibleValues = getPossibleValues();
        // This is the so-called naked single
        if (possibleValues.size() == 1) {
            setValue(possibleValues.iterator().next());
            return;
        }
        sLogger.debug("Possible values for row {}, col {} is {}", rowIndex, colIndex, possibleValues);

        findValuesForSquareCollection(block);
        findValuesForSquareCollection(row);
        findValuesForSquareCollection(col);
    }

    // This method finds so-called Hidden Singles
    private void findValuesForSquareCollection(Square[] squareCollection) {
        // We want the values that are not currently set in this block
        Set<Integer> groupNeededValues = grid.allPossibleValues();
        NeededValuePredicate neededValuePredicate = new NeededValuePredicate(squareCollection);
        CollectionUtils.filter(groupNeededValues, neededValuePredicate);
        outer:
        for (Integer i : groupNeededValues) {
            Square candidateSquare = null;
            for (Square s : squareCollection) {
                if (!s.hasValue() && s.getPossibleValues().contains(i)) {
                    if (candidateSquare != null) {
                        // cannot have more than one candidate square per value, so go to the next int
                        continue outer;
                    }
                    candidateSquare = s;
                }
            }

            if (candidateSquare == null) {
                // Shouldn't happen, something has gone horribly wrong
                throw new RuntimeException("No possible square found for value " + i);
            } else {
                candidateSquare.setValue(i);
            }
        }
    }

    private Set<Integer> getPossibleValues() {
        // Early exit if no associated square has changed
        if (!isDirty) {
            return lastComputedPossibleValues;
        }

        // Start with all possible values, then iterate over the row, col, and block, removing values that are found.
        // When only one value remains, then we know that that is the correct value for this square.
        // An int[] or boolean[] array might be most efficient, but starting with Set<Integer> for convenience now.
        Set<Integer> possibleValues = grid.allPossibleValues();
        sLogger.debug("Getting possible values for square at row " + rowIndex + ", col " + colIndex);

        // This replaces the above computing of possible values by row, col, and block with allAssociatedSquares
        for (Square s : allAssociatedSquares) {
            possibleValues.remove(s.getValue());
        }
        sLogger.debug("Removed values from all associated squares for row {}, col {}. Possible values are {}",
                rowIndex, colIndex, possibleValues);

        // Technically setting these two should be in a synchronized block
        isDirty = false;
        lastComputedPossibleValues = possibleValues;
        return possibleValues;
    }

    /**
     * Filters out integers that the row, column, or block already has set, altering the passed in set
     * of integers to include only values that this collection still needs for completeness.
     */
    private static class NeededValuePredicate implements Predicate<Integer> {
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
}
