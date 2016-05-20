package org.jwatts.sudoku;

import org.apache.commons.collections4.CollectionUtils;
import org.jwatts.sudoku.events.ValueSetObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Grid {
    private static final Logger sLogger = LoggerFactory.getLogger(Grid.class);
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

    private final Set<ValueSetObserver> valueSetObservers;

    Grid(int blockSize) {
        this.blockSize = blockSize;
        rowColLength = blockSize * blockSize;
        squares = new Square[rowColLength][rowColLength];
        columns = new Square[rowColLength][rowColLength];
        blocks = new Square[rowColLength][rowColLength];
        allPossibleValues = Collections.unmodifiableSet(initAllPossibleValues());
        valueSetObservers = new HashSet<>();
        initialize();
    }

    /**
     * Defaults to a block size of 3, uses a {@link ValueSetObserver} that just logs the value and square coordinates at
     * debug level
     */
    public Grid() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public static Grid fromIntArrays(int[][] rows) {
        if (rows.length != DEFAULT_BLOCK_SIZE * DEFAULT_BLOCK_SIZE) {
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
                grid.setSquareValueAt(rowIndex, colIndex, value);
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

    int getFilledInSquareCount() {
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

            if (filledCount == prevFilledCount) {
                return false;
            }
        }

        return true;
    }

    private void fillInValues() {
        removePointingPairsFromPossibleValues();
        findValuesForGroup(blocks);
        findValuesForGroup(squares);
        findValuesForGroup(columns);
        fillInNakedSingles();
    }

    /**
     * Fills in values that are directly implied by the values of their associated squares. This is the so-called Naked
     * Single technique.
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
     * This method finds so-called Hidden Singles, where values are deduced from the needs of a row, column, or block
     * based on the possible values that all squares in that collection can take.
     */
    private void findValuesForSquareCollection(Square[] squareCollection) {
        // We want the values that are not currently set in this block
        Set<Integer> groupNeededValues = getNeededValuesForSquareCollection(squareCollection);
        outer: for (Integer i : groupNeededValues) {
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

    private Set<Integer> getNeededValuesForSquareCollection(Square[] squareCollection) {
        Set<Integer> groupNeededValues = allPossibleValues();
        NeededValuePredicate neededValuePredicate = new NeededValuePredicate(squareCollection);
        CollectionUtils.filter(groupNeededValues, neededValuePredicate);
        return groupNeededValues;
    }

    /**
     * Uses block-column/row interactions to remove possible values from some squares, based on the so-called Pointing
     * Pair technique.
     *
     * @see <a href="http://www.sadmansoftware.com/sudoku/blockcolumnrow.php">http://www.sadmansoftware.com/sudoku/
     *      blockcolumnrow.php</a>
     */
    private void removePointingPairsFromPossibleValues() {
        for (int blockIndex = 0; blockIndex < blocks.length; blockIndex++) {
            Square[] block = blocks[blockIndex];
            Set<Integer> blockNeededValues = getNeededValuesForSquareCollection(block);
            valueLoop: for (Integer value : blockNeededValues) {
                int inRow = -1;
                int inCol = -1;
                MatchType matchType = MatchType.UNKNOWN;
                for (Square s : block) {
                    if (s.hasValue()) {
                        continue;
                    }

                    Set<Integer> possibleValues = s.getPossibleValues();
                    if (possibleValues.contains(value)) {
                        // If we already have a position for this value, either the row or the column must be the same.
                        // We can only have *one* of the two be the same; if two squares have the same row, and one of
                        // those two shares a col with a third square with that value, then we need to move on the
                        // next value.
                        if (inRow < 0 && inCol < 0) {
                            // This condition means that this is the first square these have matched on.
                            // Don't know if we're matching on row or col until our next match
                            inRow = s.getRowIndex();
                            inCol = s.getColIndex();
                            continue;
                        }

                        if (matchType == MatchType.UNKNOWN) {
                            if (inRow == s.getRowIndex()) {
                                matchType = MatchType.ROW;
                            } else if (inCol == s.getColIndex()) {
                                matchType = MatchType.COL;
                            } else {
                                // We have a second match that does not share a row or col with the previous one.
                                continue valueLoop;
                            }
                            continue;
                        }

                        if ((matchType == MatchType.ROW && inRow != s.getRowIndex())
                                        || (matchType == MatchType.COL && inCol != s.getColIndex())) {
                            continue valueLoop;
                        }
                    }
                }
                // After iterating over the squares, we should have a ROW or COL MatchType, or we should have
                // continued to the next needed value.
                Square[] squareCollection = null;
                if (matchType == MatchType.ROW) {
                    // Remove value from the possible values of other squares in this row
                    squareCollection = squares[inRow];
                } else if (matchType == MatchType.COL) {
                    // Remove value from the possible values of other squares in this col
                    squareCollection = columns[inCol];
                } else {
                    // matchType should always be ROW or COL here, but add this continue to protect against NPE below
                    continue;
                }

                for (Square s : squareCollection) {
                    if (s.hasValue() || blockIndex == computeBlockNumber(s.getRowIndex(), s.getColIndex())) {
                        continue;
                    }

                    s.removeFromPossibleValues(value);
                }
            }
        }
    }

    private enum MatchType {
        ROW,
        COL,
        UNKNOWN
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
    }

    private void initBlockView() {
        outer: for (int blockNum = 0; blockNum < rowColLength; blockNum++) {
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
        return ((row / blockSize) * blockSize) + (col / blockSize);
    }

    public Set<Integer> allPossibleValues() {
        return new HashSet<>(allPossibleValues);
    }

    void notifyObservers(Square square) {
        for (ValueSetObserver o : valueSetObservers) {
            o.notifyValueSet(square);
        }
    }

    public void addValueSetObserver(ValueSetObserver valueSetObserver) {
        valueSetObservers.add(valueSetObserver);
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

    public String toSerializedString() {
        StringBuilder sb = new StringBuilder();
        for (Square[] row : squares) {
            for (Square s : row) {
                sb.append(s.getValue());
            }
        }
        return sb.toString();
    }

    public static Grid fromSerializedString(String serializedGrid) {
        int rowLength = DEFAULT_BLOCK_SIZE * DEFAULT_BLOCK_SIZE;
        if (serializedGrid.length() != rowLength * rowLength) {
            throw new IllegalArgumentException("fromSerializedString only valid for 9x9 grids");
        }

        int[][] gridValues = new int[rowLength][rowLength];
        for (int rowIndex = 0; rowIndex < gridValues.length; rowIndex++) {
            int[] row = gridValues[rowIndex];
            for (int colIndex = 0; colIndex < row.length; colIndex++) {
                int strIndex = (rowIndex * rowLength) + colIndex;
                int squareValue = Character.getNumericValue(serializedGrid.charAt(strIndex));
                row[colIndex] = squareValue;
            }
        }

        return fromIntArrays(gridValues);
    }

    public void setSquareValueAt(int rowIndex, int colIndex, int value) {
        // Silently ignore invalid values
        if (value > 0 && value < 10) {
            squares[rowIndex][colIndex].setValue(value);
        }
    }

    public int getSquareValueAt(int rowIndex, int colIndex) {
        return squares[rowIndex][colIndex].getValue();
    }
}
