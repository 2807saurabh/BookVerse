package com.example.ecom.Service;

import com.example.ecom.Model.Order;
import com.example.ecom.Model.Payment;
import com.example.ecom.Reposetory.OrderRepository;
import com.example.ecom.Reposetory.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PendingOrderCancellationSchedulerTest {
    private OrderRepository orders;
    private PaymentRepository payments;
    private RazorpayOrderStatusInspector inspector;
    private PaymentStateFinalizer finalizer;
    private PendingOrderCancellationScheduler scheduler;

    @BeforeEach void setUp() {
        orders = mock(OrderRepository.class); payments = mock(PaymentRepository.class);
        inspector = mock(RazorpayOrderStatusInspector.class); finalizer = mock(PaymentStateFinalizer.class);
        scheduler = new PendingOrderCancellationScheduler(orders, payments, inspector, finalizer, 30);
    }

    @Test void queryUsesThirtyMinuteCutoffAndClaimsBeforeReview() throws Exception {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(11L));
        when(orders.claimExpiredOrder(eq(11L), any(), any(), any())).thenReturn(1);
        Payment payment = payment("CREATED", "PENDING", "rp_order");
        when(payments.findForOrderId(11L)).thenReturn(Optional.of(payment));
        var failed = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED, null, "created");
        when(inspector.inspect("rp_order")).thenReturn(failed);

        LocalDateTime before = LocalDateTime.now();
        scheduler.cancelExpiredPendingOrders();
        LocalDateTime after = LocalDateTime.now();

        verify(orders).findExpiredPendingOrderIds(argThat(cutoff ->
                !cutoff.isBefore(before.minusMinutes(30)) && !cutoff.isAfter(after.minusMinutes(30))), any());
        verify(orders).claimExpiredOrder(eq(11L), any(), any(), any());
        verify(finalizer).applyProviderResult("rp_order", failed);
    }

    @Test void successfulLocalPaymentIsReconciledWithoutGatewayRequest() throws Exception {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(12L));
        when(orders.claimExpiredOrder(eq(12L), any(), any(), any())).thenReturn(1);
        when(payments.findForOrderId(12L)).thenReturn(Optional.of(payment("SUCCESS", "PENDING", "rp_order2")));

        scheduler.cancelExpiredPendingOrders();

        verify(inspector, never()).inspect(anyString());
        verify(finalizer).applyProviderResult(eq("rp_order2"), argThat(result -> result.state() == RazorpayOrderStatusInspector.State.PAID));
    }

    @Test void missingPaymentIsNeverCancelled() throws Exception {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(13L));
        when(orders.claimExpiredOrder(eq(13L), any(), any(), any())).thenReturn(1);
        when(payments.findForOrderId(13L)).thenReturn(Optional.empty());

        scheduler.cancelExpiredPendingOrders();

        verify(inspector, never()).inspect(anyString());
        verify(finalizer, never()).applyProviderResult(anyString(), any());
    }

    @Test void youngerOrAlreadyFinalOrdersAreNotCandidates() {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of());
        scheduler.cancelExpiredPendingOrders();
        verify(orders, never()).claimExpiredOrder(anyLong(), any(), any(), any());
        verifyNoInteractions(payments, inspector, finalizer);
    }

    @Test void ambiguousPaymentRemainsPendingAndWillBeRetried() throws Exception {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(16L));
        when(orders.claimExpiredOrder(eq(16L), any(), any(), any())).thenReturn(1);
        when(payments.findForOrderId(16L)).thenReturn(Optional.of(payment("CREATED", "PENDING", "rp_ambiguous")));
        var ambiguous = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.PENDING_OR_AMBIGUOUS, null, "created");
        when(inspector.inspect("rp_ambiguous")).thenReturn(ambiguous);
        when(finalizer.applyProviderResult("rp_ambiguous", ambiguous)).thenReturn(PaymentStateFinalizer.AppliedState.STILL_PENDING);

        scheduler.cancelExpiredPendingOrders();

        verify(finalizer).applyProviderResult("rp_ambiguous", ambiguous);
        verify(finalizer, never()).releaseLease(16L);
    }

    @Test void aClaimLostToAnotherInstanceDoesNoDuplicateWork() {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(17L));
        when(orders.claimExpiredOrder(eq(17L), any(), any(), any())).thenReturn(0);

        scheduler.cancelExpiredPendingOrders();

        verifyNoInteractions(payments, inspector, finalizer);
    }

    @Test void providerFailureForOneOrderDoesNotStopNextOrder() throws Exception {
        when(orders.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(14L, 15L));
        when(orders.claimExpiredOrder(anyLong(), any(), any(), any())).thenReturn(1);
        when(payments.findForOrderId(14L)).thenReturn(Optional.of(payment("CREATED", "PENDING", "rp_first")));
        when(payments.findForOrderId(15L)).thenReturn(Optional.of(payment("CREATED", "PENDING", "rp_second")));
        when(inspector.inspect("rp_first")).thenThrow(new com.razorpay.RazorpayException("provider unavailable"));
        var failed = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED, null, "created");
        when(inspector.inspect("rp_second")).thenReturn(failed);

        scheduler.cancelExpiredPendingOrders();

        verify(inspector).inspect("rp_second");
        verify(finalizer).applyProviderResult("rp_second", failed);
    }

    private Payment payment(String paymentStatus, String orderStatus, String providerOrderId) {
        var order = new Order(); order.setId(100L); order.setStatus(orderStatus);
        var payment = new Payment(); payment.setOrder(order); payment.setStatus(paymentStatus); payment.setProviderOrderId(providerOrderId);
        return payment;
    }
}
