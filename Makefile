JAVA?=java
JAVAC?=javac
JAVADOC?=javadoc
JAR?=jar
GCJ?=gcj
CC?=gcc
LD?=ld
GCJFLAGS?=-fjni
JCFLAGS?=-source 5.0

PREFIX?=/usr/local
JARDIR?=$(PREFIX)/share/java
DOCDIR?=$(PREFIX)/share/doc/salliere/
SHAREDIR?=$(PREFIX)/share/salliere/
BINDIR?=$(PREFIX)/bin/

DEBUG?=disable

CLASSPATH=/usr/share/java/csv.jar:/usr/share/java/debug-$(DEBUG).jar

VERSION=0.1

SRC=$(shell find cx -name '*.java')

all: salliere-$(VERSION).jar salliere.1 bin/salliere

.bin:
	mkdir -p bin
	touch .bin
.testbin:
	mkdir -p testbin
	touch .testbin
classes: .classes 
.classes: $(SRC)
	mkdir -p classes
	$(JAVAC) $(JCFLAGS) -cp $(CLASSPATH) -d classes -cp classes $^
	touch .classes
clean:
	rm -rf classes bin testbin
	rm -f .classes .bin .testbin *.tar.gz *.jar *.1

salliere-$(VERSION).jar: .classes
	(cd classes; $(JAR) cf ../$@ cx)

salliere-$(VERSION).tar.gz: Makefile cx README INSTALL COPYING changelog todo salliere.sh
	mkdir -p salliere-$(VERSION)
	cp -a $^ salliere-$(VERSION)
	tar zcf $@ salliere-$(VERSION)

cvs.jar: 
	ln -sf /usr/share/java/csv.jar .
debug-$(DEBUG).jar: 
	ln -sf /usr/share/java/debug-$(DEBUG).jar .

bin/%: %.sh .bin
	sed 's,\%JARPATH\%,$(JARPREFIX),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@

testbin/%: %.sh .testbin salliere-$(VERSION).jar cvs.jar debug-$(DEBUG).jar
	sed 's,\%JARPATH\%,.,;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@
	chmod 755 $@

%.1: %.sgml
	docbook-to-man $< > $@

install:

