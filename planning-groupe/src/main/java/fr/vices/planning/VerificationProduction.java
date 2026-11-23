package fr.vices.planning;

import java.util.List;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * En production, vérifie avant tout le reste que les réglages secrets sont bien fournis
 * par des variables d'environnement, et sinon s'arrête avec un message clair qui les nomme.
 */
public class VerificationProduction implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /** Variables d'environnement obligatoires en production (voir application-prod.properties). */
    static final List<String> VARIABLES_OBLIGATOIRES =
            List.of("PLANIT_DB_URL", "PLANIT_DB_USER", "PLANIT_DB_PASSWORD", "PLANIT_CODE_INSTALLATION");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evenement) {
        verifier(evenement.getEnvironment());
    }

    static void verifier(ConfigurableEnvironment environnement) {
        if (!environnement.matchesProfiles("prod")) {
            return;
        }
        List<String> manquantes = VARIABLES_OBLIGATOIRES.stream()
                .filter(nom -> {
                    String valeur = environnement.getProperty(nom);
                    return valeur == null || valeur.isBlank();
                })
                .toList();
        if (!manquantes.isEmpty()) {
            throw new IllegalStateException("Démarrage en production impossible : variable(s) d'environnement "
                    + "manquante(s) : " + String.join(", ", manquantes)
                    + ". Voir application-prod.properties.");
        }
    }
}
