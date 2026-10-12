package com.groceryshop.service;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.entity.*;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private InventoryLedgerService ledgerService;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    @Autowired
    private OrderJobRepository orderJobRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private com.groceryshop.service.valkey.ValkeyStreamService valkeyStreamService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Transactional
    public OrderDTO createOrder(Long userId, OrderDTO orderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng của người dùng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        class ItemAllocation {
            Product product;
            String productName;
            int quantity;
            BigDecimal price;
        }
        List<ItemAllocation> itemAllocations = new java.util.ArrayList<>();

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.getIsActive()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện tại không hoạt động");
            }
            Inventory inventory = product.getInventory();
            int currentStock = inventory != null ? inventory.getCurrentStock() : 0;
            if (item.getQuantity() > currentStock) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho (Còn lại: " + currentStock + ")");
            }

            int reqQty = item.getQuantity();

            // Tìm lô cận date đang có salePrice và còn hạn
            List<ProductBatch> validBatches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(product.getId(), 0)
                    .stream()
                    .filter(b -> b.getExpiryDate() != null && !b.getExpiryDate().isBefore(today))
                    .collect(Collectors.toList());

            ProductBatch saleBatch = validBatches.stream()
                    .filter(b -> b.getSalePrice() != null && b.getSalePrice().compareTo(BigDecimal.ZERO) > 0 
                            && product.getPrice() != null && b.getSalePrice().compareTo(product.getPrice()) < 0)
                    .findFirst()
                    .orElse(null);

            if (saleBatch != null && saleBatch.getQuantity() > 0 && reqQty > saleBatch.getQuantity()) {
                // Tách 2 dòng:
                // 1. Phần lô sale
                int saleQty = saleBatch.getQuantity();
                BigDecimal salePrice = saleBatch.getSalePrice();
                ItemAllocation a1 = new ItemAllocation();
                a1.product = product;
                a1.productName = product.getName() + " (Xả kho cận date - HSD: " + saleBatch.getExpiryDate() + ")";
                a1.quantity = saleQty;
                a1.price = salePrice;
                itemAllocations.add(a1);
                totalAmount = totalAmount.add(salePrice.multiply(BigDecimal.valueOf(saleQty)));

                // 2. Phần lô tiêu chuẩn
                int normalQty = reqQty - saleQty;
                BigDecimal normalPrice = product.getPrice();
                ItemAllocation a2 = new ItemAllocation();
                a2.product = product;
                a2.productName = product.getName() + " (Lô tiêu chuẩn)";
                a2.quantity = normalQty;
                a2.price = normalPrice;
                itemAllocations.add(a2);
                totalAmount = totalAmount.add(normalPrice.multiply(BigDecimal.valueOf(normalQty)));
            } else {
                BigDecimal itemPrice = (saleBatch != null && reqQty <= saleBatch.getQuantity())
                        ? saleBatch.getSalePrice()
                        : (product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());

                String displayName = product.getName();
                if (saleBatch != null && reqQty <= saleBatch.getQuantity()) {
                    displayName += " (Xả kho cận date - HSD: " + saleBatch.getExpiryDate() + ")";
                }

                ItemAllocation a = new ItemAllocation();
                a.product = product;
                a.productName = displayName;
                a.quantity = reqQty;
                a.price = itemPrice;
                itemAllocations.add(a);
                totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(reqQty)));
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (orderDTO.getCouponCode() != null && !orderDTO.getCouponCode().trim().isEmpty()) {
            List<com.groceryshop.dto.CouponItemInfo> itemInfos = cart.getItems().stream()
                    .map(item -> com.groceryshop.dto.CouponItemInfo.builder()
                            .productId(item.getProduct().getId())
                            .categoryId(item.getProduct().getCategory() != null ? item.getProduct().getCategory().getId() : null)
                            .price(item.getProduct().getSalePrice() != null ? item.getProduct().getSalePrice() : item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            discountAmount = couponService.calculateDiscountForItems(orderDTO.getCouponCode(), totalAmount, itemInfos);
            appliedCoupon = couponRepository.findByCode(orderDTO.getCouponCode()).orElse(null);
        }

        // Apply Membership Tier Discount
        BigDecimal tierDiscount = BigDecimal.ZERO;
        if (user.getMembershipTier() != null) {
            if (user.getMembershipTier().equals("SILVER")) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.02));
            } else if (user.getMembershipTier().equals("GOLD")) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.05));
            } else if (user.getMembershipTier().equals("DIAMOND")) {
                tierDiscount = totalAmount.multiply(BigDecimal.valueOf(0.08));
            }
        }
        
        discountAmount = discountAmount.add(tierDiscount);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        if (orderDTO.getIdempotencyKey() != null && !orderDTO.getIdempotencyKey().trim().isEmpty()) {
            java.util.Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(orderDTO.getIdempotencyKey().trim());
            if (existingOrder.isPresent()) {
                OrderDTO dto = EntityMapper.toOrderDTO(existingOrder.get());
                orderJobRepository.findByOrderId(existingOrder.get().getId())
                        .ifPresent(job -> dto.setJobId(job.getJobId()));
                return dto;
            }
        }

        String pm = orderDTO.getPaymentMethod();
        if (pm == null || pm.trim().isEmpty() || "undefined".equalsIgnoreCase(pm.trim())) {
            pm = "COD";
        }

        String generatedJobId = "JOB_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status("CHO_TIEP_NHAN")
                .shippingName(orderDTO.getShippingName())
                .shippingPhone(orderDTO.getShippingPhone())
                .shippingAddress(orderDTO.getShippingAddress())
                .paymentMethod(pm)
                .couponCode(orderDTO.getCouponCode())
                .idempotencyKey(orderDTO.getIdempotencyKey())
                .note(orderDTO.getNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (ItemAllocation alloc : itemAllocations) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(alloc.product)
                    .quantity(alloc.quantity)
                    .price(alloc.price)
                    .productName(alloc.productName)
                    .productImage(alloc.product.getMainImage())
                    .build();
            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
        }

        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(pm)
                .paymentStatus("PENDING")
                .amount(finalAmount)
                .build();
        paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        cart.getItems().clear();
        cartRepository.save(cart);

        OrderJob orderJob = OrderJob.builder()
                .jobId(generatedJobId)
                .order(savedOrder)
                .jobType("PROCESS_ORDER")
                .status("PENDING")
                .retryCount(0)
                .maxRetries(3)
                .build();
        orderJobRepository.save(orderJob);

        java.util.Map<String, Object> payloadMap = new java.util.HashMap<>();
        payloadMap.put("jobId", generatedJobId);
        payloadMap.put("orderId", savedOrder.getId());
        payloadMap.put("userId", userId);
        payloadMap.put("totalAmount", totalAmount);
        payloadMap.put("finalAmount", finalAmount);
        payloadMap.put("couponCode", orderDTO.getCouponCode());

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.warn("Lỗi khi serialize outbox payload: {}", e.getMessage());
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .aggregateType("ORDER")
                .aggregateId(savedOrder.getId())
                .eventType("ORDER_CREATED")
                .payload(payloadJson)
                .status("NEW")
                .build();
        outboxEventRepository.save(outboxEvent);

        valkeyStreamService.setJobCache(
                generatedJobId,
                "PENDING",
                "0%",
                "Đơn hàng đã được tiếp nhận, đang xếp hàng chờ kiểm tra kho",
                null
        );

        OrderDTO responseDTO = EntityMapper.toOrderDTO(savedOrder);
        responseDTO.setJobId(generatedJobId);
        return responseDTO;
    }

    @Transactional
    public boolean processOrderInventoryAndBatches(Long orderId, String jobId) {
        OrderJob job = orderJobRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            log.warn("Không tìm thấy OrderJob với jobId: {}", jobId);
            return false;
        }

        if ("SUCCEEDED".equals(job.getStatus()) || "CANCELLED".equals(job.getStatus())) {
            log.info("Job '{}' có trạng thái '{}', bỏ qua xử lý kho (Idempotent skip)", jobId, job.getStatus());
            return "SUCCEEDED".equals(job.getStatus());
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            job.setStatus("FAILED");
            job.setErrorMessage("Không tìm thấy đơn hàng #" + orderId);
            job.setCompletedAt(LocalDateTime.now());
            orderJobRepository.save(job);
            valkeyStreamService.setJobCache(jobId, "FAILED", "100%", "Không tìm thấy đơn hàng", null);
            return false;
        }

        if ("HUY".equalsIgnoreCase(order.getStatus()) || "CANCELLED".equalsIgnoreCase(order.getStatus())) {
            log.info("Đơn hàng #{} đã bị hủy trước khi Worker xử lý kho (Job: {})", orderId, jobId);
            job.setStatus("CANCELLED");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage("Đơn hàng đã được hủy trước khi xử lý kho");
            orderJobRepository.save(job);
            valkeyStreamService.setJobCache(jobId, "CANCELLED", "100%", "Đơn hàng đã được hủy trước khi xử lý kho", null);
            return false;
        }

        job.setStatus("PROCESSING");
        job.setStartedAt(LocalDateTime.now());
        orderJobRepository.save(job);
        valkeyStreamService.setJobCache(jobId, "PROCESSING", "30%", "Đang kiểm tra tồn kho & hạn dùng", null);

        LocalDate today = LocalDate.now();

        // 1. Kiểm tra tồn kho cho TẤT CẢ các sản phẩm trong đơn hàng bằng PESSIMISTIC LOCK
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            Long productId = item.getProduct().getId();
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId).orElse(null);
            int currentStock = inventory != null ? inventory.getCurrentStock() : 0;

            if (item.getQuantity() > currentStock) {
                // HẾT HÀNG: Từ chối đơn hàng, không trừ dở dang bất kỳ mặt hàng nào
                order.setStatus("TU_CHOI");
                order.setNote("Từ chối tự động: Sản phẩm '" + item.getProductName() + "' không đủ hàng trong kho (Còn lại: " + currentStock + ", Yêu cầu: " + item.getQuantity() + ")");
                orderRepository.save(order);

                if (order.getPayment() != null) {
                    Payment payment = order.getPayment();
                    payment.setPaymentStatus("FAILED");
                    paymentRepository.save(payment);
                }

                job.setStatus("FAILED");
                job.setErrorMessage("Sản phẩm '" + item.getProductName() + "' không đủ tồn kho (Còn lại: " + currentStock + ")");
                job.setCompletedAt(LocalDateTime.now());
                orderJobRepository.save(job);

                valkeyStreamService.setJobCache(
                        jobId,
                        "FAILED",
                        "100%",
                        "Từ chối đơn: Sản phẩm '" + item.getProductName() + "' không đủ tồn kho",
                        null
                );
                return false;
            }
        }

        // 2. TẤT CẢ SẢN PHẨM ĐỦ HÀNG: Tiến hành trừ kho và trừ lô FIFO
        valkeyStreamService.setJobCache(jobId, "PROCESSING", "70%", "Đang trừ kho và phân bổ lô hàng FIFO", null);

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            Long productId = item.getProduct().getId();
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId).orElse(null);

            if (inventory != null) {
                inventory.setCurrentStock(inventory.getCurrentStock() - item.getQuantity());
                inventoryRepository.save(inventory);

                int quantityToDeduct = item.getQuantity();
                List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAscForUpdate(productId, 0);
                for (ProductBatch batch : batches) {
                    if (quantityToDeduct <= 0) break;
                    if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                        continue;
                    }
                    if (batch.getQuantity() >= quantityToDeduct) {
                        batch.setQuantity(batch.getQuantity() - quantityToDeduct);
                        quantityToDeduct = 0;
                    } else {
                        quantityToDeduct -= batch.getQuantity();
                        batch.setQuantity(0);
                    }
                    productBatchRepository.save(batch);
                }

                ledgerService.recordLog(
                        inventory,
                        "SELL",
                        -item.getQuantity(),
                        order.getId(),
                        "Bán hàng theo đơn #" + order.getId() + " (Valkey Queue Job: " + jobId + ")",
                        order.getUser()
                );
            }
        }

        // 3. Tăng lượt sử dụng mã giảm giá nếu có
        if (order.getCouponCode() != null && !order.getCouponCode().trim().isEmpty()) {
            couponRepository.findByCode(order.getCouponCode().trim()).ifPresent(c -> {
                c.setUsedCount(c.getUsedCount() + 1);
                couponRepository.save(c);
            });
        }

        // 4. Chuyển trạng thái Order sang CHO_XAC_NHAN
        order.setStatus("CHO_XAC_NHAN");
        orderRepository.save(order);

        // 5. Cập nhật OrderJob sang SUCCEEDED
        job.setStatus("SUCCEEDED");
        job.setCompletedAt(LocalDateTime.now());
        orderJobRepository.save(job);

        // 6. Cập nhật Valkey Hash
        valkeyStreamService.setJobCache(
                jobId,
                "SUCCEEDED",
                "100%",
                "Đã kiểm tra kho & phân bổ lô thành công. Đơn hàng chuyển sang trạng thái Chờ xác nhận.",
                null
        );

        return true;
    }

    public java.util.Map<String, Object> getJobStatus(String jobId) {
        java.util.Map<Object, Object> cache = valkeyStreamService.getJobCache(jobId);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (cache != null && !cache.isEmpty()) {
            cache.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }

        orderJobRepository.findByJobId(jobId).ifPresent(job -> {
            result.put("jobId", job.getJobId());
            result.put("orderId", job.getOrder() != null ? job.getOrder().getId() : null);
            result.put("status", job.getStatus());
            result.put("retryCount", job.getRetryCount());
            result.put("errorMessage", job.getErrorMessage());
            result.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
            result.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        });
        return result;
    }

    public List<OrderDTO> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> {
                    OrderDTO dto = EntityMapper.toOrderDTO(order);
                    orderJobRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                            .ifPresent(job -> dto.setJobId(job.getJobId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> {
                    OrderDTO dto = EntityMapper.toOrderDTO(order);
                    orderJobRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                            .ifPresent(job -> dto.setJobId(job.getJobId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + id));
        OrderDTO dto = EntityMapper.toOrderDTO(order);
        orderJobRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .ifPresent(job -> dto.setJobId(job.getJobId()));
        return dto;
    }

    @Transactional
    public OrderDTO cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này");
        }

        if (!order.getStatus().equals("CHO_TIEP_NHAN") && !order.getStatus().equals("CHO_XAC_NHAN") && !order.getStatus().equals("DA_XAC_NHAN") && !order.getStatus().equals("DA_NHAN_DON")) {
            throw new BadRequestException("Chỉ được phép hủy đơn hàng khi chưa bắt đầu giao (Chờ tiếp nhận, Chờ xác nhận, Đã xác nhận, Đã nhận đơn)");
        }

        String previousStatus = order.getStatus();
        order.setStatus("HUY");
        Order updatedOrder = orderRepository.save(order);

        orderJobRepository.findByOrderId(order.getId()).ifPresent(job -> {
            if ("PENDING".equals(job.getStatus()) || "PROCESSING".equals(job.getStatus()) || "RETRYING".equals(job.getStatus())) {
                job.setStatus("CANCELLED");
                job.setCompletedAt(LocalDateTime.now());
                job.setErrorMessage("Đơn hàng đã được khách hàng hủy");
                orderJobRepository.save(job);
                valkeyStreamService.setJobCache(job.getJobId(), "CANCELLED", "100%", "Đơn hàng đã được khách hàng hủy", null);
            }
        });

        if (!"CHO_TIEP_NHAN".equals(previousStatus) && !"TU_CHOI".equals(previousStatus)) {
            restoreInventoryAndCoupon(order, order.getUser(), "Hoàn trả do khách hàng hủy đơn #" + order.getId());
        }

        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
        }

        return EntityMapper.toOrderDTO(updatedOrder);
    }

    @Transactional
    public void restoreInventoryAndCoupon(Order order, User performer, String reason) {
        if (order == null || order.getItems() == null) return;
        if ("CHO_TIEP_NHAN".equals(order.getStatus()) || "TU_CHOI".equals(order.getStatus())) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Inventory inventory = item.getProduct().getInventory();
                if (inventory != null) {
                    inventory.setCurrentStock(inventory.getCurrentStock() + item.getQuantity());
                    inventoryRepository.save(inventory);
                    
                    // Add back to batch (add to the earliest valid batch)
                    List<ProductBatch> batches = productBatchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(item.getProduct().getId(), -1);
                    if (!batches.isEmpty()) {
                        ProductBatch firstBatch = batches.get(0);
                        firstBatch.setQuantity(firstBatch.getQuantity() + item.getQuantity());
                        productBatchRepository.save(firstBatch);
                    }
                    
                    ledgerService.recordLog(
                        inventory, 
                        "RETURN", 
                        item.getQuantity(), 
                        order.getId(), 
                        (reason != null && !reason.trim().isEmpty()) ? reason : ("Hoàn trả do hủy đơn #" + order.getId()), 
                        performer
                    );
                }
            }
        }

        if (order.getCouponCode() != null) {
            couponRepository.findByCode(order.getCouponCode()).ifPresent(coupon -> {
                if (coupon.getUsedCount() > 0) {
                    coupon.setUsedCount(coupon.getUsedCount() - 1);
                    couponRepository.save(coupon);
                }
            });
        }
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        String currentStatus = order.getStatus();

        if (currentStatus.equals("HUY") || currentStatus.equals("HOAN_THANH") || currentStatus.equals("TU_CHOI")) {
            throw new BadRequestException("Không thể chuyển đổi trạng thái cho đơn hàng đã Hủy, Từ chối hoặc Hoàn thành");
        }

        boolean isValidTransition = false;
        if (currentStatus.equals("CHO_TIEP_NHAN") && (newStatus.equals("CHO_XAC_NHAN") || newStatus.equals("TU_CHOI") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("CHO_XAC_NHAN") && (newStatus.equals("DA_XAC_NHAN") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("DA_XAC_NHAN") && (newStatus.equals("DA_NHAN_DON") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("DA_NHAN_DON") && (newStatus.equals("DANG_GIAO") || newStatus.equals("HUY"))) {
            isValidTransition = true;
        } else if (currentStatus.equals("DANG_GIAO") && newStatus.equals("DA_GIAO")) {
            isValidTransition = true;
        } else if (currentStatus.equals("DA_GIAO") && newStatus.equals("HOAN_THANH")) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new BadRequestException("Luồng chuyển trạng thái không hợp lệ: Không được nhảy từ '" + currentStatus + "' sang '" + newStatus + "'");
        }

        order.setStatus(newStatus);

        if (newStatus.equals("DA_XAC_NHAN")) {
            Payment payment = order.getPayment();
            boolean isCod = "TIEN_MAT".equalsIgnoreCase(order.getPaymentMethod()) || "COD".equalsIgnoreCase(order.getPaymentMethod());
            if (payment == null) {
                payment = Payment.builder()
                        .order(order)
                        .paymentMethod(order.getPaymentMethod())
                        .amount(order.getFinalAmount())
                        .paymentStatus(isCod ? "APPROVED_COD" : "COMPLETED")
                        .paidAt(isCod ? null : LocalDateTime.now())
                        .build();
            } else {
                payment.setPaymentStatus(isCod ? "APPROVED_COD" : "COMPLETED");
                if (!isCod && payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
            }
            paymentRepository.save(payment);
            order.setPayment(payment);
        }

        if (newStatus.equals("HUY")) {
            orderJobRepository.findByOrderId(order.getId()).ifPresent(job -> {
                if ("PENDING".equals(job.getStatus()) || "PROCESSING".equals(job.getStatus()) || "RETRYING".equals(job.getStatus())) {
                    job.setStatus("CANCELLED");
                    job.setCompletedAt(LocalDateTime.now());
                    job.setErrorMessage("Đơn hàng đã bị hủy bởi Quản trị viên");
                    orderJobRepository.save(job);
                    valkeyStreamService.setJobCache(job.getJobId(), "CANCELLED", "100%", "Đơn hàng đã bị hủy bởi Quản trị viên", null);
                }
            });

            if (!"CHO_TIEP_NHAN".equals(currentStatus) && !"TU_CHOI".equals(currentStatus)) {
                restoreInventoryAndCoupon(order, null, "Hoàn trả do hủy đơn (Admin/Shipper) #" + order.getId());
            }
            if (order.getPayment() != null) {
                Payment payment = order.getPayment();
                payment.setPaymentStatus("FAILED");
                paymentRepository.save(payment);
            }
        }

        if (newStatus.equals("HOAN_THANH")) {
            if (order.getPayment() != null) {
                Payment payment = order.getPayment();
                payment.setPaymentStatus("COMPLETED");
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            
            // Add Loyalty Points (1 point per 100,000 VND)
            if (order.getUser() != null && order.getFinalAmount() != null) {
                User user = order.getUser();
                int earnedPoints = order.getFinalAmount().divide(BigDecimal.valueOf(100000)).intValue();
                user.setLoyaltyPoints((user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0) + earnedPoints);
                
                // Update tier logic if needed (Bronze -> Silver -> Gold -> Diamond)
                int points = user.getLoyaltyPoints();
                if (points >= 1000) {
                    user.setMembershipTier("DIAMOND");
                } else if (points >= 500) {
                    user.setMembershipTier("GOLD");
                } else if (points >= 100) {
                    user.setMembershipTier("SILVER");
                }
                userRepository.save(user);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return EntityMapper.toOrderDTO(updatedOrder);
    }

    public List<OrderDTO> getAvailableOrdersForShipper() {
        return orderRepository.findByStatusAndShipperIsNullOrderByCreatedAtAsc("DA_XAC_NHAN").stream()
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getMyDeliveries(Long shipperUserId) {
        return orderRepository.findByShipperIdOrderByCreatedAtDesc(shipperUserId).stream()
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO acceptOrder(Long orderId, Long shipperUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (!"DA_XAC_NHAN".equals(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể nhận đơn hàng ở trạng thái 'Đã xác nhận'");
        }

        if (order.getShipper() != null) {
            throw new BadRequestException("Đơn hàng này đã được shipper khác nhận giao");
        }

        User shipper = userRepository.findById(shipperUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin shipper"));

        order.setShipper(shipper);
        order.setStatus("DA_NHAN_DON");
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }

    @Transactional
    public OrderDTO updateDeliveryStatus(Long orderId, Long shipperUserId, String newStatus, String deliveryNote, String deliveryFailedReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperUserId)) {
            // Cho phép Admin can thiệp nếu cần, nhưng đối với shipper phải là chính chủ
            User currentUser = userRepository.findById(shipperUserId).orElse(null);
            boolean isAdmin = currentUser != null && currentUser.getRole() != null && "ROLE_ADMIN".equals(currentUser.getRole().getName());
            if (!isAdmin) {
                throw new BadRequestException("Bạn không được phân công giao đơn hàng này");
            }
        }

        String currentStatus = order.getStatus();

        if ("DANG_GIAO".equals(newStatus)) {
            if (!"DA_NHAN_DON".equals(currentStatus)) {
                throw new BadRequestException("Đơn hàng phải ở trạng thái 'Đã nhận đơn' trước khi bắt đầu giao");
            }
            order.setStatus("DANG_GIAO");
        } else if ("DA_GIAO".equals(newStatus)) {
            if (!"DANG_GIAO".equals(currentStatus)) {
                throw new BadRequestException("Đơn hàng phải ở trạng thái 'Đang giao' trước khi xác nhận đã giao");
            }
            order.setStatus("DA_GIAO");
            order.setDeliveredAt(LocalDateTime.now());
            if (order.getPayment() != null && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.getPayment().setPaymentStatus("COMPLETED");
                order.getPayment().setPaidAt(LocalDateTime.now());
                paymentRepository.save(order.getPayment());
            }
        } else if ("GIAO_THAT_BAI".equals(newStatus)) {
            if (!"DANG_GIAO".equals(currentStatus)) {
                throw new BadRequestException("Chỉ có thể báo giao thất bại khi đang trong quá trình giao");
            }
            order.setStatus("GIAO_THAT_BAI");
            order.setDeliveryFailedReason(deliveryFailedReason != null ? deliveryFailedReason : "Khách hàng không nhận hàng hoặc không liên lạc được");
        } else {
            throw new BadRequestException("Trạng thái giao hàng không hợp lệ: " + newStatus);
        }

        if (deliveryNote != null && !deliveryNote.trim().isEmpty()) {
            order.setDeliveryNote(deliveryNote.trim());
        }
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }

    @Transactional
    public OrderDTO confirmPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getPayment() == null) {
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(order.getPaymentMethod())
                    .amount(order.getFinalAmount())
                    .paymentStatus("COMPLETED")
                    .paidAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);
            order.setPayment(payment);
        } else {
            Payment payment = order.getPayment();
            payment.setPaymentStatus("COMPLETED");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        if ("CHO_XAC_NHAN".equals(order.getStatus())) {
            order.setStatus("DA_XAC_NHAN");
        }
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return EntityMapper.toOrderDTO(saved);
    }
}
