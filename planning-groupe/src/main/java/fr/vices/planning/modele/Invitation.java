package fr.vices.planning.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

/**
 * Une invitation : un administrateur crée un code et le transmet à un futur membre,
 * qui s'en sert pour créer lui-même son compte.
 */
@Entity
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Le code à transmettre, par exemple « k7mq-x2pd ». */
    @Column(nullable = false, unique = true)
    private String code;

    /** La date et l'heure limites pour l'utiliser. */
    @Column(nullable = false)
    private LocalDateTime expireLe;

    /** Vrai une fois que le code a servi : un code ne sert qu'une seule fois. */
    private boolean utilisee;

    protected Invitation() {
    }

    public Invitation(String code, LocalDateTime expireLe) {
        this.code = code;
        this.expireLe = expireLe;
    }

    /**
     * Vrai si le code peut encore servir : il n'a pas été utilisé et la date limite n'est pas passée.
     *
     * @param maintenant l'heure actuelle (donnée en paramètre pour pouvoir tester n'importe quelle date)
     */
    public boolean estValide(LocalDateTime maintenant) {
        return !utilisee && maintenant.isBefore(expireLe);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpireLe() {
        return expireLe;
    }

    public boolean isUtilisee() {
        return utilisee;
    }

    public void setUtilisee(boolean utilisee) {
        this.utilisee = utilisee;
    }
}
