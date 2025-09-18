package com.rep.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaFXConfig {

    @Bean
    public SpringFXMLLoader springFXMLLoader(ApplicationContext context) {
        return new SpringFXMLLoader(context);
    }
}