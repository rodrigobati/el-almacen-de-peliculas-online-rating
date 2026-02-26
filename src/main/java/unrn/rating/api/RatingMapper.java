package unrn.rating.api;

import unrn.rating.model.Rating;

public class RatingMapper {
    public static RatingResponseDto toDto(Rating r) {
        var dto = new RatingResponseDto();
        dto.id = r.id();
        dto.peliculaId = r.peliculaId();
        dto.usuarioId = r.usuarioId();
        dto.usuarioUsername = r.usuarioUsername();
        dto.valor = r.valor();
        dto.comentario = r.comentario();
        dto.fechaCreacion = r.fechaCreacion();
        return dto;
    }

    public static Rating toModel(RatingRequestDto req, String usuarioId, String usuarioUsername) {
        return Rating.crear(req.peliculaId, usuarioId, req.valor, req.comentario, usuarioUsername);
    }
}
