package com.leastfixedpoint.json;

import java.io.IOException;

/**
 * Signalled by JSONReader and JSONWriter on various kinds of formatting errors,
 * be they errors in JSON text or errors in some Java object graph being
 * serialized.
 */
public abstract class JSONError extends IOException {
    public JSONError(String message) {
        super(message);
    }
}
