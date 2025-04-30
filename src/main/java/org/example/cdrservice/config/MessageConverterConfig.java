package org.example.cdrservice.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class MessageConverterConfig {
    @Bean
    public MessageConverter jsonMessageConverter(Jackson2ObjectMapperBuilder builder) {
        return new Jackson2JsonMessageConverter(builder.build());
    }
}
