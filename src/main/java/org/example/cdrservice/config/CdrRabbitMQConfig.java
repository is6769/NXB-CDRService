package org.example.cdrservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Конфигурационный класс для RabbitMQ, связанный с обработкой CDR (Call Data Record).
 * Определяет бины для очередей, обменников и связываний, используемых для отправки и обработки сообщений CDR,
 * включая настройку очереди недоставленных сообщений (DLQ).
 */
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


    /**
     * Создает обменник для недоставленных сообщений CDR (dead-letter exchange).
     * Сообщения, которые не могут быть успешно обработаны в основной очереди CDR, могут быть направлены сюда.
     *
     * @return {@link DirectExchange} для недоставленных сообщений CDR.
     */
    @Bean
    public DirectExchange deadLetterCdrExchange(){
        return new DirectExchange(CDR_EXCHANGE_NAME+DEAD_LETTER_EXCHANGE_POSTFIX,false,false);
    }

    /**
     * Создает очередь для недоставленных сообщений CDR (dead-letter queue).
     * Эта очередь будет хранить сообщения, обработка которых в основной очереди CDR завершилась неудачей.
     *
     * @return {@link Queue} для недоставленных сообщений CDR.
     */
    @Bean
    public Queue deadLetterCdrQueue(){
        return new Queue(CDR_QUEUE_NAME+DEAD_LETTER_QUEUE_POSTFIX);
    }

    /**
     * Создает binding между обменником недоставленных сообщений CDR и очередью недоставленных сообщений CDR.
     * Использует специальный ключ маршрутизации для недоставленных сообщений.
     *
     * @return {@link Binding} для настройки недоставленных сообщений CDR.
     */
    @Bean
    public Binding deadLetterCdrBinding(){
        return BindingBuilder
                .bind(deadLetterCdrQueue())
                .to(deadLetterCdrExchange())
                .with(CDR_ROUTING_KEY+DEAD_LETTER_ROUTING_KEY_POSTFIX);
    }

    /**
     * Создает основную очередь для сообщений CDR.
     *
     * @return {@link Queue} для сообщений CDR.
     */
    @Bean
    public Queue cdrQueue(){
        return new Queue(CDR_QUEUE_NAME);
    }

    /**
     * Создает основной прямой обменник (direct exchange) для сообщений CDR.
     *
     * @return {@link DirectExchange} для сообщений CDR.
     */
    @Bean
    public DirectExchange cdrExchange(){
        return new DirectExchange(CDR_EXCHANGE_NAME,false,false);
    }

    /**
     * Создает binding между основным обменником CDR и основной очередью CDR.
     * Использует основной ключ маршрутизации для сообщений CDR.
     *
     * @return {@link Binding} для основной настройки CDR.
     */
    @Bean
    public Binding cdrBinding(){
        return BindingBuilder
                .bind(cdrQueue())
                .to(cdrExchange())
                .with(CDR_ROUTING_KEY);
    }
}
