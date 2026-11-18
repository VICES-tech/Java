package fr.vices.planning.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Transforme certaines erreurs en messages clairs pour l'interface. */
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> fichierTropGros() {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "Fichier trop volumineux (500 Mo maximum)"));
    }

    /** Un champ de formulaire mal rempli : on renvoie le message écrit sur le champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> champInvalide(MethodArgumentNotValidException erreur) {
        String message = erreur.getBindingResult().getFieldErrors().stream()
                .map(champ -> champ.getDefaultMessage())
                .filter(texte -> texte != null && !texte.isBlank())
                .findFirst()
                .orElse("Formulaire incomplet ou invalide");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
