package fr.vices.planning.depot;

import fr.vices.planning.modele.Chant;
import fr.vices.planning.modele.PieceJointe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux pièces jointes en base. */
public interface PieceJointeDepot extends JpaRepository<PieceJointe, Long> {

    List<PieceJointe> findByChantOrderByAjouteLeAsc(Chant chant);
}
