package fr.vices.planning.fichiers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Range les fichiers envoyés dans un dossier du disque.
 * Chaque fichier reçoit un nom aléatoire : impossible d'écraser un autre fichier
 * ou de sortir du dossier, même avec un nom d'origine piégé.
 */
@Component
public class StockageFichiers {

    private final Path dossier;

    public StockageFichiers(@Value("${planning.dossier-fichiers}") String dossier) {
        this.dossier = Path.of(dossier).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.dossier);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier " + this.dossier, e);
        }
    }

    /** Enregistre le contenu et renvoie le nom choisi pour le stocker. */
    public String enregistrer(InputStream contenu) throws IOException {
        String nomStocke = UUID.randomUUID().toString();
        Files.copy(contenu, chemin(nomStocke), StandardCopyOption.REPLACE_EXISTING);
        return nomStocke;
    }

    public Path chemin(String nomStocke) {
        return dossier.resolve(nomStocke);
    }

    public void supprimer(String nomStocke) {
        try {
            Files.deleteIfExists(chemin(nomStocke));
        } catch (IOException e) {
            // le fichier reste sur le disque, mais l'application continue de fonctionner
        }
    }
}
