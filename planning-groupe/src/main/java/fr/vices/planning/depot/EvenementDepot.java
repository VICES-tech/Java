package fr.vices.planning.depot;

import fr.vices.planning.modele.Chant;
import fr.vices.planning.modele.Evenement;
import fr.vices.planning.modele.Membre;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux événements en base. */
public interface EvenementDepot extends JpaRepository<Evenement, Long> {

    /** Événements qui chevauchent la période [du, au[. */
    List<Evenement> findByDebutLessThanAndFinGreaterThanOrderByDebutAsc(LocalDateTime au, LocalDateTime du);

    List<Evenement> findByParticipantsContaining(Membre membre);

    List<Evenement> findByChantsContaining(Chant chant);
}
