import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Panneau de dialogue pour configurer une nouvelle partie du Démineur.
 * Permet au joueur de saisir le nombre de lignes, de colonnes et de mines.
 */
public class DialogueDimensions extends JPanel {

    /** Champ de saisie pour le nombre de lignes. */
    private JTextField champLignes;
    /** Champ de saisie pour le nombre de colonnes. */
    private JTextField champColonnes;
    /** Champ de saisie pour le nombre de mines. */
    private JTextField champMines;

    /** Valeur minimale acceptable pour les dimensions. */
    public static final int MIN_DIM = 4;
    /** Valeur maximale acceptable pour les dimensions. */
    public static final int MAX_DIM = 30;

    /**
     * Crée le panneau de configuration avec des valeurs par défaut (10x10, 15 mines).
     */
    public DialogueDimensions() {
        setLayout(new GridLayout(3, 2, 5, 5));
        champLignes = new JTextField("10");
        champColonnes = new JTextField("10");
        champMines = new JTextField("15");
        add(new JLabel("Nombre de lignes (4-30) :"));
        add(champLignes);
        add(new JLabel("Nombre de colonnes (4-30) :"));
        add(champColonnes);
        add(new JLabel("Nombre de mines :"));
        add(champMines);
    }

    /**
     * Retourne le nombre de lignes saisi et validé.
     *
     * @return le nombre de lignes, ou -1 si la valeur est invalide
     */
    public int getLignes() {
        try {
            int v = Integer.parseInt(champLignes.getText().trim());
            return (v >= MIN_DIM && v <= MAX_DIM) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Retourne le nombre de colonnes saisi et validé.
     *
     * @return le nombre de colonnes, ou -1 si la valeur est invalide
     */
    public int getColonnes() {
        try {
            int v = Integer.parseInt(champColonnes.getText().trim());
            return (v >= MIN_DIM && v <= MAX_DIM) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Retourne le nombre de mines saisi.
     *
     * @return le nombre de mines, ou -1 si la saisie n'est pas un entier
     */
    public int getNbMines() {
        try {
            return Integer.parseInt(champMines.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
