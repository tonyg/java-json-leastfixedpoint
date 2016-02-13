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
 *     <li>JSON null is represented as {@link JSONNull#INSTANCE}.</li>
 *     <li>JSON numbers are represented as Java java.math.BigDecimal.</li>
 *     <li>JSON arrays are represented as java.util.List.</li>
 *     <li>JSON maps/objects are represented as java.util.Map.</li>
 * </ul>
 * <p>
 * Syntax errors are reported with JSONSyntaxError or, in case of short input, EOFException.
 * <p>
 * This class is able to read multiple adjacent JSON values from a single input stream. However, some care is needed
 * when doing this, since this class maintains a one-character internal lookahead buffer. Reading a single JSON value
 * generally consumes up to one character more than needed. For example, given a reader with ready input "123x",
 * JSONReader will consume all four bytes. When reading multiple JSON values from a stream, it is important to use the
 * same JSONReader object, since it will maintain its internal lookahead buffer between objects and so will not
 * accidentally discard input.
 * <p>
 * Furthermore, when given a Reader that is not a LineNumberReader, this class creates a wrapping LineNumberReader,
 * which may internally consume input from the underlying reader in a way not under our control.
 * <p>
 * Finally, this class can be used as a simple SAX-style JSON tokenizer; see {@link JSONReader#nextLexeme()} and the
 * class {@link JSONEventReader}.
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
     * Calls expectEOF() after reading, to ensure no trailing junk is present.
     */
    public static Object readFrom(Reader r) throws IOException {
        return readFrom(r, true);
    }

    /**
     * Reads and returns a single JSON value from the given input JSON source text.
     * Calls expectEOF() after reading, to ensure no trailing junk is present.
     */
    public static Object readFrom(String s) throws IOException {
        return readFrom(new StringReader(s), true);
    }

    /**
     * Reads and returns a single JSON value from the given input JSON source text.
     * If ensureSingleValue is true, calls expectEOF() after reading, to ensure no trailing junk is present.
     * Otherwise, ignores any input following the JSON value returned.
     */
    public static Object readFrom(String s, boolean ensureSingleValue) throws IOException {
        return readFrom(new StringReader(s), ensureSingleValue);
    }

    /**
     * Reads and returns a single JSON value from the given Reader.
     * If ensureSingleValue is true, calls expectEOF() after reading, to ensure no trailing junk is present.
     * Otherwise, leaves the given Reader in good condition to yield additional input.
     *
     * This is protected rather than public because reading multiple JSON values depends on the state of the
     * internal lookahead buffer, which with this static method is clearly not preserved. It would be dangerous to
     * encourage use of this method to read multiple JSON values from a stream. Instead, a long-running instance
     * of JSONReader should be used to parse the whole stream.
     */
    protected static Object readFrom(Reader r, boolean ensureSingleValue) throws IOException {
        JSONReader jsonReader = new JSONReader(r);
        Object result = jsonReader.read();
        if (ensureSingleValue) jsonReader.expectEOF();
        return result;
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
            while (Character.isWhitespace(this.buffer)) drop();
            if (checkDrop('/')) {
                if (checkDrop('/')) {
                    //noinspection StatementWithEmptyBody
                    while (!atEOF() && !check('\n')) drop();
                    continue;
                }
                syntaxError("Invalid comment");
            }
            break;
        }
    }

    protected Object valueGuard(Object value) throws JSONSyntaxError {
        if (value instanceof Lexeme) {
            unexpectedLexeme((Lexeme) value);
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

    /**
     * As read(), but wraps the result in {@link JSONValue}.
     */
    public JSONValue readValue() throws IOException {
        return JSONValue.wrap(read());
    }

    /**
     * Consumes any whitespace in the stream, and returns normally if it then finds itself
     * at the end of the stream. Throws JSONSyntaxError if, after consuming whitespace, some
     * non-whitespace input remains to be consumed. Useful for enforcing rules about files
     * containing some fixed number of JSON values and no more.
     */
    public void expectEOF() throws IOException {
        skipWhiteSpace();
        if (!atEOF()) {
            syntaxError("Expected, but did not see, end-of-file");
        }
    }

    protected Object readAtom(String atom, Object value) throws IOException {
        for (int i = 0; i < atom.length(); i++) {
            if (!checkDrop(atom.charAt(i))) {
                if (atEOF()) throw new EOFException();
                syntaxError("Invalid input parsing '" + atom + "'");
            }
        }
        return value;
    }

    /**
     * Read the next JSON token from the input stream. Strings, numbers, booleans and null are returned as the
     * Java representations of their JSON values, as described in the class comment for this class. Array and
     * object delimiters are returned as instances of {@link Lexeme}. Throws EOFException at the end of the input.
     */
    public Object nextLexeme() throws IOException {
        skipWhiteSpace();
        switch (curr()) {
            case '"': // fall through
            case '\'':
                char sep = curr();
                drop();
                return string(sep);
            case '[': drop(); return Lexeme.ARRAY_START;
            case ',': drop(); return Lexeme.COMMA;
            case ']': drop(); return Lexeme.ARRAY_END;
            case '{': drop(); return Lexeme.OBJECT_START;
            case ':': drop(); return Lexeme.COLON;
            case '}': drop(); return Lexeme.OBJECT_END;
            case 't': return readAtom("true", Boolean.TRUE);
            case 'f': return readAtom("false", Boolean.FALSE);
            case 'n': return readAtom("null", JSONNull.INSTANCE);
            default:
                if (!(Character.isDigit(curr()) || check('-'))) syntaxError("Invalid character: {" + curr() + "}");
                return number();
        }
    }

    protected Object _read() throws IOException {
        Object lexeme = nextLexeme();
        if (lexeme instanceof Lexeme) {
            switch ((Lexeme) lexeme) {
                case ARRAY_START:
                    return array();
                case OBJECT_START:
                    return object();
                default:
                    return lexeme;
            }
        } else {
            return lexeme;
        }
    }

    protected Map<String, Object> object() throws IOException {
        Map<String, Object> ret = new HashMap<>();
        Object _key = _read();
        if (_key == Lexeme.OBJECT_END) {
            return ret;
        }
        while (true) {
            if (!(_key instanceof String)) {
                expectedMapKey();
            }
            if (_read() != Lexeme.COLON) {
                expectedMapColon();
            }
            ret.put((String) _key, read());
            _key = _read();
            if (_key == Lexeme.OBJECT_END) {
                return ret;
            }
            if (_key != Lexeme.COMMA) {
                expectedMapComma();
            }
            _key = _read();
        }
    }

    protected List<Object> array() throws IOException {
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
                expectedArrayComma();
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

        return new BigDecimal(buf.toString());
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
                        default: syntaxError("Invalid string escape {" + curr() + "}");
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

    void unexpectedLexeme(Lexeme value) throws JSONSyntaxError {
        syntaxError("Unexpected lexeme " + value.toString());
    }

    void expectedMapComma() throws JSONSyntaxError {
        syntaxError("Expected comma separating map keys or end of map");
    }

    void expectedMapColon() throws JSONSyntaxError {
        syntaxError("Expected colon separating key from value");
    }

    void expectedMapKey() throws JSONSyntaxError {
        syntaxError("Expected string map key");
    }

    void expectedArrayComma() throws JSONSyntaxError {
        syntaxError("Expected comma separating array values or end of array");
    }

    void syntaxError(String message) throws JSONSyntaxError {
        throw new JSONSyntaxError(message, this.reader.getLineNumber());
    }

    /**
     * Most JSON tokens are self-representing; the remainder are represented with instances of Lexeme.
     */
    public enum Lexeme {
        OBJECT_START,
        OBJECT_END,
        ARRAY_START,
        ARRAY_END,
        COLON,
        COMMA
    }
}
