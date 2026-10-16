package fr.vices.planning.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Un membre du groupe (chanteur, musicien, organisateur…).
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
}
