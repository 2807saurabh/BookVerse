package com.example.ecom.Service;

import com.razorpay.Payment;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RazorpayOrderStatusInspectorTest {
    private Payment attempt(String status) throws Exception { return new Payment(new JSONObject().put("status", status).put("id", "pay_test")); }

    @Test void allFailedAttemptsAreTheOnlyFailureOutcome() throws Exception {
        var result = RazorpayOrderStatusInspector.evaluate("created", List.of(attempt("failed"), attempt("failed")));
        assertEquals(RazorpayOrderStatusInspector.State.ALL_ATTEMPTS_FAILED, result.state());
    }

    @Test void capturedAttemptIsPaid() throws Exception {
        var result = RazorpayOrderStatusInspector.evaluate("created", List.of(attempt("captured")));
        assertEquals(RazorpayOrderStatusInspector.State.PAID, result.state());
        assertEquals("pay_test", result.capturedPaymentId());
    }

    @Test void paidOrderIsPaidEvenWhenAttemptListIsEmpty() throws Exception {
        assertEquals(RazorpayOrderStatusInspector.State.PAID,
                RazorpayOrderStatusInspector.evaluate("paid", List.of()).state());
    }

    @Test void emptyAttemptListDoesNotProveNonPayment() throws Exception {
        assertEquals(RazorpayOrderStatusInspector.State.PENDING_OR_AMBIGUOUS,
                RazorpayOrderStatusInspector.evaluate("created", List.of()).state());
    }

    @Test void authorizedCreatedAndUnknownAttemptsRemainAmbiguous() throws Exception {
        for (String state : List.of("authorized", "created", "processing", "unexpected")) {
            assertEquals(RazorpayOrderStatusInspector.State.PENDING_OR_AMBIGUOUS,
                    RazorpayOrderStatusInspector.evaluate("created", List.of(attempt(state))).state());
        }
    }

    @Test void mixedFailedAndInProgressAttemptsRemainAmbiguous() throws Exception {
        assertEquals(RazorpayOrderStatusInspector.State.PENDING_OR_AMBIGUOUS,
                RazorpayOrderStatusInspector.evaluate("created", List.of(attempt("failed"), attempt("authorized"))).state());
    }
}
