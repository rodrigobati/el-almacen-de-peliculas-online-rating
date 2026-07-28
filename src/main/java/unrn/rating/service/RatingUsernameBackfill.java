package unrn.rating.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import unrn.rating.repository.RatingRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RatingUsernameBackfill {

    private final RatingRepository repository;

    public RatingUsernameBackfill(RatingRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            List<unrn.rating.model.Rating> missing = repository.findByUsuarioUsernameIsNull();
            if (missing.isEmpty()) return;

            var ids = missing.stream().map(r -> r.usuarioId()).distinct().filter(id -> id != null).collect(Collectors.toList());
            if (ids.isEmpty()) return;

            var kc = new KeycloakUserService();
            Map<String, String> map = kc.findUsernamesByIds(ids);
            if (map.isEmpty()) return;

            boolean changed = false;
            for (var r : missing) {
                var uid = r.usuarioId();
                if (uid != null && map.containsKey(uid)) {
                    r.actualizarUsuarioUsername(map.get(uid));
                    repository.save(r);
                    changed = true;
                }
            }
            if (changed) System.out.println("Backfilled usuarioUsername for ratings");
        } catch (Exception e) {
            System.err.println("RatingUsernameBackfill failed: " + e.getMessage());
        }
    }
}
