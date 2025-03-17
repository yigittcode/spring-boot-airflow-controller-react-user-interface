package com.yigit.airflow_spring_rest_controller.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson configuration class.
 * 
 * This class configures how dates from Airflow API are serialized to the frontend.
 * The default behavior is to send dates as Unix timestamps, but since this is not readable,
 * we serialize dates in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSXXX).
 */
@Configuration
public class JacksonConfig {

    // ISO 8601 format (2023-04-15T14:30:45.123+00:00)
    private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // ZonedDateTime custom serializer
        DateTimeFormatter zonedFormatter = DateTimeFormatter.ofPattern(ISO_8601_PATTERN);
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(zonedFormatter));
        
        // LocalDateTime custom serializer and deserializer
        DateTimeFormatter localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(localFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(localFormatter));
        
        // Custom module to convert numeric values to ISO dates
        SimpleModule dateConversionModule = new SimpleModule();
        dateConversionModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeToISOSerializer());
        
        // Register modules
        mapper.registerModules(javaTimeModule, dateConversionModule);
        
        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Ignore unknown properties
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        // Exclude null values from JSON
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Settings for proper date/time handling
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        
        return mapper;
    }
    
    /**
     * Custom serializer that converts ZonedDateTime objects to ISO 8601 format
     */
    private static class ZonedDateTimeToISOSerializer extends StdSerializer<ZonedDateTime> {

        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_8601_PATTERN);

        public ZonedDateTimeToISOSerializer() {
            super(ZonedDateTime.class);
        }

        @Override
        public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value != null) {
                gen.writeString(formatter.format(value));
            } else {
                gen.writeNull();
            }
        }
    }
} 