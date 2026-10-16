package fr.vices.planning.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Teste l'API de bout en bout, avec une base H2 en mémoire. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private long creer(String url, String corps) throws Exception {
        String reponse = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode noeud = json.readTree(reponse);
        return noeud.get("id").asLong();
    }

    @Test
    void scenarioComplet() throws Exception {
        long alice = creer("/api/membres", "{\"nom\":\"Alice\",\"role\":\"Chant\",\"couleur\":\"#ff0000\"}");
        long bob = creer("/api/membres", "{\"nom\":\"Bob\",\"role\":\"Guitare\",\"couleur\":\"\"}");

        // un nom déjà pris est refusé
        mvc.perform(post("/api/membres").contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"alice\"}"))
                .andExpect(status().isConflict());

        long chant = creer("/api/chants", "{\"titre\":\"Hallelujah\",\"tonalite\":\"C\",\"contenu\":\"C Am\"}");

        long repet = creer("/api/evenements", """
                {"titre":"Répétition","type":"REPETITION",
                 "debut":"2026-10-20T19:00:00","fin":"2026-10-20T21:00:00",
                 "lieu":"Salle","participantIds":[%d],"chantIds":[%d]}""".formatted(alice, chant));

        mvc.perform(post("/api/evenements/" + repet + "/participants/" + bob))
                .andExpect(jsonPath("$.participants", hasSize(2)));

        mvc.perform(get("/api/evenements?du=2026-10-19&au=2026-10-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].chants[0].titre").value("Hallelujah"));

        mvc.perform(get("/api/evenements?du=2026-10-26&au=2026-11-01"))
                .andExpect(jsonPath("$", hasSize(0)));

        // supprimer un membre le retire des événements
        mvc.perform(delete("/api/membres/" + bob)).andExpect(status().isNoContent());
        mvc.perform(get("/api/evenements/" + repet))
                .andExpect(jsonPath("$.participants", hasSize(1)));

        // supprimer un chant le retire du programme
        mvc.perform(delete("/api/chants/" + chant)).andExpect(status().isNoContent());
        mvc.perform(get("/api/evenements/" + repet))
                .andExpect(jsonPath("$.chants", hasSize(0)));
    }

    @Test
    void refuseUnEvenementQuiFinitAvantDeCommencer() throws Exception {
        mvc.perform(post("/api/evenements").contentType(MediaType.APPLICATION_JSON).content("""
                        {"titre":"Oups","debut":"2026-10-20T21:00:00","fin":"2026-10-20T19:00:00"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transpose() throws Exception {
        mvc.perform(post("/api/transposition").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texte\":\"G D Em C\",\"tonaliteCible\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texte").value("A E F#m D"))
                .andExpect(jsonPath("$.demiTons").value(2));
    }
}
