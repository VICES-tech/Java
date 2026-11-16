package fr.vices.planning.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.vices.planning.modele.Membre;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** Teste les comptes : connexion, droits des administrateurs, mots de passe, vie privée. */
class SecuriteTest extends TestAvecComptes {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Test
    void sansConnexionRienNEstVisible() throws Exception {
        // l'API répond « 401 », les pages renvoient vers la page de connexion
        mvc.perform(get("/api/membres"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Connexion requise"));
        mvc.perform(get("/api/chants")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/evenements?du=2026-01-01&au=2026-12-31")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(redirectedUrl("/connexion.html"));
        mvc.perform(get("/app.js")).andExpect(redirectedUrl("/connexion.html"));

        // seule la page de connexion est ouverte à tous
        mvc.perform(get("/connexion.html")).andExpect(status().isOk());
        mvc.perform(get("/api/session"))
                .andExpect(jsonPath("$.connecte").value(false))
                .andExpect(jsonPath("$.premierCompte").value(true));
    }

    @Test
    void lePremierCompteEstAdministrateurPuisLInscriptionSeFerme() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "Fabien@Exemple.fr");

        mvc.perform(en(fabien, get("/api/session")))
                .andExpect(jsonPath("$.connecte").value(true))
                .andExpect(jsonPath("$.premierCompte").value(false))
                .andExpect(jsonPath("$.membre.nom").value("Fabien"))
                .andExpect(jsonPath("$.membre.email").value("fabien@exemple.fr"))   // rangé en minuscules
                .andExpect(jsonPath("$.membre.admin").value(true));

        // un inconnu ne peut plus créer de compte tout seul
        mvc.perform(post("/api/inscription").with(csrf()).contentType(JSON)
                        .content("{\"nom\":\"Intrus\",\"email\":\"intrus@exemple.fr\",\"motDePasse\":\"motdepasse1\"}"))
                .andExpect(status().isForbidden());
        assertThat(membres.count()).isEqualTo(1);
    }

    @Test
    void connexionRefuseeAvecUnMauvaisMotDePasse() throws Exception {
        inscrireAdmin("Fabien", "fabien@exemple.fr");

        mvc.perform(post("/api/connexion").with(csrf())
                        .param("email", "fabien@exemple.fr").param("motDePasse", "pas-le-bon"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou mot de passe incorrect"));

        // même réponse pour un e-mail inconnu : on ne révèle pas quels comptes existent
        mvc.perform(post("/api/connexion").with(csrf())
                        .param("email", "inconnu@exemple.fr").param("motDePasse", MOT_DE_PASSE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou mot de passe incorrect"));

        // l'e-mail se tape sans se soucier des majuscules
        connecter("FABIEN@exemple.fr", MOT_DE_PASSE);
    }

    @Test
    void unEnvoiSansJetonAntiCsrfEstRefuse() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");

        mvc.perform(post("/api/chants").session(fabien).contentType(JSON).content("{\"titre\":\"Sans jeton\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Session expirée : recharge la page"));
        mvc.perform(en(fabien, get("/api/chants"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unMotDePasseTropCourtEstRefuse() throws Exception {
        mvc.perform(post("/api/inscription").with(csrf()).contentType(JSON)
                        .content("{\"nom\":\"Fabien\",\"email\":\"fabien@exemple.fr\",\"motDePasse\":\"court\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le mot de passe doit contenir entre 8 et 64 caractères"));
        mvc.perform(post("/api/inscription").with(csrf()).contentType(JSON)
                        .content("{\"nom\":\"Fabien\",\"email\":\"pas-un-email\",\"motDePasse\":\"motdepasse1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Adresse e-mail invalide"));
        assertThat(membres.count()).isZero();
    }

    @Test
    void unAdminCreeUnCompteAvecUnMotDePasseProvisoire() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");
        assertThat(marie.motDePasseProvisoire()).matches("[a-z2-9]{4}-[a-z2-9]{4}-[a-z2-9]{4}");

        // Marie se connecte : on lui demande de choisir son propre mot de passe
        MockHttpSession sessionMarie = connecter("marie@exemple.fr", marie.motDePasseProvisoire());
        mvc.perform(en(sessionMarie, get("/api/session")))
                .andExpect(jsonPath("$.membre.admin").value(false))
                .andExpect(jsonPath("$.membre.doitChangerMotDePasse").value(true));

        mvc.perform(en(sessionMarie, post("/api/compte/mot-de-passe")).contentType(JSON)
                        .content("{\"ancien\":\"pas-le-bon\",\"nouveau\":\"mon-nouveau-secret\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, post("/api/compte/mot-de-passe")).contentType(JSON)
                        .content("{\"ancien\":\"%s\",\"nouveau\":\"court\"}".formatted(marie.motDePasseProvisoire())))
                .andExpect(status().isBadRequest());
        mvc.perform(en(sessionMarie, post("/api/compte/mot-de-passe")).contentType(JSON)
                        .content("{\"ancien\":\"%s\",\"nouveau\":\"mon-nouveau-secret\"}"
                                .formatted(marie.motDePasseProvisoire())))
                .andExpect(status().isNoContent());

        mvc.perform(en(sessionMarie, get("/api/session")))
                .andExpect(jsonPath("$.membre.doitChangerMotDePasse").value(false));

        // l'ancien mot de passe ne marche plus, le nouveau oui
        mvc.perform(post("/api/connexion").with(csrf())
                        .param("email", "marie@exemple.fr").param("motDePasse", marie.motDePasseProvisoire()))
                .andExpect(status().isUnauthorized());
        connecter("marie@exemple.fr", "mon-nouveau-secret");
    }

    @Test
    void unAdminRedonneUnMotDePasseAUnMembreQuiLAOublie() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");

        String reponse = mvc.perform(en(fabien, post("/api/membres/" + marie.id() + "/mot-de-passe")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nouveau = json.readTree(reponse).get("motDePasseProvisoire").asString();

        assertThat(nouveau).isNotEqualTo(marie.motDePasseProvisoire());
        mvc.perform(post("/api/connexion").with(csrf())
                        .param("email", "marie@exemple.fr").param("motDePasse", marie.motDePasseProvisoire()))
                .andExpect(status().isUnauthorized());
        MockHttpSession sessionMarie = connecter("marie@exemple.fr", nouveau);
        mvc.perform(en(sessionMarie, get("/api/session")))
                .andExpect(jsonPath("$.membre.doitChangerMotDePasse").value(true));
    }

    @Test
    void unMembreSimpleNePeutPasAdministrer() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        long idFabien = membres.findByEmail("fabien@exemple.fr").orElseThrow().getId();
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");
        MockHttpSession sessionMarie = connecter("marie@exemple.fr", marie.motDePasseProvisoire());

        mvc.perform(en(sessionMarie, post("/api/membres")).contentType(JSON)
                        .content("{\"nom\":\"Lucas\",\"email\":\"lucas@exemple.fr\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, delete("/api/membres/" + idFabien))).andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, post("/api/membres/" + idFabien + "/mot-de-passe")))
                .andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, put("/api/membres/" + marie.id() + "/admin")).contentType(JSON)
                        .content("{\"admin\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(en(sessionMarie, put("/api/membres/" + idFabien)).contentType(JSON)
                        .content("{\"nom\":\"Piraté\"}"))
                .andExpect(status().isForbidden());
        assertThat(membres.count()).isEqualTo(2);
        assertThat(membres.findById(idFabien).orElseThrow().getNom()).isEqualTo("Fabien");

        // mais elle peut modifier sa propre fiche
        mvc.perform(en(sessionMarie, put("/api/membres/" + marie.id())).contentType(JSON)
                        .content("{\"nom\":\"Marie D.\",\"role\":\"Chant\",\"couleur\":\"#0891b2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Marie D."))
                .andExpect(jsonPath("$.email").value("marie@exemple.fr"));   // e-mail absent = inchangé
    }

    @Test
    void lesEmailsEtLesMotsDePasseRestentPrives() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        long idFabien = membres.findByEmail("fabien@exemple.fr").orElseThrow().getId();
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");
        MockHttpSession sessionMarie = connecter("marie@exemple.fr", marie.motDePasseProvisoire());

        // en base, le mot de passe est chiffré : on ne retrouve le texte tapé nulle part
        Membre enBase = membres.findById(idFabien).orElseThrow();
        assertThat(enBase.getMotDePasse()).startsWith("{bcrypt}").doesNotContain(MOT_DE_PASSE);

        // Marie voit son e-mail, pas celui des autres ; l'administrateur les voit tous
        mvc.perform(en(sessionMarie, get("/api/membres")))
                .andExpect(jsonPath("$[?(@.nom=='Marie')].email").value("marie@exemple.fr"))
                .andExpect(jsonPath("$[?(@.nom=='Fabien')].email").value((Object) null))
                .andExpect(content().string(not(containsString("fabien@exemple.fr"))))
                .andExpect(content().string(not(containsString("bcrypt"))))
                .andExpect(content().string(not(containsString("motDePasse"))));
        mvc.perform(en(fabien, get("/api/membres")))
                .andExpect(jsonPath("$[?(@.nom=='Marie')].email").value("marie@exemple.fr"));

        // dans un événement, les participants n'exposent ni e-mail ni mot de passe
        long repet = creer(fabien, "/api/evenements", """
                {"titre":"Répétition","debut":"2026-10-20T19:00:00","fin":"2026-10-20T21:00:00",
                 "participantIds":[%d,%d]}""".formatted(idFabien, marie.id()));
        mvc.perform(en(sessionMarie, get("/api/evenements/" + repet)))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(content().string(not(containsString("exemple.fr"))))
                .andExpect(content().string(not(containsString("bcrypt"))));
    }

    @Test
    void ilResteToujoursUnAdministrateur() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        long idFabien = membres.findByEmail("fabien@exemple.fr").orElseThrow().getId();
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");

        // seul administrateur : Fabien ne peut ni se retirer ce droit, ni supprimer son compte
        mvc.perform(en(fabien, put("/api/membres/" + idFabien + "/admin")).contentType(JSON)
                        .content("{\"admin\":false}"))
                .andExpect(status().isConflict());
        mvc.perform(en(fabien, post("/api/compte/suppression")).contentType(JSON)
                        .content("{\"motDePasse\":\"%s\"}".formatted(MOT_DE_PASSE)))
                .andExpect(status().isConflict());
        assertThat(membres.findById(idFabien).orElseThrow().isAdmin()).isTrue();

        // il nomme Marie administratrice : il peut alors partir
        mvc.perform(en(fabien, put("/api/membres/" + marie.id() + "/admin")).contentType(JSON)
                        .content("{\"admin\":true}"))
                .andExpect(jsonPath("$.admin").value(true));
        mvc.perform(en(fabien, post("/api/compte/suppression")).contentType(JSON)
                        .content("{\"motDePasse\":\"%s\"}".formatted(MOT_DE_PASSE)))
                .andExpect(status().isNoContent());

        // son compte n'existe plus et sa session ne donne plus accès à rien
        assertThat(membres.findById(idFabien)).isEmpty();
        mvc.perform(en(fabien, get("/api/membres"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/connexion").with(csrf())
                        .param("email", "fabien@exemple.fr").param("motDePasse", MOT_DE_PASSE))
                .andExpect(status().isUnauthorized());
        assertThat(membres.findById(marie.id()).orElseThrow().isAdmin()).isTrue();
    }

    @Test
    void supprimerSonCompteDemandeSonMotDePasseEtNettoieLesEvenements() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        CompteCree marie = creerMembre(fabien, "Marie", "marie@exemple.fr");
        MockHttpSession sessionMarie = connecter("marie@exemple.fr", marie.motDePasseProvisoire());
        long repet = creer(fabien, "/api/evenements", """
                {"titre":"Répétition","debut":"2026-10-20T19:00:00","fin":"2026-10-20T21:00:00"}""");
        mvc.perform(en(sessionMarie, post("/api/evenements/" + repet + "/participation")))
                .andExpect(jsonPath("$.participants.length()").value(1));

        mvc.perform(en(sessionMarie, post("/api/compte/suppression")).contentType(JSON)
                        .content("{\"motDePasse\":\"pas-le-bon\"}"))
                .andExpect(status().isForbidden());
        assertThat(membres.findById(marie.id())).isPresent();

        mvc.perform(en(sessionMarie, post("/api/compte/suppression")).contentType(JSON)
                        .content("{\"motDePasse\":\"%s\"}".formatted(marie.motDePasseProvisoire())))
                .andExpect(status().isNoContent());
        assertThat(membres.findById(marie.id())).isEmpty();
        mvc.perform(en(fabien, get("/api/evenements/" + repet)))
                .andExpect(jsonPath("$.participants.length()").value(0));
    }

    @Test
    void laDeconnexionFermeLaSession() throws Exception {
        MockHttpSession fabien = inscrireAdmin("Fabien", "fabien@exemple.fr");
        mvc.perform(en(fabien, get("/api/chants"))).andExpect(status().isOk());

        mvc.perform(en(fabien, post("/api/deconnexion"))).andExpect(status().isNoContent());
        mvc.perform(en(fabien, get("/api/chants"))).andExpect(status().isUnauthorized());
    }
}
