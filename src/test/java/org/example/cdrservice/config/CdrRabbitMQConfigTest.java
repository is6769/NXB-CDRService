package org.example.cdrservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class CdrRabbitMQConfigTest {

    @InjectMocks
    private CdrRabbitMQConfig rabbitMQConfig;

    private final String exchangeName = "cdr.exchange";
    private final String queueName = "cdr.queue";
    private final String routingKey = "cdr.routing.key";
    private final String deadLetterExchangePostfix = ".dlx";
    private final String deadLetterRoutingKeyPostfix = ".dlr";
    private final String deadLetterQueuePostfix = ".dlq";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rabbitMQConfig, "CDR_EXCHANGE_NAME", exchangeName);
        ReflectionTestUtils.setField(rabbitMQConfig, "CDR_QUEUE_NAME", queueName);
        ReflectionTestUtils.setField(rabbitMQConfig, "CDR_ROUTING_KEY", routingKey);
        ReflectionTestUtils.setField(rabbitMQConfig, "DEAD_LETTER_EXCHANGE_POSTFIX", deadLetterExchangePostfix);
        ReflectionTestUtils.setField(rabbitMQConfig, "DEAD_LETTER_ROUTING_KEY_POSTFIX", deadLetterRoutingKeyPostfix);
        ReflectionTestUtils.setField(rabbitMQConfig, "DEAD_LETTER_QUEUE_POSTFIX", deadLetterQueuePostfix);
    }

    @Test
    void cdrExchange_shouldCreateExchangeWithCorrectName() {
        DirectExchange exchange = rabbitMQConfig.cdrExchange();
        
        assertNotNull(exchange);
        assertEquals(exchangeName, exchange.getName());
    }

    @Test
    void cdrQueue_shouldCreateQueueWithCorrectName() {
        Queue queue = rabbitMQConfig.cdrQueue();

        assertNotNull(queue);
        assertEquals(queueName, queue.getName());
    }

    @Test
    void bindingCdr_shouldCreateBindingWithCorrectParameters() {
        Binding binding = rabbitMQConfig.cdrBinding();

        assertNotNull(binding);
        assertEquals(routingKey, binding.getRoutingKey());
        assertEquals(queueName, binding.getDestination());
        assertEquals(exchangeName, binding.getExchange());
    }

    @Test
    void deadLetterCdrExchange_shouldCreateExchangeWithCorrectName() {
        DirectExchange exchange = rabbitMQConfig.deadLetterCdrExchange();

        assertNotNull(exchange);
        assertEquals(exchangeName + deadLetterExchangePostfix, exchange.getName());
    }

    @Test
    void deadLetterCdrQueue_shouldCreateQueueWithCorrectName() {
        Queue queue = rabbitMQConfig.deadLetterCdrQueue();

        assertNotNull(queue);
        assertEquals(queueName + deadLetterQueuePostfix, queue.getName());
    }

    @Test
    void bindingDeadLetterCdr_shouldCreateBindingWithCorrectParameters() {
        Binding binding = rabbitMQConfig.deadLetterCdrBinding();

        assertNotNull(binding);
        assertEquals(routingKey + deadLetterRoutingKeyPostfix, binding.getRoutingKey());
        assertEquals(queueName + deadLetterQueuePostfix, binding.getDestination());
        assertEquals(exchangeName + deadLetterExchangePostfix, binding.getExchange());
    }
}
