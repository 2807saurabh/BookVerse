package com.example.ecom.Service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

@Component
public class RazorpayOrderStatusInspector {
    public enum State { PAID, ALL_ATTEMPTS_FAILED, PENDING_OR_AMBIGUOUS }
    public record Result(State state, String capturedPaymentId, String providerOrderStatus) {}

    private final RazorpayClient razorpayClient;
    public RazorpayOrderStatusInspector(RazorpayClient razorpayClient) { this.razorpayClient = razorpayClient; }

    public Result inspect(String providerOrderId) throws RazorpayException {
        Order providerOrder = razorpayClient.orders.fetch(providerOrderId);
        String orderStatus = providerOrder.get("status");
        List<Payment> attempts = razorpayClient.orders.fetchPayments(providerOrderId);
        return evaluate(orderStatus, attempts);
    }

    static Result evaluate(String orderStatus, List<Payment> attempts) {
        String normalizedOrderStatus = normalize(orderStatus);
        String capturedPaymentId = null;
        boolean allFailed = attempts != null && !attempts.isEmpty();
        if (attempts != null) {
            for (Payment attempt : attempts) {
                String status = normalize(attempt.get("status"));
                if ("captured".equals(status)) {
                    capturedPaymentId = attempt.get("id");
                    return new Result(State.PAID, capturedPaymentId, normalizedOrderStatus);
                }
                if (!"failed".equals(status)) allFailed = false;
            }
        }
        if ("paid".equals(normalizedOrderStatus)) return new Result(State.PAID, null, normalizedOrderStatus);
        if (allFailed) return new Result(State.ALL_ATTEMPTS_FAILED, null, normalizedOrderStatus);
        return new Result(State.PENDING_OR_AMBIGUOUS, null, normalizedOrderStatus);
    }

    private static String normalize(String status) { return status == null ? "" : status.trim().toLowerCase(Locale.ROOT); }
}
