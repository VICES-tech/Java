import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Écouteur de fermeture de la fenêtre de jeu.
 * Sauvegarde la partie en cours avant de quitter l'application.
 */
public class EcouteurFermeture extends WindowAdapter {

    /** La fenêtre de jeu associée. */
    private FenetreJeu fenetreJeu;

    /**
     * Crée un écouteur de fermeture pour la fenêtre de jeu donnée.
     *
     * @param fenetreJeu la fenêtre de jeu
     */
    public EcouteurFermeture(FenetreJeu fenetreJeu) {
        this.fenetreJeu = fenetreJeu;
    }

    /**
     * Appelé lors de la demande de fermeture de la fenêtre.
     * Si la partie est en cours, la sauvegarde puis quitte l'application.
     * Si la partie est terminée, quitte sans sauvegarder.
     *
     * @param e l'événement de fenêtre
     */
    public void windowClosing(WindowEvent e) {
        fenetreJeu.sauvegarderEtQuitter();
    }
}
