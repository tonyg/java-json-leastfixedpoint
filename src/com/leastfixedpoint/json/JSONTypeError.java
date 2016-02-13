package com.leastfixedpoint.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Signalled to indicate a violation of a type constraint. For example, the program may be expecting a JSON number,
 * only to be given a JSON array.
 */
public class JSONTypeError extends JSONError {
    protected Class[] expected;
    protected Object actual;

    public JSONTypeError(Class expected, Object actual) {
        this(new Class[]{expected}, actual);
    }

    public JSONTypeError(Class[] expected, Object actual) {
        super("Expected JSON value of type " + formatClassList(expected) + " but got: " + actual);
        this.expected = expected;
        this.actual = actual;
    }

    public Class[] getExpected() {
        return expected;
    }

    public Object getActual() {
        return actual;
    }

    protected static String formatClassList(Class[] classes) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < classes.length; i++) {
            if (i > 0) {
                if (i == classes.length - 1) {
                    b.append(" or ");
                } else {
                    b.append(", ");
                }
            }
            b.append(classes[i].toString());
        }
        return b.toString();
    }
}
