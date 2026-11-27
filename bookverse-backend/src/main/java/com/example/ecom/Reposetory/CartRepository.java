package com.example.ecom.Reposetory;

import com.example.ecom.Model.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct c from Cart c left join fetch c.items i left join fetch i.product where c.user.email = :email")
    Optional<Cart> findForUserUpdate(@Param("email") String email);
    @Query("select distinct c from Cart c left join fetch c.items i left join fetch i.product where c.user.email = :email")
    Optional<Cart> findForUser(@Param("email") String email);
}
