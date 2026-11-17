package fr.vices.planning.depot;

import fr.vices.planning.modele.Invitation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Accès aux invitations en base. */
public interface InvitationDepot extends JpaRepository<Invitation, Long> {

    /** Retrouve une invitation à partir du code tapé (écrite par Fabien). */
    Optional<Invitation> findByCode(String code);

    /** Les invitations encore utilisables, de la plus proche de sa date limite à la plus lointaine. */
    List<Invitation> findByUtiliseeFalseAndExpireLeAfterOrderByExpireLeAsc(LocalDateTime maintenant);

    /**
     * Marque l'invitation comme utilisée, seulement si elle ne l'était pas encore.
     * La base fait la vérification et la modification d'un seul coup : si deux personnes
     * tapent le même code au même moment, une seule obtient 1, l'autre obtient 0.
     *
     * @return 1 si on vient de l'utiliser, 0 si quelqu'un l'avait déjà fait
     */
    @Modifying
    @Query("update Invitation i set i.utilisee = true where i.id = :id and i.utilisee = false")
    int marquerUtilisee(Long id);
}
