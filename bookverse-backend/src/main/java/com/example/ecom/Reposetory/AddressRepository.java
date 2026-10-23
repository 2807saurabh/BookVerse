package com.example.ecom.Reposetory;

import com.example.ecom.Model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserEmailOrderByDefaultAddressDescUpdatedAtDesc(String email);
    Optional<Address> findByIdAndUserEmail(Long id, String email);
    boolean existsByUserEmail(String email);
    List<Address> findByUserEmail(String email);
}
