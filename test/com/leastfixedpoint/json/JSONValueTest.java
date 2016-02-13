package com.leastfixedpoint.json;

import org.testng.annotations.Test;

public class JSONValueTest {
    @Test(expectedExceptions = {JSONTypeError.class})
    public void testAddToScalar() throws JSONTypeError {
        JSONValue.wrap(123).add(234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testAddToMap() throws JSONTypeError {
        JSONValue.newMap().add(234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testSetInMap() throws JSONTypeError {
        JSONValue.newMap().set(0, 234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testPutInScalar() throws JSONTypeError {
        JSONValue.wrap(123).put("a", 234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testPutInList() throws JSONTypeError {
        JSONValue.newList().put("a", 234);
    }
}
