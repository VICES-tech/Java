package fr.vices.planning.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.vices.planning.modele.Invitation;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/** Teste l'invitation par code : création par un administrateur, puis inscription avec le code. */
class InvitationApiTest extends TestAvecComptes {

    private MockHttpSession fabien;

    @BeforeEach
    void connecterFabien() throws Exception {
        fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
    }

    private JsonNode creerInvitation() throws Exception {
        String reponse = mvc.perform(en(fabien, post("/api/invitations")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse);
    }

    /** Une personne non connectée crée son compte avec un code. */
    private ResultActions sInscrire(String code, String nom, String email) throws Exception {
        return mvc.perform(post("/api/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"%s\",\"email\":\"%s\",\"motDePasse\":\"secret-de-%s\",\"code\":\"%s\"}"
                        .formatted(nom, email, nom, code)));
    }

    @Test
    void unNouveauMembreSInscritAvecUnCode() throws Exception {
        JsonNode invitation = creerInvitation();
        String code = invitation.get("code").asString();
        assertThat(code).matches("[a-z2-9]{4}-[a-z2-9]{4}-[a-z2-9]{4}");
        assertThat(LocalDateTime.parse(invitation.get("expireLe").asString()))
                .isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8));

        // le code se tape sans se soucier des majuscules ni des espaces autour
        sInscrire("  " + code.toUpperCase() + " ", "Marie", "marie@exemple.fr")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.doitChangerMotDePasse").value(false));

        MockHttpSession marie = connecter("marie@exemple.fr", "secret-de-Marie");
        mvc.perform(en(marie, get("/api/session"))).andExpect(jsonPath("$.membre.nom").value("Marie"));

        // le code a servi : il disparaît de la liste et ne marche plus
        mvc.perform(en(fabien, get("/api/invitations"))).andExpect(jsonPath("$.length()").value(0));
        sInscrire(code, "Lucas", "lucas@exemple.fr")
                .andExpect(status().isForbidden())
                .andExpect(status().reason(
                        "Code d'invitation inconnu, expiré ou déjà utilisé : demande-en un nouveau"));
        assertThat(membres.count()).isEqualTo(2);
    }

    @Test
    void sansCodeOuAvecUnFauxCodeLInscriptionEstRefusee() throws Exception {
        creerInvitation();
        sInscrire("", "Intrus", "intrus@exemple.fr").andExpect(status().isForbidden());
        sInscrire("aaaa-bbbb-cccc", "Intrus", "intrus@exemple.fr").andExpect(status().isForbidden());
        assertThat(membres.count()).isEqualTo(1);
    }

    @Test
    void unCodeExpireEstRefuse() throws Exception {
        invitations.save(new Invitation("vieu-xcod-e222", LocalDateTime.now().minusMinutes(1)));
        sInscrire("vieu-xcod-e222", "Lucas", "lucas@exemple.fr").andExpect(status().isForbidden());
        mvc.perform(en(fabien, get("/api/invitations"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unEmailDejaPrisNeConsommePasLeCode() throws Exception {
        String code = creerInvitation().get("code").asString();
        sInscrire(code, "Faux Fabien", "fabien@exemple.fr").andExpect(status().isConflict());

        // le code reste utilisable pour une nouvelle tentative avec le bon e-mail
        assertThat(invitations.findByCode(code).orElseThrow().isUtilisee()).isFalse();
        sInscrire(code, "Marie", "marie@exemple.fr").andExpect(status().isCreated());
    }

    @Test
    void unAdminPeutAnnulerUneInvitation() throws Exception {
        JsonNode invitation = creerInvitation();
        creerInvitation();
        mvc.perform(en(fabien, get("/api/invitations"))).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(en(fabien, delete("/api/invitations/" + invitation.get("id").asLong())))
                .andExpect(status().isNoContent());
        mvc.perform(en(fabien, get("/api/invitations"))).andExpect(jsonPath("$.length()").value(1));
        sInscrire(invitation.get("code").asString(), "Marie", "marie@exemple.fr").andExpect(status().isForbidden());
    }

    @Test
    void seulsLesAdministrateursGerentLesInvitations() throws Exception {
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");
        MockHttpSession sessionMarie = connecter("marie@exemple.fr", marie.motDePasseProvisoire());
        long id = creerInvitation().get("id").asLong();

        mvc.perform(en(sessionMarie, get("/api/invitations"))).andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, post("/api/invitations"))).andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, delete("/api/invitations/" + id))).andExpect(status().isForbidden());
        mvc.perform(get("/api/invitations")).andExpect(status().isUnauthorized());
        assertThat(invitations.count()).isEqualTo(1);
    }
}
