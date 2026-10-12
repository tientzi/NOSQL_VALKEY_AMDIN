package com.groceryshop.repository;

import com.groceryshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusAndShipperIsNullOrderByCreatedAtAsc(String status);
    List<Order> findByShipperIdOrderByCreatedAtDesc(Long shipperId);
    List<Order> findByShipperIdAndStatusOrderByCreatedAtDesc(Long shipperId, String status);
    java.util.Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
