package fr.vices.planning.securite;

import fr.vices.planning.depot.MembreDepot;
import fr.vices.planning.modele.Membre;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Donne le membre connecté qui fait la demande en cours.
 * Le membre est relu dans la base à chaque fois : un compte supprimé ou un
 * droit d'administrateur retiré est donc pris en compte tout de suite.
 */
@Component
public class MembreCourant {

    private final MembreDepot membres;

    public MembreCourant(MembreDepot membres) {
        this.membres = membres;
    }

    /** Le membre connecté, s'il y en a un. */
    public Optional<Membre> chercher() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification != null && authentification.getPrincipal() instanceof CompteConnecte compte) {
            return membres.findById(compte.getId());
        }
        return Optional.empty();
    }

    /** Le membre connecté, ou une erreur 401 si personne n'est connecté. */
    public Membre exiger() {
        return chercher().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise"));
    }

    /** Le membre connecté s'il est administrateur, sinon une erreur 403. */
    public Membre exigerAdmin() {
        Membre membre = exiger();
        if (!membre.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux administrateurs");
        }
        return membre;
    }
}
