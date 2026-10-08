package com.banque.transaction.Exceptions;

import com.banque.transaction.ServerResponse.ServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ServerResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ServerResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SoldeInsuffisantException.class)
    public ResponseEntity<ServerResponse> handleSolde(SoldeInsuffisantException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(OperationNonAutoriseeException.class)
    public ResponseEntity<ServerResponse> handleForbidden(OperationNonAutoriseeException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Échec de @Valid sur un corps de requête : on liste les champs en erreur. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ServerResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /** JSON malformé, UUID ou montant illisible dans le corps. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ServerResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Corps de requête invalide ou illisible");
    }

    /** Ex. /findbyid/abc au lieu d'un UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ServerResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Valeur invalide pour le paramètre '" + ex.getName() + "'");
    }

    /** Filet de sécurité de la base (clé étrangère, unicité). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ServerResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violation d'intégrité : {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Opération impossible : des données liées existent ou une valeur est déjà utilisée");
    }

    /**
     * Dernier recours. Les exceptions du framework (URL inconnue → 404, méthode HTTP
     * non supportée → 405...) implémentent ErrorResponse : on garde leur code HTTP
     * au lieu de tout transformer en 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServerResponse> handleOthers(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            String detail = er.getBody().getDetail();
            return build(er.getStatusCode(), detail != null ? detail : "Requête invalide");
        }
        log.error("Erreur inattendue", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
    }

    private ResponseEntity<ServerResponse> build(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(new ServerResponse(message, false));
    }
}