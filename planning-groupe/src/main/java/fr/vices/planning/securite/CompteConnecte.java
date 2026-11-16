package fr.vices.planning.securite;

import fr.vices.planning.modele.Membre;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Ce que le serveur garde en mémoire pour une personne connectée.
 * Il ne retient que l'identifiant du membre : ses informations (nom, droits…)
 * sont relues dans la base à chaque demande, pour être toujours à jour.
 */
public class CompteConnecte implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private String motDePasse;

    public CompteConnecte(Membre membre) {
        this.id = membre.getId();
        this.email = membre.getEmail();
        this.motDePasse = membre.getMotDePasse();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Une fois la connexion vérifiée, on oublie le mot de passe chiffré. */
    @Override
    public void eraseCredentials() {
        this.motDePasse = null;
    }
}
