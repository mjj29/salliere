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

PREFIX?=/usr
JARLIBDIR?=$(PREFIX)/share/java
JARINSTALLDIR?=$(PREFIX)/share/salliere
DOCDIR?=$(PREFIX)/share/doc/salliere
MANDIR?=$(PREFIX)/share/man/man1
SHAREDIR?=$(PREFIX)/share/salliere
BINDIR?=$(PREFIX)/bin

DEBUG?=disable

CLASSPATH=$(JARLIBDIR)/csv.jar:$(JARLIBDIR)/debug-$(DEBUG).jar:$(JARLIBDIR)/itext.jar:$(JARLIBDIR)/commons-net.jar:$(JARLIBDIR)/kxml2.jar

VERSION=$(shell sed -n '1s/Version \(.*\):/\1/p' changelog)

SRC=$(shell find cx -name '*.java')

all: salliere-$(VERSION).jar salliere.1 bin/salliere gsalliere-$(VERSION).jar gsalliere.1 bin/gsalliere salliere-handicaps-$(VERSION).jar salliere-handicaps.1 bin/salliere-handicaps bin/leaderboard leaderboard-$(VERSION).jar leaderboard.1 bin/ecl2salliere

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
	(cd classes; $(JAR) cfm ../$@ ../$< $(shell cd classes; find cx -name '*.class' -and -not -name 'GSalliere*' -and -not -name 'Leader*' -and -not -name 'Handicaps*'  | sed 's/\$$/\\$$/g') *localized*.class)

gsalliere-$(VERSION).jar: GSalliereManifest.txt .classes
	(cd classes; $(JAR) cfm ../$@ ../$< cx/ath/matthew/salliere/GSalliere*.class)
leaderboard-$(VERSION).jar: LeaderBoardManifest.txt .classes
	(cd classes; $(JAR) cfm ../$@ ../$< cx/ath/matthew/salliere/Leader*.class)
salliere-handicaps-$(VERSION).jar: SalliereHandicapsManifest.txt .classes
	(cd classes; $(JAR) cfm ../$@ ../$< cx/ath/matthew/salliere/Handicaps*.class)

salliere-$(VERSION).tar.gz: Makefile cx README INSTALL COPYING changelog todo salliere.sh salliere.sgml Manifest.txt.in salliere-handicaps.sh salliere-handicaps.sgml gsalliere.sh gsalliere.sgml translations ecl2salliere.sh leaderboard.sh leaderboard.sgml
	mkdir -p salliere-$(VERSION)
	cp -a $^ salliere-$(VERSION)
	tar zcf $@ salliere-$(VERSION)

csv.jar: 
	ln -sf /usr/share/java/csv.jar .
itext.jar: 
	ln -sf /usr/share/java/itext.jar .
kxml2.jar: 
	ln -sf /usr/share/java/kxml2.jar .
commons-net.jar: 
	ln -sf /usr/share/java/commons-net.jar .
debug-$(DEBUG).jar: 
	ln -sf /usr/share/java/debug-$(DEBUG).jar .

bin/%: %.sh .bin
	sed 's,\%JARINSTPATH\%,$(JARINSTALLDIR),;s,\%JARLIBPATH\%,$(JARLIBDIR),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@

testbin/%: %.sh .testbin salliere-$(VERSION).jar csv.jar debug-$(DEBUG).jar itext.jar kxml2.jar salliere-handicaps-$(VERSION).jar gsalliere-$(VERSION).jar commons-net.jar leaderboard-$(VERSION).jar
	sed 's,\%JARPATH\%,'"`pwd`"',;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@
	chmod 755 $@

%.1: %.sgml
	docbook-to-man $< > $@

SalliereManifest.txt: Manifest.txt.in
	echo Main-Class: cx.ath.matthew.salliere.Salliere > $@
ifeq ($(DEBUG),enable)
	echo Class-Path: $(JARLIBDIR)/csv.jar $(JARLIBDIR)/debug-$(DEBUG).jar $(JARLIBDIR)/itext.jar $(JARLIBDIR)/kxml2.jar $(JARLIBDIR)/commons-net.jar >> $@
