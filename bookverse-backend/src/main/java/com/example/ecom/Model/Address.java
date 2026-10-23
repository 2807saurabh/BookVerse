package com.example.ecom.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses", indexes = @Index(name = "idx_addresses_user", columnList = "user_id"))
@Getter @Setter
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 120) private String recipientName;
    @Column(nullable = false, length = 20) private String phoneNumber;
    @Column(nullable = false, length = 180) private String addressLine;
    @Column(nullable = false, length = 120) private String locality;
    @Column(length = 120) private String landmark;
    @Column(nullable = false, length = 100) private String city;
    @Column(nullable = false, length = 100) private String state;
    @Column(nullable = false, length = 10) private String pinCode;
    @Column(nullable = false, length = 80) private String country = "India";
    @Column(nullable = false) private boolean defaultAddress;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
