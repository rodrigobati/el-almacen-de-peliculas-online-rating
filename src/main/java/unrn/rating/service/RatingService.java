package unrn.rating.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unrn.rating.model.Rating;
import unrn.rating.repository.RatingRepository;
import unrn.rating.messaging.Event;
import unrn.rating.messaging.EventType;
import unrn.rating.messaging.MessagePublisher;

import java.util.List;

@Service
public class RatingService {

    private final RatingRepository repository;
    private final MessagePublisher publisher;

    public RatingService(RatingRepository repository, MessagePublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public Rating createRating(Rating rating) {
        Rating saved = repository.save(rating);
        // publish domain event
        Event<Long, Rating> event = new Event<>(EventType.CREATE, saved.id(), saved, "rating.created");
        publisher.publish(event);
        return saved;
    }

    public List<Rating> ratingsPorPelicula(Long peliculaId) {
        return repository.findByPeliculaId(peliculaId);
    }

    public List<Rating> ratingsPorUsuario(String usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    public double promedioPorPelicula(Long peliculaId) {
        Double avg = repository.promedioPorPelicula(peliculaId);
        return avg == null ? 0.0 : avg;
    }
}
