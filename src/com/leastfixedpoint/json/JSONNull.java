package com.leastfixedpoint.json;

/**
 * Quasi-singleton; represents JSON null values. Java's own null
 * causes confusion when it appears against keys in object, for
 * instance.
 */
public enum JSONNull {
    /**
     * The single instance representing JSON nulls.
     */
    INSTANCE
}
