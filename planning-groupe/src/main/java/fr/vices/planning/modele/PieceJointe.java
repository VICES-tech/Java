package fr.vices.planning.modele;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/**
 * Un fichier joint à un chant : enregistrement audio, vidéo, partition PDF, photo…
 * Le fichier lui-même est rangé sur le disque ; la base ne garde que ses informations.
 */
@Entity
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chant auquel le fichier est rattaché. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chant_id")
    @JsonIgnore
    private Chant chant;

    /** Nom d'origine du fichier, ex. « Hallelujah - alto.mp3 ». */
    @Column(nullable = false)
    private String nom;

    /** Type du fichier, ex. « audio/mpeg », « application/pdf ». */
    private String type;

    /** Taille en octets. */
    private long taille;

    private LocalDateTime ajouteLe;

    /** Nom sous lequel le fichier est rangé sur le disque (identifiant aléatoire). */
    @Column(nullable = false, unique = true)
    @JsonIgnore
    private String nomStocke;

    protected PieceJointe() {
    }

    public PieceJointe(Chant chant, String nom, String type, long taille, String nomStocke) {
        this.chant = chant;
        this.nom = nom;
        this.type = type;
        this.taille = taille;
        this.nomStocke = nomStocke;
        this.ajouteLe = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Chant getChant() {
        return chant;
    }

    public String getNom() {
        return nom;
    }

    public String getType() {
        return type;
    }

    public long getTaille() {
        return taille;
    }

    public LocalDateTime getAjouteLe() {
        return ajouteLe;
    }

    public String getNomStocke() {
        return nomStocke;
    }
}
