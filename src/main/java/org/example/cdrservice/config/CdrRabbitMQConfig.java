package org.example.cdrservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CdrRabbitMQConfig {

    @Value("${const.rabbitmq.cdr.CDR_QUEUE_NAME}")
    private String CDR_QUEUE_NAME;

    @Value("${const.rabbitmq.cdr.CDR_EXCHANGE_NAME}")
    private String CDR_EXCHANGE_NAME;

    @Value("${const.rabbitmq.cdr.CDR_ROUTING_KEY}")
    private String CDR_ROUTING_KEY;

    @Value("${const.rabbitmq.dead-letter.DEAD_LETTER_EXCHANGE_POSTFIX}")
    private String DEAD_LETTER_EXCHANGE_POSTFIX;

    @Value("${const.rabbitmq.dead-letter.DEAD_LETTER_ROUTING_KEY_POSTFIX}")
    private String DEAD_LETTER_ROUTING_KEY_POSTFIX;

    @Value("${const.rabbitmq.dead-letter.DEAD_LETTER_QUEUE_POSTFIX}")
    private String DEAD_LETTER_QUEUE_POSTFIX;


    @Bean
    public DirectExchange deadLetterCdrExchange(){
        return new DirectExchange(CDR_EXCHANGE_NAME+DEAD_LETTER_EXCHANGE_POSTFIX,false,false);
    }

    @Bean
    public Queue deadLetterCdrQueue(){
        return new Queue(CDR_QUEUE_NAME+DEAD_LETTER_QUEUE_POSTFIX);
    }

    @Bean
    public Binding deadLetterCdrBinding(){
        return BindingBuilder
                .bind(deadLetterCdrQueue())
                .to(deadLetterCdrExchange())
                .with(CDR_ROUTING_KEY+DEAD_LETTER_ROUTING_KEY_POSTFIX);
    }

    @Bean
    public Queue cdrQueue(){
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
}
