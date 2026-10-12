package com.groceryshop.repository;

import com.groceryshop.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    Optional<OutboxEvent> findByEventId(String eventId);
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
    long countByStatus(String status);
}
