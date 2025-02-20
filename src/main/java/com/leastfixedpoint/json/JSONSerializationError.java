package com.leastfixedpoint.json;

/**
 * Describes some portion of a Java object graph that cannot be serialized to JSON.
 */
public class JSONSerializationError extends JSONError {
    public JSONSerializationError(String message) {
        super(message);
    }
}
