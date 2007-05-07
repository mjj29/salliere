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
MANDIR?=$(PREFIX)/share/man/man1/
SHAREDIR?=$(PREFIX)/share/salliere/
BINDIR?=$(PREFIX)/bin/

DEBUG?=disable

CLASSPATH=/usr/share/java/csv.jar:/usr/share/java/debug-$(DEBUG).jar:/usr/share/java/itext.jar

VERSION=0.3

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
	rm -rf classes bin testbin salliere-$(VERSION)
	rm -f .classes .bin .testbin *.tar.gz *.jar *.1

salliere-$(VERSION).jar: .classes
	(cd classes; $(JAR) cfm ../$@ ../Manifest.txt cx)

salliere-$(VERSION).tar.gz: Makefile cx README INSTALL COPYING changelog todo salliere.sh salliere.sgml Manifest.txt
	mkdir -p salliere-$(VERSION)
	cp -a $^ salliere-$(VERSION)
	tar zcf $@ salliere-$(VERSION)

cvs.jar: 
	ln -sf /usr/share/java/csv.jar .
itext.jar: 
	ln -sf /usr/share/java/itext.jar .
debug-$(DEBUG).jar: 
	ln -sf /usr/share/java/debug-$(DEBUG).jar .

bin/%: %.sh .bin
	sed 's,\%JARPATH\%,$(JARDIR),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@

testbin/%: %.sh .testbin salliere-$(VERSION).jar cvs.jar debug-$(DEBUG).jar itext.jar
	sed 's,\%JARPATH\%,.,;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@
	chmod 755 $@

%.1: %.sgml
	docbook-to-man $< > $@

install: salliere.1 bin/salliere salliere-$(VERSION).jar changelog COPYING INSTALL README todo
	install -d $(DESTDIR)$(BINDIR)
	install bin/salliere $(DESTDIR)$(BINDIR)
	install -d $(DESTDIR)$(MANDIR)
	install -m 644 salliere.1 $(DESTDIR)$(MANDIR)
	install -d $(DESTDIR)$(JARDIR)
	install -m 644 salliere-$(VERSION).jar $(DESTDIR)$(JARDIR)
	ln -sf salliere-$(VERSION).jar $(DESTDIR)$(JARDIR)/salliere.jar
	install -d $(DESTDIR)$(DOCDIR)
	install -m 644 changelog COPYING INSTALL README todo $(DESTDIR)$(DOCDIR)

