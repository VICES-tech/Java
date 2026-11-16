package fr.vices.planning.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.vices.planning.depot.ChantDepot;
import fr.vices.planning.depot.EvenementDepot;
import fr.vices.planning.depot.MembreDepot;
import fr.vices.planning.depot.PieceJointeDepot;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Base commune des tests de l'API : une base H2 en mémoire, vidée avant chaque test,
 * et de quoi créer des comptes et se connecter comme le ferait un navigateur.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "planning.dossier-fichiers=target/test-fichiers"})
@AutoConfigureMockMvc
abstract class TestAvecComptes {

    protected static final String MOT_DE_PASSE = "motdepasse-de-test";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected MembreDepot membres;

    @Autowired
    private EvenementDepot evenements;

    @Autowired
    private PieceJointeDepot piecesJointes;

    @Autowired
    private ChantDepot chants;

    @BeforeEach
    void viderLaBase() {
        evenements.deleteAll();
        piecesJointes.deleteAll();
        chants.deleteAll();
        membres.deleteAll();
    }

    /** Ajoute à une requête la session du membre connecté et le jeton anti-CSRF. */
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B en(MockHttpSession session, B requete) {
        return requete.session(session).with(csrf());
    }

    /** Crée le tout premier compte (l'administrateur) et le connecte. */
    protected MockHttpSession inscrireAdmin(String nom, String email) throws Exception {
        mvc.perform(post("/api/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"%s\",\"email\":\"%s\",\"motDePasse\":\"%s\"}"
                                .formatted(nom, email, MOT_DE_PASSE)))
                .andExpect(status().isCreated());
        return connecter(email, MOT_DE_PASSE);
    }

    protected MockHttpSession connecter(String email, String motDePasse) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/connexion").with(csrf()).session(session)
                        .param("email", email).param("motDePasse", motDePasse))
                .andExpect(status().isNoContent());
        return session;
    }

    /** Un compte créé par un administrateur : son identifiant et son mot de passe provisoire. */
    protected record CompteCree(long id, String motDePasseProvisoire) {
    }

    protected CompteCree creerMembre(MockHttpSession admin, String nom, String email) throws Exception {
        String reponse = mvc.perform(en(admin, post("/api/membres")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"%s\",\"email\":\"%s\"}".formatted(nom, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode noeud = json.readTree(reponse);
        return new CompteCree(noeud.get("membre").get("id").asLong(),
                noeud.get("motDePasseProvisoire").asString());
    }

    /** Envoie une création (POST) et renvoie l'identifiant de ce qui a été créé. */
    protected long creer(MockHttpSession session, String url, String corps) throws Exception {
        String reponse = mvc.perform(en(session, post(url)).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("id").asLong();
    }
}
