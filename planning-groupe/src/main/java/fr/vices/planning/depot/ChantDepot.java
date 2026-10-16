package fr.vices.planning.depot;

import fr.vices.planning.modele.Chant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux chants en base. */
public interface ChantDepot extends JpaRepository<Chant, Long> {

    List<Chant> findAllByOrderByTitreAsc();
}
