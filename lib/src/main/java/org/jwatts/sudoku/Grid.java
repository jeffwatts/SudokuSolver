package org.jwatts.sudoku;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Grid {
    private static final int DEFAULT_BLOCK_SIZE = 3;

    // Each block is blockSize * blockSize, whole grid is blockSize^2 * blockSize^2
    private final int blockSize;
    private final int rowColLength;
    private final Square[][] squares;

    // All values (e.g., 1-9) that a square in this grid can take
    private final Set<Integer> allPossibleValues;

    public Grid(int blockSize) {
        this.blockSize = blockSize;
        rowColLength = blockSize * blockSize;
        squares = new Square[rowColLength][rowColLength];
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
                if (value > 0) { // TODO fully validate value, throw IllegalArgumentException if invalid
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

    /**
     * Does one iteration over the grid, filling in any values that are implied by the game constraints
     * @return the number of squares currently filled in on the grid
     */
    private int fillInValues() {
        int filledInSquareCount = 0;
        for (int row = 0; row < rowColLength; row++) {
            for (int col = 0; col < rowColLength; col++) {
                Square currentSquare = squares[row][col];
                currentSquare.attemptFindValue();
                if (currentSquare.hasValue()) {
                    filledInSquareCount++;
                }
            }
        }

        return filledInSquareCount;
    }

    public void solve() throws NotSolvableByConstraintsException {
        int totalGridSize = rowColLength * rowColLength;
        int filledCount = fillInValues();

        while (filledCount < totalGridSize) {
            int prevFilledCount = filledCount;
            filledCount = fillInValues();

            if (prevFilledCount == filledCount) {
                throw new NotSolvableByConstraintsException("Final grid was \n" + toString());
            }
        }
    }

    public Square[] getRow(int rowIndex) {
        return squares[rowIndex];
    }

    public Square[] getColumn(int columnIndex) {
        // More work than getRow() because the arrays are stored by row, so this must be computed
        // TODO Should optimize by pre-computing these column-oriented arrays once and caching!
        Square[] column = new Square[rowColLength];
        for (int row = 0; row < rowColLength; row++) {
            column[row] = squares[row][columnIndex];
        }
        return column;
    }

    public Square[] getBlockSquares(int row, int col) {
        // TODO Should optimize by computing each of the blockSize * blockSize blocks only once and caching!
        Square[] block = new Square[rowColLength];

        int rowBase = (row / blockSize) * blockSize; // drop the remainder
        int rowMax = rowBase + blockSize;
        int colBase = (col / blockSize) * blockSize; // drop the remainder
        int colMax = colBase + blockSize;
        int blockIndex = 0;
        for (int rowIndex = rowBase; rowIndex < rowMax; rowIndex++) {
            for (int colIndex = colBase; colIndex < colMax; colIndex++) {
                block[blockIndex] = squares[rowIndex][colIndex];
                blockIndex++;
            }
        }

        return block;
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
