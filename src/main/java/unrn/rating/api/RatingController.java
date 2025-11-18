package main.java.unrn.rating.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unrn.rating.model.Rating;
import unrn.rating.service.RatingService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RatingResponseDto> crear(@RequestBody RatingRequestDto req) {
        Rating rating = RatingMapper.toModel(req);
        Rating saved = service.createRating(rating);
        return ResponseEntity.ok(RatingMapper.toDto(saved));
    }

    @GetMapping("/pelicula/{peliculaId}")
    public ResponseEntity<List<RatingResponseDto>> porPelicula(@PathVariable Long peliculaId) {
        var list = service.ratingsPorPelicula(peliculaId).stream().map(RatingMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/pelicula/{peliculaId}/promedio")
    public ResponseEntity<Double> promedio(@PathVariable Long peliculaId) {
        return ResponseEntity.ok(service.promedioPorPelicula(peliculaId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<RatingResponseDto>> porUsuario(@PathVariable String usuarioId) {
        var list = service.ratingsPorUsuario(usuarioId).stream().map(RatingMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.ratingsPorPelicula(id); // placeholder to keep behavior clear; implement delete if desired
        return ResponseEntity.noContent().build();
    }
}
