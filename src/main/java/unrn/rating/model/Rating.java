package unrn.rating.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rating")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long peliculaId;

    private String usuarioId;

    private int valor;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    private LocalDateTime fechaCreacion;

    static final String ERROR_VALOR_FUERA_DE_RANGO = "El puntaje debe estar entre 1 y 10";
    static final String ERROR_PELICULA_OBLIGATORIA = "El id de la pelicula es obligatorio";
    static final String ERROR_USUARIO_OBLIGATORIO = "El id del usuario es obligatorio";
    static final String ERROR_COMENTARIO_VACIO = "El comentario no puede estar vacío si se proporciona";

    // Constructor usado por JPA
    protected Rating() {
    }

    public Rating(Long peliculaId, String usuarioId, int valor, String comentario) {
        this.peliculaId = peliculaId;
        this.usuarioId = usuarioId;
        this.valor = valor;
        this.comentario = comentario == null ? null : comentario.trim();
        this.fechaCreacion = LocalDateTime.now();

        assertPeliculaIdPresente();
        assertUsuarioIdPresente();
        assertValorEnRango();
        assertComentarioSiSeInformaNoVacio();
    }

    private void assertValorEnRango() {
        if (valor < 1 || valor > 10) {
            throw new RuntimeException(ERROR_VALOR_FUERA_DE_RANGO);
        }
    }

    private void assertPeliculaIdPresente() {
        if (peliculaId == null) {
            throw new RuntimeException(ERROR_PELICULA_OBLIGATORIA);
        }
    }

    private void assertUsuarioIdPresente() {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new RuntimeException(ERROR_USUARIO_OBLIGATORIO);
        }
    }

    private void assertComentarioSiSeInformaNoVacio() {
        if (comentario != null && comentario.isBlank()) {
            throw new RuntimeException(ERROR_COMENTARIO_VACIO);
        }
    }

    // Accessors with intent (not plain setters/getters)
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
        return valor;
    }

    public String comentario() {
        return comentario;
    }

    public LocalDateTime fechaCreacion() {
        return fechaCreacion;
    }
}
