package unrn.rating.api;

public class RatingRequestDto {
    public Long peliculaId;
    public String usuarioId;
    public int valor;
    public String comentario;

    public RatingRequestDto() {
    }
}
