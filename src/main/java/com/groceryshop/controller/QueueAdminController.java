package com.groceryshop.controller;

import com.groceryshop.entity.OrderJob;
import com.groceryshop.repository.OrderJobRepository;
import com.groceryshop.service.valkey.OrderRetryScheduler;
import com.groceryshop.service.valkey.ValkeyStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/queue")
@PreAuthorize("hasRole('ADMIN')")
public class QueueAdminController {

    @Autowired
    private ValkeyStreamService valkeyStreamService;

    @Autowired
    private OrderRetryScheduler orderRetryScheduler;

    @Autowired
    private OrderJobRepository orderJobRepository;

    @org.springframework.beans.factory.annotation.Value("${valkey.admin.url:http://localhost:8085}")
    private String valkeyAdminUrl;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getQueueSummary() {
        Map<String, Object> response = new HashMap<>(valkeyStreamService.getQueueSummary());
        response.put("dbPendingCount", orderJobRepository.countByStatus("PENDING"));
        response.put("dbProcessingCount", orderJobRepository.countByStatus("PROCESSING"));
        response.put("dbSucceededCount", orderJobRepository.countByStatus("SUCCEEDED"));
        response.put("dbFailedCount", orderJobRepository.countByStatus("FAILED"));
        response.put("valkeyAdminUrl", valkeyAdminUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<OrderJob>> getJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            return ResponseEntity.ok(orderJobRepository.findByStatusOrderByCreatedAtDesc(status.trim(), pageRequest));
        }
        return ResponseEntity.ok(orderJobRepository.findAllByOrderByCreatedAtDesc(pageRequest));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<Map<String, Object>> retryJob(@PathVariable String jobId) {
        boolean ok = orderRetryScheduler.manualRetryJob(jobId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("jobId", jobId);
        result.put("message", ok ? "Đã gửi lại Job vào hàng đợi thành công" : "Không tìm thấy Job để thử lại");
        return ResponseEntity.ok(result);
    }

    @Autowired
    private com.groceryshop.repository.OrderRepository orderRepository;

    @PostMapping("/simulate-failure")
    public ResponseEntity<Map<String, Object>> simulateFailure(
            @RequestParam(defaultValue = "Lỗi giả lập: Timeout kết nối cơ sở dữ liệu khi trừ kho (Đã vượt quá 3 lần thử lại)") String reason) {
        com.groceryshop.entity.Order order = orderRepository.findAll().stream().findFirst().orElse(null);
        if (order == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Chưa có đơn hàng nào trong CSDL để tạo Job test lỗi");
            return ResponseEntity.badRequest().body(err);
        }

        String jobId = "JOB_TEST_ERR_" + System.currentTimeMillis();
        OrderJob failedJob = OrderJob.builder()
                .jobId(jobId)
                .order(order)
                .jobType("PROCESS_ORDER")
                .status("FAILED")
                .retryCount(3)
                .maxRetries(3)
                .errorMessage(reason)
                .startedAt(java.time.LocalDateTime.now().minusSeconds(30))
                .completedAt(java.time.LocalDateTime.now())
                .build();
        orderJobRepository.save(failedJob);

        // Đẩy vào Dead Letter Queue (DLQ) của Valkey
        valkeyStreamService.sendToDeadLetterQueue(jobId, order.getId(), "PROCESS_ORDER", reason);

        // Cập nhật Valkey Hash
        valkeyStreamService.setJobCache(
                jobId,
                "FAILED",
                "100%",
                reason,
                "test-worker-dlq"
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("jobId", jobId);
        result.put("orderId", order.getId());
        result.put("message", "Đã tạo thành công Job lỗi và đưa vào Dead Letter Queue (DLQ) của Valkey!");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/toggle-pause")
    public ResponseEntity<Map<String, Object>> togglePause() {
        boolean currentlyPaused = valkeyStreamService.isWorkerPaused();
        boolean newState = !currentlyPaused;
        valkeyStreamService.setWorkerPaused(newState);

        Map<String, Object> result = new HashMap<>();
        result.put("isPaused", newState);
        result.put("message", newState ? "Đã tạm dừng Worker nhận đơn mới" : "Worker đã tiếp tục nhận đơn");
        return ResponseEntity.ok(result);
    }
}
