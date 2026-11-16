package fr.vices.planning.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Démarre un vrai serveur et lui parle comme un navigateur, cookies compris.
 * Vérifie ce que les autres tests ne voient pas : le texte des erreurs tel qu'il
 * arrive réellement à l'interface (« Ce nom est déjà pris », etc.).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:http;DB_CLOSE_DELAY=-1",
        "planning.dossier-fichiers=target/test-fichiers"})
class MessagesErreurHttpTest {

    @Value("${local.server.port}")
    private int port;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient navigateur = HttpClient.newBuilder().cookieHandler(cookies).build();
    private final ObjectMapper json = new ObjectMapper();

    private HttpResponse<String> lire(String chemin) throws Exception {
        return navigateur.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> envoyer(String chemin, String type, String corps) throws Exception {
        String jeton = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(cookie -> cookie.getValue())
                .findFirst().orElse("");
        return navigateur.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                        .header("Content-Type", type)
                        .header("X-XSRF-TOKEN", jeton)
                        .POST(HttpRequest.BodyPublishers.ofString(corps))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void lesMessagesDErreurArriventJusquALInterface() throws Exception {
        // première visite : le serveur dépose le jeton anti-CSRF dans un cookie
        JsonNode session = json.readTree(lire("/api/session").body());
        assertThat(session.get("connecte").asBoolean()).isFalse();

        String fabien = "{\"nom\":\"Fabien\",\"email\":\"fabien@exemple.fr\",\"motDePasse\":\"motdepasse1\"}";
        assertThat(envoyer("/api/inscription", "application/json", fabien).statusCode()).isEqualTo(201);

        // l'inscription est maintenant fermée : le message doit être lisible
        HttpResponse<String> refus = envoyer("/api/inscription", "application/json",
                "{\"nom\":\"Intrus\",\"email\":\"intrus@exemple.fr\",\"motDePasse\":\"motdepasse1\"}");
        assertThat(refus.statusCode()).isEqualTo(403);
        assertThat(json.readTree(refus.body()).get("message").asString())
                .startsWith("L'inscription se fait sur invitation");

        // connexion comme dans le navigateur, puis une erreur côté planning
        assertThat(envoyer("/api/connexion", "application/x-www-form-urlencoded",
                "email=fabien%40exemple.fr&motDePasse=motdepasse1").statusCode()).isEqualTo(204);
        assertThat(json.readTree(lire("/api/session").body()).get("membre").get("nom").asString())
                .isEqualTo("Fabien");

        HttpResponse<String> invalide = envoyer("/api/evenements", "application/json",
                "{\"titre\":\"Oups\",\"debut\":\"2026-10-20T21:00:00\",\"fin\":\"2026-10-20T19:00:00\"}");
        assertThat(invalide.statusCode()).isEqualTo(400);
        assertThat(json.readTree(invalide.body()).get("message").asString())
                .isEqualTo("La fin doit être après le début");

        HttpResponse<String> introuvable = lire("/api/chants/999");
        assertThat(introuvable.statusCode()).isEqualTo(404);
        assertThat(json.readTree(introuvable.body()).get("message").asString()).isEqualTo("Chant introuvable");
    }
}
