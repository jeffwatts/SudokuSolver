package org.jwatts.sudoku;

/**
 * Thrown when a complete iteration is completed without being able to fill in the value on any square
 */
public class NotSolvableByConstraintsException extends Exception {
    public NotSolvableByConstraintsException(String message) {
        super(message);
    }
}
