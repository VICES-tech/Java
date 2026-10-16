package fr.vices.planning.depot;

import fr.vices.planning.modele.Membre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux membres en base. Spring génère l'implémentation tout seul. */
public interface MembreDepot extends JpaRepository<Membre, Long> {

    List<Membre> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);
}
