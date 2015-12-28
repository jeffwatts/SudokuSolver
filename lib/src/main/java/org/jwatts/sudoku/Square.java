package org.jwatts.sudoku;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Square {
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
        System.out.println(String.format("row %d, col %d; setting value %d", rowIndex, colIndex, value));
        this.value = value;

        // null out lastComputedPossibleValues as cleanup
        lastComputedPossibleValues = null;
        // TODO also null out row, col, block, and allAssociatedSquares?

        for (Square s : allAssociatedSquares) {
            if (!s.hasValue()) {
                s.setDirty();
            }
        }
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
        System.out.println(String.format("Possible values for row %d, col %d is %s", rowIndex, colIndex, possibleValues));

        findValuesForSquareCollection(block);
        findValuesForSquareCollection(row);
        findValuesForSquareCollection(col);
    }

    // TODO we should be doing this for the whole block, not for a single square. That's what we're doing here,
    // but the method call makes it look like this is in the search for a single square.
    // This method finds so-called Hidden Singles
    private void findValuesForSquareCollection(Square[] squareCollection) {
        // We want the values that are not currently set in this block
        Set<Integer> blockNeededValues = grid.allPossibleValues();
        NeededValuePredicate neededValuePredicate = new NeededValuePredicate(squareCollection);
        CollectionUtils.filter(blockNeededValues, neededValuePredicate);
        outer:
        for (Integer i : blockNeededValues) {
            Square candidateSquare = null;
            for (Square s : squareCollection) {
                if (!s.hasValue() && s.getPossibleValues().contains(i)) {
                    if (candidateSquare != null) {
                        continue outer; // cannot have more than one candidate square per value, so go to the next int
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
        System.out.println("Getting possible values for square at row " + rowIndex + ", col " + colIndex);

        for (Square s : row) {
            possibleValues.remove(s.getValue());
        }
        System.out.println("Removed row " + rowIndex + ". Possible values are " + possibleValues);
        if (possibleValues.size() == 1) {
            isDirty = false;
            return possibleValues;
        }

        for (Square s : col) {
            possibleValues.remove(s.getValue());
        }
        System.out.println("Removed col " + colIndex + ". Possible values are " + possibleValues);
        if (possibleValues.size() == 1) {
            isDirty = false;
            return possibleValues;
        }

        for (Square s : block) {
            possibleValues.remove(s.getValue());
        }
        System.out.println("Removed block for row " + rowIndex + ", col " + colIndex + ". Possible values are " + possibleValues);

        // TODO replace the above with allAssociatedSquares after we've debugged everything

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

//    private static int computeBlockNum(Square square) {
//        // e.g., for block size = 3
//        // rows 0-2, col 0-2 get mapped to block 0
//        // rows 0-2, col 3-5 get mapped to block 1
//        // rows 0-2, col 6-8 get mapped to block 2
//        // rows 3-5, col 0-2 get mapped to block 3
//        // etc
//        // put differently,
//        // rows 0-2 get mapped to blocks 0-2
//        // rows 3-5 get mapped to blocks 3-5
//        // rows 6-8 get mapped to blocks 6-8
//
//        // blocks per block row = blockSize
//        int blockNum = square.blockRow * square.getGridBlockSize();
//        blockNum = blockNum + square.blockCol;
//        return blockNum;
//    }
}
