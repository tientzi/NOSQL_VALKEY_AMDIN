package com.groceryshop.service.valkey;

import com.groceryshop.entity.OrderJob;
import com.groceryshop.repository.OrderJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class OrderRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderRetryScheduler.class);

    @Autowired
    private ValkeyStreamService valkeyStreamService;

    @Autowired
    private OrderJobRepository orderJobRepository;

    @Value("${valkey.queue.orders.stream:mm:queue:orders}")
    private String ordersStream;

    /**
     * Quét hàng đợi Retry ZSet mỗi 1 giây để đẩy lại vào Stream khi đến hẹn
     */
    @Scheduled(fixedDelay = 1000)
    public void processDueRetries() {
        Set<String> dueJobIds = valkeyStreamService.getDueRetryJobs();
        if (dueJobIds == null || dueJobIds.isEmpty()) {
            return;
        }

        for (String jobId : dueJobIds) {
            try {
                OrderJob job = orderJobRepository.findByJobId(jobId).orElse(null);
                if (job == null) {
                    valkeyStreamService.removeRetryJob(jobId);
                    continue;
                }

                // Đẩy lại vào Stream chính
                Map<String, String> data = new HashMap<>();
                data.put("jobId", job.getJobId());
                data.put("orderId", String.valueOf(job.getOrder().getId()));
                data.put("jobType", job.getJobType());
                data.put("isRetry", "true");
                data.put("retryCount", String.valueOf(job.getRetryCount()));

                valkeyStreamService.publishToStream(ordersStream, data);
                valkeyStreamService.removeRetryJob(jobId);

                log.info("Đã tái phát hành (Re-publish) Job '{}' vào Stream '{}' để thử lại", jobId, ordersStream);
            } catch (Exception e) {
                log.warn("Lỗi khi xử lý retry cho Job '{}': {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Kích hoạt thử lại thủ công một Job từ Dead Letter Queue (Admin Action)
     */
    public boolean manualRetryJob(String jobId) {
        OrderJob job = orderJobRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            return false;
        }

        job.setStatus("PENDING");
        job.setRetryCount(0);
        job.setErrorMessage("Kích hoạt thử lại thủ công bởi Admin lúc " + LocalDateTime.now());
        orderJobRepository.save(job);

        Map<String, String> data = new HashMap<>();
        data.put("jobId", job.getJobId());
        data.put("orderId", String.valueOf(job.getOrder().getId()));
        data.put("jobType", job.getJobType());
        data.put("manualRetry", "true");

        valkeyStreamService.publishToStream(ordersStream, data);
        valkeyStreamService.setJobCache(jobId, "PENDING", "0%", "Admin đã kích hoạt thử lại thủ công", null);
        log.info("Admin đã kích hoạt thử lại thủ công cho Job '{}'", jobId);
        return true;
    }
}
