package org.example.cdrservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;


@Configuration
public class RabbitMQConfig {

    @Bean
    public org.springframework.amqp.core.Queue cdrQueue(){
        return new Queue("cdr.queue");
    }

    @Bean
    public DirectExchange cdrExchange(){
        return new DirectExchange("cdr.direct",false,false);
    }

    @Bean
    public Binding cdrBinding(){
        return BindingBuilder
                .bind(cdrQueue())
                .to(cdrExchange())
                .with("cdr.created");
    }

    @Bean
    public MessageConverter jsonMessageConverter(Jackson2ObjectMapperBuilder builder) {
        return new Jackson2JsonMessageConverter(builder.build());
    }
}
