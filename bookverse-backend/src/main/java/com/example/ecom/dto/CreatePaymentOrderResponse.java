package com.example.ecom.dto;

public record CreatePaymentOrderResponse(
        Long orderId,
        String razorpayOrderId,
        long amount,
        String currency,
        String keyId
) {}