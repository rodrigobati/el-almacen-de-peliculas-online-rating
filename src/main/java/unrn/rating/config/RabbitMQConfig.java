package unrn.rating.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange ratingExchange(@Value("${rating.rabbitmq.exchange:rating.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue ratingQueue(@Value("${rating.rabbitmq.queue:rating.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding binding(Queue ratingQueue, TopicExchange ratingExchange,
            @Value("${rating.rabbitmq.routing-key:rating.#}") String routingKey) {
        return BindingBuilder.bind(ratingQueue).to(ratingExchange).with(routingKey);
    }
}
