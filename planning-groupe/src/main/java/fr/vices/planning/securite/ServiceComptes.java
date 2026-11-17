package fr.vices.planning.securite;

import fr.vices.planning.depot.EvenementDepot;
import fr.vices.planning.depot.InvitationDepot;
import fr.vices.planning.depot.MembreDepot;
import fr.vices.planning.modele.Evenement;
import fr.vices.planning.modele.Invitation;
import fr.vices.planning.modele.Membre;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tout ce qui touche aux comptes : création, mot de passe, suppression.
 * Spring Security s'en sert aussi pour retrouver un compte au moment de la connexion.
 */
@Service
public class ServiceComptes implements UserDetailsService {

    /** Lettres et chiffres faciles à lire et à dicter (pas de 0/O, 1/l/i). */
    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    /** Durée de validité d'une invitation. */
    public static final int JOURS_VALIDITE_INVITATION = 7;

    private final MembreDepot membres;
    private final EvenementDepot evenements;
    private final InvitationDepot invitations;
    private final PasswordEncoder encodeur;
    private final SecureRandom hasard = new SecureRandom();

    public ServiceComptes(MembreDepot membres, EvenementDepot evenements, InvitationDepot invitations,
                          PasswordEncoder encodeur) {
        this.membres = membres;
        this.evenements = evenements;
        this.invitations = invitations;
        this.encodeur = encodeur;
    }

    /** Appelé par Spring Security à chaque tentative de connexion. */
    @Override
    public UserDetails loadUserByUsername(String email) {
        return membres.findByEmail(normaliserEmail(email))
                .filter(membre -> membre.getMotDePasse() != null)
                .map(CompteConnecte::new)
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
    }

    /** Un e-mail s'écrit toujours de la même façon en base : sans espaces, en minuscules. */
    public static String normaliserEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Crée un compte.
     *
     * @param provisoire vrai si le mot de passe a été choisi par quelqu'un d'autre :
     *                   le membre devra alors le changer à sa première connexion
     */
    @Transactional
    public Membre creer(String nom, String email, String motDePasse, String role, String couleur,
                        boolean admin, boolean provisoire) {
        String nomPropre = nom.trim();
        String emailPropre = normaliserEmail(email);
        if (membres.existsByNomIgnoreCase(nomPropre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom est déjà pris : " + nomPropre);
        }
        if (membres.existsByEmail(emailPropre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet e-mail");
        }
        Membre membre = new Membre(nomPropre, role, couleur);
        membre.setEmail(emailPropre);
        membre.setAdmin(admin);
        definirMotDePasse(membre, motDePasse, provisoire);
        return membres.save(membre);
    }

    /** Chiffre le mot de passe et l'enregistre dans le compte. */
    public void definirMotDePasse(Membre membre, String motDePasse, boolean provisoire) {
        membre.setMotDePasse(encodeur.encode(motDePasse));
        membre.setDoitChangerMotDePasse(provisoire);
    }

    public boolean motDePasseCorrect(Membre membre, String motDePasse) {
        return membre.getMotDePasse() != null && motDePasse != null
                && encodeur.matches(motDePasse, membre.getMotDePasse());
    }

    /** Fabrique un mot de passe provisoire au hasard, de la forme « k7mq-x2pd-9fhw ». */
    public String motDePasseProvisoire() {
        return codeAleatoire();
    }

    /** Crée une invitation valable {@value #JOURS_VALIDITE_INVITATION} jours. */
    @Transactional
    public Invitation creerInvitation() {
        String code;
        do {
            code = codeAleatoire();
        } while (invitations.findByCode(code).isPresent());
        return invitations.save(new Invitation(code, LocalDateTime.now().plusDays(JOURS_VALIDITE_INVITATION)));
    }

    /**
     * Crée un compte de membre à partir d'un code d'invitation.
     * Tout se fait d'un bloc : si l'e-mail est déjà pris, le code n'est pas consommé.
     */
    @Transactional
    public Membre inscrireAvecInvitation(String code, String nom, String email, String motDePasse,
                                         String role, String couleur) {
        Invitation invitation = invitations.findByCode(normaliserCode(code))
                .filter(trouvee -> trouvee.estValide(LocalDateTime.now()))
                .orElseThrow(ServiceComptes::codeRefuse);
        Membre membre = creer(nom, email, motDePasse, role, couleur, false, false);
        if (invitations.marquerUtilisee(invitation.getId()) == 0) {
            throw codeRefuse();   // utilisé par quelqu'un d'autre entre-temps : le compte est annulé
        }
        return membre;
    }

    /** Un code se tape sans se soucier des espaces autour ni des majuscules. */
    public static String normaliserCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException codeRefuse() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Code d'invitation inconnu, expiré ou déjà utilisé : demande-en un nouveau");
    }

    /** 12 caractères au hasard, groupés par 4 : « k7mq-x2pd-9fhw ». */
    private String codeAleatoire() {
        StringBuilder resultat = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                resultat.append('-');
            }
            resultat.append(ALPHABET.charAt(hasard.nextInt(ALPHABET.length())));
        }
        return resultat.toString();
    }

    /** Donne ou retire le droit d'administrateur, en gardant toujours au moins un administrateur. */
    @Transactional
    public void changerAdmin(Membre membre, boolean admin) {
        if (membre.isAdmin() && !admin) {
            verifierQuIlResteUnAdmin(membre);
        }
        membre.setAdmin(admin);
    }

    /** Supprime un compte : le membre est aussi retiré des événements où il était inscrit. */
    @Transactional
    public void supprimer(Membre membre) {
        if (membre.isAdmin() && membres.count() > 1) {
            verifierQuIlResteUnAdmin(membre);
        }
        for (Evenement evenement : evenements.findByParticipantsContaining(membre)) {
            evenement.getParticipants().remove(membre);
        }
        membres.delete(membre);
    }

    private void verifierQuIlResteUnAdmin(Membre adminQuiPart) {
        if (adminQuiPart.isAdmin() && membres.countByAdminTrue() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le groupe doit garder au moins un administrateur : nommes-en un autre d'abord");
        }
    }
}
