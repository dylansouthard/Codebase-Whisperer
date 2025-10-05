package info.dylansouthard.StraysBookAPI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Force UTF-8 decoding of response bodies
        restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .findFirst()
                .ifPresent(c -> ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8));

        return restTemplate;
    }
}