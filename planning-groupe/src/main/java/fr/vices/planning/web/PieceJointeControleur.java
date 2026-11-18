package fr.vices.planning.web;

import fr.vices.planning.depot.ChantDepot;
import fr.vices.planning.depot.PieceJointeDepot;
import fr.vices.planning.fichiers.StockageFichiers;
import fr.vices.planning.modele.Chant;
import fr.vices.planning.modele.PieceJointe;
import fr.vices.planning.securite.MembreCourant;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * API des pièces jointes des chants (audio, vidéo, PDF, images, ou tout autre fichier).
 */
@RestController
public class PieceJointeControleur {

    private final PieceJointeDepot piecesJointes;
    private final ChantDepot chants;
    private final StockageFichiers stockage;

    private final MembreCourant membreCourant;

    public PieceJointeControleur(PieceJointeDepot piecesJointes, ChantDepot chants, StockageFichiers stockage,
                                 MembreCourant membreCourant) {
        this.piecesJointes = piecesJointes;
        this.chants = chants;
        this.stockage = stockage;
        this.membreCourant = membreCourant;
    }

    @GetMapping("/api/chants/{chantId}/pieces-jointes")
    public List<PieceJointe> lister(@PathVariable Long chantId) {
        return piecesJointes.findByChantOrderByAjouteLeAsc(trouverChant(chantId));
    }

    /** Ajoute un fichier à un chant (formulaire « multipart », champ « fichier »). */
    @PostMapping("/api/chants/{chantId}/pieces-jointes")
    @ResponseStatus(HttpStatus.CREATED)
    public PieceJointe ajouter(@PathVariable Long chantId, @RequestParam("fichier") MultipartFile fichier)
            throws IOException {
        Chant chant = trouverChant(chantId);
        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est vide");
        }
        String nom = fichier.getOriginalFilename() != null && !fichier.getOriginalFilename().isBlank()
                ? fichier.getOriginalFilename()
                : "fichier";
        String type = fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream";

        String nomStocke;
        try (InputStream contenu = fichier.getInputStream()) {
            nomStocke = stockage.enregistrer(contenu);
        }
        try {
            return piecesJointes.save(new PieceJointe(chant, nom, type, fichier.getSize(), nomStocke));
        } catch (RuntimeException e) {
            stockage.supprimer(nomStocke);   // pas de fichier orphelin sur le disque
            throw e;
        }
    }

    /**
     * Renvoie le fichier. Les sons, vidéos, images et PDF s'ouvrent directement dans le navigateur
     * (on peut avancer dans un morceau) ; les autres types sont proposés en téléchargement.
     */
    @GetMapping("/api/pieces-jointes/{id}")
    public ResponseEntity<Resource> telecharger(@PathVariable Long id) {
        PieceJointe pj = trouver(id);
        Resource fichier = new FileSystemResource(stockage.chemin(pj.getNomStocke()));
        if (!fichier.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier absent du serveur");
        }
        MediaType type = MediaType.APPLICATION_OCTET_STREAM;
        boolean affichable = false;
        if (estAffichable(pj.getType())) {
            try {
                type = MediaType.parseMediaType(pj.getType());
                affichable = true;
            } catch (IllegalArgumentException e) {
                // type mal formé : on propose simplement le téléchargement
            }
        }
        ContentDisposition disposition = (affichable ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(pj.getNom(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(fichier);
    }

    @DeleteMapping("/api/pieces-jointes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        membreCourant.exigerAdmin();
        PieceJointe pj = trouver(id);
        piecesJointes.delete(pj);
        stockage.supprimer(pj.getNomStocke());
    }

    /**
     * Seuls ces types sont affichés dans le navigateur. Un fichier HTML ou SVG ouvert
     * directement pourrait exécuter du code : il est donc toujours téléchargé.
     */
    static boolean estAffichable(String type) {
        if (type == null) {
            return false;
        }
        String t = type.toLowerCase();
        return t.startsWith("audio/") || t.startsWith("video/") || t.equals("application/pdf")
                || t.equals("image/jpeg") || t.equals("image/png") || t.equals("image/gif")
                || t.equals("image/webp");
    }

    private Chant trouverChant(Long id) {
        return chants.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Chant introuvable"));
    }

    private PieceJointe trouver(Long id) {
        return piecesJointes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
    }
}
