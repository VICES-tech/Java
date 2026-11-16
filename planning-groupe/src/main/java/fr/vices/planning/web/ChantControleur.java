package fr.vices.planning.web;

import fr.vices.planning.depot.ChantDepot;
import fr.vices.planning.depot.EvenementDepot;
import fr.vices.planning.depot.PieceJointeDepot;
import fr.vices.planning.fichiers.StockageFichiers;
import fr.vices.planning.modele.Chant;
import fr.vices.planning.modele.Evenement;
import fr.vices.planning.modele.PieceJointe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

/** API du répertoire de chants : /api/chants */
@RestController
@RequestMapping("/api/chants")
public class ChantControleur {

    /** Données envoyées par l'interface pour créer ou modifier un chant. */
    public record ChantRequete(@NotBlank String titre, String artiste, String tonalite, Integer tempo,
                               String contenu) {
    }

    private final ChantDepot chants;
    private final EvenementDepot evenements;
    private final PieceJointeDepot piecesJointes;
    private final StockageFichiers stockage;

    public ChantControleur(ChantDepot chants, EvenementDepot evenements,
                           PieceJointeDepot piecesJointes, StockageFichiers stockage) {
        this.chants = chants;
        this.evenements = evenements;
        this.piecesJointes = piecesJointes;
        this.stockage = stockage;
    }

    @GetMapping
    public List<Chant> lister() {
        return chants.findAllByOrderByTitreAsc();
    }

    @GetMapping("/{id}")
    public Chant lire(@PathVariable Long id) {
        return trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Chant creer(@Valid @RequestBody ChantRequete requete) {
        Chant chant = new Chant(requete.titre().trim(), requete.artiste(),
                requete.tonalite(), requete.contenu());
        chant.setTempo(requete.tempo());
        return chants.save(chant);
    }

    @PutMapping("/{id}")
    @Transactional
    public Chant modifier(@PathVariable Long id, @Valid @RequestBody ChantRequete requete) {
        Chant chant = trouver(id);
        chant.setTitre(requete.titre().trim());
        chant.setArtiste(requete.artiste());
        chant.setTonalite(requete.tonalite());
        chant.setTempo(requete.tempo());
        chant.setContenu(requete.contenu());
        return chant;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void supprimer(@PathVariable Long id) {
        Chant chant = trouver(id);
        for (Evenement e : evenements.findByChantsContaining(chant)) {
            e.getChants().remove(chant);
        }
        List<PieceJointe> fichiers = piecesJointes.findByChantOrderByAjouteLeAsc(chant);
        piecesJointes.deleteAll(fichiers);
        chants.delete(chant);
        fichiers.forEach(pj -> stockage.supprimer(pj.getNomStocke()));
    }

    private Chant trouver(Long id) {
        return chants.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Chant introuvable"));
    }
}
