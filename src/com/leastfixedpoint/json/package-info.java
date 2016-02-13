/**
 A package for JSON reading and writing for Java. It offers:
 <ul>
 <li>construction of natural Java representations of JSON values (see class comment for {@link com.leastfixedpoint.json.JSONReader JSONReader})</li>
 <li>optional indented printing of JSON values (see {@link com.leastfixedpoint.json.JSONWriter JSONWriter})</li>
 <li>reading of sequential/adjacent/concatenated JSON values from a file or stream (e.g. a {@link java.net.Socket Socket})</li>
 <li>both DOM-style ({@link com.leastfixedpoint.json.JSONReader JSONReader}) and SAX-style ({@link com.leastfixedpoint.json.JSONEventReader JSONEventReader}) parsing of JSON input</li>
 <li>a helper class, {@link com.leastfixedpoint.json.JSONValue JSONValue}, for interrogating and manipulating representations of JSON values</li>
 </ul>
 */
package com.leastfixedpoint.json;