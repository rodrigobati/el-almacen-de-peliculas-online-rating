package unrn.rating.api;

import unrn.rating.model.Rating;

public class RatingMapper {
    public static RatingResponseDto toDto(Rating r) {
        var dto = new RatingResponseDto();
        dto.id = r.id();
        dto.peliculaId = r.peliculaId();
        dto.usuarioId = r.usuarioId();
        dto.valor = r.valor();
        dto.comentario = r.comentario();
        dto.fechaCreacion = r.fechaCreacion();
        return dto;
    }

    public static Rating toModel(RatingRequestDto req) {
        return Rating.crear(req.peliculaId, req.usuarioId, req.valor, req.comentario);
    }
}
