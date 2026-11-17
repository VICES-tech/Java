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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Qui est connecté, et création des comptes.
 * La connexion et la déconnexion elles-mêmes (/api/connexion, /api/deconnexion)
 * sont gérées par Spring Security, voir ConfigurationSecurite.
 */
@RestController
public class SessionControleur {

    /** Le compte de la personne connectée, tel que l'interface le reçoit. */
    public record CompteVue(Long id, String nom, String role, String couleur, String email,
                            boolean admin, boolean doitChangerMotDePasse) {

        static CompteVue de(Membre membre) {
            return new CompteVue(membre.getId(), membre.getNom(), membre.getRole(), membre.getCouleur(),
                    membre.getEmail(), membre.isAdmin(), membre.isDoitChangerMotDePasse());
        }
    }

    /**
     * État de la session.
     *
     * @param premierCompte          vrai tant qu'aucun compte n'existe : la page de connexion
     *                               propose alors de créer le compte administrateur
     * @param codeInstallationRequis vrai si ce premier compte demande le code d'installation
     */
    public record SessionVue(boolean connecte, boolean premierCompte, boolean codeInstallationRequis,
                             CompteVue membre) {
    }

    /** Données du formulaire d'inscription. */
    public record InscriptionRequete(
            @NotBlank(message = "Le nom est obligatoire")
            @Size(max = 60, message = "Le nom est trop long (60 caractères maximum)") String nom,
            @NotBlank(message = "L'e-mail est obligatoire")
            @Email(message = "Adresse e-mail invalide") String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, max = 64, message = "Le mot de passe doit contenir entre 8 et 64 caractères")
            String motDePasse,
            String role,
            @Pattern(regexp = "^$|#[0-9a-fA-F]{6}", message = "Couleur invalide") String couleur,
            String code) {
    }

    private final MembreDepot membres;
    private final MembreCourant membreCourant;
    private final ServiceComptes comptes;
    private final String codeInstallation;

    public SessionControleur(MembreDepot membres, MembreCourant membreCourant, ServiceComptes comptes,
                             @Value("${planit.code-installation:}") String codeInstallation) {
        this.membres = membres;
        this.membreCourant = membreCourant;
        this.comptes = comptes;
        this.codeInstallation = codeInstallation.trim();
    }

    @GetMapping("/api/session")
    public SessionVue session() {
        Optional<Membre> moi = membreCourant.chercher();
        boolean premierCompte = moi.isEmpty() && !membres.existsByMotDePasseIsNotNull();
        return new SessionVue(moi.isPresent(), premierCompte,
                premierCompte && !codeInstallation.isEmpty(),
                moi.map(CompteVue::de).orElse(null));
    }

    /**
     * Crée un compte depuis la page de connexion.
     * Le tout premier compte devient administrateur du groupe. Ensuite, il faut un
     * code d'invitation, créé par un administrateur.
     */
    @PostMapping("/api/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public synchronized CompteVue inscrire(@Valid @RequestBody InscriptionRequete requete) {
        if (membres.existsByMotDePasseIsNotNull()) {
            if (requete.code() == null || requete.code().isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "L'inscription se fait sur invitation : demande un code à un administrateur");
            }
            return CompteVue.de(comptes.inscrireAvecInvitation(requete.code(), requete.nom(), requete.email(),
                    requete.motDePasse(), requete.role(), requete.couleur()));
        }
        if (!codeInstallation.isEmpty() && !memeTexte(codeInstallation, requete.code())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Code d'installation incorrect");
        }
        Membre admin = comptes.creer(requete.nom(), requete.email(), requete.motDePasse(),
                requete.role(), requete.couleur(), true, false);
        return CompteVue.de(admin);
    }

    /** Compare deux textes sans s'arrêter à la première différence (ne révèle rien par le temps de réponse). */
    private static boolean memeTexte(String attendu, String recu) {
        return recu != null && MessageDigest.isEqual(
                attendu.getBytes(StandardCharsets.UTF_8), recu.trim().getBytes(StandardCharsets.UTF_8));
    }
}
