package fr.vices.planning.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Un événement du planning : répétition, concert, réunion…
 * Il réunit des participants et peut avoir un programme de chants.
 */
@Entity
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEvenement type = TypeEvenement.REPETITION;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    private String lieu;

    @Column(length = 5_000)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "evenement_participant")
    private Set<Membre> participants = new LinkedHashSet<>();

    /** Programme : chants dans l'ordre où ils seront joués. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "evenement_chant")
    @OrderColumn(name = "position")
    private List<Chant> chants = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public TypeEvenement getType() {
        return type;
    }

    public void setType(TypeEvenement type) {
        this.type = type;
    }

    public LocalDateTime getDebut() {
        return debut;
    }

    public void setDebut(LocalDateTime debut) {
        this.debut = debut;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Membre> getParticipants() {
        return participants;
    }

    public List<Chant> getChants() {
        return chants;
    }
}
