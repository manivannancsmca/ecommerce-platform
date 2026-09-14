package com.ecommerce.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.UUID;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToUuidConverter());
    }

    private static class StringToUuidConverter implements Converter<String, UUID> {
        @Override
        public UUID convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }

            // Strip optional MySQL "0x" prefix
            String cleaned = source.startsWith("0x") || source.startsWith("0X") 
                ? source.substring(2) 
                : source;

            // Handle unhyphenated 32-character hex strings
            if (cleaned.length() == 32) {
                String formatted = cleaned.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
                );
                return UUID.fromString(formatted);
            }

            // Fallback for standard hyphenated UUID format
            return UUID.fromString(cleaned);
        }
    }
}