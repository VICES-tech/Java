SOURCES = Case.java Grille.java Partie.java \
          VueCase.java VueGrille.java \
          EcouteurFermeture.java DialogueDimensions.java \
          FenetreMenu.java FenetreJeu.java \
          Demineur.java

all: $(SOURCES)
	javac -encoding UTF-8 -implicit:none $(SOURCES)

run: all
	java Demineur

clean:
	rm -f *.class sauvegarde.dat

.PHONY: all run clean
