package fr.vices.planning.musique;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transpose des grilles d'accords (chants, chansons, partitions texte).
 *
 * <p>Formats reconnus :
 * <ul>
 *   <li>lignes d'accords placées au-dessus des paroles (l'alignement est conservé) ;</li>
 *   <li>accords entre crochets dans les paroles, format ChordPro : {@code [Am]Je chante} ;</li>
 *   <li>notation anglo-saxonne (C, D, E…) et notation française (Do, Ré, Mi…).</li>
 * </ul>
 *
 * <p>Une note est représentée par un index de 0 (Do / C) à 11 (Si / B) :
 * transposer revient à ajouter un nombre de demi-tons modulo 12.
 */
public final class Transposeur {

    /** Choix de l'écriture des altérations dans le résultat. */
    public enum Notation {
        /** Choisit automatiquement selon la tonalité d'arrivée. */
        AUTO,
        /** Toujours des dièses (C#, F#…). */
        DIESES,
        /** Toujours des bémols (Db, Gb…). */
        BEMOLS
    }

    /** Résultat d'une transposition. */
    public record Resultat(String texte, int demiTons, String tonaliteSource, String tonaliteCible) {
    }

    private static final String[] ANGLO_DIESES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] ANGLO_BEMOLS = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
    private static final String[] LATIN_DIESES = {"Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si"};
    private static final String[] LATIN_BEMOLS = {"Do", "Réb", "Ré", "Mib", "Mi", "Fa", "Solb", "Sol", "Lab", "La", "Sib", "Si"};

    /** Tonalités majeures qui s'écrivent plutôt avec des bémols (C inclus : Bb, Eb y sont fréquents). */
    private static final boolean[] MAJEUR_BEMOLS = new boolean[12];
    /** Tonalités mineures qui s'écrivent plutôt avec des bémols. */
    private static final boolean[] MINEUR_BEMOLS = new boolean[12];

    /** Index (0..11) de chaque nom de note sans altération. */
    private static final Map<String, Integer> NOTES_DE_BASE = new HashMap<>();

    static {
        for (int i : new int[] {0, 5, 10, 3, 8, 1}) {        // C F Bb Eb Ab Db
            MAJEUR_BEMOLS[i] = true;
        }
        for (int i : new int[] {9, 2, 7, 0, 5, 10, 3}) {     // Am Dm Gm Cm Fm Bbm Ebm
            MINEUR_BEMOLS[i] = true;
        }
        String[] anglo = {"C", "D", "E", "F", "G", "A", "B"};
        String[] latin = {"Do", "Ré", "Mi", "Fa", "Sol", "La", "Si"};
        int[] index = {0, 2, 4, 5, 7, 9, 11};
        for (int i = 0; i < index.length; i++) {
            NOTES_DE_BASE.put(anglo[i], index[i]);
            NOTES_DE_BASE.put(latin[i], index[i]);
        }
        NOTES_DE_BASE.put("Re", 2);
    }

    private static final String BASE = "(?:Do|Ré|Re|Mi|Fa|Sol|La|Si|[A-G])";
    private static final String ALTERATION = "(?:#|b|♯|♭)?";
    private static final String NOTE = BASE + ALTERATION;
    /** Ce qui peut suivre la note : m, 7, maj7, sus4, add9, dim, °, b5, (9)… */
    private static final String SUFFIXE = "(?:maj|min|dim|aug|sus|add|no|m|M|\\+|-|°|ø|Δ|\\(|\\)|[0-9]|#|b|♯|♭)*";

    private static final Pattern NOTE_DECOUPEE = Pattern.compile("(" + BASE + ")(#|b|♯|♭)?");
    private static final Pattern ACCORD = Pattern.compile("(" + NOTE + ")(" + SUFFIXE + ")(?:/(" + NOTE + "))?");
    private static final Pattern TONALITE = Pattern.compile("(" + NOTE + ")\\s*(m|min|mineur|M|maj|majeur)?");
    private static final Pattern CROCHETS = Pattern.compile("\\[([^\\]\\s]+)\\]");
    private static final Pattern JETON = Pattern.compile("\\S+");
    /** Symboles tolérés sur une ligne d'accords : barres de mesure, répétitions, « Intro: »… */
    private static final Pattern DECOR = Pattern.compile(
            "\\|+:?|:?\\|+|-+|/+|%|\\.+|N\\.?C\\.?|\\(?[xX×]\\d+\\)?|\\p{L}[\\p{L}\\d]*:");

    private Transposeur() {
    }

    // ------------------------------------------------------------------
    // Notes et accords
    // ------------------------------------------------------------------

    /**
     * Donne l'index (0..11) d'une note, ex. « C# » → 1, « Sib » → 10.
     *
     * @throws IllegalArgumentException si la note n'est pas reconnue
     */
    public static int indexNote(String note) {
        Matcher m = NOTE_DECOUPEE.matcher(note.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Note inconnue : " + note);
        }
        int index = NOTES_DE_BASE.get(m.group(1));
        String alteration = m.group(2);
        if (alteration != null) {
            index += alteration.equals("#") || alteration.equals("♯") ? 1 : -1;
        }
        return modulo12(index);
    }

    /** Transpose une note seule en gardant sa notation (française ou anglo-saxonne). */
    public static String transposerNote(String note, int demiTons, boolean bemols) {
        Matcher m = NOTE_DECOUPEE.matcher(note);
        if (!m.matches()) {
            throw new IllegalArgumentException("Note inconnue : " + note);
        }
        return nomNote(indexNote(note) + demiTons, estLatin(note), bemols);
    }

    /** Vrai si la note est écrite en notation française (Do, Ré, Mi…). */
    private static boolean estLatin(String note) {
        Matcher m = NOTE_DECOUPEE.matcher(note);
        return m.matches() && m.group(1).length() > 1;
    }

    /** Vrai si le texte est un accord, ex. « Am7 », « F#m7b5 », « Sol/Si ». */
    public static boolean estAccord(String texte) {
        return ACCORD.matcher(texte).matches();
    }

    /**
     * Transpose un accord complet (note, suffixe et basse éventuelle).
     *
     * @return l'accord transposé, ou {@code null} si le texte n'est pas un accord
     */
    public static String transposerAccord(String accord, int demiTons, boolean bemols) {
        Matcher m = ACCORD.matcher(accord);
        if (!m.matches()) {
            return null;
        }
        StringBuilder resultat = new StringBuilder();
        resultat.append(transposerNote(m.group(1), demiTons, bemols));
        resultat.append(m.group(2));
        if (m.group(3) != null) {
            resultat.append('/').append(transposerNote(m.group(3), demiTons, bemols));
        }
        return resultat.toString();
    }

    // ------------------------------------------------------------------
    // Tonalités
    // ------------------------------------------------------------------

    /**
     * Devine la tonalité d'un chant à partir de son premier accord.
     * C'est une approximation simple, mais elle suffit dans la grande majorité des cas.
     */
    public static Optional<String> detecterTonalite(String texte) {
        for (String ligne : texte.split("\\R")) {
            Matcher crochets = CROCHETS.matcher(ligne);
            while (crochets.find()) {
                if (estAccord(crochets.group(1))) {
                    return Optional.of(tonaliteDeLAccord(crochets.group(1)));
                }
            }
            if (estLigneAccords(ligne)) {
                Matcher jetons = JETON.matcher(ligne);
                while (jetons.find()) {
                    String accord = sansParentheses(jetons.group());
                    if (estAccord(accord)) {
                        return Optional.of(tonaliteDeLAccord(accord));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Nombre de demi-tons pour aller d'une tonalité à une autre, entre -5 et +6. */
    public static int ecart(String tonaliteSource, String tonaliteCible) {
        int ecart = modulo12(indexTonalite(tonaliteCible) - indexTonalite(tonaliteSource));
        return ecart > 6 ? ecart - 12 : ecart;
    }

    // ------------------------------------------------------------------
    // Texte complet
    // ------------------------------------------------------------------

    /**
     * Transpose tout un texte d'un nombre donné de demi-tons.
     *
     * @param texte          le chant (paroles + accords)
     * @param demiTons       décalage, positif (monter) ou négatif (descendre)
     * @param notation       écriture souhaitée des altérations
     * @param tonaliteSource tonalité d'origine, ou {@code null} pour la deviner
     */
    public static Resultat transposer(String texte, int demiTons, Notation notation, String tonaliteSource) {
        String source = tonaliteSource != null && !tonaliteSource.isBlank()
                ? tonaliteSource.trim()
                : detecterTonalite(texte).orElse(null);
        String cible = null;
        boolean bemols;
        if (source != null) {
            Matcher m = parserTonalite(source);
            boolean mineur = estMineur(m.group(2));
            int indexCible = indexNote(m.group(1)) + demiTons;
            bemols = choisirBemols(notation, indexCible, mineur, null);
            cible = nomNote(indexCible, estLatin(m.group(1)), bemols) + (mineur ? "m" : "");
        } else {
            bemols = notation == Notation.BEMOLS;
        }
        if (notation == Notation.AUTO && modulo12(demiTons) == 0) {
            return new Resultat(texte, 0, source, source);
        }
        return new Resultat(transposerTexte(texte, demiTons, bemols), demiTons, source, cible);
    }

    /**
     * Transpose un texte vers une tonalité donnée, ex. de « G » vers « Bb ».
     * Si la cible est écrite avec un « b » ou un « # », cette écriture est respectée.
     */
    public static Resultat transposerVers(String texte, String tonaliteSource, String tonaliteCible, Notation notation) {
        String source = tonaliteSource != null && !tonaliteSource.isBlank()
                ? tonaliteSource.trim()
                : detecterTonalite(texte).orElseThrow(() ->
                        new IllegalArgumentException("Impossible de deviner la tonalité d'origine : précise-la."));
        Matcher cible = parserTonalite(tonaliteCible);
        int demiTons = ecart(source, tonaliteCible);
        boolean bemols = choisirBemols(notation, indexNote(cible.group(1)),
                estMineur(cible.group(2)), cible.group(1));
        return new Resultat(transposerTexte(texte, demiTons, bemols), demiTons, source, tonaliteCible.trim());
    }

    /** Transpose ligne par ligne, sans toucher aux paroles. */
    public static String transposerTexte(String texte, int demiTons, boolean bemols) {
        String[] lignes = texte.split("\\R", -1);
        StringBuilder resultat = new StringBuilder();
        for (int i = 0; i < lignes.length; i++) {
            if (i > 0) {
                resultat.append('\n');
            }
            resultat.append(transposerLigne(lignes[i], demiTons, bemols));
        }
        return resultat.toString();
    }

    /** Transpose une ligne : crochets ChordPro, ligne d'accords, ou paroles (inchangées). */
    static String transposerLigne(String ligne, int demiTons, boolean bemols) {
        if (CROCHETS.matcher(ligne).find()) {
            Matcher m = CROCHETS.matcher(ligne);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String accord = transposerAccord(m.group(1), demiTons, bemols);
                String remplacement = accord != null ? "[" + accord + "]" : m.group();
                m.appendReplacement(sb, Matcher.quoteReplacement(remplacement));
            }
            m.appendTail(sb);
            return sb.toString();
        }
        if (estLigneAccords(ligne)) {
            return transposerLigneAccords(ligne, demiTons, bemols);
        }
        return ligne;
    }

    /**
     * Une ligne est une « ligne d'accords » si elle ne contient que des accords
     * (au moins un) et des symboles de mise en forme.
     * Ainsi « La vie est belle » reste des paroles, car « vie » n'est pas un accord.
     */
    static boolean estLigneAccords(String ligne) {
        Matcher m = JETON.matcher(ligne);
        boolean auMoinsUnAccord = false;
        while (m.find()) {
            String jeton = m.group();
            if (estAccord(sansParentheses(jeton))) {
                auMoinsUnAccord = true;
            } else if (!DECOR.matcher(jeton).matches()) {
                return false;
            }
        }
        return auMoinsUnAccord;
    }

    /**
     * Transpose une ligne d'accords en gardant chaque accord dans sa colonne d'origine,
     * pour qu'il reste au-dessus de la bonne syllabe.
     */
    static String transposerLigneAccords(String ligne, int demiTons, boolean bemols) {
        StringBuilder resultat = new StringBuilder();
        Matcher m = JETON.matcher(ligne);
        while (m.find()) {
            String jeton = m.group();
            String nouveau = transposerJeton(jeton, demiTons, bemols);
            int colonne = m.start();
            if (resultat.length() < colonne) {
                resultat.append(" ".repeat(colonne - resultat.length()));
            } else if (resultat.length() > 0) {
                resultat.append(' ');   // l'accord précédent s'est allongé : on garde un espace
            }
            resultat.append(nouveau);
        }
        return resultat.toString();
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    private static String transposerJeton(String jeton, int demiTons, boolean bemols) {
        String accord = transposerAccord(jeton, demiTons, bemols);
        if (accord != null) {
            return accord;
        }
        if (jeton.length() > 2 && jeton.startsWith("(") && jeton.endsWith(")")) {
            accord = transposerAccord(jeton.substring(1, jeton.length() - 1), demiTons, bemols);
            if (accord != null) {
                return "(" + accord + ")";
            }
        }
        return jeton;
    }

    private static String sansParentheses(String jeton) {
        if (jeton.length() > 2 && jeton.startsWith("(") && jeton.endsWith(")")) {
            return jeton.substring(1, jeton.length() - 1);
        }
        return jeton;
    }

    private static String tonaliteDeLAccord(String accord) {
        Matcher m = ACCORD.matcher(accord);
        m.matches();
        String suffixe = m.group(2);
        boolean mineur = (suffixe.startsWith("m") && !suffixe.startsWith("maj")) || suffixe.startsWith("min");
        return m.group(1) + (mineur ? "m" : "");
    }

    private static Matcher parserTonalite(String tonalite) {
        Matcher m = TONALITE.matcher(tonalite.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Tonalité inconnue : " + tonalite);
        }
        return m;
    }

    private static int indexTonalite(String tonalite) {
        return indexNote(parserTonalite(tonalite).group(1));
    }

    private static boolean estMineur(String mode) {
        return mode != null && mode.startsWith("m");
    }

    private static boolean choisirBemols(Notation notation, int indexCible, boolean mineur, String noteCibleEcrite) {
        if (notation == Notation.BEMOLS) {
            return true;
        }
        if (notation == Notation.DIESES) {
            return false;
        }
        if (noteCibleEcrite != null && noteCibleEcrite.length() > 1) {
            char derniere = noteCibleEcrite.charAt(noteCibleEcrite.length() - 1);
            if (derniere == 'b' || derniere == '♭') {
                return true;
            }
            if (derniere == '#' || derniere == '♯') {
                return false;
            }
        }
        int i = modulo12(indexCible);
        return mineur ? MINEUR_BEMOLS[i] : MAJEUR_BEMOLS[i];
    }

    private static String nomNote(int index, boolean latin, boolean bemols) {
        String[] noms = latin
                ? (bemols ? LATIN_BEMOLS : LATIN_DIESES)
                : (bemols ? ANGLO_BEMOLS : ANGLO_DIESES);
        return noms[modulo12(index)];
    }

    private static int modulo12(int n) {
        return ((n % 12) + 12) % 12;
    }
}
