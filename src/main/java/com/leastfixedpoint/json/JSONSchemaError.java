package com.leastfixedpoint.json;

/**
 * Signalled to indicate a problem with the structure of a value not being as expected.
 */
public abstract class JSONSchemaError extends JSONError {
    public JSONSchemaError(String message) {
        super(message);
    }
}
