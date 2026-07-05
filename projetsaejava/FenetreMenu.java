import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Fenêtre principale du menu du Démineur.
 * Propose de démarrer une nouvelle partie, de reprendre une partie sauvegardée
 * ou de quitter l'application.
 */
public class FenetreMenu extends JFrame implements ActionListener {

    /** Bouton pour démarrer une nouvelle partie. */
    private JButton btnNouvelle;
    /** Bouton pour reprendre la partie sauvegardée. */
    private JButton btnReprendre;
    /** Bouton pour quitter l'application. */
    private JButton btnQuitter;

    /**
     * Crée et affiche la fenêtre de menu.
     */
    public FenetreMenu() {
        super("Démineur");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel titre = new JLabel("DÉMINEUR", SwingConstants.CENTER);
        titre.setFont(new Font("SansSerif", Font.BOLD, 40));
        titre.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));
        add(titre, BorderLayout.NORTH);

        JPanel panelBoutons = new JPanel(new GridLayout(3, 1, 10, 10));
        panelBoutons.setBorder(BorderFactory.createEmptyBorder(20, 60, 40, 60));

        btnNouvelle = new JButton("Nouvelle partie");
        btnReprendre = new JButton("Reprendre la partie");
        btnQuitter = new JButton("Quitter");

        btnNouvelle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btnReprendre.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btnQuitter.setFont(new Font("SansSerif", Font.PLAIN, 16));

        btnNouvelle.addActionListener(this);
        btnReprendre.addActionListener(this);
        btnQuitter.addActionListener(this);

        btnReprendre.setEnabled(Partie.sauvegardeExiste());

        panelBoutons.add(btnNouvelle);
        panelBoutons.add(btnReprendre);
        panelBoutons.add(btnQuitter);
        add(panelBoutons, BorderLayout.CENTER);

        setMinimumSize(new Dimension(320, 300));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Gère les clics sur les boutons du menu.
     *
     * @param e l'événement d'action
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnNouvelle) {
            demarrerNouvellePartie();
        } else if (e.getSource() == btnReprendre) {
            reprendrePartie();
        } else if (e.getSource() == btnQuitter) {
            System.exit(0);
        }
    }

    /**
     * Affiche le dialogue de configuration et démarre une nouvelle partie.
     * Redemande les paramètres tant qu'ils sont invalides.
     */
    private void demarrerNouvellePartie() {
        DialogueDimensions dlg = new DialogueDimensions();
        boolean valide = false;
        while (!valide) {
            int choix = JOptionPane.showConfirmDialog(
                    this, dlg, "Nouvelle partie",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (choix != JOptionPane.OK_OPTION) {
                return;
            }
            int l = dlg.getLignes();
            int c = dlg.getColonnes();
            int m = dlg.getNbMines();
            if (l == -1 || c == -1) {
                JOptionPane.showMessageDialog(this,
                        "Les dimensions doivent être entre 4 et 30.",
                        "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
            } else if (m < 1 || m >= l * c) {
                JOptionPane.showMessageDialog(this,
                        "Le nombre de mines doit être entre 1 et " + (l * c - 1) + ".",
                        "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
            } else {
                valide = true;
                Partie.supprimerSauvegarde();
                Partie partie = new Partie(l, c, m);
                setVisible(false);
                new FenetreJeu(partie, this);
            }
        }
    }

    /**
     * Charge la partie sauvegardée et ouvre la fenêtre de jeu.
     */
    private void reprendrePartie() {
        Partie partie = Partie.charger();
        if (partie == null) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de charger la sauvegarde.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            btnReprendre.setEnabled(false);
        } else {
            setVisible(false);
            new FenetreJeu(partie, this);
        }
    }

    /**
     * Met à jour l'état du bouton "Reprendre" selon la présence d'une sauvegarde.
     * Appelé par FenetreJeu quand elle retourne au menu.
     */
    public void actualiserBoutons() {
        btnReprendre.setEnabled(Partie.sauvegardeExiste());
    }
}
