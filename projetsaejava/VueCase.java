import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * Représentation graphique d'une case du Démineur.
 * Hérite de JButton et gère les clics gauche (révélation)
 * et droit (marqueur) via l'interface MouseListener.
 */
public class VueCase extends JButton implements MouseListener {

    /** La ligne de la case dans la grille. */
    private int ligne;
    /** La colonne de la case dans la grille. */
    private int colonne;
    /** Référence à la fenêtre de jeu. */
    private FenetreJeu fenetreJeu;

    /** Couleur de fond d'une case cachée. */
    private static final Color COULEUR_CACHEE = new Color(34, 85, 34);
    /** Couleur de fond d'une case révélée sans mine. */
    private static final Color COULEUR_REVELEE = new Color(225, 205, 170);
    /** Couleur de fond d'une mine révélée. */
    private static final Color COULEUR_MINE = new Color(190, 40, 40);
    /** Couleur de fond de la mine qui a explosé. */
    private static final Color COULEUR_EXPLOSION = new Color(255, 70, 70);
    /** Couleur de fond d'un marqueur \u2605 incorrect (pas de mine). */
    private static final Color COULEUR_ERREUR = new Color(230, 130, 0);

    /** Couleurs pour les chiffres 1 à 8. */
    private static final Color[] COULEURS_CHIFFRES = {
        new Color(0, 0, 210),
        new Color(0, 130, 0),
        new Color(200, 0, 0),
        new Color(0, 0, 128),
        new Color(130, 0, 0),
        new Color(0, 130, 130),
        new Color(0, 0, 0),
        new Color(100, 100, 100)
    };

    /**
     * Crée une VueCase pour la position (ligne, colonne) dans la grille.
     *
     * @param ligne      la ligne de la case
     * @param colonne    la colonne de la case
     * @param fenetreJeu la fenêtre de jeu parente
     */
    public VueCase(int ligne, int colonne, FenetreJeu fenetreJeu) {
        this.ligne = ligne;
        this.colonne = colonne;
        this.fenetreJeu = fenetreJeu;
        setPreferredSize(new Dimension(40, 40));
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setFocusPainted(false);
        setOpaque(true);
        setContentAreaFilled(true);
        setBorderPainted(true);
        setMargin(new java.awt.Insets(0, 0, 0, 0));
        addMouseListener(this);
    }

    /**
     * Met à jour l'apparence du bouton selon l'état de la case.
     * Gère l'affichage normal, les marqueurs et l'affichage de fin de partie.
     *
     * @param cas              la case du modèle
     * @param finDePartie      true si la partie est terminée
     * @param ligneExplosion   ligne de la mine explosée, -1 si aucune
     * @param colonneExplosion colonne de la mine explosée, -1 si aucune
     */
    public void mettreAJour(Case cas, boolean finDePartie,
                             int ligneExplosion, int colonneExplosion) {
        if (cas.estRevelee()) {
            afficherCaseRevelee(cas, ligneExplosion, colonneExplosion);
        } else if (finDePartie) {
            afficherCaseFinDePartie(cas);
        } else {
            afficherCaseCachee(cas);
        }
    }

    /**
     * Affiche une case révélée (chiffre, mine ou explosion).
     *
     * @param cas              la case
     * @param ligneExplosion   ligne de l'explosion
     * @param colonneExplosion colonne de l'explosion
     */
    private void afficherCaseRevelee(Case cas, int ligneExplosion, int colonneExplosion) {
        if (cas.estMinee()) {
            if (ligne == ligneExplosion && colonne == colonneExplosion) {
                setBackground(COULEUR_EXPLOSION);
                setText("X");
                setForeground(Color.WHITE);
            } else {
                setBackground(COULEUR_MINE);
                setText("\u2605");
                setForeground(Color.WHITE);
            }
        } else {
            setBackground(COULEUR_REVELEE);
            int nb = cas.getMinesAdjacentes();
            if (nb > 0) {
                setText(String.valueOf(nb));
                setForeground(COULEURS_CHIFFRES[nb - 1]);
            } else {
                setText("");
            }
        }
    }

    /**
     * Affiche une case cachée en fin de partie (montre les erreurs et les mines).
     *
     * @param cas la case cachée
     */
    private void afficherCaseFinDePartie(Case cas) {
        // Modification ici : Case.MINE au lieu de Case.Marqueur.MINE
        if (cas.estMinee() && cas.getMarqueur() != Case.MINE) {
            // Mine non marquée : on la montre
            setBackground(COULEUR_MINE);
            setText("\u2605");
            setForeground(Color.WHITE);
        } else if (!cas.estMinee() && cas.getMarqueur() == Case.MINE) {
            // Marqueur \u2605 mais pas de mine : erreur
            setBackground(COULEUR_ERREUR);
            setText("X");
            setForeground(Color.WHITE);
        } else {
            // Cas correct (mine bien marquée ou case safe sans marqueur)
            setBackground(COULEUR_CACHEE);
            afficherMarqueur(cas);
        }
    }

    /**
     * Affiche une case cachée en cours de partie avec son marqueur éventuel.
     *
     * @param cas la case cachée
     */
    private void afficherCaseCachee(Case cas) {
        setBackground(COULEUR_CACHEE);
        afficherMarqueur(cas);
    }

    /**
     * Affiche le symbole correspondant au marqueur de la case.
     *
     * @param cas la case dont on affiche le marqueur
     */
    private void afficherMarqueur(Case cas) {
        // Modification ici : ajout de Case. devant les constantes entières
        switch (cas.getMarqueur()) {
            case Case.MINE:
                setText("\u2605");
                setForeground(Color.RED);
                break;
            case Case.SOUPCON:
                setText("?");
                setForeground(Color.ORANGE);
                break;
            default:
                setText("");
                setForeground(Color.BLACK);
                break;
        }
    }

    /**
     * Gère le clic de la souris sur la case.
     * Clic droit : cycle le marqueur. Clic gauche : révèle la case.
     *
     * @param e l'événement souris
     */
    public void mouseClicked(MouseEvent e) {
        if (fenetreJeu.getPartie().estTerminee()) {
            return;
        }
        Case cas = fenetreJeu.getPartie().getGrille().getCase(ligne, colonne);
        if (cas.estRevelee()) {
            return;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            fenetreJeu.cyclerMarqueur(ligne, colonne);
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            // Modification ici : Case.AUCUN au lieu de Case.Marqueur.AUCUN
            if (cas.getMarqueur() != Case.AUCUN) {
                return; // sécurité : clic gauche ignoré si marqueur présent
            }
            fenetreJeu.jouer(ligne, colonne);
        }
    }

    /**
     * Non utilisé.
     *
     * @param e l'événement souris
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Non utilisé.
     *
     * @param e l'événement souris
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Non utilisé.
     *
     * @param e l'événement souris
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Non utilisé.
     *
     * @param e l'événement souris
     */
    public void mouseExited(MouseEvent e) {
    }
}