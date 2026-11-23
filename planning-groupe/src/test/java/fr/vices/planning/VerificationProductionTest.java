package fr.vices.planning;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class VerificationProductionTest {

    private static MockEnvironment production() {
        MockEnvironment environnement = new MockEnvironment();
        environnement.setActiveProfiles("prod");
        return environnement;
    }

    @Test
    void enProductionLesVariablesManquantesSontNommees() {
        MockEnvironment environnement = production()
                .withProperty("PLANIT_DB_URL", "jdbc:postgresql://localhost:5432/planit")
                .withProperty("PLANIT_DB_PASSWORD", "  ");

        assertThatThrownBy(() -> VerificationProduction.verifier(environnement))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLANIT_DB_USER, PLANIT_DB_PASSWORD, PLANIT_CODE_INSTALLATION")
                .hasMessageNotContaining("PLANIT_DB_URL,");
    }

    @Test
    void enProductionToutEstFourni() {
        MockEnvironment environnement = production()
                .withProperty("PLANIT_DB_URL", "jdbc:postgresql://localhost:5432/planit")
                .withProperty("PLANIT_DB_USER", "planit")
                .withProperty("PLANIT_DB_PASSWORD", "secret")
                .withProperty("PLANIT_CODE_INSTALLATION", "code");

        assertThatCode(() -> VerificationProduction.verifier(environnement)).doesNotThrowAnyException();
    }

    @Test
    void enDeveloppementRienNEstExige() {
        assertThatCode(() -> VerificationProduction.verifier(new MockEnvironment())).doesNotThrowAnyException();
    }
}
