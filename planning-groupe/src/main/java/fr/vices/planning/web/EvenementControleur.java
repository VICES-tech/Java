package fr.vices.planning.web;

import fr.vices.planning.depot.ChantDepot;
import fr.vices.planning.depot.EvenementDepot;
import fr.vices.planning.depot.MembreDepot;
import fr.vices.planning.modele.Evenement;
import fr.vices.planning.modele.TypeEvenement;
import fr.vices.planning.securite.MembreCourant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API du planning : /api/evenements
 * Tout le monde peut ajouter un événement et s'y inscrire ; seuls les administrateurs
 * le modifient ou le suppriment.
 */
@RestController
@RequestMapping("/api/evenements")
public class EvenementControleur {

    /** Données envoyées par l'interface pour créer ou modifier un événement. */
    public record EvenementRequete(
            @NotBlank String titre,
            TypeEvenement type,
            @NotNull LocalDateTime debut,
            @NotNull LocalDateTime fin,
            String lieu,
            String description,
            List<Long> participantIds,
            List<Long> chantIds) {
    }

    private final EvenementDepot evenements;
    private final MembreDepot membres;
    private final ChantDepot chants;
    private final MembreCourant membreCourant;

    public EvenementControleur(EvenementDepot evenements, MembreDepot membres, ChantDepot chants,
                               MembreCourant membreCourant) {
        this.evenements = evenements;
        this.membres = membres;
        this.chants = chants;
        this.membreCourant = membreCourant;
    }

    /** Liste les événements entre deux dates (incluses), ex. ?du=2026-10-12&amp;au=2026-10-18 */
    @GetMapping
    public List<Evenement> lister(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return evenements.findByDebutLessThanAndFinGreaterThanOrderByDebutAsc(
                au.plusDays(1).atStartOfDay(), du.atStartOfDay());
    }

    @GetMapping("/{id}")
    public Evenement lire(@PathVariable Long id) {
        return trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Evenement creer(@Valid @RequestBody EvenementRequete requete) {
        Evenement evenement = new Evenement();
        remplir(evenement, requete);
        return evenements.save(evenement);
    }

    @PutMapping("/{id}")
    @Transactional
    public Evenement modifier(@PathVariable Long id, @Valid @RequestBody EvenementRequete requete) {
        membreCourant.exigerAdmin();
        Evenement evenement = trouver(id);
        remplir(evenement, requete);
        return evenement;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        membreCourant.exigerAdmin();
        evenements.delete(trouver(id));
    }

    /** Le membre connecté s'inscrit à l'événement (« Je participe »). */
    @PostMapping("/{id}/participation")
    @Transactional
    public Evenement participer(@PathVariable Long id) {
        Evenement evenement = trouver(id);
        evenement.getParticipants().add(membreCourant.exiger());
        return evenement;
    }

    /** Le membre connecté se désinscrit de l'événement. */
    @DeleteMapping("/{id}/participation")
    @Transactional
    public Evenement seRetirer(@PathVariable Long id) {
        Evenement evenement = trouver(id);
        evenement.getParticipants().remove(membreCourant.exiger());
        return evenement;
    }

    private void remplir(Evenement evenement, EvenementRequete requete) {
        if (!requete.fin().isAfter(requete.debut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fin doit être après le début");
        }
        evenement.setTitre(requete.titre().trim());
        evenement.setType(requete.type() != null ? requete.type() : TypeEvenement.AUTRE);
        evenement.setDebut(requete.debut());
        evenement.setFin(requete.fin());
        evenement.setLieu(requete.lieu());
        evenement.setDescription(requete.description());

        evenement.getParticipants().clear();
        if (requete.participantIds() != null) {
            evenement.getParticipants().addAll(membres.findAllById(requete.participantIds()));
        }
        evenement.getChants().clear();
        if (requete.chantIds() != null) {
            for (Long chantId : requete.chantIds()) {     // garde l'ordre choisi
                chants.findById(chantId).ifPresent(evenement.getChants()::add);
            }
        }
    }

    private Evenement trouver(Long id) {
        return evenements.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
    }
}
