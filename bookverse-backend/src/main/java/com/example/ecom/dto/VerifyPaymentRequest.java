
package com.example.ecom.dto;

public record VerifyPaymentRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}
