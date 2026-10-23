package com.example.ecom.Reposetory;

import com.example.ecom.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserEmailOrderByCreatedAtDesc(String email);

    @Query("select o.id from Order o where o.status = 'PENDING' and o.createdAt <= :cutoff and (o.cancellationCheckUntil is null or o.cancellationCheckUntil <= :now) order by o.createdAt asc")
    List<Long> findExpiredPendingOrderIds(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Order o set o.cancellationCheckUntil = :leaseUntil where o.id = :id and o.status = 'PENDING' and o.createdAt <= :cutoff and (o.cancellationCheckUntil is null or o.cancellationCheckUntil <= :now)")
    int claimExpiredOrder(@Param("id") Long id, @Param("cutoff") LocalDateTime cutoff,
                          @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);
}
