import java.awt.GridLayout;
import javax.swing.JPanel;

/**
 * Panneau graphique représentant la grille du Démineur.
 * Contient un tableau de VueCase disposés en grille.
 */
public class VueGrille extends JPanel {

    /** Le tableau de vues des cases. */
    private VueCase[][] vueCases;
    /** Le nombre de lignes de la grille. */
    private int lignes;
    /** Le nombre de colonnes de la grille. */
    private int colonnes;

    /**
     * Crée la vue de la grille à partir de la partie et de la fenêtre de jeu.
     *
     * @param partie     la partie en cours
     * @param fenetreJeu la fenêtre de jeu parente
     */
    public VueGrille(Partie partie, FenetreJeu fenetreJeu) {
        Grille grille = partie.getGrille();
        this.lignes = grille.getLignes();
        this.colonnes = grille.getColonnes();
        this.vueCases = new VueCase[lignes][colonnes];
        setLayout(new GridLayout(lignes, colonnes));
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                vueCases[i][j] = new VueCase(i, j, fenetreJeu);
                add(vueCases[i][j]);
            }
        }
        actualiser(partie);
    }

    /**
     * Met à jour l'affichage de toutes les cases selon l'état de la partie.
     *
     * @param partie la partie en cours
     */
    public void actualiser(Partie partie) {
        Grille grille = partie.getGrille();
        boolean fin = partie.estTerminee();
        int le = partie.getLigneExplosion();
        int ce = partie.getColonneExplosion();
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                vueCases[i][j].mettreAJour(grille.getCase(i, j), fin, le, ce);
            }
        }
        revalidate();
        repaint();
    }
}
