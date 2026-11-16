package fr.vices.planning.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

/** Teste le planning, les chants et les fichiers de bout en bout, connecté comme administrateur. */
class ApiTest extends TestAvecComptes {

    private MockHttpSession alice;

    @BeforeEach
    void connecterAlice() throws Exception {
        alice = inscrireAdmin("Alice", "alice@exemple.fr");
    }

    @Test
    void scenarioComplet() throws Exception {
        long idAlice = membres.findByEmail("alice@exemple.fr").orElseThrow().getId();
        CompteCree bob = creerMembre(alice, "Bob", "bob@exemple.fr");

        // un nom déjà pris est refusé, avec un message clair
        mvc.perform(en(alice, post("/api/membres")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"alice\",\"email\":\"autre@exemple.fr\"}"))
                .andExpect(status().isConflict())
                .andExpect(status().reason("Ce nom est déjà pris : alice"));

        long chant = creer(alice, "/api/chants", "{\"titre\":\"Hallelujah\",\"tonalite\":\"C\",\"contenu\":\"C Am\"}");

        long repet = creer(alice, "/api/evenements", """
                {"titre":"Répétition","type":"REPETITION",
                 "debut":"2026-10-20T19:00:00","fin":"2026-10-20T21:00:00",
                 "lieu":"Salle","participantIds":[%d],"chantIds":[%d]}""".formatted(idAlice, chant));

        // Bob se connecte et clique sur « Je participe », puis se ravise et revient
        MockHttpSession sessionBob = connecter("bob@exemple.fr", bob.motDePasseProvisoire());
        mvc.perform(en(sessionBob, post("/api/evenements/" + repet + "/participation")))
                .andExpect(jsonPath("$.participants", hasSize(2)));
        mvc.perform(en(sessionBob, delete("/api/evenements/" + repet + "/participation")))
                .andExpect(jsonPath("$.participants", hasSize(1)));
        mvc.perform(en(sessionBob, post("/api/evenements/" + repet + "/participation")))
                .andExpect(jsonPath("$.participants", hasSize(2)));

        mvc.perform(en(alice, get("/api/evenements?du=2026-10-19&au=2026-10-25")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].chants[0].titre").value("Hallelujah"));

        mvc.perform(en(alice, get("/api/evenements?du=2026-10-26&au=2026-11-01")))
                .andExpect(jsonPath("$", hasSize(0)));

        // supprimer un membre le retire des événements
        mvc.perform(en(alice, delete("/api/membres/" + bob.id()))).andExpect(status().isNoContent());
        mvc.perform(en(alice, get("/api/evenements/" + repet)))
                .andExpect(jsonPath("$.participants", hasSize(1)));

        // supprimer un chant le retire du programme
        mvc.perform(en(alice, delete("/api/chants/" + chant))).andExpect(status().isNoContent());
        mvc.perform(en(alice, get("/api/evenements/" + repet)))
                .andExpect(jsonPath("$.chants", hasSize(0)));
    }

    @Test
    void refuseUnEvenementQuiFinitAvantDeCommencer() throws Exception {
        mvc.perform(en(alice, post("/api/evenements")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"titre":"Oups","debut":"2026-10-20T21:00:00","fin":"2026-10-20T19:00:00"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("La fin doit être après le début"));
    }

    @Test
    void transpose() throws Exception {
        mvc.perform(en(alice, post("/api/transposition")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texte\":\"G D Em C\",\"tonaliteCible\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texte").value("A E F#m D"))
                .andExpect(jsonPath("$.demiTons").value(2));
    }

    @Test
    void piecesJointes() throws Exception {
        long chant = creer(alice, "/api/chants", "{\"titre\":\"Ave Maria\"}");
        byte[] son = {1, 2, 3, 4, 5};

        String reponse = mvc.perform(en(alice, multipart("/api/chants/" + chant + "/pieces-jointes")
                        .file(new MockMultipartFile("fichier", "Ave Maria - alto.mp3", "audio/mpeg", son))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Ave Maria - alto.mp3"))
                .andExpect(jsonPath("$.taille").value(5))
                .andExpect(jsonPath("$.nomStocke").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long audio = json.readTree(reponse).get("id").asLong();

        // un fichier HTML ne doit jamais s'ouvrir dans le navigateur (risque de script)
        reponse = mvc.perform(en(alice, multipart("/api/chants/" + chant + "/pieces-jointes")
                        .file(new MockMultipartFile("fichier", "piege.html", "text/html", "<script>".getBytes()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long html = json.readTree(reponse).get("id").asLong();

        mvc.perform(en(alice, get("/api/chants/" + chant + "/pieces-jointes")))
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(en(alice, get("/api/pieces-jointes/" + audio)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(content().bytes(son));

        mvc.perform(en(alice, get("/api/pieces-jointes/" + html)))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));

        // sans être connecté, un fichier ne se télécharge pas
        mvc.perform(get("/api/pieces-jointes/" + audio)).andExpect(status().isUnauthorized());

        mvc.perform(en(alice, delete("/api/pieces-jointes/" + html))).andExpect(status().isNoContent());

        // supprimer le chant supprime aussi ses fichiers
        mvc.perform(en(alice, delete("/api/chants/" + chant))).andExpect(status().isNoContent());
        mvc.perform(en(alice, get("/api/pieces-jointes/" + audio)))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Pièce jointe introuvable"));
    }

    @Test
    void tempoDesChants() throws Exception {
        long chant = creer(alice, "/api/chants", "{\"titre\":\"Oh Happy Day\",\"tempo\":96}");
        mvc.perform(en(alice, get("/api/chants/" + chant)))
                .andExpect(jsonPath("$.tempo").value(96));

        // on peut changer le tempo, ou le laisser vide
        mvc.perform(en(alice, put("/api/chants/" + chant)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Oh Happy Day\",\"tempo\":104}"))
                .andExpect(jsonPath("$.tempo").value(104));
        mvc.perform(en(alice, put("/api/chants/" + chant)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Oh Happy Day\"}"))
                .andExpect(jsonPath("$.tempo").doesNotExist());
    }
}
