package fr.vices.planning.web;

import fr.vices.planning.depot.MembreDepot;
import fr.vices.planning.modele.Membre;
import fr.vices.planning.securite.MembreCourant;
import fr.vices.planning.securite.ServiceComptes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API des membres du groupe : /api/membres
 * Tout le monde voit la liste ; seuls les administrateurs créent, suppriment
 * ou réinitialisent un compte. Chacun peut modifier sa propre fiche.
 */
@RestController
@RequestMapping("/api/membres")
public class MembreControleur {

    /** Un membre tel que l'interface le reçoit. L'e-mail est vide quand on n'a pas le droit de le voir. */
    public record MembreVue(Long id, String nom, String role, String couleur, boolean admin, String email) {

        static MembreVue de(Membre membre, boolean avecEmail) {
            return new MembreVue(membre.getId(), membre.getNom(), membre.getRole(), membre.getCouleur(),
                    membre.isAdmin(), avecEmail ? membre.getEmail() : null);
        }
    }

    /** Création d'un compte par un administrateur : le mot de passe provisoire est fabriqué par le serveur. */
    public record NouveauMembreRequete(
            @NotBlank(message = "Le nom est obligatoire")
            @Size(max = 60, message = "Le nom est trop long (60 caractères maximum)") String nom,
            @NotBlank(message = "L'e-mail est obligatoire")
            @Email(message = "Adresse e-mail invalide") String email,
            String role,
            @Pattern(regexp = "^$|#[0-9a-fA-F]{6}", message = "Couleur invalide") String couleur) {
    }

    /** Réponse à la création ou à la réinitialisation : le mot de passe provisoire n'est montré qu'ici. */
    public record CompteCree(MembreVue membre, String motDePasseProvisoire) {
    }

    /** Modification de la fiche d'un membre. L'e-mail est facultatif : absent, il ne change pas. */
    public record MembreRequete(
            @NotBlank(message = "Le nom est obligatoire")
            @Size(max = 60, message = "Le nom est trop long (60 caractères maximum)") String nom,
            String role,
            @Pattern(regexp = "^$|#[0-9a-fA-F]{6}", message = "Couleur invalide") String couleur,
            @Email(message = "Adresse e-mail invalide") String email) {
    }

    public record DroitAdmin(boolean admin) {
    }

    private final MembreDepot membres;
    private final MembreCourant membreCourant;
    private final ServiceComptes comptes;

    public MembreControleur(MembreDepot membres, MembreCourant membreCourant, ServiceComptes comptes) {
        this.membres = membres;
        this.membreCourant = membreCourant;
        this.comptes = comptes;
    }

    @GetMapping
    public List<MembreVue> lister() {
        Membre moi = membreCourant.exiger();
        return membres.findAllByOrderByNomAsc().stream()
                .map(membre -> MembreVue.de(membre, peutVoirEmail(moi, membre)))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CompteCree creer(@Valid @RequestBody NouveauMembreRequete requete) {
        membreCourant.exigerAdmin();
        String motDePasse = comptes.motDePasseProvisoire();
        Membre membre = comptes.creer(requete.nom(), requete.email(), motDePasse,
                requete.role(), requete.couleur(), false, true);
        return new CompteCree(MembreVue.de(membre, true), motDePasse);
    }

    @PutMapping("/{id}")
    @Transactional
    public MembreVue modifier(@PathVariable Long id, @Valid @RequestBody MembreRequete requete) {
        Membre moi = membreCourant.exiger();
        Membre membre = trouver(id);
        if (!moi.isAdmin() && !moi.getId().equals(membre.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu ne peux modifier que ta propre fiche");
        }
        String nom = requete.nom().trim();
        if (!membre.getNom().equalsIgnoreCase(nom) && membres.existsByNomIgnoreCase(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom est déjà pris : " + nom);
        }
        membre.setNom(nom);
        membre.setRole(requete.role());
        membre.setCouleur(requete.couleur());

        String email = ServiceComptes.normaliserEmail(requete.email());
        if (!email.isEmpty() && !email.equals(membre.getEmail())) {
            if (membres.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet e-mail");
            }
            membre.setEmail(email);
        }
        return MembreVue.de(membre, true);
    }

    /** Donne ou retire le droit d'administrateur. */
    @PutMapping("/{id}/admin")
    @Transactional
    public MembreVue changerAdmin(@PathVariable Long id, @RequestBody DroitAdmin requete) {
        membreCourant.exigerAdmin();
        Membre membre = trouver(id);
        comptes.changerAdmin(membre, requete.admin());
        return MembreVue.de(membre, true);
    }

    /** Pour un membre qui a oublié son mot de passe : l'administrateur lui en donne un provisoire. */
    @PostMapping("/{id}/mot-de-passe")
    @Transactional
    public CompteCree reinitialiserMotDePasse(@PathVariable Long id) {
        Membre moi = membreCourant.exigerAdmin();
        Membre membre = trouver(id);
        if (moi.getId().equals(membre.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pour ton propre mot de passe, passe par « Mon compte »");
        }
        if (membre.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce membre n'a pas d'e-mail : ajoute-le d'abord à sa fiche");
        }
        String motDePasse = comptes.motDePasseProvisoire();
        comptes.definirMotDePasse(membre, motDePasse, true);
        return new CompteCree(MembreVue.de(membre, true), motDePasse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void supprimer(@PathVariable Long id) {
        Membre moi = membreCourant.exigerAdmin();
        Membre membre = trouver(id);
        if (moi.getId().equals(membre.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pour supprimer ton propre compte, passe par « Mon compte »");
        }
        comptes.supprimer(membre);
    }

    private static boolean peutVoirEmail(Membre moi, Membre membre) {
        return moi.isAdmin() || moi.getId().equals(membre.getId());
    }

    private Membre trouver(Long id) {
        return membres.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
    }
}
