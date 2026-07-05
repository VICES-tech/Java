import java.io.Serializable;
import java.util.Random;

/**
 * Représente la grille de jeu du Démineur.
 * Contient toutes les cases et implémente la logique de jeu
 * (placement des mines, révélation, marqueurs, chaîne).
 */
public class Grille implements Serializable {

    /** Nombre de lignes de la grille. */
    private int lignes;
    /** Nombre de colonnes de la grille. */
    private int colonnes;
    /** Le tableau de cases. */
    private Case[][] cases;
    /** Le nombre total de mines dans la grille. */
    private int nbMines;

    /**
     * Crée une grille avec les dimensions et le nombre de mines spécifiés.
     * Les mines sont placées aléatoirement.
     *
     * @param lignes   le nombre de lignes (entre 4 et 30)
     * @param colonnes le nombre de colonnes (entre 4 et 30)
     * @param nbMines  le nombre de mines à placer
     */
    public Grille(int lignes, int colonnes, int nbMines) {
        this.lignes = lignes;
        this.colonnes = colonnes;
        this.nbMines = nbMines;
        this.cases = new Case[lignes][colonnes];
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                cases[i][j] = new Case();
            }
        }
        placerMines();
        calculerAdjacences();
    }

    /**
     * Retourne le nombre de lignes de la grille.
     *
     * @return le nombre de lignes
     */
    public int getLignes() {
        return lignes;
    }

    /**
     * Retourne le nombre de colonnes de la grille.
     *
     * @return le nombre de colonnes
     */
    public int getColonnes() {
        return colonnes;
    }

    /**
     * Retourne le nombre total de mines.
     *
     * @return le nombre de mines
     */
    public int getNbMines() {
        return nbMines;
    }

    /**
     * Retourne la case à la position (ligne, colonne).
     *
     * @param ligne   l'indice de la ligne
     * @param colonne l'indice de la colonne
     * @return la case à cette position
     */
    public Case getCase(int ligne, int colonne) {
        return cases[ligne][colonne];
    }

    /**
     * Place les mines aléatoirement dans la grille.
     */
    private void placerMines() {
        Random rand = new Random();
        int placees = 0;
        while (placees < nbMines) {
            int l = rand.nextInt(lignes);
            int c = rand.nextInt(colonnes);
            if (!cases[l][c].estMinee()) {
                cases[l][c].setMinee(true);
                placees++;
            }
        }
    }

    /**
     * Calcule et affecte le nombre de mines adjacentes pour chaque case.
     */
    private void calculerAdjacences() {
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                cases[i][j].setMinesAdjacentes(compterMinesAdjacentes(i, j));
            }
        }
    }

    /**
     * Compte le nombre de mines dans les cases voisines de (l, c).
     *
     * @param l la ligne
     * @param c la colonne
     * @return le nombre de mines adjacentes
     */
    private int compterMinesAdjacentes(int l, int c) {
        int count = 0;
        for (int dl = -1; dl <= 1; dl++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dl == 0 && dc == 0) {
                    continue;
                }
                int nl = l + dl;
                int nc = c + dc;
                if (nl >= 0 && nl < lignes && nc >= 0 && nc < colonnes) {
                    if (cases[nl][nc].estMinee()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Compte le nombre de marqueurs MINE (\u2605) posés sur la grille.
     *
     * @return le nombre de marqueurs \u2605
     */
    public int compterMarqueursMine() {
        int count = 0;
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                // Modification ici : Case.MINE au lieu de Case.Marqueur.MINE
                if (cases[i][j].getMarqueur() == Case.MINE) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Tente de révéler la case à (l, c).
     * Si la case porte un marqueur, ne fait rien (sécurité).
     * Si la case est minée, la révèle et retourne false (mine touchée).
     * Sinon révèle la case et déclenche éventuellement une chaîne.
     *
     * @param l la ligne
     * @param c la colonne
     * @return true si tout s'est bien passé, false si une mine a été touchée
     */
    public boolean reveler(int l, int c) {
        Case cas = cases[l][c];
        if (cas.estRevelee()) {
            return true;
        }
        // Modification ici : Case.AUCUN au lieu de Case.Marqueur.AUCUN
        if (cas.getMarqueur() != Case.AUCUN) {
            return true;
        }
        if (cas.estMinee()) {
            cas.reveler();
            return false;
        }
        revelerRecursivement(l, c);
        return true;
    }

    /**
     * Révèle récursivement la case (l, c) et ses voisines si aucune mine adjacente.
     * Ne révèle pas les cases marquées.
     *
     * @param l la ligne
     * @param c la colonne
     */
    private void revelerRecursivement(int l, int c) {
        Case cas = cases[l][c];
        if (cas.estRevelee()) {
            return;
        }
        // Modification ici : Case.AUCUN au lieu de Case.Marqueur.AUCUN
        if (cas.getMarqueur() != Case.AUCUN) {
            return;
        }
        cas.reveler();
        if (cas.getMinesAdjacentes() == 0) {
            for (int dl = -1; dl <= 1; dl++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dl == 0 && dc == 0) {
                        continue;
                    }
                    int nl = l + dl;
                    int nc = c + dc;
                    if (nl >= 0 && nl < lignes && nc >= 0 && nc < colonnes) {
                        revelerRecursivement(nl, nc);
                    }
                }
            }
        }
    }

    /**
     * Fait tourner le marqueur de la case (l, c).
     * Ne fait rien si la case est déjà révélée.
     *
     * @param l la ligne
     * @param c la colonne
     */
    public void cyclerMarqueur(int l, int c) {
        if (!cases[l][c].estRevelee()) {
            cases[l][c].cyclerMarqueur();
        }
    }

    /**
     * Vérifie si la partie est gagnée.
     * La partie est gagnée quand toutes les cases non minées sont révélées.
     *
     * @return true si le joueur a gagné
     */
    public boolean estGagnee() {
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                if (!cases[i][j].estMinee() && !cases[i][j].estRevelee()) {
                    return false;
                }
            }
        }
        return true;
    }
}