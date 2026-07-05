import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Représente l'état complet d'une partie du Démineur.
 * Gère la logique de jeu (révélation, fin de partie) et la
 * sauvegarde/chargement par sérialisation Java.
 */
public class Partie implements Serializable {

    /** Nom du fichier de sauvegarde. */
    public static final String FICHIER_SAUVEGARDE = "sauvegarde.dat";

    /** La grille de jeu. */
    private Grille grille;
    /** Indique si la partie est terminée. */
    private boolean terminee;
    /** Indique si la partie est gagnée (valable seulement si terminee). */
    private boolean gagnee;
    /** Ligne de la mine qui a explosé, -1 si aucune. */
    private int ligneExplosion;
    /** Colonne de la mine qui a explosé, -1 si aucune. */
    private int colonneExplosion;

    /**
     * Crée une nouvelle partie avec une grille de dimensions et mines données.
     *
     * @param lignes   le nombre de lignes (4 à 30)
     * @param colonnes le nombre de colonnes (4 à 30)
     * @param nbMines  le nombre de mines à placer
     */
    public Partie(int lignes, int colonnes, int nbMines) {
        this.grille = new Grille(lignes, colonnes, nbMines);
        this.terminee = false;
        this.gagnee = false;
        this.ligneExplosion = -1;
        this.colonneExplosion = -1;
    }

    /**
     * Retourne la grille de jeu.
     *
     * @return la grille
     */
    public Grille getGrille() {
        return grille;
    }

    /**
     * Indique si la partie est terminée.
     *
     * @return true si la partie est terminée
     */
    public boolean estTerminee() {
        return terminee;
    }

    /**
     * Indique si la partie est gagnée.
     *
     * @return true si le joueur a gagné
     */
    public boolean estGagnee() {
        return gagnee;
    }

    /**
     * Retourne la ligne de la mine qui a explosé.
     *
     * @return la ligne de l'explosion, ou -1 si aucune
     */
    public int getLigneExplosion() {
        return ligneExplosion;
    }

    /**
     * Retourne la colonne de la mine qui a explosé.
     *
     * @return la colonne de l'explosion, ou -1 si aucune
     */
    public int getColonneExplosion() {
        return colonneExplosion;
    }

    /**
     * Tente de révéler la case (l, c) et met à jour l'état de la partie.
     * Si la case est minée, la partie est perdue.
     * Si toutes les cases non minées sont révélées, la partie est gagnée.
     *
     * @param l la ligne
     * @param c la colonne
     */
    public void jouer(int l, int c) {
        if (terminee) {
            return;
        }
        boolean ok = grille.reveler(l, c);
        if (!ok) {
            terminee = true;
            gagnee = false;
            ligneExplosion = l;
            colonneExplosion = c;
        } else if (grille.estGagnee()) {
            terminee = true;
            gagnee = true;
        }
    }

    /**
     * Sauvegarde la partie dans le fichier de sauvegarde par sérialisation.
     *
     * @throws IOException en cas d'erreur d'écriture
     */
    public void sauvegarder() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(FICHIER_SAUVEGARDE));
        oos.writeObject(this);
        oos.close();
    }

    /**
     * Charge une partie depuis le fichier de sauvegarde.
     *
     * @return la partie chargée, ou null si aucune sauvegarde valide
     */
    public static Partie charger() {
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(FICHIER_SAUVEGARDE));
            Partie p = (Partie) ois.readObject();
            ois.close();
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Supprime le fichier de sauvegarde s'il existe.
     */
    public static void supprimerSauvegarde() {
        new File(FICHIER_SAUVEGARDE).delete();
    }

    /**
     * Indique si un fichier de sauvegarde valide existe.
     *
     * @return true si une sauvegarde est disponible
     */
    public static boolean sauvegardeExiste() {
        return new File(FICHIER_SAUVEGARDE).exists();
    }
}