else
	echo Class-Path: $(JARLIBDIR)/csv.jar $(JARLIBDIR)/itext.jar $(JARLIBDIR)/kxml2.jar $(JARLIBDIR)/commons-net.jar >> $@
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

SalliereHandicapsManifest.txt: Manifest.txt.in
	echo Main-Class: cx.ath.matthew.salliere.Handicaps > $@
ifeq ($(DEBUG),enable)
	echo Class-Path: $(JARLIBDIR)/debug-$(DEBUG).jar $(JARINSTALLDIR)/salliere-$(VERSION).jar >> $@
else
	echo Class-Path: $(JARINSTALLDIR)/salliere-$(VERSION).jar >> $@
endif
	cat $< >> $@
	echo "Implementation-Version: $(VERSION)" >> $@

LeaderBoardManifest.txt: Manifest.txt.in
	echo Main-Class: cx.ath.matthew.salliere.Leaders > $@
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

install: salliere.1 gsalliere.1 bin/salliere-handicaps bin/salliere bin/gsalliere salliere-handicaps-$(VERSION).jar gsalliere-$(VERSION).jar salliere-$(VERSION).jar changelog COPYING INSTALL README todo bin/ecl2salliere bin/leaderboard leaderboard-$(VERSION).jar leaderboard.1 salliere-handicaps.1
	install -d $(DESTDIR)$(BINDIR)
	install bin/salliere $(DESTDIR)$(BINDIR)
	install bin/gsalliere $(DESTDIR)$(BINDIR)
	install bin/salliere-handicaps $(DESTDIR)$(BINDIR)
	install bin/leaderboard $(DESTDIR)$(BINDIR)
	install bin/ecl2salliere $(DESTDIR)$(BINDIR)
	install -d $(DESTDIR)$(MANDIR)
	install -m 644 salliere.1 $(DESTDIR)$(MANDIR)
	install -m 644 leaderboard.1 $(DESTDIR)$(MANDIR)
	install -m 644 gsalliere.1 $(DESTDIR)$(MANDIR)
	install -m 644 salliere-handicaps.1 $(DESTDIR)$(MANDIR)
	install -d $(DESTDIR)$(JARINSTALLDIR)
	install -m 644 salliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)
	install -m 644 gsalliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)
	install -m 644 leaderboard-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)
	install -m 644 salliere-handicaps-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)
	install -d $(DESTDIR)$(DOCDIR)
	install -m 644 changelog COPYING INSTALL README todo $(DESTDIR)$(DOCDIR)

uninstall:
	rm -f $(DESTDIR)$(BINDIR)/salliere $(DESTDIR)$(BINDIR)/gsalliere $(DESTDIR)$(BINDIR)/ecl2salliere $(DESTDIR)$(BINDIR)/leaderboard $(DESTDIR)$(BINDIR)/salliere-handicaps
	rm -f $(DESTDIR)$(JARINSTALLDIR)/salliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)/gsalliere-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)/leaderboard-$(VERSION).jar $(DESTDIR)$(JARINSTALLDIR)/salliere-handicaps-$(VERSION).jar
	rm -f $(DESTDIR)$(MANDIR)/salliere.1 $(DESTDIR)$(MANDIR)/gsalliere.1 $(DESTDIR)$(MANDIR)/leaderboard.1 $(DESTDIR)$(MANDIR)/salliere-handicaps.1
	rm -f $(DESTDIR)$(DOCDIR)/changelog $(DESTDIR)$(DOCDIR)/COPYING $(DESTDIR)$(DOCDIR)/INSTALL $(DESTDIR)$(DOCDIR)/README $(DESTDIR)$(DOCDIR)/todo
	rmdir --ignore-fail-on-non-empty $(DESTDIR)$(BINDIR) $(DESTDIR)$(MANDIR) $(DESTDIR)$(JARINSTALLDIR) $(DESTDIR)$(DOCDIR)
