package com.leastfixedpoint.json;

import com.leastfixedpoint.json.JSONReader.Lexeme;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SAX-style event-emitting JSON parser.
 */
public class JSONEventReader {
    protected JSONReader jsonReader;
    protected List<State> stateStack;
    protected State state;

    public JSONEventReader(JSONReader jsonReader) {
        this.jsonReader = jsonReader;
        this.stateStack = new ArrayList<>();
        this.state = State.GENERAL;
    }

    /**
     * Yields the next JSON token. Returns null at the end of the input (and thus differs from the behaviour of
     * {@link JSONReader#nextLexeme()}).
     */
    public Object next() throws IOException {
        while (true) {
            Object token = null;
            try {
                token = jsonReader.nextLexeme();
            } catch (EOFException e) {
                return null;
            }
            switch (state) {
                case GENERAL:
                    maybeEnterNested(token);
                    return token;

                case FIRST_MAP_KEY:
                    if (token == Lexeme.OBJECT_END) {
                        pop();
                        return token;
                    }
                    /* FALL THROUGH */
                case SUBSEQUENT_MAP_KEY:
                    if (!(token instanceof String)) jsonReader.expectedMapKey();
                    gotoState(State.MAP_COLON);
                    return token;

                case MAP_COLON:
                    if (token != Lexeme.COLON) jsonReader.expectedMapColon();
                    gotoState(State.MAP_VALUE);
                    continue;

                case MAP_VALUE:
                    gotoState(State.MAP_COMMA_OR_END);
                    maybeEnterNested(token);
                    return token;

                case MAP_COMMA_OR_END:
                    if (token == Lexeme.OBJECT_END) {
                        pop();
                        return token;
                    }
                    if (token != Lexeme.COMMA) jsonReader.expectedMapComma();
                    gotoState(State.SUBSEQUENT_MAP_KEY);
                    continue;

                case FIRST_ARRAY_VALUE:
                    if (token == Lexeme.ARRAY_END) {
                        pop();
                        return token;
                    }
                    /* FALL THROUGH */
                case SUBSEQUENT_ARRAY_VALUE:
                    gotoState(State.ARRAY_COMMA_OR_END);
                    maybeEnterNested(token);
                    return token;

                case ARRAY_COMMA_OR_END:
                    if (token == Lexeme.ARRAY_END) {
                        pop();
                        return token;
                    }
                    if (token != Lexeme.COMMA) jsonReader.expectedArrayComma();
                    gotoState(State.SUBSEQUENT_ARRAY_VALUE);
                    continue;
            }
        }
    }

    protected void maybeEnterNested(Object token) throws JSONSyntaxError {
        if (token instanceof Lexeme) {
            switch ((Lexeme) token) {
                case OBJECT_START:
                    pushAndGoto(State.FIRST_MAP_KEY);
                    break;
                case ARRAY_START:
                    pushAndGoto(State.FIRST_ARRAY_VALUE);
                    break;
                default:
                    jsonReader.unexpectedLexeme((Lexeme) token);
                    break;
            }
        }
    }

    protected void gotoState(State newState) {
        this.state = newState;
    }

    protected void pushAndGoto(State newState) {
        this.stateStack.add(this.state);
        this.state = newState;
    }

    protected void pop() {
        this.state = this.stateStack.remove(this.stateStack.size() - 1);
    }

    protected enum State {
        GENERAL,
        FIRST_MAP_KEY,
        SUBSEQUENT_MAP_KEY,
        MAP_COLON,
        MAP_VALUE,
        MAP_COMMA_OR_END,
        FIRST_ARRAY_VALUE,
        SUBSEQUENT_ARRAY_VALUE,
        ARRAY_COMMA_OR_END
    }
}
