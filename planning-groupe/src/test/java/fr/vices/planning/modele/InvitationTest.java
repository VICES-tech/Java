package fr.vices.planning.modele;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InvitationTest {

    private static final LocalDateTime LIMITE = LocalDateTime.of(2026, 11, 24, 20, 0);

    @Test
    void valableAvantLaDateLimite() {
        Invitation invitation = new Invitation("k7mq-x2pd", LIMITE);
        assertTrue(invitation.estValide(LIMITE.minusDays(3)));
        assertTrue(invitation.estValide(LIMITE.minusSeconds(1)));
    }

    @Test
    void plusValableUneFoisLaDateLimiteAtteinte() {
        Invitation invitation = new Invitation("k7mq-x2pd", LIMITE);
        assertFalse(invitation.estValide(LIMITE));
        assertFalse(invitation.estValide(LIMITE.plusDays(1)));
    }

    @Test
    void plusValableUneFoisUtilisee() {
        Invitation invitation = new Invitation("k7mq-x2pd", LIMITE);
        invitation.setUtilisee(true);
        assertFalse(invitation.estValide(LIMITE.minusDays(3)));
    }
}
