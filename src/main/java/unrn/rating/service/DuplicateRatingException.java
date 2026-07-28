package unrn.rating.service;

/**
 * Runtime exception thrown when a user attempts to create a duplicate rating
 * (same peliculaId + usuarioId). Message is package-visible so tests can assert
 * on it.
 */
public class DuplicateRatingException extends RuntimeException {

    static final String ERROR_RATING_DUPLICADO = "El usuario ya calificó esta película";

    public DuplicateRatingException() {
        super(ERROR_RATING_DUPLICADO);
    }
}
