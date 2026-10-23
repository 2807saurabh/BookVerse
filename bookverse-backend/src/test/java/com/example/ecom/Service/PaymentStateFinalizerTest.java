package com.example.ecom.Service;

import com.example.ecom.Model.Order;
import com.example.ecom.Model.Payment;
import com.example.ecom.Reposetory.OrderRepository;
import com.example.ecom.Reposetory.PaymentRepository;
import com.example.ecom.Reposetory.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentStateFinalizerTest {
    private PaymentRepository payments;
    private OrderRepository orders;
    private ProductRepo products;
    private PaymentStateFinalizer finalizer;
    private Payment payment;
    private Order order;

    @BeforeEach void setUp() {
        payments = mock(PaymentRepository.class); orders = mock(OrderRepository.class); products = mock(ProductRepo.class);
        finalizer = new PaymentStateFinalizer(payments, orders, products);
        order = new Order(); order.setId(41L); order.setStatus("PENDING");
        payment = new Payment(); payment.setOrder(order); payment.setStatus("CREATED"); payment.setProviderOrderId("rp_41");
        when(payments.findByProviderOrderIdForUpdate("rp_41")).thenReturn(Optional.of(payment));
    }

    @Test void failedProviderAttemptsCancelOnlyPendingOrder() {
        var result = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED, null, "created");
        assertEquals(PaymentStateFinalizer.AppliedState.CANCELLED, finalizer.applyProviderResult("rp_41", result));
        assertEquals("CANCELLED", order.getStatus());
        assertEquals("FAILED", payment.getStatus());
    }

    @Test void alreadyPaidOrderIsNeverChangedToCancelled() {
        order.setStatus("PAID"); payment.setStatus("SUCCESS");
        var result = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED, null, "created");
        assertEquals(PaymentStateFinalizer.AppliedState.PAID, finalizer.applyProviderResult("rp_41", result));
        assertEquals("PAID", order.getStatus());
        verify(payments, never()).save(argThat(p -> "FAILED".equals(p.getStatus())));
    }

    @Test void repeatedFailureReviewDoesNotRepeatCancellation() {
        order.setStatus("CANCELLED"); payment.setStatus("FAILED");
        var result = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED, null, "created");
        assertEquals(PaymentStateFinalizer.AppliedState.ALREADY_FINAL, finalizer.applyProviderResult("rp_41", result));
        assertEquals("CANCELLED", order.getStatus());
    }

    @Test void providerConfirmedPaymentWinsOverStalePendingState() {
        var result = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.PAID, "pay_41", "paid");
        assertEquals(PaymentStateFinalizer.AppliedState.PAID, finalizer.applyProviderResult("rp_41", result));
        assertEquals("PAID", order.getStatus());
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals("pay_41", payment.getProviderPaymentId());
    }
}
