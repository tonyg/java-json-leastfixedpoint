package com.leastfixedpoint.json;

import java.util.Map;

/**
 * Signalled to indicate access to a missing (but required) element of a JSON dictionary.
 */
public class JSONMissingKeyError extends JSONSchemaError {
    private final String key;
    private final Map<String,Object> container;

    public JSONMissingKeyError(String key, Map<String,Object> container) {
        super("Key " + JSONWriter.writeToString(key) + " is not present in " + container);
        this.key = key;
        this.container = container;
    }

    public String getKey() {
        return key;
    }

    public Map<String,Object> getContainer() {
        return container;
    }
}
