package com.bantumi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Глобальная конфигурация веб-слоя.
 * CORS вынесен сюда, чтобы не дублировать @CrossOrigin в каждом контроллере.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Разрешаем запросы с любых источников для всех /api/v1/ эндпоинтов.
     * В продакшене стоит ограничить originPatterns конкретными адресами.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
