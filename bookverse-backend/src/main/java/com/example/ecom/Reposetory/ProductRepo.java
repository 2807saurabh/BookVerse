package com.example.ecom.Reposetory;

import com.example.ecom.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product,Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") int id);
    @Query("SELECT p from Product p WHERE "+
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword,'%')) OR "+
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword,'%')) OR "+
            "LOWER(p.author) LIKE LOWER(CONCAT('%', :keyword,'%')) OR "+
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword,'%'))")
    List<Product> searchProducts(String keyword);
}
