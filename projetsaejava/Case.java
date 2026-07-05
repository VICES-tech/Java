import java.io.Serializable;

/**
 * Représente une case de la grille du Démineur.
 * Une case peut être minée ou non, révélée ou cachée,
 * et peut porter un marqueur.
 */
public class Case implements Serializable {

    /**
     * Les états possibles du marqueur d'une case cachée.
     * (Remplacement de l'enum par des constantes entières classiques)
     */
    public static final int AUCUN = 0;
    public static final int MINE = 1;
    public static final int SOUPCON = 2;

    /** Indique si la case est minée. */
    private boolean minee;
    /** Indique si la case est révélée. */
    private boolean revelee;
    /** Le marqueur actuel de la case. */
    private int marqueur;
    /** Le nombre de mines dans les cases adjacentes. */
    private int minesAdjacentes;

    /**
     * Crée une case non minée, cachée, sans marqueur.
     */
    public Case() {
        this.minee = false;
        this.revelee = false;
        this.marqueur = AUCUN;
        this.minesAdjacentes = 0;
    }

    /**
     * Retourne vrai si la case est minée.
     *
     * @return true si la case est minée
     */
    public boolean estMinee() {
        return minee;
    }

    /**
     * Définit si la case est minée.
     *
     * @param minee true si la case doit être minée
     */
    public void setMinee(boolean minee) {
        this.minee = minee;
    }

    /**
     * Retourne vrai si la case est révélée.
     *
     * @return true si la case est révélée
     */
    public boolean estRevelee() {
        return revelee;
    }

    /**
     * Révèle la case.
     */
    public void reveler() {
        this.revelee = true;
    }

    /**
     * Retourne le marqueur actuel de la case.
     *
     * @return le marqueur de la case (sous forme d'entier)
     */
    public int getMarqueur() {
        return marqueur;
    }

    /**
     * Définit le marqueur de la case.
     *
     * @param marqueur le nouveau marqueur (utiliser les constantes Case.AUCUN, etc.)
     */
    public void setMarqueur(int marqueur) {
        this.marqueur = marqueur;
    }

    /**
     * Fait tourner le marqueur dans le cycle : AUCUN → MINE → SOUPCON → AUCUN.
     */
    public void cyclerMarqueur() {
        switch (marqueur) {
            case AUCUN:
                marqueur = MINE;
                break;
            case MINE:
                marqueur = SOUPCON;
                break;
            case SOUPCON:
                marqueur = AUCUN;
                break;
        }
    }

    /**
     * Retourne le nombre de mines dans les cases voisines.
     *
     * @return le nombre de mines adjacentes
     */
    public int getMinesAdjacentes() {
        return minesAdjacentes;
    }

    /**
     * Définit le nombre de mines adjacentes.
     *
     * @param nb le nombre de mines adjacentes
     */
    public void setMinesAdjacentes(int nb) {
        this.minesAdjacentes = nb;
    }
}