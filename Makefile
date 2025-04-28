all:
	mvn package

clean:
	mvn clean

doc: target/site/apidocs

target/site/apidocs:
	mvn javadoc:javadoc

run:
	mvn compile test-compile
	java -cp target/classes:target/test-classes \
		com.leastfixedpoint.json.examples.JSONEchoServer

pages:
	@(git branch -v | grep -q gh-pages || (echo local gh-pages branch missing; false))
	@echo
	@git branch -av | grep gh-pages
	@echo
	@(echo 'Is the branch up to date? Press enter to continue.'; read dummy)
	git clone -b gh-pages . pages

publish: target/site/apidocs pages
	rm -rf pages/doc
	mkdir -p pages/doc
	cp -r target/site/apidocs/. pages/doc/.
	(cd pages; git add -A)
	-(cd pages; git commit -m "Update $$(date +%Y%m%d%H%M%S)")
	(cd pages; git push)
	rm -rf pages
