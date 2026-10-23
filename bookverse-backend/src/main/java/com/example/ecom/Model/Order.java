package com.example.ecom.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "delivery_recipient_name", length = 120) private String deliveryRecipientName;
    @Column(name = "delivery_phone_number", length = 20) private String deliveryPhoneNumber;
    @Column(name = "delivery_address_line", length = 180) private String deliveryAddressLine;
    @Column(name = "delivery_locality", length = 120) private String deliveryLocality;
    @Column(name = "delivery_landmark", length = 120) private String deliveryLandmark;
    @Column(name = "delivery_city", length = 100) private String deliveryCity;
    @Column(name = "delivery_state", length = 100) private String deliveryState;
    @Column(name = "delivery_pin_code", length = 10) private String deliveryPinCode;
    @Column(name = "delivery_country", length = 80) private String deliveryCountry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "cancellation_check_until")
    private LocalDateTime cancellationCheckUntil;

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();


}
