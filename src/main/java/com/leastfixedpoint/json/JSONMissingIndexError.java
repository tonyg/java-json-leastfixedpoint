package com.leastfixedpoint.json;

import java.util.List;

/**
 * Signalled to indicate access to a missing (but required) position in a JSON array.
 */
public class JSONMissingIndexError extends JSONSchemaError {
    private final int index;
    private final List<Object> container;

    public JSONMissingIndexError(int index, List<Object> container) {
        super("Index " + index + " is not present in " + container);
        this.index = index;
        this.container = container;
    }

    public int getIndex() {
        return index;
    }

    public List<Object> getContainer() {
        return container;
    }
}
