package fr.vices.planning.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Une fois l'application en ligne, n'importe qui peut ouvrir la page de connexion :
 * le code d'installation empêche un inconnu de créer le premier compte (l'administrateur).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:installation;DB_CLOSE_DELAY=-1",
        "planning.dossier-fichiers=target/test-fichiers",
        "planit.code-installation=code-secret-42"})
@AutoConfigureMockMvc
class CodeInstallationTest {

    @Autowired
    private MockMvc mvc;

    private static String inscription(String code) {
        return "{\"nom\":\"Fabien\",\"email\":\"fabien@exemple.fr\",\"motDePasse\":\"motdepasse1\"%s}"
                .formatted(code == null ? "" : ",\"code\":\"" + code + "\"");
    }

    @Test
    void lePremierCompteDemandeLeCodeDInstallation() throws Exception {
        mvc.perform(get("/api/session"))
                .andExpect(jsonPath("$.premierCompte").value(true))
                .andExpect(jsonPath("$.codeInstallationRequis").value(true));

        mvc.perform(post("/api/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(inscription(null)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(inscription("mauvais-code")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(inscription("code-secret-42")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admin").value(true));
    }
}
