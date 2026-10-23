
package com.example.ecom.Reposetory;

import com.example.ecom.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);
    List<Payment> findByOrderIdIn(List<Long> orderIds);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<Payment> findByProviderOrderIdAndOrderUserEmail(String providerOrderId, String email);

    @Query("select p from Payment p join fetch p.order where p.order.id = :orderId")
    Optional<Payment> findForOrderId(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerOrderId = :providerOrderId")
    Optional<Payment> findByProviderOrderIdForUpdate(@Param("providerOrderId") String providerOrderId);
}
