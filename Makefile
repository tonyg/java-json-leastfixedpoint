PROJECT=com.leastfixedpoint.json
VERSION=1.0

JAVASOURCES=$(shell find examples src -iname '*.java')

all: build/lib build/classes/main doc

clean:
	ant clean

build/lib/${PROJECT}.jar: ${JAVASOURCES}
	ant jar

build/classes/examples build/classes/main: ${JAVASOURCES}
	ant build

doc:
	ant javadoc

run: build/classes/examples build/lib/${PROJECT}.jar
	java -cp build/lib/${PROJECT}.jar:build/classes/examples \
		com.leastfixedpoint.json.examples.JSONEchoServer
