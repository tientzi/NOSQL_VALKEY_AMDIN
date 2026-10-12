package com.groceryshop.service.valkey;

import com.groceryshop.entity.OrderJob;
import com.groceryshop.repository.OrderJobRepository;
import com.groceryshop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderJobWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderJobWorker.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ValkeyStreamService valkeyStreamService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderJobRepository orderJobRepository;

    @Value("${valkey.queue.orders.stream:mm:queue:orders}")
    private String ordersStream;

    @Value("${valkey.queue.orders.group:mm-order-workers}")
    private String ordersGroup;

    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * Vòng lặp Worker đọc message từ Consumer Group mm-order-workers mỗi 300ms
     */
    @Scheduled(fixedDelay = 300)
    public void consumeOrderJobs() {
        if (valkeyStreamService.isWorkerPaused()) {
            return;
        }

        try {
            StreamReadOptions options = StreamReadOptions.empty().count(10).block(Duration.ofMillis(300));
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(ordersGroup, workerId),
                    options,
                    StreamOffset.create(ordersStream, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                processSingleRecord(record);
            }
        } catch (Exception e) {
            // Khi Valkey chưa bật hoặc Stream chưa có message, bắt lỗi và không làm crash app
            log.trace("Worker chờ message từ Valkey Stream: {}", e.getMessage());
        }
    }

    private void processSingleRecord(MapRecord<String, Object, Object> record) {
        String recordId = record.getId().getValue();
        Map<Object, Object> value = record.getValue();

        String jobId = (String) value.get("jobId");
        String orderIdStr = (String) value.get("orderId");
        String jobType = (String) value.get("jobType");

        if (jobId == null || orderIdStr == null) {
            log.warn("Message trên Stream thiếu jobId hoặc orderId, bỏ qua và XACK: {}", value);
            valkeyStreamService.acknowledge(ordersStream, ordersGroup, recordId);
            return;
        }

        Long orderId = Long.valueOf(orderIdStr);
        log.info("Worker [{}] tiếp nhận Job '{}' cho Đơn hàng #{}", workerId, jobId, orderId);

        try {
            // Thực thi kiểm tra tồn kho & phân bổ lô FIFO trong Transaction SQL
            boolean success = orderService.processOrderInventoryAndBatches(orderId, jobId);

            // Xác nhận hoàn tất Job (XACK)
            valkeyStreamService.acknowledge(ordersStream, ordersGroup, recordId);
            log.info("Worker [{}] đã hoàn tất Job '{}' (Kết quả: {})", workerId, jobId, success ? "SUCCEEDED" : "REJECTED_OUT_OF_STOCK");
        } catch (Exception e) {
            log.error("Worker [{}] gặp lỗi kỹ thuật khi xử lý Job '{}': {}", workerId, jobId, e.getMessage(), e);
            handleTechnicalError(recordId, jobId, orderId, jobType, e.getMessage());
        }
    }

    private void handleTechnicalError(String recordId, String jobId, Long orderId, String jobType, String error) {
        OrderJob job = orderJobRepository.findByJobId(jobId).orElse(null);
        int retryCount = job != null ? job.getRetryCount() + 1 : 1;
        int maxRetries = job != null ? job.getMaxRetries() : 3;

        if (job != null) {
            job.setRetryCount(retryCount);
            job.setErrorMessage("Lỗi kỹ thuật: " + error);
            orderJobRepository.save(job);
        }

        // XACK tin nhắn hiện tại để giải phóng hàng đợi chính
        valkeyStreamService.acknowledge(ordersStream, ordersGroup, recordId);

        if (retryCount <= maxRetries) {
            // Lên lịch retry với Backoff tăng dần (5s, 15s, 30s)
            long delay = retryCount == 1 ? 5 : (retryCount == 2 ? 15 : 30);
            valkeyStreamService.scheduleRetry(jobId, delay);
            valkeyStreamService.setJobCache(jobId, "RETRYING", "0%",
                    "Gặp lỗi kỹ thuật, lên lịch thử lại lần " + retryCount + "/" + maxRetries + " sau " + delay + "s: " + error,
                    workerId);
        } else {
            // Đã thử quá số lần cho phép: Đưa vào Dead Letter Queue (DLQ)
            valkeyStreamService.sendToDeadLetterQueue(jobId, orderId, jobType, error);
            if (job != null) {
                job.setStatus("FAILED");
                orderJobRepository.save(job);
            }
        }
    }
}
