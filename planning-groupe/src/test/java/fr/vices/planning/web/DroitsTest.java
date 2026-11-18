package fr.vices.planning.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Règle décidée par le groupe : tout le monde ajoute des dates, des chants et des fichiers ;
 * seuls les administrateurs les modifient ou les suppriment.
 */
class DroitsTest extends TestAvecComptes {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String EVENEMENT = """
            {"titre":"%s","debut":"2026-12-20T19:00:00","fin":"2026-12-20T21:00:00"}""";

    private MockHttpSession admin;
    private MockHttpSession membre;

    @BeforeEach
    void connecter() throws Exception {
        admin = inscrireAdmin("Fabien", "fabien@exemple.fr");
        CompteCree marie = creerMembre(admin, "Marie", "marie@exemple.fr");
        membre = connecter("marie@exemple.fr", marie.motDePasseProvisoire());
    }

    private long ajouterFichier(MockHttpSession session, long chant) throws Exception {
        String reponse = mvc.perform(en(session, multipart("/api/chants/" + chant + "/pieces-jointes")
                        .file(new MockMultipartFile("fichier", "alto.mp3", "audio/mpeg", new byte[] {1, 2, 3}))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("id").asLong();
    }

    @Test
    void unMembreAjouteDesDatesDesChantsEtDesFichiers() throws Exception {
        long repet = creer(membre, "/api/evenements", EVENEMENT.formatted("Répétition proposée par Marie"));
        long chant = creer(membre, "/api/chants", "{\"titre\":\"Oh Happy Day\",\"tonalite\":\"G\"}");
        ajouterFichier(membre, chant);

        mvc.perform(en(membre, post("/api/evenements/" + repet + "/participation")))
                .andExpect(jsonPath("$.participants.length()").value(1));
        mvc.perform(en(admin, get("/api/chants/" + chant + "/pieces-jointes")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unMembreNePeutNiModifierNiSupprimer() throws Exception {
        long repet = creer(admin, "/api/evenements", EVENEMENT.formatted("Répétition"));
        long chant = creer(admin, "/api/chants", "{\"titre\":\"Hallelujah\",\"tonalite\":\"C\"}");
        long fichier = ajouterFichier(admin, chant);

        // même ce qu'il a ajouté lui-même
        long sienne = creer(membre, "/api/evenements", EVENEMENT.formatted("Ma date"));
        long sonChant = creer(membre, "/api/chants", "{\"titre\":\"Mon chant\"}");

        for (long id : new long[] {repet, sienne}) {
            mvc.perform(en(membre, put("/api/evenements/" + id)).contentType(JSON)
                            .content(EVENEMENT.formatted("Modifiée")))
                    .andExpect(status().isForbidden());
            mvc.perform(en(membre, delete("/api/evenements/" + id))).andExpect(status().isForbidden());
        }
        for (long id : new long[] {chant, sonChant}) {
            mvc.perform(en(membre, put("/api/chants/" + id)).contentType(JSON).content("{\"titre\":\"Modifié\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(en(membre, delete("/api/chants/" + id))).andExpect(status().isForbidden());
        }
        mvc.perform(en(membre, delete("/api/pieces-jointes/" + fichier))).andExpect(status().isForbidden());

        // rien n'a bougé
        mvc.perform(en(admin, get("/api/evenements/" + repet))).andExpect(jsonPath("$.titre").value("Répétition"));
        mvc.perform(en(admin, get("/api/chants/" + chant))).andExpect(jsonPath("$.titre").value("Hallelujah"));
        mvc.perform(en(admin, get("/api/chants/" + chant + "/pieces-jointes")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unAdministrateurModifieEtSupprimeCeQueLesMembresOntAjoute() throws Exception {
        long date = creer(membre, "/api/evenements", EVENEMENT.formatted("Répétiton"));
        long chant = creer(membre, "/api/chants", "{\"titre\":\"Oh Hapy Day\"}");
        long fichier = ajouterFichier(membre, chant);

        mvc.perform(en(admin, put("/api/evenements/" + date)).contentType(JSON)
                        .content(EVENEMENT.formatted("Répétition")))
                .andExpect(jsonPath("$.titre").value("Répétition"));
        mvc.perform(en(admin, put("/api/chants/" + chant)).contentType(JSON).content("{\"titre\":\"Oh Happy Day\"}"))
                .andExpect(jsonPath("$.titre").value("Oh Happy Day"));
        mvc.perform(en(admin, delete("/api/pieces-jointes/" + fichier))).andExpect(status().isNoContent());
        mvc.perform(en(admin, delete("/api/chants/" + chant))).andExpect(status().isNoContent());
        mvc.perform(en(admin, delete("/api/evenements/" + date))).andExpect(status().isNoContent());
        assertThat(membres.count()).isEqualTo(2);
    }
}
