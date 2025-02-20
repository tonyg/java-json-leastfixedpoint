package com.leastfixedpoint.json;

/**
 * Signalled during JSONReader operation to indicate problems with the input JSON text.
 */
public class JSONSyntaxError extends JSONError {
    private int lineNumber;

    /**
     * Construct a syntax error report exception.
     * @param message Description of the error condition.
     * @param lineNumber Approximate line number of the error condition in the input.
     */
    public JSONSyntaxError(String message, int lineNumber) {
        super(message + " (line " + lineNumber + ")");
        this.lineNumber = lineNumber;
    }

    /**
     * @return The line number associated with this error report.
     */
    public int getLineNumber() {
        return lineNumber;
    }
}
