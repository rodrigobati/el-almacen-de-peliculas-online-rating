package unrn.rating.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import unrn.rating.api.RatingController;
import unrn.rating.config.SecurityConfiguration;
import unrn.rating.model.Rating;
import unrn.rating.service.RatingService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RatingController.class)
@Import({RatingController.class, SecurityConfiguration.class, RatingSecurityClaimsIntegrationTest.TestDoubles.class})
class RatingSecurityClaimsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET ratings por pelicula es publico")
    void ratingEndpoint_sinAutenticacion_retorna200() throws Exception {
        mockMvc.perform(get("/api/ratings/pelicula/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST rating sin autenticacion retorna401")
    void crearRating_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post("/api/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"peliculaId\":1,\"valor\":5,\"comentario\":\"ok\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rating endpoint con claims jwt admin evita401")
    void ratingEndpoint_conClaimsJwtAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/api/ratings/pelicula/1")
                .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk());
    }

    private static Jwt jwtConRealmRoleAdmin() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("realm_access", Map.of("roles", List.of("admin")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @TestConfiguration
    static class TestDoubles {
        @Bean
        RatingService ratingService() {
            return new RatingService(null, null) {
                @Override
                public Rating createRating(Rating rating) {
                    return rating;
                }

                @Override
                public List<Rating> ratingsPorPelicula(Long peliculaId) {
                    return List.of();
                }

                @Override
                public List<Rating> ratingsPorUsuario(String usuarioId) {
                    return List.of();
                }

                @Override
                public double promedioPorPelicula(Long peliculaId) {
                    return 0.0;
                }
            };
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> jwtConRealmRoleAdmin();
        }
    }
}