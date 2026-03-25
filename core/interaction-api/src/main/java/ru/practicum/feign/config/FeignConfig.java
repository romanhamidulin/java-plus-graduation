package ru.practicum.feign.config;

import feign.Feign;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.feign.decoder.FeignErrorDecoder;

@Configuration
public class FeignConfig {

    @Bean
    public Feign.Builder feignBuilderDecoder() {
        return Feign.builder()
                .errorDecoder(new FeignErrorDecoder());
    }
}
