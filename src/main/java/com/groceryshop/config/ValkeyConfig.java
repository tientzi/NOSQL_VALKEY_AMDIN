package com.groceryshop.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ValkeyConfig {

    private static final Logger log = LoggerFactory.getLogger(ValkeyConfig.class);

    @Value("${valkey.queue.orders.stream:mm:queue:orders}")
    private String ordersStream;

    @Value("${valkey.queue.orders.group:mm-order-workers}")
    private String ordersGroup;

    @Value("${valkey.queue.orders.dead-stream:mm:queue:orders:dead}")
    private String deadLetterStream;

    @Bean
    public RedisTemplate<String, Object> valkeyTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CommandLineRunner initValkeyStreamAndGroup(StringRedisTemplate stringRedisTemplate) {
        return args -> {
            try {
                // Kiểm tra kết nối Valkey bằng lệnh PING
                String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
                log.info("Valkey Server kết nối thành công: PING -> {}", pong);

                // Khởi tạo Consumer Group cho Order Stream nếu chưa tồn tại
                try {
                    stringRedisTemplate.opsForStream().createGroup(ordersStream, ReadOffset.from("0"), ordersGroup);
                    log.info("Đã tạo Consumer Group '{}' trên Stream '{}'", ordersGroup, ordersStream);
                } catch (Exception e) {
                    // Nếu group đã tồn tại, Valkey sẽ ném lỗi BUSYGROUP, điều này là bình thường
                    if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                        log.info("Consumer Group '{}' đã tồn tại trên Stream '{}'", ordersGroup, ordersStream);
                    } else {
                        log.warn("Không thể tạo Consumer Group (có thể Stream chưa có dữ liệu): {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Chưa thể kết nối tới Valkey lúc khởi động (Hệ thống vẫn hoạt động an toàn qua Outbox SQL): {}", e.getMessage());
            }
        };
    }
}
