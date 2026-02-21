package unrn.rating.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import unrn.rating.api.RatingController;
import unrn.rating.config.SecurityConfiguration;
import unrn.rating.service.RatingService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RatingController.class)
@Import(SecurityConfiguration.class)
class RatingSecurityClaimsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RatingService ratingService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("rating endpoint sin autenticacion retorna401")
    void ratingEndpoint_sinAutenticacion_retorna401() throws Exception {
        // Setup: Preparar el escenario

        // Ejercitación: Ejecutar la acción a probar
        mockMvc.perform(get("/api/ratings/pelicula/1"))
                // Verificación: Verificar el resultado esperado
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rating endpoint con claims jwt admin evita401")
    void ratingEndpoint_conClaimsJwtAdmin_retorna404() throws Exception {
        // Setup: Preparar el escenario
        when(jwtDecoder.decode(anyString())).thenReturn(jwtConRealmRoleAdmin());
        when(ratingService.ratingsPorPelicula(1L)).thenReturn(List.of());

        // Ejercitación: Ejecutar la acción a probar
        mockMvc.perform(get("/api/ratings/pelicula/1")
                .header("Authorization", "Bearer token-admin"))
                // Verificación: Verificar el resultado esperado
                .andExpect(status().isNotFound());
    }

    private Jwt jwtConRealmRoleAdmin() {
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
}
