package com.zzw.zzwgx.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson配置类
 * 配置LocalDateTime的序列化和反序列化格式
 */
@Configuration
public class JacksonConfig {
    
    /**
     * 日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 日期时间格式化器
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 配置LocalDateTime的序列化器
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        
        // 配置LocalDateTime的反序列化器，支持多种格式
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)) {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String dateString = parser.getText();
                if (dateString == null || dateString.isEmpty()) {
                    return null;
                }
                
                // 尝试使用自定义格式解析
                try {
                    return LocalDateTime.parse(dateString, DATE_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    // 如果失败，尝试使用ISO格式（带T的格式）
                    try {
                        return LocalDateTime.parse(dateString);
                    } catch (DateTimeParseException e2) {
                        // 如果都失败，尝试替换空格为T
                        try {
                            return LocalDateTime.parse(dateString.replace(" ", "T"));
                        } catch (DateTimeParseException e3) {
                            throw new IOException("无法解析日期时间: " + dateString, e3);
                        }
                    }
                }
            }
        });
        
        return builder
                .modules(javaTimeModule)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}

