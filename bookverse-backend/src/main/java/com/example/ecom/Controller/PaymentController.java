package com.example.ecom.Controller;

import com.example.ecom.Service.PaymentService;
import com.example.ecom.dto.CreatePaymentOrderRequest;
import com.example.ecom.dto.CreatePaymentOrderResponse;
import com.example.ecom.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.example.ecom.dto.VerifyPaymentRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<CreatePaymentOrderResponse> createOrder(
            @RequestBody CreatePaymentOrderRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        CreatePaymentOrderResponse response =
                paymentService.createOrder(
                        userDetails.getUsername(), request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestBody VerifyPaymentRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        paymentService.verifyPayment(
                userDetails.getUsername(), request);

        return ResponseEntity.ok("Payment verified successfully");
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOrder(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String razorpayOrderId = request.get("razorpayOrderId");

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Razorpay order ID is required"));
        }

        String result = paymentService.cancelOrderIfSafe(
                authentication.getName(),
                razorpayOrderId
        );

        return ResponseEntity.ok(Map.of("message", result));
    }

}