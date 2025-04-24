package org.example.cdrservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;


@Configuration
public class RabbitMQConfig {

    @Value("${const.rabbitmq.CDR_QUEUE_NAME}")
    private String CDR_QUEUE_NAME;

    @Value("${const.rabbitmq.CDR_EXCHANGE_NAME}")
    private String CDR_EXCHANGE_NAME;

    @Value("${const.rabbitmq.CDR_ROUTING_KEY}")
    private String CDR_ROUTING_KEY;

    @Bean
    public org.springframework.amqp.core.Queue cdrQueue(){
        return new Queue(CDR_QUEUE_NAME);
    }

    @Bean
    public DirectExchange cdrExchange(){
        return new DirectExchange(CDR_EXCHANGE_NAME,false,false);
    }

    @Bean
    public Binding cdrBinding(){
        return BindingBuilder
                .bind(cdrQueue())
                .to(cdrExchange())
                .with(CDR_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(Jackson2ObjectMapperBuilder builder) {
        return new Jackson2JsonMessageConverter(builder.build());
    }
}
