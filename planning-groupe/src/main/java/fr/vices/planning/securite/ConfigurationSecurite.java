package fr.vices.planning.securite;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Règles de sécurité de l'application.
 *
 * <ul>
 *   <li>Sans être connecté, on ne voit que la page de connexion.</li>
 *   <li>La connexion se fait par e-mail et mot de passe ; le serveur retient
 *       ensuite la personne grâce à un cookie de session.</li>
 *   <li>Toute demande qui modifie quelque chose doit porter un jeton anti-CSRF,
 *       pour qu'un autre site ne puisse pas agir à la place d'un membre connecté.</li>
 * </ul>
 */
@Configuration
public class ConfigurationSecurite {

    /** Chiffrement des mots de passe (BCrypt) : on ne peut pas retrouver le mot de passe à partir du résultat. */
    @Bean
    PasswordEncoder encodeurDeMotsDePasse() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain reglesDeSecurite(HttpSecurity http) throws Exception {
        RequestMatcher api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

        http
                .authorizeHttpRequests(regles -> regles
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        // la page de connexion et ce dont elle a besoin
                        .requestMatchers("/connexion.html", "/connexion.js", "/style.css", "/favicon.ico").permitAll()
                        .requestMatchers("/api/session", "/api/inscription").permitAll()
                        // tout le reste demande d'être connecté
                        .anyRequest().authenticated())
                .formLogin(connexion -> connexion
                        .loginPage("/connexion.html")
                        .loginProcessingUrl("/api/connexion")
                        .usernameParameter("email")
                        .passwordParameter("motDePasse")
                        .successHandler((requete, reponse, compte) ->
                                reponse.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .failureHandler((requete, reponse, erreur) ->
                                repondre(reponse, HttpServletResponse.SC_UNAUTHORIZED,
                                        "E-mail ou mot de passe incorrect")))
                .logout(deconnexion -> deconnexion
                        .logoutUrl("/api/deconnexion")
                        .logoutSuccessHandler((requete, reponse, compte) ->
                                reponse.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(erreurs -> erreurs
                        // sans connexion : l'API répond par une erreur 401 lisible par l'interface,
                        // et les pages renvoient vers la page de connexion
                        .authenticationEntryPoint((requete, reponse, erreur) -> {
                            if (api.matches(requete)) {
                                repondre(reponse, HttpServletResponse.SC_UNAUTHORIZED, "Connexion requise");
                            } else {
                                reponse.sendRedirect(requete.getContextPath() + "/connexion.html");
                            }
                        })
                        .accessDeniedHandler((requete, reponse, erreur) ->
                                repondre(reponse, HttpServletResponse.SC_FORBIDDEN,
                                        erreur instanceof CsrfException
                                                ? "Session expirée : recharge la page"
                                                : "Action non autorisée")))
                .csrf(csrf -> csrf.spa());

        return http.build();
    }

    /** Écrit une petite réponse JSON {"message": "..."} comme le reste de l'API. */
    private static void repondre(HttpServletResponse reponse, int statut, String message) throws IOException {
        reponse.setStatus(statut);
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
