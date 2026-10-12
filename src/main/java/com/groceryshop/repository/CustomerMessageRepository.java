package com.groceryshop.repository;

import com.groceryshop.entity.CustomerMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerMessageRepository extends JpaRepository<CustomerMessage, Long> {

    List<CustomerMessage> findByUserIdOrderByCreatedAtAsc(Long userId);

    long countByUserIdAndSenderTypeAndIsReadByAdminFalse(Long userId, String senderType);

    long countBySenderTypeAndIsReadByAdminFalse(String senderType);

    @Query("SELECT DISTINCT m.user.id FROM CustomerMessage m")
    List<Long> findDistinctUserIds();
}
