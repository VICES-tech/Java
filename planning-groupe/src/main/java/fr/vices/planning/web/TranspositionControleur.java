package fr.vices.planning.web;

import fr.vices.planning.musique.Transposeur;
import fr.vices.planning.musique.Transposeur.Notation;
import fr.vices.planning.musique.Transposeur.Resultat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de transposition : /api/transposition
 * Fonctionne pour n'importe quel texte, enregistré ou non dans le répertoire.
 */
@RestController
@RequestMapping("/api/transposition")
public class TranspositionControleur {

    /**
     * Demande de transposition. Préciser soit {@code demiTons}, soit {@code tonaliteCible}.
     */
    public record TranspositionRequete(
            String texte,
            Integer demiTons,
            String tonaliteSource,
            String tonaliteCible,
            Notation notation) {
    }

    @PostMapping
    public Resultat transposer(@RequestBody TranspositionRequete requete) {
        String texte = requete.texte() != null ? requete.texte() : "";
        Notation notation = requete.notation() != null ? requete.notation() : Notation.AUTO;
        try {
            if (requete.tonaliteCible() != null && !requete.tonaliteCible().isBlank()) {
                return Transposeur.transposerVers(texte, requete.tonaliteSource(),
                        requete.tonaliteCible(), notation);
            }
            int demiTons = requete.demiTons() != null ? requete.demiTons() : 0;
            return Transposeur.transposer(texte, demiTons, notation, requete.tonaliteSource());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
