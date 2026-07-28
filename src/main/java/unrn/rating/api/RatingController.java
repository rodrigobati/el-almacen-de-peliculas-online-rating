package unrn.rating.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import unrn.rating.model.Rating;
import unrn.rating.service.RatingService;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RatingResponseDto> crear(
            @RequestBody RatingRequestDto req,
            @AuthenticationPrincipal(errorOnInvalidType = false) Jwt jwt) {
        // Extraer el userId del JWT (claim "sub" contiene el ID de usuario de Keycloak)
        // Para testing sin auth, usar un ID fijo si jwt es null
        String usuarioId = (jwt != null) ? jwt.getSubject() : "test-user-123";
        String usuarioUsername = null;
        if (jwt != null) {
            Object claim = jwt.getClaims().get("preferred_username");
            if (claim != null) usuarioUsername = claim.toString();
        }
        Rating rating = RatingMapper.toModel(req, usuarioId, usuarioUsername);
        try {
            Rating saved = service.createRating(rating);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                    .body(RatingMapper.toDto(saved));
        } catch (unrn.rating.service.DuplicateRatingException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/pelicula/{peliculaId}")
    public ResponseEntity<List<RatingResponseDto>> porPelicula(@PathVariable Long peliculaId) {
        var list = service.ratingsPorPelicula(peliculaId).stream().map(RatingMapper::toDto)
                .collect(Collectors.toList());
        // Enrich with usernames from Keycloak if available
        try {
            var ids = list.stream().map(r -> r.usuarioId).distinct().filter(id -> id != null).toList();
            if (!ids.isEmpty()) {
                var kc = new unrn.rating.service.KeycloakUserService();
                var map = kc.findUsernamesByIds(ids);
                list.forEach(r -> {
                    if (r.usuarioId != null && map.containsKey(r.usuarioId))
                        r.usuarioUsername = map.get(r.usuarioId);
                });
            }
        } catch (Exception ignore) {
            // best-effort
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<Map<String, String>> nombresDeUsuarios(@RequestParam(name = "ids") String idsCsv) {
        if (idsCsv == null || idsCsv.isBlank())
            return ResponseEntity.ok(Collections.emptyMap());
        var ids = List.of(idsCsv.split(","));
        var kc = new unrn.rating.service.KeycloakUserService();
        var map = kc.findUsernamesByIds(ids);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/pelicula/{peliculaId}/promedio")
    public ResponseEntity<Double> promedio(@PathVariable Long peliculaId) {
        return ResponseEntity.ok(service.promedioPorPelicula(peliculaId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<RatingResponseDto>> porUsuario(
            @PathVariable String usuarioId,
            @AuthenticationPrincipal Jwt jwt) {
        // Opcional: validar que el usuario solo pueda ver sus propios ratings
        // String usuarioIdToken = jwt.getSubject();
        // if (!usuarioIdToken.equals(usuarioId)) throw new
        // AccessDeniedException("...");

        var list = service.ratingsPorUsuario(usuarioId).stream().map(RatingMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.ratingsPorPelicula(id); // placeholder to keep behavior clear; implement delete if desired
        return ResponseEntity.noContent().build();
    }
}
