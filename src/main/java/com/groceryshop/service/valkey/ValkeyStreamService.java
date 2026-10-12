package com.groceryshop.service.valkey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ValkeyStreamService {

    private static final Logger log = LoggerFactory.getLogger(ValkeyStreamService.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${valkey.queue.orders.stream:mm:queue:orders}")
    private String ordersStream;

    @Value("${valkey.queue.orders.group:mm-order-workers}")
    private String ordersGroup;

    @Value("${valkey.queue.orders.dead-stream:mm:queue:orders:dead}")
    private String deadLetterStream;

    public static final String RETRY_ZSET_KEY = "mm:retry:orders";
    public static final String PAUSE_FLAG_KEY = "mm:system:worker_pause";
    public static final String JOB_HASH_PREFIX = "mm:job:";

    /**
     * Gửi message vào Valkey Stream
     */
    public String publishToStream(String streamKey, Map<String, String> data) {
        try {
            StringRecord record = StreamRecords.string(data).withStreamKey(streamKey);
            RecordId recordId = stringRedisTemplate.opsForStream().add(record);
            return recordId != null ? recordId.getValue() : null;
        } catch (Exception e) {
            log.error("Lỗi khi gửi message vào Valkey Stream '{}': {}", streamKey, e.getMessage());
            throw new RuntimeException("Không thể gửi tin nhắn vào Valkey Stream: " + e.getMessage(), e);
        }
    }

    /**
     * Xác nhận xử lý xong message trong Stream (XACK)
     */
    public void acknowledge(String streamKey, String groupName, String recordId) {
        try {
            stringRedisTemplate.opsForStream().acknowledge(streamKey, groupName, recordId);
        } catch (Exception e) {
            log.warn("Lỗi khi XACK message '{}' trên Stream '{}': {}", recordId, streamKey, e.getMessage());
        }
    }

    /**
     * Lưu cache trạng thái Job vào Valkey Hash để client tra cứu tức thì
     */
    public void setJobCache(String jobId, String status, String progress, String message, String workerId) {
        try {
            String key = JOB_HASH_PREFIX + jobId;
            Map<String, String> map = new HashMap<>();
            map.put("jobId", jobId);
            map.put("status", status);
            if (progress != null) map.put("progress", progress);
            if (message != null) map.put("message", message);
            if (workerId != null) map.put("workerId", workerId);
            map.put("updatedAt", Instant.now().toString());

            stringRedisTemplate.opsForHash().putAll(key, map);
            // TTL 24 giờ cho cache Job đã hoàn tất
            if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
                stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi cập nhật Valkey Hash Job '{}': {}", jobId, e.getMessage());
        }
    }

    /**
     * Lấy thông tin trạng thái Job từ Valkey Hash
     */
    public Map<Object, Object> getJobCache(String jobId) {
        try {
            return stringRedisTemplate.opsForHash().entries(JOB_HASH_PREFIX + jobId);
        } catch (Exception e) {
            log.warn("Lỗi khi đọc Valkey Hash Job '{}': {}", jobId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Đưa Job vào hàng đợi Retry với lịch hẹn (Sorted Set)
     */
    public void scheduleRetry(String jobId, long delaySeconds) {
        try {
            long executeAt = System.currentTimeMillis() + (delaySeconds * 1000);
            stringRedisTemplate.opsForZSet().add(RETRY_ZSET_KEY, jobId, executeAt);
            log.info("Đã lên lịch retry cho Job '{}' sau {} giây", jobId, delaySeconds);
        } catch (Exception e) {
            log.error("Lỗi khi lên lịch retry cho Job '{}': {}", jobId, e.getMessage());
        }
    }

    /**
     * Lấy các Job đến hạn retry
     */
    public Set<String> getDueRetryJobs() {
        try {
            long now = System.currentTimeMillis();
            return stringRedisTemplate.opsForZSet().rangeByScore(RETRY_ZSET_KEY, 0, now);
        } catch (Exception e) {
            log.warn("Lỗi khi đọc Sorted Set Retry: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Xóa Job khỏi hàng đợi Retry
     */
    public void removeRetryJob(String jobId) {
        try {
            stringRedisTemplate.opsForZSet().remove(RETRY_ZSET_KEY, jobId);
        } catch (Exception e) {
            log.warn("Lỗi khi xóa Job '{}' khỏi Retry ZSet: {}", jobId, e.getMessage());
        }
    }

    /**
     * Đưa Job quá hạn retry vào Dead Letter Queue (DLQ)
     */
    public void sendToDeadLetterQueue(String jobId, Long orderId, String jobType, String error) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("jobId", jobId);
            data.put("orderId", String.valueOf(orderId));
            data.put("jobType", jobType != null ? jobType : "PROCESS_ORDER");
            data.put("error", error != null ? error : "Unknown error");
            data.put("failedAt", Instant.now().toString());

            publishToStream(deadLetterStream, data);
            setJobCache(jobId, "FAILED_DLQ", "100%", "Chuyển vào hàng đợi lỗi DLQ: " + error, null);
            log.warn("Đã chuyển Job '{}' vào Dead Letter Queue: {}", jobId, error);
        } catch (Exception e) {
            log.error("Lỗi khi đưa Job '{}' vào DLQ: {}", jobId, e.getMessage());
        }
    }

    /**
     * Bật/tắt trạng thái tạm dừng của Worker
     */
    public void setWorkerPaused(boolean paused) {
        try {
            stringRedisTemplate.opsForValue().set(PAUSE_FLAG_KEY, String.valueOf(paused));
        } catch (Exception e) {
            log.error("Lỗi khi đặt cờ tạm dừng Worker: {}", e.getMessage());
        }
    }

    public boolean isWorkerPaused() {
        try {
            String val = stringRedisTemplate.opsForValue().get(PAUSE_FLAG_KEY);
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy thống kê số lượng Message trong Queue & DLQ cho Admin Dashboard
     */
    public Map<String, Object> getQueueSummary() {
        Map<String, Object> summary = new HashMap<>();
        try {
            Long streamLength = stringRedisTemplate.opsForStream().size(ordersStream);
            Long dlqLength = stringRedisTemplate.opsForStream().size(deadLetterStream);
            Long retryLength = stringRedisTemplate.opsForZSet().zCard(RETRY_ZSET_KEY);

            summary.put("streamLength", streamLength != null ? streamLength : 0);
            summary.put("deadLetterLength", dlqLength != null ? dlqLength : 0);
            summary.put("retryLength", retryLength != null ? retryLength : 0);
            summary.put("isPaused", isWorkerPaused());
            summary.put("status", "CONNECTED");
        } catch (Exception e) {
            summary.put("status", "DISCONNECTED");
            summary.put("error", e.getMessage());
            summary.put("streamLength", 0);
            summary.put("deadLetterLength", 0);
            summary.put("retryLength", 0);
            summary.put("isPaused", false);
        }
        return summary;
    }
}
