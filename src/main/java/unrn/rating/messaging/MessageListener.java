package main.java.unrn.rating.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import unrn.rating.messaging.Event;

@Component
public class MessageListener {

    // placeholder to handle events from other services, e.g., pelicula.deleted
    @RabbitListener(queues = "${rating.rabbitmq.queue:rating.queue}")
    public void onMessage(Event<?, ?> event) {
        // For future use: react to other events (e.g., delete ratings when a movie is
        // deleted)
    }
}
