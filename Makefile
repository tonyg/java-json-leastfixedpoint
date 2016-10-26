# omg polyglot
include build.properties

JAVASOURCES=$(shell find examples src -iname '*.java')

all: build/lib/${PROJECT}-${VERSION}.jar build/classes/main doc

clean:
	ant clean

build/lib/${PROJECT}-${VERSION}.jar: ${JAVASOURCES}
	ant jar

build/classes/examples build/classes/main: ${JAVASOURCES}
	ant build

doc:
	ant javadoc

run: build/classes/examples build/lib/${PROJECT}-${VERSION}.jar
	java -cp build/lib/${PROJECT}-${VERSION}.jar:build/classes/examples \
		com.leastfixedpoint.json.examples.JSONEchoServer

pages:
	@(git branch -v | grep -q gh-pages || (echo local gh-pages branch missing; false))
	@echo
	@git branch -av | grep gh-pages
	@echo
	@(echo 'Is the branch up to date? Press enter to continue.'; read dummy)
	git clone -b gh-pages . pages

publish: doc pages
	rm -rf pages/doc
	cp -r doc pages/.
	(cd pages; git add -A)
	-(cd pages; git commit -m "Update $$(date +%Y%m%d%H%M%S)")
	(cd pages; git push)
	rm -rf pages
