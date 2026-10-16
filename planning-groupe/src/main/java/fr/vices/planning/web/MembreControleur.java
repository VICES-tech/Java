package fr.vices.planning.web;

import fr.vices.planning.depot.EvenementDepot;
import fr.vices.planning.depot.MembreDepot;
import fr.vices.planning.modele.Evenement;
import fr.vices.planning.modele.Membre;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

/** API des membres du groupe : /api/membres */
@RestController
@RequestMapping("/api/membres")
public class MembreControleur {

    /** Données envoyées par l'interface pour créer ou modifier un membre. */
    public record MembreRequete(
            @NotBlank String nom,
            String role,
            @Pattern(regexp = "^$|#[0-9a-fA-F]{6}") String couleur) {
    }

    private final MembreDepot membres;
    private final EvenementDepot evenements;

    public MembreControleur(MembreDepot membres, EvenementDepot evenements) {
        this.membres = membres;
        this.evenements = evenements;
    }

    @GetMapping
    public List<Membre> lister() {
        return membres.findAllByOrderByNomAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Membre creer(@Valid @RequestBody MembreRequete requete) {
        String nom = requete.nom().trim();
        if (membres.existsByNomIgnoreCase(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom est déjà pris : " + nom);
        }
        return membres.save(new Membre(nom, requete.role(), requete.couleur()));
    }

    @PutMapping("/{id}")
    @Transactional
    public Membre modifier(@PathVariable Long id, @Valid @RequestBody MembreRequete requete) {
        Membre membre = trouver(id);
        String nom = requete.nom().trim();
        if (!membre.getNom().equalsIgnoreCase(nom) && membres.existsByNomIgnoreCase(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom est déjà pris : " + nom);
        }
        membre.setNom(nom);
        membre.setRole(requete.role());
        membre.setCouleur(requete.couleur());
        return membre;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void supprimer(@PathVariable Long id) {
        Membre membre = trouver(id);
        for (Evenement e : evenements.findByParticipantsContaining(membre)) {
            e.getParticipants().remove(membre);
        }
        membres.delete(membre);
    }

    private Membre trouver(Long id) {
        return membres.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
    }
}
