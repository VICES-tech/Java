package fr.vices.planning.web;

import fr.vices.planning.modele.Membre;
import fr.vices.planning.securite.MembreCourant;
import fr.vices.planning.securite.ServiceComptes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Ce que chaque membre peut faire sur son propre compte : /api/compte */
@RestController
@RequestMapping("/api/compte")
public class CompteControleur {

    public record ChangementMotDePasse(
            @NotBlank(message = "Le mot de passe actuel est obligatoire") String ancien,
            @NotBlank(message = "Le nouveau mot de passe est obligatoire")
            @Size(min = 8, max = 64, message = "Le mot de passe doit contenir entre 8 et 64 caractères")
            String nouveau) {
    }

    public record SuppressionCompte(@NotBlank(message = "Le mot de passe est obligatoire") String motDePasse) {
    }

    private final MembreCourant membreCourant;
    private final ServiceComptes comptes;

    public CompteControleur(MembreCourant membreCourant, ServiceComptes comptes) {
        this.membreCourant = membreCourant;
        this.comptes = comptes;
    }

    @PostMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void changerMotDePasse(@Valid @RequestBody ChangementMotDePasse requete) {
        Membre moi = membreCourant.exiger();
        verifierMotDePasse(moi, requete.ancien());
        comptes.definirMotDePasse(moi, requete.nouveau(), false);
    }

    /** Supprime son propre compte, après avoir redonné son mot de passe, puis déconnecte. */
    @PostMapping("/suppression")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void supprimerMonCompte(@Valid @RequestBody SuppressionCompte requete, HttpServletRequest http) {
        Membre moi = membreCourant.exiger();
        verifierMotDePasse(moi, requete.motDePasse());
        comptes.supprimer(moi);

        HttpSession session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private void verifierMotDePasse(Membre membre, String motDePasse) {
        if (!comptes.motDePasseCorrect(membre, motDePasse)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mot de passe incorrect");
        }
    }
}
