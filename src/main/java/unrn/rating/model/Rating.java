package unrn.rating.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rating")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long peliculaId;

    @Column(nullable = false)
    private String usuarioId;

    @Embedded
    private Puntaje puntaje;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private static final String ERROR_PELICULA_OBLIGATORIA = "El id de la película es obligatorio";
    private static final String ERROR_USUARIO_OBLIGATORIO = "El id del usuario es obligatorio";
    private static final String ERROR_COMENTARIO_VACIO = "El comentario no puede estar vacío si se proporciona";

    protected Rating() {
        // sólo JPA
    }

    private Rating(Long peliculaId,
            String usuarioId,
            Puntaje puntaje,
            String comentario,
            LocalDateTime fechaCreacion) {

        if (peliculaId == null) {
            throw new IllegalArgumentException(ERROR_PELICULA_OBLIGATORIA);
        }
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException(ERROR_USUARIO_OBLIGATORIO);
        }

        this.peliculaId = peliculaId;
        this.usuarioId = usuarioId;
        this.puntaje = puntaje;
        this.comentario = normalizarComentario(comentario);
        this.fechaCreacion = fechaCreacion;
    }

    // Fábrica estática: forma “oficial” de crear un rating
    public static Rating crear(Long peliculaId,
            String usuarioId,
            int valor,
            String comentario) {

        Puntaje puntaje = Puntaje.de(valor);
        LocalDateTime ahora = LocalDateTime.now();
        return new Rating(peliculaId, usuarioId, puntaje, comentario, ahora);
    }

    // --- Comportamiento de dominio ---

    public void cambiarPuntaje(int nuevoValor) {
        this.puntaje = Puntaje.de(nuevoValor);
    }

    public void cambiarComentario(String nuevoComentario) {
        this.comentario = normalizarComentario(nuevoComentario);
    }

    private String normalizarComentario(String comentario) {
        if (comentario == null) {
            return null;
        }
        String trimmed = comentario.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(ERROR_COMENTARIO_VACIO);
        }
        return trimmed;
    }

    public Long id() {
        return id;
    }

    public Long peliculaId() {
        return peliculaId;
    }

    public String usuarioId() {
        return usuarioId;
    }

    public int valor() {
        return puntaje.value();
    }

    public String comentario() {
        return comentario;
    }

    public LocalDateTime fechaCreacion() {
        return fechaCreacion;
    }
}
