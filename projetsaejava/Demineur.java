import javax.swing.SwingUtilities;

/**
 * Classe principale du jeu Démineur.
 * Lance l'interface graphique sur le fil de dispatch Swing.
 * Implémente Runnable pour être passée à SwingUtilities.invokeLater.
 */
public class Demineur implements Runnable {

    /**
     * Point d'entrée de l'application.
     * Lance la création de l'interface sur le fil de dispatch Swing.
     *
     * @param args les arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Demineur());
    }

    /**
     * Crée et affiche la fenêtre de menu principale.
     */
    public void run() {
        new FenetreMenu();
    }
}
