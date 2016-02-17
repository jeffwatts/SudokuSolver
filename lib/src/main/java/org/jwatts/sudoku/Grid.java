package org.jwatts.sudoku;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Grid {
    private static final int DEFAULT_BLOCK_SIZE = 3;

    // Each block is blockSize * blockSize, whole grid is blockSize^2 * blockSize^2
    private final int blockSize;
    private final int rowColLength;

    // Row-oriented view of all squares (default)
    private final Square[][] squares;

    // Column-oriented view of all squares
    private final Square[][] columns;

    // Block-oriented view of all squares
    private final Square[][] blocks;

    // All values (e.g., 1-9) that a square in this grid can take
    private final Set<Integer> allPossibleValues;

    public Grid(int blockSize) {
        this.blockSize = blockSize;
        rowColLength = blockSize * blockSize;
        squares = new Square[rowColLength][rowColLength];
        columns = new Square[rowColLength][rowColLength];
        blocks = new Square[rowColLength][rowColLength];
        allPossibleValues = Collections.unmodifiableSet(initAllPossibleValues());
        initialize();
    }

    /**
     * Defaults to a block size of 3
     */
    public Grid() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public static Grid fromIntArrays(int[][] rows) {
        if (rows.length != DEFAULT_BLOCK_SIZE*DEFAULT_BLOCK_SIZE) {
            throw new IllegalArgumentException("fromIntArrays only valid for 9x9 grids");
        }

        Grid grid = new Grid();
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            int[] row = rows[rowIndex];
            if (row.length != grid.getRowColLength()) {
                throw new IllegalArgumentException("fromIntArrays only valid for 9x9 grids");
            }

            for (int colIndex = 0; colIndex < row.length; colIndex++) {
                int value = row[colIndex];
                // Silently ignore invalid values
                if (value > 0 && value < 10) {
                    grid.squares[rowIndex][colIndex].setValue(value);
                }
            }
        }
        return grid;
    }

    /**
     * Possible values are 1 up to rowColLength
     */
    private Set<Integer> initAllPossibleValues() {
        Set<Integer> possibleValues = new HashSet<>();
        for (int i = 1; i <= getRowColLength(); i++) {
            possibleValues.add(i);
        }
        return possibleValues;
    }

    private void initialize() {
        for (int row = 0; row < rowColLength; row++) {
            for (int col = 0; col < rowColLength; col++) {
                squares[row][col] = new Square(row, col, this);
            }
        }

        // This is pretty hideous but we want to call initialize on the squares only after the whole grid is initialized
        for (int row = 0; row < rowColLength; row++) {
            for (int col = 0; col < rowColLength; col++) {
                squares[row][col].lazyInitialize();
            }
        }
    }

    public int getRowColLength() {
        return rowColLength;
    }

    public Square[][] getSquares() {
        return squares;
    }

    private int getFilledInSquareCount() {
        int filledInSquareCount = 0;
        for (int row = 0; row < rowColLength; row++) {
            for (int col = 0; col < rowColLength; col++) {
                Square currentSquare = squares[row][col];
                if (currentSquare.hasValue()) {
                    filledInSquareCount++;
                }
            }
        }
        return filledInSquareCount;
    }

    /**
     * @return true if the puzzle was fully solved; false otherwise
     */
    public boolean solve() {
        int totalGridSize = rowColLength * rowColLength;
        int filledCount = getFilledInSquareCount();

        while (filledCount < totalGridSize) {
            int prevFilledCount = filledCount;
            fillInValues();
            filledCount = getFilledInSquareCount();

            if (prevFilledCount == filledCount) {
                return false;
            }
        }

        return true;
    }

    private void fillInValues() {
        findValuesForGroup(blocks);
        findValuesForGroup(squares);
        findValuesForGroup(columns);
        fillInNakedSingles();
    }

    /**
     * Fills in values that are directly implied by the values of their associated squares.
     * This is the so-called Naked Single technique.
     */
    private void fillInNakedSingles() {
        for (int row = 0; row < rowColLength; row++) {
            for (int col = 0; col < rowColLength; col++) {
                Square currentSquare = squares[row][col];
                if (!currentSquare.hasValue()) {
                    Set<Integer> possibleValues = currentSquare.getPossibleValues();
                    if (possibleValues.size() == 1) {
                        currentSquare.setValue(possibleValues.iterator().next());
                    }
                }
            }
        }
    }

    private void findValuesForGroup(Square[][] squareGroup) {
        for (Square[] s : squareGroup) {
            findValuesForSquareCollection(s);
        }
    }

    /**
     * This method finds so-called Hidden Singles, where values are deduced from the needs of a row,
     * column, or block based on the possible values that all squares in that collection can take.
     */
    private void findValuesForSquareCollection(Square[] squareCollection) {
        // We want the values that are not currently set in this block
        Set<Integer> groupNeededValues = allPossibleValues();
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
            }

            candidateSquare.setValue(i);
        }
    }

    public Square[] getRow(int rowIndex) {
        return squares[rowIndex];
    }

    public Square[] getColumn(int columnIndex) {
        // Column-oriented view not yet initialized, so create it
        if (columns[0][0] == null) {
            initColumnView();
        }

        return columns[columnIndex];
    }

    private void initColumnView() {
        for (int col = 0; col < rowColLength; col++) {
            Square[] column = new Square[rowColLength];
            for (int row = 0; row < rowColLength; row++) {
                column[row] = squares[row][col];
            }
            columns[col] = column;
        }
    }

    public Square[] getBlockSquares(int row, int col) {
        if (blocks[0][0] == null) {
            initBlockView();
        }

        return blocks[computeBlockNumber(row, col)];
//        Square[] block = new Square[rowColLength];
//
//        int rowBase = (row / blockSize) * blockSize; // drop the remainder
//        int rowMax = rowBase + blockSize;
//        int colBase = (col / blockSize) * blockSize; // drop the remainder
//        int colMax = colBase + blockSize;
//        int blockIndex = 0;
//        for (int rowIndex = rowBase; rowIndex < rowMax; rowIndex++) {
//            for (int colIndex = colBase; colIndex < colMax; colIndex++) {
//                block[blockIndex] = squares[rowIndex][colIndex];
//                blockIndex++;
//            }
//        }
//
//        return block;
    }

    private void initBlockView() {
        outer:
        for (int blockNum = 0; blockNum < rowColLength; blockNum++) {
            int currentBlockIndex = 0;
            for (int row = 0; row < rowColLength; row++) {
                for (int col = 0; col < rowColLength; col++) {
                    // No doubt there is a more efficient way to do this
                    if (computeBlockNumber(row, col) == blockNum) {
                        blocks[blockNum][currentBlockIndex] = squares[row][col];
                        currentBlockIndex++;
                        if (currentBlockIndex == rowColLength) {
                            continue outer;
                        }
                    }
                }
            }
        }
    }

    int computeBlockNumber(int row, int col) {
        // e.g., for block size = 3
        // rows 0-2, col 0-2 get mapped to block 0
        // rows 0-2, col 3-5 get mapped to block 1
        // rows 0-2, col 6-8 get mapped to block 2
        // rows 3-5, col 0-2 get mapped to block 3
        // etc
        // put differently,
        // rows 0-2 get mapped to blocks 0-2
        // rows 3-5 get mapped to blocks 3-5
        // rows 6-8 get mapped to blocks 6-8
        return ((row/blockSize) * blockSize) + (col/blockSize);
    }

    public Set<Integer> allPossibleValues() {
        return new HashSet<>(allPossibleValues);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("___________________\n");
        for (Square[] row : squares) {
            sb.append('|');
            for (Square s : row) {
                if (s.hasValue()) {
                    sb.append(s.getValue());
                } else {
                    sb.append(' ');
                }
                sb.append('|');
            }
            sb.append("\n");
        }
        sb.append("-------------------\n");

        return sb.toString();
    }
}
