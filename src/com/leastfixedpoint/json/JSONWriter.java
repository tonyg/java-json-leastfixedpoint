/*
   Copyright (c) 2006-2007 Frank Carver
   Copyright (c) 2007-2016 Pivotal Software, Inc. All Rights Reserved
   Copyright (c) 2016 Tony Garnock-Jones

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * Based on org.stringtree.json.JSONWriter, licensed under APL and
 * LGPL. We've chosen APL (see above). The original code was written
 * by Frank Carver. Tony Garnock-Jones has made many changes to it
 * since then.
 */
package com.leastfixedpoint.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Writes certain Java objects as JSON to a Writer.
 * <p>
 * <ul>
 *     <li>java.lang.String becomes a JSON string.</li>
 *     <li>java.lang.Boolean become JSON booleans.</li>
 *     <li>com.leastfixedpoint.json.JSONNull.INSTANCE becomes JSON null.</li>
 *     <li>java.lang.Number and subclasses become JSON numbers (via BigDecimal).</li>
 *     <li>Java arrays and Iterable objects become JSON arrays.</li>
 *     <li>java.util.Map objects become JSON objects/maps.</li>
 * </ul>
 * <p>
 * If a Map has a non-String key, or an unsupported object is discovered while writing,
 * JSONSerializationError will be thrown.
 */
public class JSONWriter {
    protected int indentLevel = 0;
    protected Writer writer;
    protected boolean indentMode;
    protected boolean sortKeys = true;

    /**
     * Serializes value as JSON, outputting to writer, without pretty indentation.
     */
    public static void writeTo(Writer w, Object value) throws IOException {
        writeTo(w, value, false);
    }

    /**
     * Serializes value as JSON, outputting to writer, with optional pretty indentation.
     */
    public static void writeTo(Writer w, Object value, boolean indenting) throws IOException {
        new JSONWriter(w, indenting).write(value);
    }

    /**
     * Returns JSON text corresponding to value, without pretty indentation.
     */
    public static String writeToString(Object value) throws JSONSerializationError {
        return writeToString(value, false);
    }

    /**
     * Returns JSON text corresponding to value, with optional pretty indentation.
     */
    public static String writeToString(Object value, boolean indenting) throws JSONSerializationError {
        StringWriter w = new StringWriter();
        try {
            writeTo(w, value, indenting);
        } catch (JSONSerializationError jse) {
            throw jse;
        } catch (IOException e) {
            throw new RuntimeException("IOException while writing to string buffer", e);
        }
        return w.getBuffer().toString();
    }

    /**
     * Construct a JSONWriter that will output on the given Writer, by default without pretty indentation.
     */
    public JSONWriter(Writer writer) {
        this(writer, false);
    }

    /**
     * Construct a JSONWriter that will output on the given Writer, with optional pretty indentation.
     */
    public JSONWriter(Writer writer, boolean indenting) {
        this.writer = writer;
        this.indentMode = indenting;
    }

    /**
     * Retrieve the underlying Writer.
     */
    public Writer getWriter() {
        return this.writer;
    }

    /**
     * Answers true iff pretty indentation is turned on for this instance.
     */
    public boolean getIndentMode() {
        return indentMode;
    }

    /**
     * Alters the pretty indentation mode of this instance for future writes.
     */
    public void setIndentMode(boolean value) {
        indentMode = value;
    }

    /**
     * Answers true iff the writer sorts keys when writing objects, to ensure deterministic ordering.
     */
    public boolean getSortKeys() {
        return sortKeys;
    }

    /**
     * If given true, enables object key-sorting on write; false disables sorting.
     */
    public void setSortKeys(boolean value) {
        sortKeys = value;
    }

    /**
     * If pretty indentation is turned on, outputs a newline character followed by a number of spaces
     * corresponding to the current indentation level (which is not under direct control of any public methods)
     */
    public void newline() throws IOException {
        if (indentMode) {
            emit('\n');
            for (int i = 0; i < indentLevel; i++) emit(' ');
        }
    }

    /**
     * Emit the given object as JSON to the embedded Writer.
     */
    public void write(Object object) throws IOException {
        if (object instanceof JSONNull) emit("null");
        else if (object instanceof JSONSerializable) {
            ((JSONSerializable) object).jsonSerialize(this);
        } else if (object instanceof Class) string(object);
        else if (object instanceof Boolean) bool((Boolean) object);
        else if (object instanceof Number) number((Number) object);
        else if (object instanceof String) string(object);
        else if (object instanceof Character) string(object);
        else if (object instanceof Map) map((Map<?, ?>) object);
        else if (object instanceof Iterable) iterable((Iterable<?>) object);
        else if (object != null && object.getClass().isArray()) array(object);
        else throw new JSONSerializationError("Cannot write object in JSON format: " + object);
    }

    protected void number(Number n) throws IOException {
        String s = n.toString();
        if (s.endsWith(".0")) {
            emit(s.substring(0, s.length() - 2));
        } else {
            emit(s);
        }
    }

    protected void map(Map<?, ?> map) throws IOException {
        emit('{');
        indentLevel += 2;
        newline();
        boolean needComma = false;

        if (sortKeys) {
            ArrayList<String> sortedKeys = new ArrayList<>(map.size());
            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    throw new JSONSerializationError("Cannot write non-string JSON map key: " + key);
                }
                sortedKeys.add((String) key);
            }
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                if (needComma) {
                    emit(',');
                    newline();
                }
                needComma = true;
                emit('"');
                emit(key);
                emit("\":");
                write(map.get(key));
            }
        } else {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (needComma) {
                    emit(',');
                    newline();
                }
                needComma = true;
                emit('"');
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new JSONSerializationError("Cannot write non-string JSON map key: " + key);
                }
                emit((String) key);
                emit("\":");
                write(entry.getValue());
            }
        }

        indentLevel -= 2;
        newline();
        emit('}');
    }

    protected void iterable(Iterable<?> it) throws IOException {
        emit('[');
        boolean needComma = false;
        for (Object value : it) {
            if (needComma) emit(',');
            needComma = true;
            write(value);
        }
        emit(']');
    }

    protected void array(Object object) throws IOException {
        emit('[');
        int length = Array.getLength(object);
        boolean needComma = false;
        for (int i = 0; i < length; i++) {
            if (needComma) emit(',');
            needComma = true;
            write(Array.get(object, i));
        }
        emit(']');
    }

    protected void bool(boolean b) throws IOException {
        emit(b ? "true" : "false");
    }

    protected void string(Object obj) throws IOException {
        emit('"');
        CharacterIterator it = new StringCharacterIterator(obj.toString());
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if (c == '"') emit("\\\"");
            else if (c == '\\') emit("\\\\");
            else if (c == '/') emit("\\/");
            else if (c == '\b') emit("\\b");
            else if (c == '\f') emit("\\f");
            else if (c == '\n') emit("\\n");
            else if (c == '\r') emit("\\r");
            else if (c == '\t') emit("\\t");
            else if (Character.isISOControl(c)) {
                emitUnicode(c);
            } else {
                emit(c);
            }
        }
        emit('"');
    }

    protected void emit(Object obj) throws IOException {
        writer.write(obj.toString());
    }

    protected void emit(char c) throws IOException {
        writer.write(c);
    }

    protected static final char[] hex = "0123456789ABCDEF".toCharArray();
    protected void emitUnicode(char c) throws IOException {
        emit("\\u");
        int n = c;
        for (int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;
            emit(hex[digit]);
            n <<= 4;
        }
    }
}
