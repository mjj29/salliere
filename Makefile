JAVA?=java
JAVAC?=javac
JAVADOC?=javadoc
JAR?=jar
GCJ?=gcj
CC?=gcc
LD?=ld
GCJFLAGS?=-fjni
JCFLAGS?=-source 1.5 -Xlint:all
MSGFMT?=msgfmt

PREFIX?=/usr/local
JARLIBDIR?=$(PREFIX)/share/java
JARINSTALLDIR?=$(PREFIX)/share/salliere
DOCDIR?=$(PREFIX)/share/doc/salliere
MANDIR?=$(PREFIX)/share/man/man1
SHAREDIR?=$(PREFIX)/share/salliere
BINDIR?=$(PREFIX)/bin

DEBUG?=disable

CLASSPATH=/usr/share/java/csv.jar:/usr/share/java/debug-$(DEBUG).jar:/usr/share/java/itext.jar

VERSION=$(shell sed -n '1s/Version \(.*\):/\1/p' changelog)

SRC=$(shell find cx -name '*.java')

all: salliere-$(VERSION).jar salliere.1 bin/salliere gsalliere-$(VERSION).jar gsalliere.1 bin/gsalliere

.bin:
	mkdir -p bin
	touch .bin
.testbin:
	mkdir -p testbin
	touch .testbin
classes: .classes 
.classes: $(SRC) translations/*.po
	mkdir -p classes
	$(JAVAC) $(JCFLAGS) -cp $(CLASSPATH):classes -d classes $(SRC)
	(cd translations; for i in *.po; do $(MSGFMT) --java2 -r salliere_localized -d ../classes -l $${i%.po} $$i; done)
	$(MSGFMT) --java2 -r salliere_localized -d classes translations/en_GB.po
	touch .classes
clean:
	rm -rf classes bin testbin salliere-$(VERSION)
	rm -f .classes .bin .testbin *.tar.gz *.jar *.1 *Manifest.txt

salliere-$(VERSION).jar: SalliereManifest.txt .classes
	(cd classes; $(JAR) cfm ../$@ ../$< $(shell cd classes; find cx -name '*.class' -and -not -name 'GSalliere*' | sed 's/\$$/\\$$/g') *localized*.class)

gsalliere-$(VERSION).jar: GSalliereManifest.txt .classes
	(cd classes; $(JAR) cfm ../$@ ../$< cx/ath/matthew/salliere/GSalliere*.class)

salliere-$(VERSION).tar.gz: Makefile cx README INSTALL COPYING changelog todo salliere.sh salliere.sgml Manifest.txt.in gsalliere.sh gsalliere.sgml translations ecl2salliere.sh
	mkdir -p salliere-$(VERSION)
	cp -a $^ salliere-$(VERSION)
	tar zcf $@ salliere-$(VERSION)

csv.jar: 
	ln -sf /usr/share/java/csv.jar .
itext.jar: 
	ln -sf /usr/share/java/itext.jar .
debug-$(DEBUG).jar: 
	ln -sf /usr/share/java/debug-$(DEBUG).jar .

bin/%: %.sh .bin
	sed 's,\%JARINSTPATH\%,$(JARINSTALLDIR),;s,\%JARLIBPATH\%,$(JARLIBDIR),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@

testbin/%: %.sh .testbin salliere-$(VERSION).jar csv.jar debug-$(DEBUG).jar itext.jar gsalliere-$(VERSION).jar
	sed 's,\%JARPATH\%,.,;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@
	chmod 755 $@

%.1: %.sgml
	docbook-to-man $< > $@

SalliereManifest.txt: Manifest.txt.in
	echo Main-Class: cx.ath.matthew.salliere.Salliere > $@
ifeq ($(DEBUG),enable)
	echo Class-Path: $(JARLIBDIR)/csv.jar $(JARLIBDIR)/debug-$(DEBUG).jar $(JARLIBDIR)/itext.jar >> $@
else
	echo Class-Path: $(JARLIBDIR)/csv.jar $(JARLIBDIR)/itext.jar >> $@
endif
	cat $< >> $@
	echo "Implementation-Version: $(VERSION)" >> $@

GSalliereManifest.txt: Manifest.txt.in
	echo Main-Class: cx.ath.matthew.salliere.GSalliere > $@
ifeq ($(DEBUG),enable)
	echo Class-Path: $(JARLIBDIR)/debug-$(DEBUG).jar $(JARINSTALLDIR)/salliere-$(VERSION).jar >> $@
else
	echo Class-Path: $(JARINSTALLDIR)/salliere-$(VERSION).jar >> $@
endif
	cat $< >> $@
	echo "Implementation-Version: $(VERSION)" >> $@

translations/en_GB.po: $(SRC)
	echo "#java-format" > $@
	sed -n '/_(/s/.*_("\([^"]*\)").*/\1/p' $^ | sort -u | sed 's/\(.*\)/msgid "\1"\nmsgstr "\1"/' >> $@

install: salliere.1 gsalliere.1 bin/salliere bin/gsalliere gsalliere-$(VERSION).jar salliere-$(VERSION).jar changelog COPYING INSTALL README todo bin/ecl2salliere
	install -d $(DESTDIR)$(BINDIR)
	install bin/salliere $(DESTDIR)$(BINDIR)
	install bin/gsalliere $(DESTDIR)$(BINDIR)
	install bin/ecl2salliere $(DESTDIR)$(BINDIR)
	install -d $(DESTDIR)$(MANDIR)
	install -m 644 salliere.1 $(DESTDIR)$(MANDIR)
	install -m 644 gsalliere.1 $(DESTDIR)$(MANDIR)
	install -d $(DESTDIR)$(JARINSTALLDIR)
	install -m 644 salliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)
	install -m 644 gsalliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)
	install -d $(DESTDIR)$(DOCDIR)
	install -m 644 changelog COPYING INSTALL README todo $(DESTDIR)$(DOCDIR)

uninstall:
	rm -f $(DESTDIR)$(BINDIR)/salliere $(DESTDIR)$(BINDIR)/gsalliere $(DESTDIR)$(BINDIR)/ecl2salliere
	rm -f $(DESTDIR)$(JARINSTALLDIR)/salliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)/gsalliere-$(VERSION).jar
	rm -f $(DESTDIR)$(MANDIR)/salliere.1 $(DESTDIR)$(MANDIR)/gsalliere.1
	rm -f $(DESTDIR)$(DOCDIR)/changelog $(DESTDIR)$(DOCDIR)/COPYING $(DESTDIR)$(DOCDIR)/INSTALL $(DESTDIR)$(DOCDIR)/README $(DESTDIR)$(DOCDIR)/todo
	rmdir --ignore-fail-on-non-empty $(DESTDIR)$(BINDIR) $(DESTDIR)$(MANDIR) $(DESTDIR)$(JARINSTALLDIR) $(DESTDIR)$(DOCDIR)
