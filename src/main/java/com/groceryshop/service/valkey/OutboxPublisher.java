package com.groceryshop.service.valkey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groceryshop.entity.OutboxEvent;
import com.groceryshop.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ValkeyStreamService valkeyStreamService;

    @Value("${valkey.queue.orders.stream:mm:queue:orders}")
    private String ordersStream;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Quét các bản ghi Outbox chưa gửi mỗi 500ms và đẩy vào Valkey Stream
     */
    @Scheduled(fixedDelay = 500)
    public void publishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("NEW", PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    @Transactional
    public void publishSingleEvent(OutboxEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(event.getPayload(), Map.class);

            Map<String, String> streamData = new HashMap<>();
            streamData.put("eventId", event.getEventId());
            streamData.put("aggregateType", event.getAggregateType());
            streamData.put("aggregateId", String.valueOf(event.getAggregateId()));
            streamData.put("eventType", event.getEventType());

            for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                if (entry.getValue() != null) {
                    streamData.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            // Gửi sang Valkey Stream
            String recordId = valkeyStreamService.publishToStream(ordersStream, streamData);

            // Đánh dấu đã gửi thành công
            event.setStatus("PUBLISHED");
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            log.debug("Outbox Event '{}' đã publish vào Valkey Stream '{}' với recordId: {}",
                    event.getEventId(), ordersStream, recordId);
        } catch (Exception e) {
            log.warn("Không thể publish Outbox Event '{}' (Valkey tạm thời không sẵn sàng, sẽ thử lại sau): {}",
                    event.getEventId(), e.getMessage());
            event.setRetryCount(event.getRetryCount() + 1);
            outboxEventRepository.save(event);
        }
    }
}
