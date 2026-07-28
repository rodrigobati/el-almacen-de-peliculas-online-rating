package unrn.rating.infra.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import unrn.rating.model.Rating;
import unrn.rating.repository.RatingRepository;

/**
 * Integration tests: real JPA/Hibernate + real schema.
 * No mocks.
 */
@SpringBootTest
class RatingRepositoryIT {

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    RatingRepository ratingRepository;

    @BeforeEach
    void beforeEach() {
        // Reset DB state between tests
        ratingRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("listarRatingsPorPelicula_ratingPersistido_seLeeSinErrorDeConstructorJPA")
    void listarRatingsPorPelicula_ratingPersistido_seLeeSinErrorDeConstructorJPA() {
        // Setup: persist a Rating
        var peliculaId = 1L;
        var usuarioId = UUID.randomUUID().toString();

        // IMPORTANT: constructor signature matches project domain
        var rating = new Rating(peliculaId, usuarioId, 5, "ok");

        ratingRepository.save(rating);

        // Exercitation: read back via repository query
        var encontrados = ratingRepository.findByPeliculaId(peliculaId);

        // Verification
        assertEquals(1, encontrados.size(), "Should return exactly one rating for the movie");
        var first = encontrados.get(0);
        assertEquals(5, first.valor(), "Stored rating value should match");
        assertEquals("ok", first.comentario(), "Stored comment should match");
        assertEquals(usuarioId, first.usuarioId(), "Stored userId should match");
        assertEquals(peliculaId, first.peliculaId(), "Stored peliculaId should match");
    }
}
