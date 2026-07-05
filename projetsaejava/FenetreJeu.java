import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Fenêtre principale du jeu Démineur.
 * Affiche la grille, le compteur de mines restantes,
 * l'état de la partie et le bouton d'action.
 */
public class FenetreJeu extends JFrame implements ActionListener {

    /** La partie en cours. */
    private Partie partie;
    /** La fenêtre de menu, pour y retourner en fin de partie. */
    private FenetreMenu fenetreMenu;
    /** La vue graphique de la grille. */
    private VueGrille vueGrille;
    /** Étiquette affichant le nombre de mines restantes (mines - marqueurs \u2605). */
    private JLabel labelMines;
    /** Étiquette affichant l'état de la partie (victoire / défaite). */
    private JLabel labelEtat;
    /** Bouton "Sauvegarder et Quitter" ou "Retour au menu" selon l'état. */
    private JButton btnAction;

    /**
     * Crée et affiche la fenêtre de jeu pour la partie donnée.
     *
     * @param partie      la partie à jouer ou à reprendre
     * @param fenetreMenu la fenêtre de menu parente
     */
    public FenetreJeu(Partie partie, FenetreMenu fenetreMenu) {
        super("Démineur");
        this.partie = partie;
        this.fenetreMenu = fenetreMenu;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new EcouteurFermeture(this));
        setLayout(new BorderLayout(5, 5));

        // --- Panneau nord : compteur et état ---
        JPanel panelNord = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panelNord.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        JLabel labelLibelle = new JLabel("Mines restantes :");
        labelLibelle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        labelMines = new JLabel("0");
        labelMines.setFont(new Font("SansSerif", Font.BOLD, 16));
        labelEtat = new JLabel("");
        labelEtat.setFont(new Font("SansSerif", Font.BOLD, 15));
        panelNord.add(labelLibelle);
        panelNord.add(labelMines);
        panelNord.add(labelEtat);
        add(panelNord, BorderLayout.NORTH);

        // --- Centre : grille avec défilement ---
        vueGrille = new VueGrille(partie, this);
        JScrollPane scrollPane = new JScrollPane(vueGrille);
        add(scrollPane, BorderLayout.CENTER);

        // --- Panneau sud : bouton action ---
        JPanel panelSud = new JPanel(new FlowLayout());
        btnAction = new JButton("sauvegarder et Quitter");
        btnAction.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnAction.addActionListener(this);
        panelSud.add(btnAction);
        add(panelSud, BorderLayout.SOUTH);

        actualiserAffichage();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Retourne la partie en cours.
     *
     * @return la partie
     */
    public Partie getPartie() {
        return partie;
    }

    /**
     * Met à jour le compteur de mines et la grille, gère la fin de partie.
     */
    public void actualiserAffichage() {
        int restantes = partie.getGrille().getNbMines()
                - partie.getGrille().compterMarqueursMine();
        labelMines.setText(String.valueOf(restantes));
        vueGrille.actualiser(partie);
        if (partie.estTerminee()) {
            afficherFinDePartie();
        }
    }

    /**
     * Met à jour l'interface pour indiquer la fin de la partie.
     * Modifie l'étiquette d'état, le texte du bouton et supprime la sauvegarde.
     */
    private void afficherFinDePartie() {
        if (partie.estGagnee()) {
            labelEtat.setText("  \u2714 Victoire !");
            labelEtat.setForeground(new Color(0, 140, 0));
        } else {
            labelEtat.setText("  \u2718 Défaite !");
            labelEtat.setForeground(new Color(180, 0, 0));
        }
        btnAction.setText("Retour au menu");
        Partie.supprimerSauvegarde();
    }

    /**
     * Révèle la case (l, c) et met à jour l'affichage.
     *
     * @param l la ligne
     * @param c la colonne
     */
    public void jouer(int l, int c) {
        partie.jouer(l, c);
        actualiserAffichage();
    }

    /**
     * Fait tourner le marqueur de la case (l, c) et met à jour l'affichage.
     *
     * @param l la ligne
     * @param c la colonne
     */
    public void cyclerMarqueur(int l, int c) {
        partie.getGrille().cyclerMarqueur(l, c);
        actualiserAffichage();
    }

    /**
     * Sauvegarde la partie en cours et quitte l'application.
     * Si la partie est terminée, quitte sans sauvegarder.
     */
    public void sauvegarderEtQuitter() {
        if (!partie.estTerminee()) {
            try {
                partie.sauvegarder();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Erreur lors de la sauvegarde : " + ex.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
        System.exit(0);
    }

    /**
     * Gère les clics sur le bouton d'action de la fenêtre.
     * Soit sauvegarde et quitte, soit retourne au menu selon l'état.
     *
     * @param e l'événement d'action
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAction) {
            if (partie.estTerminee()) {
                retourAuMenu();
            } else {
                sauvegarderEtQuitter();
            }
        }
    }

    /**
     * Ferme la fenêtre de jeu et retourne à la fenêtre de menu.
     */
    private void retourAuMenu() {
        dispose();
        fenetreMenu.actualiserBoutons();
        fenetreMenu.setVisible(true);
    }
}
