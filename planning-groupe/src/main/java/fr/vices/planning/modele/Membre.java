package fr.vices.planning.modele;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;

/**
 * Un membre du groupe (chanteur, musicien, organisateur…) et son compte de connexion.
 */
@Entity
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom affiché, unique dans le groupe. */
    @Column(nullable = false, unique = true)
    private String nom;

    /** Rôle ou instrument : « Chant », « Guitare », « Batterie »… */
    private String role;

    /** Couleur d'affichage au format #RRGGBB. */
    private String couleur;

    /**
     * Adresse e-mail, en minuscules : c'est l'identifiant de connexion.
     * Elle n'est jamais envoyée avec le membre ({@code @JsonIgnore}) : seuls
     * le membre lui-même et les administrateurs la voient.
     */
    @Column(unique = true)
    @JsonIgnore
    private String email;

    /** Mot de passe chiffré. Le mot de passe en clair n'est jamais enregistré. */
    @JsonIgnore
    private String motDePasse;

    /** Vrai si le membre est administrateur du groupe. */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean admin;

    /** Vrai tant que le membre utilise un mot de passe provisoire donné par un administrateur. */
    @Column(nullable = false)
    @ColumnDefault("false")
    @JsonIgnore
    private boolean doitChangerMotDePasse;

    protected Membre() {
    }

    public Membre(String nom, String role, String couleur) {
        this.nom = nom;
        this.role = role;
        this.couleur = couleur;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isDoitChangerMotDePasse() {
        return doitChangerMotDePasse;
    }

    public void setDoitChangerMotDePasse(boolean doitChangerMotDePasse) {
        this.doitChangerMotDePasse = doitChangerMotDePasse;
    }
}
