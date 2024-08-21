# com.leastfixedpoint.json

A package for JSON reading and writing for Java.

It offers:

 - construction of natural Java representations of JSON values (see class comment for [JSONReader][])
 - optional indented printing of JSON values (see [JSONWriter][])
 - reading of sequential/adjacent/concatenated JSON values from a file or stream (e.g. a Socket; see [the TCP/IP JSON "echo" server example][example])
 - both DOM-style ([JSONReader][]) and SAX-style ([JSONEventReader][]) parsing of JSON input
 - a helper class, [JSONValue][], for interrogating and manipulating representations of JSON values

[JSONReader]: https://tonyg.github.io/java-json-leastfixedpoint/doc/com/leastfixedpoint/json/JSONReader.html
[JSONwriter]: https://tonyg.github.io/java-json-leastfixedpoint/doc/com/leastfixedpoint/json/JSONWriter.html
[example]: https://github.com/tonyg/java-json-leastfixedpoint/tree/master/examples/com/leastfixedpoint/json/examples/JSONEchoServer.java
[JSONEventReader]: https://tonyg.github.io/java-json-leastfixedpoint/doc/com/leastfixedpoint/json/JSONEventReader.html
[JSONValue]: https://tonyg.github.io/java-json-leastfixedpoint/doc/com/leastfixedpoint/json/JSONValue.html

### License

Copyright (c) 2016,2024 Tony Garnock-Jones  
Copyright (c) 2007-2016 Pivotal Software, Inc. All Rights Reserved  
Copyright (c) 2006-2007 Frank Carver

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this library except in compliance with the License.
You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
