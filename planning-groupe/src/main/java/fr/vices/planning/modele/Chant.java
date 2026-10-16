package fr.vices.planning.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Un chant du répertoire : paroles et accords.
 */
@Entity
public class Chant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    private String artiste;

    /** Tonalité de référence, ex. « G », « Am », « Sol ». Peut être vide. */
    private String tonalite;

    /** Paroles avec accords (lignes d'accords ou format [Am]ChordPro). */
    @Column(length = 50_000)
    private String contenu;

    protected Chant() {
    }

    public Chant(String titre, String artiste, String tonalite, String contenu) {
        this.titre = titre;
        this.artiste = artiste;
        this.tonalite = tonalite;
        this.contenu = contenu;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getArtiste() {
        return artiste;
    }

    public void setArtiste(String artiste) {
        this.artiste = artiste;
    }

    public String getTonalite() {
        return tonalite;
    }

    public void setTonalite(String tonalite) {
        this.tonalite = tonalite;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }
}
