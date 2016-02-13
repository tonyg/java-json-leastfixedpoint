package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    @Test
    public void testBigDecimalValue() throws JSONTypeError {
        assert JSONValue.wrap(new BigDecimal("1e40")).bigDecimalValue().compareTo(new BigDecimal("1E+40")) == 0;
        assert JSONValue.wrap(new BigInteger("10000000000000000000000000000000000000000"))
                .bigDecimalValue().compareTo(new BigDecimal("1E+40")) == 0;
    }
}
