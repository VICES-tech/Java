package fr.vices.planning.musique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.vices.planning.musique.Transposeur.Notation;
import fr.vices.planning.musique.Transposeur.Resultat;
import org.junit.jupiter.api.Test;

class TransposeurTest {

    @Test
    void indexDesNotes() {
        assertEquals(0, Transposeur.indexNote("C"));
        assertEquals(1, Transposeur.indexNote("C#"));
        assertEquals(1, Transposeur.indexNote("Db"));
        assertEquals(10, Transposeur.indexNote("Sib"));
        assertEquals(2, Transposeur.indexNote("Ré"));
        assertEquals(11, Transposeur.indexNote("Cb"));
    }

    @Test
    void transposeAccordsSimples() {
        assertEquals("D", Transposeur.transposerAccord("C", 2, false));
        assertEquals("Bm7", Transposeur.transposerAccord("Am7", 2, false));
        assertEquals("G#m7b5", Transposeur.transposerAccord("F#m7b5", 2, false));
        assertEquals("Bb/D", Transposeur.transposerAccord("C/E", -2, true));
        assertEquals("Fsus4", Transposeur.transposerAccord("Csus4", 5, true));
        assertEquals("Cmaj7", Transposeur.transposerAccord("Gmaj7", -7, false));
    }

    @Test
    void transposeNotationFrancaise() {
        assertEquals("Ré", Transposeur.transposerAccord("Do", 2, false));
        assertEquals("Sim", Transposeur.transposerAccord("Lam", 2, false));
        assertEquals("Mib7", Transposeur.transposerAccord("Ré7", 1, true));
        assertEquals("La/Do#", Transposeur.transposerAccord("Sol/Si", 2, false));
    }

    @Test
    void reconnaitLesAccords() {
        assertTrue(Transposeur.estAccord("Am"));
        assertTrue(Transposeur.estAccord("C#m7"));
        assertTrue(Transposeur.estAccord("Solm"));
        assertTrue(Transposeur.estAccord("Bbadd9"));
        assertFalse(Transposeur.estAccord("vie"));
        assertFalse(Transposeur.estAccord("Bonjour"));
        assertFalse(Transposeur.estAccord("la"));
    }

    @Test
    void distingueAccordsEtParoles() {
        assertTrue(Transposeur.estLigneAccords("  Am      F     C   G"));
        assertTrue(Transposeur.estLigneAccords("| Am | F | x2"));
        assertTrue(Transposeur.estLigneAccords("Intro: Do Sol Lam Fa"));
        assertFalse(Transposeur.estLigneAccords("La vie est belle"));
        assertFalse(Transposeur.estLigneAccords("Si je pouvais"));
        assertFalse(Transposeur.estLigneAccords(""));
    }

    @Test
    void garderLAlignementDesAccords() {
        String ligne = "C       G      Am     F";
        assertEquals("D       A      Bm     G", Transposeur.transposerLigneAccords(ligne, 2, false));
        // un accord qui s'allonge garde au moins un espace avec le suivant
        assertEquals("C# G#", Transposeur.transposerLigneAccords("C G", 1, false));
    }

    @Test
    void transposeUnChantComplet() {
        String chant = """
                C           G
                Amazing grace, how sweet the sound
                Am          F
                That saved a wretch like me""";
        String attendu = """
                D           A
                Amazing grace, how sweet the sound
                Bm          G
                That saved a wretch like me""";
        Resultat r = Transposeur.transposer(chant, 2, Notation.AUTO, null);
        assertEquals(attendu, r.texte());
        assertEquals("C", r.tonaliteSource());
        assertEquals("D", r.tonaliteCible());
    }

    @Test
    void transposeLeFormatChordPro() {
        String chant = "[G]Je chante [D/F#]pour toi [Em]ce soir";
        assertEquals("[A]Je chante [E/G#]pour toi [F#m]ce soir",
                Transposeur.transposer(chant, 2, Notation.AUTO, null).texte());
    }

    @Test
    void transposeVersUneTonalite() {
        Resultat r = Transposeur.transposerVers("G D Em C", null, "Bb", Notation.AUTO);
        assertEquals("Bb F Gm Eb", r.texte());
        assertEquals(3, r.demiTons());
        assertEquals("G", r.tonaliteSource());
    }

    @Test
    void ecartLePlusCourt() {
        assertEquals(2, Transposeur.ecart("C", "D"));
        assertEquals(-2, Transposeur.ecart("D", "C"));
        assertEquals(-1, Transposeur.ecart("C", "B"));
        assertEquals(0, Transposeur.ecart("Am", "La m"));
    }

    @Test
    void choisitBemolsOuDieses() {
        // D → Eb : tonalité à bémols
        assertEquals("Eb Bb Cm Ab", Transposeur.transposer("D A Bm G", 1, Notation.AUTO, null).texte());
        // C → E : tonalité à dièses
        assertEquals("E B C#m A", Transposeur.transposer("C G Am F", 4, Notation.AUTO, null).texte());
        // forcé
        assertEquals("C# G# A#m F#", Transposeur.transposer("C G Am F", 1, Notation.DIESES, null).texte());
    }

    @Test
    void detecteLaTonalite() {
        assertEquals("Am", Transposeur.detecterTonalite("Paroles\nAm  G\nla la").orElseThrow());
        assertEquals("Sol", Transposeur.detecterTonalite("[Sol]Alléluia [Ré]amen").orElseThrow());
        assertTrue(Transposeur.detecterTonalite("Pas d'accords ici").isEmpty());
    }

    @Test
    void neTouchePasAuxParoles() {
        String paroles = "Do you remember\nLa la la";
        assertEquals(paroles, Transposeur.transposer(paroles, 3, Notation.AUTO, "C").texte());
    }
}
