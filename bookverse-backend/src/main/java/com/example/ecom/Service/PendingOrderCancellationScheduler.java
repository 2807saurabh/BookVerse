package com.example.ecom.Service;

import com.example.ecom.Reposetory.OrderRepository;
import com.example.ecom.Reposetory.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PendingOrderCancellationScheduler {
    private static final Logger log = LoggerFactory.getLogger(PendingOrderCancellationScheduler.class);
    private static final long CLAIM_LEASE_MINUTES = 2;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RazorpayOrderStatusInspector inspector;
    private final PaymentStateFinalizer finalizer;
    private final long timeoutMinutes;

    public PendingOrderCancellationScheduler(OrderRepository orders, PaymentRepository payments,
            RazorpayOrderStatusInspector inspector, PaymentStateFinalizer finalizer,
            @Value("${bookverse.orders.cancellation.timeout-minutes:30}") long timeoutMinutes) {
        this.orders = orders; this.payments = payments; this.inspector = inspector; this.finalizer = finalizer;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${bookverse.orders.cancellation.poll-interval-ms:60000}")
    public void cancelExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(timeoutMinutes);
        var eligibleIds = orders.findExpiredPendingOrderIds(cutoff, now);
        if (eligibleIds.isEmpty()) {
            log.debug("Pending-order cancellation check completed; no eligible orders");
            return;
        }
        log.info("Pending-order cancellation check started: eligibleCount={}, cutoff={}", eligibleIds.size(), cutoff);
        int claimed = 0;
        int completed = 0;
        int retryable = 0;
        for (Long orderId : eligibleIds) {
            LocalDateTime claimTime = LocalDateTime.now();
            int acquired = orders.claimExpiredOrder(orderId, cutoff, claimTime, claimTime.plusMinutes(CLAIM_LEASE_MINUTES));
            if (acquired == 0) continue;
            claimed++;
            try {
                var payment = payments.findForOrderId(orderId);
                if (payment.isEmpty()) {
                    log.warn("Expired pending order has no local payment record; leaving it pending: orderId={}", orderId);
                    retryable++;
                    continue;
                }
                var localPayment = payment.get();
                if ("SUCCESS".equalsIgnoreCase(localPayment.getStatus())) {
                    var localPaid = new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.PAID,
                            localPayment.getProviderPaymentId(), "local_success");
                    finalizer.applyProviderResult(localPayment.getProviderOrderId(), localPaid);
                    completed++;
                    continue;
                }
                if (!"PENDING".equalsIgnoreCase(localPayment.getOrder().getStatus())) {
                    finalizer.releaseLease(orderId);
                    continue;
                }
                var providerResult = inspector.inspect(localPayment.getProviderOrderId());
                if (providerResult.state() == RazorpayOrderStatusInspector.State.PENDING_OR_AMBIGUOUS) retryable++;
                else completed++;
                finalizer.applyProviderResult(localPayment.getProviderOrderId(), providerResult);
            } catch (Exception exception) {
                retryable++;
                log.warn("Unable to verify expired order with Razorpay; leaving it unchanged for retry: orderId={}", orderId, exception);
            }
        }
        log.info("Pending-order cancellation check finished: eligibleCount={}, claimedCount={}, resolvedCount={}, retryableCount={}",
                eligibleIds.size(), claimed, completed, retryable);
    }
}
