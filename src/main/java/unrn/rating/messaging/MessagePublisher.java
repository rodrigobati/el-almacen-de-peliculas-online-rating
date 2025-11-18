package unrn.rating.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;

    public MessagePublisher(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public <K, T> void publish(Event<K, T> event) {
        rabbitTemplate.convertAndSend(exchange.getName(), event.routingKey(), event);
    }
}
