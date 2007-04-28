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

all: salliere-$(VERSION).jar

classes: .classes 
.classes: $(SRC)
	mkdir -p classes
	$(JAVAC) $(JCFLAGS) -cp $(CLASSPATH) -d classes -cp classes $^
	touch .classes
clean:
	rm -rf classes
	rm -f .classes *.tar.gz *.jar 

salliere-$(VERSION).jar: .classes
	(cd classes; $(JAR) cf ../$@ cx)

salliere-$(VERSION).tar.gz: Makefile cx README INSTALL COPYING changelog todo
	mkdir -p salliere-$(VERSION)
	cp -a $^ salliere-$(VERSION)
	tar zcf $@ salliere-$(VERSION)

install:

