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
 * Based on org.stringtree.json.JSONReader, licensed under APL and
 * LGPL. We've chosen APL (see above). The original code was written
 * by Frank Carver. Tony Garnock-Jones has made many changes to it
 * since then.
 */
package com.leastfixedpoint.json;

import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse JSON text to Java values.
 * <p>
 * <ul>
 *     <li>JSON strings are represented as java.lang.String.</li>
 *     <li>JSON true and false are represented as java.lang.Boolean.</li>
 *     <li>JSON null is represented as Java null.</li>
 *     <li>JSON numbers are represented as Java java.lang.Double.</li>
 *     <li>JSON arrays are represented as java.util.List.</li>
 *     <li>JSON maps/objects are represented as java.util.Map.</li>
 * </ul>
 * <p>
 * Syntax errors are reported with JSONSyntaxError or, in case of short input, EOFException.
 */
public class JSONReader {
    private final StringBuilder buf = new StringBuilder();
    protected LineNumberReader reader;
    protected int buffer = -1;

    /**
     * Construct a reader that reads JSON text from the given Reader.
     * If the Reader is not a LineNumberReader, it is wrapped in a LineNumberReader.
     * @param r Input to the JSONReader.
     */
    public JSONReader(Reader r) {
        if (r instanceof LineNumberReader) {
            this.reader = (LineNumberReader) r;
        } else {
            this.reader = new LineNumberReader(r);
        }
    }

    /**
     * Retrieve the underlying LineNumberReader.
     */
    public LineNumberReader getReader() {
        return reader;
    }

    /**
     * Reads and returns a single JSON value from the given Reader.
     */
    public static Object readFrom(Reader r) throws IOException {
        return new JSONReader(r).read();
    }

    /**
     * Reads and returns a single JSON value from the given input JSON source text.
     */
    public static Object readFrom(String s) throws IOException {
        return readFrom(new StringReader(s));
    }

    protected void drop() throws IOException {
        this.buffer = reader.read();
    }

    protected boolean atEOF() {
        return this.buffer == -1;
    }

    protected char curr() throws EOFException {
        if (atEOF()) throw new EOFException();
        return (char) this.buffer;
    }

    protected boolean check(char expected) {
        return (this.buffer == expected);
    }

    protected boolean checkDrop(char expected) throws IOException {
        if (check(expected)) {
            drop();
            return true;
        } else {
            return false;
        }
    }

    protected void skipWhiteSpace() throws IOException {
        if (atEOF()) drop(); // prime the buffer
        while (!atEOF()) {
            while (Character.isWhitespace(curr())) drop();
            if (checkDrop('/')) {
                if (checkDrop('/')) {
                    //noinspection StatementWithEmptyBody
                    while (!atEOF() && !check('\n')) drop();
                    continue;
                }
                throw new JSONSyntaxError("Invalid comment", reader.getLineNumber());
            }
            break;
        }
    }

    protected Object valueGuard(Object value) throws JSONSyntaxError {
        if (value instanceof Lexeme) {
            throw new JSONSyntaxError("Unexpected lexeme " + value.toString(), reader.getLineNumber());
        }
        return value;
    }

    /**
     * Reads and returns the next JSON value.
     * Throws EOFException if no complete JSON value is available.
     */
    public Object read() throws IOException {
        return valueGuard(_read());
    }

    protected Object readAtom(String atom, Object value) throws IOException {
        for (int i = 0; i < atom.length(); i++) {
            if (!checkDrop(atom.charAt(i))) {
                if (atEOF()) throw new EOFException();
                throw new JSONSyntaxError("Invalid input parsing '" + atom + "'", reader.getLineNumber());
            }
        }
        return value;
    }

    protected Object _read() throws IOException {
        skipWhiteSpace();
        switch (curr()) {
            case '"': // fall through
            case '\'':
                char sep = curr();
                drop();
                return string(sep);
            case '[': drop(); return array();
            case ',': drop(); return Lexeme.COMMA;
            case ']': drop(); return Lexeme.ARRAY_END;
            case '{': drop(); return object();
            case ':': drop(); return Lexeme.COLON;
            case '}': drop(); return Lexeme.OBJECT_END;
            case 't': return readAtom("true", Boolean.TRUE);
            case 'f': return readAtom("false", Boolean.FALSE);
            case 'n': return readAtom("null", null);
            default:
                if (Character.isDigit(curr()) || check('-')) {
                    return number();
                }
                throw new JSONSyntaxError("Invalid character: >>>" + curr() + "<<<", reader.getLineNumber());
        }
    }

    protected Object object() throws IOException {
        Map<String, Object> ret = new HashMap<>();
        Object _key = _read();
        if (_key == Lexeme.OBJECT_END) {
            return ret;
        }
        while (true) {
            if (!(_key instanceof String)) {
                throw new JSONSyntaxError("Expected string map key", reader.getLineNumber());
            }
            if (_read() != Lexeme.COLON) {
                throw new JSONSyntaxError("Expected colon separating key from value", reader.getLineNumber());
            }
            ret.put((String) _key, read());
            _key = _read();
            if (_key == Lexeme.OBJECT_END) {
                return ret;
            }
            if (_key != Lexeme.COMMA) {
                throw new JSONSyntaxError("Expected comma separating map keys", reader.getLineNumber());
            }
            _key = _read();
        }
    }

    protected Object array() throws IOException {
        List<Object> ret = new ArrayList<>();
        Object _value = _read();
        if (_value == Lexeme.ARRAY_END) {
            return ret;
        }
        while (true) {
            ret.add(valueGuard(_value));
            _value = _read();
            if (_value == Lexeme.ARRAY_END) {
                return ret;
            }
            if (_value != Lexeme.COMMA) {
                throw new JSONSyntaxError("Expected comma separating array values", reader.getLineNumber());
            }
            _value = _read();
        }
    }

    private boolean checkShift(char expected) throws IOException {
        if (check(expected)) {
            shift();
            return true;
        }
        return false;
    }

    protected Object number() throws IOException {
        buf.setLength(0);

        checkShift('-');
        shiftDigits();
        if (checkShift('.')) shiftDigits();
        if (checkShift('e') || checkShift('E')) {
            if (!checkShift('+')) checkShift('-');
            shiftDigits();
        }

        return new Double(buf.toString());
    }

    /**
     * Read a string with a specific delimiter (either ' or ")
     */
    protected Object string(char sep) throws IOException {
        buf.setLength(0);

        while (!check(sep)) {
            if (checkDrop('\\')) {
                if (checkDrop('u')) {
                    shiftUnicode();
                } else {
                    int replacement = -1;
                    switch (curr()) {
                        case '"': replacement = '"'; break;
                        case '\\': replacement = '\\'; break;
                        case '/': replacement = '/'; break;

                        case 'b': replacement = '\b'; break;
                        case 'f': replacement = '\f'; break;
                        case 'n': replacement = '\n'; break;
                        case 'r': replacement = '\r'; break;
                        case 't': replacement = '\t'; break;
                        default:
                            throw new JSONSyntaxError("Invalid string escape >>>" + curr() + "<<<",
                                    reader.getLineNumber());
                    }
                    drop();
                    buf.append((char) replacement);
                }
            } else {
                shift();
            }
        }
        drop();

        return buf.toString();
    }

    private void shift() throws IOException {
        buf.append(curr());
        drop();
    }

    private void shiftDigits() throws IOException {
        while (Character.isDigit(this.buffer)) {
            shift();
        }
    }

    private void shiftUnicode() throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            switch (curr()) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    value = (value << 4) + curr() - '0';
                    break;
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    value = (value << 4) + curr() - 'a' + 10;
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                    value = (value << 4) + curr() - 'A' + 10;
                    break;
            }
            drop();
        }
        buf.append((char) value);
    }

    protected enum Lexeme {
        OBJECT_END,
        ARRAY_END,
        COLON,
        COMMA
    }
}
