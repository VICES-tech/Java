package fr.vices.planning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application « Planning de groupe ».
 * Démarre le serveur web qui sert l'API REST et l'interface (dossier static).
 */
@SpringBootApplication
public class PlanningGroupeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanningGroupeApplication.class, args);
    }
}
