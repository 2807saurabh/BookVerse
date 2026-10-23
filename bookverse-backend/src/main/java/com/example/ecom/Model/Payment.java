package com.example.ecom.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_provider_order",
                        columnNames = "provider_order_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_provider_payment",
                        columnNames = "provider_payment_id"
                )
        }
)
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 30)
    private String provider = "RAZORPAY";

    @Column(name = "provider_order_id", unique = true)
    private String providerOrderId;

    @Column(name = "provider_payment_id", unique = true)
    private String providerPaymentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    private String status = "CREATED";

    private LocalDateTime createdAt = LocalDateTime.now();


    public JSONObject toJsonObject() {
        return null;
    }
}