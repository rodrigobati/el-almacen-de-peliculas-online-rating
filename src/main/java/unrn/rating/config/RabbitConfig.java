package unrn.rating.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rating.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${rating.rabbitmq.queue}")
    private String queueName;

    @Value("${rating.rabbitmq.routing-key}")
    private String routingKey;

    @Bean
    public TopicExchange ratingExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue ratingQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding ratingBinding(Queue ratingQueue, TopicExchange ratingExchange) {
        return BindingBuilder.bind(ratingQueue).to(ratingExchange).with(routingKey);
    }
}
