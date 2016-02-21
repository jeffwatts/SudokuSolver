package org.jwatts.sudoku;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GridTest {
    @Test
    public void testSolve_easyPuzzleFromUnitedInFlightMagazine() {
        Grid underTest = new Grid();
        Square[][] squares = underTest.getSquares();
        /*
          Starting grid:
          _____________________
          | | | || | | ||1| | |
          | | | ||2|4| ||9|5| |
          | |6|3|| | |7|| | | |
          ---------------------
          | | | || | | ||5| |7|
          | |8| || |5| || |4| |
          |6|2| || | |4|| | | |
          ---------------------
          | |4| ||7|1| || |6| |
          | | | || | | || | | |
          |9| |8|| | | || | |2|
          ---------------------
         */
        // row 0:
        squares[0][6].setValue(1);

        // row 1
        squares[1][3].setValue(2);
        squares[1][4].setValue(4);
        squares[1][6].setValue(9);
        squares[1][7].setValue(5);

        // row 2
        squares[2][1].setValue(6);
        squares[2][2].setValue(3);
        squares[2][5].setValue(7);

        // row 3
        squares[3][6].setValue(5);
        squares[3][8].setValue(7);

        // row 4
        squares[4][1].setValue(8);
        squares[4][4].setValue(5);
        squares[4][7].setValue(4);

        // row 5
        squares[5][0].setValue(6);
        squares[5][1].setValue(2);
        squares[5][5].setValue(4);

        // row 6
        squares[6][1].setValue(4);
        squares[6][3].setValue(7);
        squares[6][4].setValue(1);
        squares[6][7].setValue(6);

        // row 7 is empty

        // row 8
        squares[8][0].setValue(9);
        squares[8][2].setValue(8);
        squares[8][8].setValue(2);

        // for now just test that this method does not throw
        boolean solved = underTest.solve();
        assertTrue("Expected puzzle to be solved, instead filled in as \n" + underTest,
                solved);

        printPuzzle(underTest);
    }

    @Test
    public void testSolve_mediumPuzzleFromFebruaryUnitedInFlightMagazine() {
        Grid underTest = new Grid();
        Square[][] squares = underTest.getSquares();
        /*
          Starting grid:
          _____________________
          | |1| || | | ||4| | |
          | |8| || | |9|| | | |
          |4| |5||1|6|8|| | | |
          ---------------------
          |5| | || | | || | | |
          |2|9| || | | || | |6|
          | | |7|| | |2|| | |9|
          ---------------------
          | | |8|| |1| || | |5|
          |7| | || |5| || |2| |
          | | |6|| |9| || | |7|
          ---------------------
         */
        // row 0:
        squares[0][1].setValue(1);
        squares[0][6].setValue(4);

        // row 1
        squares[1][1].setValue(8);
        squares[1][5].setValue(9);

        // row 2
        squares[2][0].setValue(4);
        squares[2][2].setValue(5);
        squares[2][3].setValue(1);
        squares[2][4].setValue(6);
        squares[2][5].setValue(8);

        // row 3
        squares[3][0].setValue(5);

        // row 4
        squares[4][0].setValue(2);
        squares[4][1].setValue(9);
        squares[4][8].setValue(6);

        // row 5
        squares[5][2].setValue(7);
        squares[5][5].setValue(2);
        squares[5][8].setValue(9);

        // row 6
        squares[6][2].setValue(8);
        squares[6][4].setValue(1);
        squares[6][8].setValue(5);

        // row 7
        squares[7][0].setValue(7);
        squares[7][4].setValue(5);
        squares[7][7].setValue(2);

        // row 8
        squares[8][2].setValue(6);
        squares[8][4].setValue(9);
        squares[8][8].setValue(7);

        // for now just test that this method does not throw
        boolean solved = underTest.solve();
        assertTrue("Expected puzzle to be solved, instead filled in as \n" + underTest,
                solved);

        printPuzzle(underTest);
    }

    @Test
    public void testSolve_mediumPuzzleTwoFromFebruaryUnitedInFlightMagazine() {
        int[][] initialPuzzleRows = new int[][] {
                new int[] { 0, 7, 6, 4, 9, 0, 0, 0, 0 },
                new int[] { 0, 0, 0, 0, 0, 2, 8, 0, 0 },
                new int[] { 0, 2, 0, 3, 0, 0, 0, 0, 0 },
                new int[] { 0, 6, 1, 0, 0, 0, 0, 0, 0 },
                new int[] { 5, 0, 0, 7, 0, 8, 3, 1, 0 },
                new int[] { 0, 0, 0, 6, 0, 0, 0, 2, 0 },
                new int[] { 0, 0, 3, 0, 0, 0, 0, 0, 0 },
                new int[] { 0, 0, 0, 0, 0, 0, 5, 0, 2 },
                new int[] { 9, 4, 2, 0, 7, 0, 0, 0, 0 },
        };

        Grid underTest = Grid.fromIntArrays(initialPuzzleRows);
        boolean solved = underTest.solve();
        assertTrue("Expected puzzle to be solved, instead filled in as \n" + underTest,
                solved);

        printPuzzle(underTest);
    }

    @Test
    public void testSolve_randomEasyInternetPuzzle() {
        int[][] initialPuzzleRows = new int[][] {
                new int[] { 0, 0, 6, 0, 0, 7, 3, 0, 0 },
                new int[] { 0, 1, 8, 0, 0, 9, 0, 5, 0 },
                new int[] { 5, 0, 0, 0, 0, 0, 0, 6, 4 },
                new int[] { 9, 2, 0, 0, 8, 0, 0, 0, 0 },
                new int[] { 0, 0, 0, 7, 6, 3, 0, 0, 0 },
                new int[] { 0, 0, 0, 0, 9, 0, 0, 7, 5 },
                new int[] { 6, 3, 0, 0, 0, 0, 0, 0, 8 },
                new int[] { 0, 9, 0, 3, 0, 0, 5, 2, 0 },
                new int[] { 0, 0, 2, 4, 0, 0, 6, 0, 0 },
        };
        Grid underTest = Grid.fromIntArrays(initialPuzzleRows);
        assertTrue(underTest.solve());
        printPuzzle(underTest);
    }

    @Test
    public void testToSerializedString() {
        int[][] puzzleRows = new int[][] {
                new int[] { 0, 0, 6, 0, 0, 7, 3, 0, 0 },
                new int[] { 0, 1, 8, 0, 0, 9, 0, 5, 0 },
                new int[] { 5, 0, 0, 0, 0, 0, 0, 6, 4 },
                new int[] { 9, 2, 0, 0, 8, 0, 0, 0, 0 },
                new int[] { 0, 0, 0, 7, 6, 3, 0, 0, 0 },
                new int[] { 0, 0, 0, 0, 9, 0, 0, 7, 5 },
                new int[] { 6, 3, 0, 0, 0, 0, 0, 0, 8 },
                new int[] { 0, 9, 0, 3, 0, 0, 5, 2, 0 },
                new int[] { 0, 0, 2, 4, 0, 0, 6, 0, 0 },
        };
        Grid underTest = Grid.fromIntArrays(puzzleRows);
        String serializedString = underTest.toSerializedString();
        assertEquals(
                "006007300018009050500000064920080000000763000000090075630000008090300520002400600",
                serializedString);
    }

    @Test
    public void testFromSerializedString() {
        Grid gridFromString = Grid.fromSerializedString(
                "006007300018009050500000064920080000000763000000090075630000008090300520002400600");
        Square[][] squares = gridFromString.getSquares();
        int[][] expectedPuzzleRows = new int[][] {
                new int[] { 0, 0, 6, 0, 0, 7, 3, 0, 0 },
                new int[] { 0, 1, 8, 0, 0, 9, 0, 5, 0 },
                new int[] { 5, 0, 0, 0, 0, 0, 0, 6, 4 },
                new int[] { 9, 2, 0, 0, 8, 0, 0, 0, 0 },
                new int[] { 0, 0, 0, 7, 6, 3, 0, 0, 0 },
                new int[] { 0, 0, 0, 0, 9, 0, 0, 7, 5 },
                new int[] { 6, 3, 0, 0, 0, 0, 0, 0, 8 },
                new int[] { 0, 9, 0, 3, 0, 0, 5, 2, 0 },
                new int[] { 0, 0, 2, 4, 0, 0, 6, 0, 0 },
        };
        for (Square[] row : squares) {
            for (Square s : row) {
                int expectedValue = expectedPuzzleRows[s.getRowIndex()][s.getColIndex()];
                assertEquals(String.format(
                                "Values differ at row %d, col %d; expected %d, got %d",
                                s.getRowIndex(), s.getColIndex(), expectedValue, s.getValue()),
                        expectedValue,
                        s.getValue());
            }
        }
    }

    private void printPuzzle(Grid grid) {
        System.out.println("Final solved puzzle: \n" + grid.toString());
    }

    @Test
    public void testComputeBlockNumber() {
        Grid underTest = new Grid();

        assertEquals(0, underTest.computeBlockNumber(0, 0));
        assertEquals(0, underTest.computeBlockNumber(0, 2));
        assertEquals(0, underTest.computeBlockNumber(1, 1));
        assertEquals(0, underTest.computeBlockNumber(2, 0));
        assertEquals(0, underTest.computeBlockNumber(2, 2));

        assertEquals(4, underTest.computeBlockNumber(3, 3));
        assertEquals(4, underTest.computeBlockNumber(3, 5));
        assertEquals(4, underTest.computeBlockNumber(4, 4));
        assertEquals(4, underTest.computeBlockNumber(5, 3));
        assertEquals(4, underTest.computeBlockNumber(5, 5));

        assertEquals(8, underTest.computeBlockNumber(6, 6));
        assertEquals(8, underTest.computeBlockNumber(6, 8));
        assertEquals(8, underTest.computeBlockNumber(7, 6));
        assertEquals(8, underTest.computeBlockNumber(8, 6));
        assertEquals(8, underTest.computeBlockNumber(8, 8));
    }
}