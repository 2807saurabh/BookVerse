package com.example.ecom.Service;

import com.example.ecom.Model.Payment;
import com.example.ecom.Reposetory.OrderRepository;
import com.example.ecom.Reposetory.PaymentRepository;
import com.example.ecom.Reposetory.ProductRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
public class PaymentStateFinalizer {
    private static final Logger log = LoggerFactory.getLogger(PaymentStateFinalizer.class);
    public enum AppliedState { PAID, CANCELLED, STILL_PENDING, ALREADY_FINAL }
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final ProductRepo products;
    public PaymentStateFinalizer(PaymentRepository payments, OrderRepository orders, ProductRepo products) { this.payments = payments; this.orders = orders; this.products = products; }

    @Transactional
    public AppliedState applyProviderResult(String providerOrderId, RazorpayOrderStatusInspector.Result result) {
        Payment payment = payments.findByProviderOrderIdForUpdate(providerOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        var order = payment.getOrder();
        if (result.state() == RazorpayOrderStatusInspector.State.PAID) {
            boolean newlyConfirmed = !"SUCCESS".equalsIgnoreCase(payment.getStatus());
            if (result.capturedPaymentId() != null) payment.setProviderPaymentId(result.capturedPaymentId());
            payment.setStatus("SUCCESS");
            order.setStatus("PAID");
            order.setCancellationCheckUntil(null);
            if (newlyConfirmed) {
                order.getItems().forEach(item -> {
                    var product = item.getProduct();
                    if (product.getStockQuantity() < item.getQuantity()) {
                        log.error("Provider confirmed payment but stock could not be updated: orderId={}, productId={}", order.getId(), product.getId());
                    } else {
                        product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                        if (product.getStockQuantity() == 0) product.setProductAvailable(false);
                        products.save(product);
                    }
                });
            }
            payments.save(payment);
            orders.save(order);
            log.info("Provider-confirmed payment reconciled: orderId={}, status=PAID", order.getId());
            return AppliedState.PAID;
        }
        if (result.state() == RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED) {
            if ("PAID".equalsIgnoreCase(order.getStatus()) || "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
                order.setStatus("PAID");
                order.setCancellationCheckUntil(null);
                orders.save(order);
                log.info("Cancellation skipped because local payment is already successful: orderId={}", order.getId());
                return AppliedState.PAID;
            }
            if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                payment.setStatus("FAILED");
                order.setStatus("CANCELLED");
                order.setCancellationCheckUntil(null);
                payments.save(payment);
                orders.save(order);
                log.info("Unpaid order cancelled after provider confirmed all attempts failed: orderId={}", order.getId());
                return AppliedState.CANCELLED;
            }
            order.setCancellationCheckUntil(null);
            orders.save(order);
            log.info("Cancellation skipped for orderId={} with status={}", order.getId(), order.getStatus());
            return AppliedState.ALREADY_FINAL;
        }
        log.info("Order remains pending after ambiguous provider status: orderId={}, providerStatus={}", order.getId(), result.providerOrderStatus());
        return AppliedState.STILL_PENDING;
    }

    @Transactional
    public void releaseLease(Long orderId) {
        orders.findById(orderId).ifPresent(order -> {
            if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                order.setCancellationCheckUntil(null);
                orders.save(order);
            }
        });
    }
}
