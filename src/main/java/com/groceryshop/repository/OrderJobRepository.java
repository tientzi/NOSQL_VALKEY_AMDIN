package com.groceryshop.repository;

import com.groceryshop.entity.OrderJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderJobRepository extends JpaRepository<OrderJob, Long> {
    Optional<OrderJob> findByJobId(String jobId);
    Optional<OrderJob> findByOrderId(Long orderId);
    Optional<OrderJob> findFirstByOrderIdOrderByIdDesc(Long orderId);
    List<OrderJob> findByStatus(String status);
    Page<OrderJob> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<OrderJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);
}
