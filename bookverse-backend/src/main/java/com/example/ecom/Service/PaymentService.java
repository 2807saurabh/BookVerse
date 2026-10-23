package com.example.ecom.Service;

import com.example.ecom.Model.Order;
import com.example.ecom.Model.OrderItem;
import com.example.ecom.Model.Payment;
import com.example.ecom.Model.Product;
import com.example.ecom.Model.User;
import com.example.ecom.Model.Address;
import com.example.ecom.Reposetory.AddressRepository;
import com.example.ecom.Reposetory.OrderRepository;
import com.example.ecom.Reposetory.PaymentRepository;
import com.example.ecom.Reposetory.ProductRepo;
import com.example.ecom.Reposetory.UserRepo;
import com.example.ecom.dto.CreatePaymentOrderRequest;
import com.example.ecom.dto.CreatePaymentOrderResponse;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.ecom.dto.VerifyPaymentRequest;
import com.razorpay.Utils;


import java.util.Objects;




@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final OrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final AddressRepository addressRepo;
    private final RazorpayClient razorpayClient;
    private final RazorpayOrderStatusInspector statusInspector;
    private final PaymentStateFinalizer paymentStateFinalizer;

    @Value("${razorpay.key.id}")
    private String keyId;


    @Value("${razorpay.key.secret}")
    private String keySecret;

    public PaymentService(
            ProductRepo productRepo,
            UserRepo userRepo,
            OrderRepository orderRepo,
            PaymentRepository paymentRepo,
            AddressRepository addressRepo,
            RazorpayClient razorpayClient,
            RazorpayOrderStatusInspector statusInspector,
            PaymentStateFinalizer paymentStateFinalizer) {

        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
        this.addressRepo = addressRepo;
        this.razorpayClient = razorpayClient;
        this.statusInspector = statusInspector;
        this.paymentStateFinalizer = paymentStateFinalizer;
    }

    @Transactional
    public void verifyPayment(String email, VerifyPaymentRequest request) {
        log.info("Payment verification started for authenticated account");

        if (request == null
                || request.razorpayOrderId() == null
                || request.razorpayPaymentId() == null
                || request.razorpaySignature() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing payment details");
        }

        Payment payment = paymentRepo
                .findByProviderOrderIdForUpdate(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment order not found"));

        Order order = payment.getOrder();

        // A user can verify only their own order.
        if (!Objects.equals(order.getUser().getEmail(), email)) {
            log.warn("Payment verification ownership check failed: orderId={}", order.getId());
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This order does not belong to you");
        }

        // Make repeated verification requests safe.
        if ("SUCCESS".equals(payment.getStatus())) {
            if (Objects.equals(
                    payment.getProviderPaymentId(),
                    request.razorpayPaymentId())) {
                return;
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Payment already verified");
        }

        if (!"CREATED".equals(payment.getStatus())
                || !"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Order cannot be verified");
        }

        try {
            String payload = request.razorpayOrderId()
                    + "|" + request.razorpayPaymentId();

            boolean valid = Utils.verifySignature(
                    payload,
                    request.razorpaySignature(),
                    keySecret);

            if (!valid) {
                log.warn("Payment signature rejected for orderId={}", order.getId());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }
        } catch (RazorpayException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Payment signature verification failed");
        }

        // Reduce stock only after signature verification.
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Verified payment cannot complete due to insufficient stock: orderId={}, productId={}", order.getId(), product.getId());
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(
                    product.getStockQuantity() - item.getQuantity());

            if (product.getStockQuantity() == 0) {
                product.setProductAvailable(false);
            }

            productRepo.save(product);
        }

        payment.setProviderPaymentId(request.razorpayPaymentId());
        payment.setStatus("SUCCESS");
        order.setStatus("PAID");
        log.info("Payment verified; orderId={} transitioned to PAID", order.getId());

        paymentRepo.save(payment);
        orderRepo.save(order);
    }


    public String cancelOrderIfSafe(String email, String razorpayOrderId) {

        Payment localPayment = paymentRepo
                .findByProviderOrderIdAndOrderUserEmail(razorpayOrderId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        Order localOrder = localPayment.getOrder();
        if ("PAID".equalsIgnoreCase(localOrder.getStatus())
                || "SUCCESS".equalsIgnoreCase(localPayment.getStatus())) {
            paymentStateFinalizer.applyProviderResult(razorpayOrderId,
                    new RazorpayOrderStatusInspector.Result(RazorpayOrderStatusInspector.State.PAID,
                            localPayment.getProviderPaymentId(), "local_success"));
            return "Payment successful. Order remains PAID.";
        }

        try {
            var providerResult = statusInspector.inspect(razorpayOrderId);
            var applied = paymentStateFinalizer.applyProviderResult(razorpayOrderId, providerResult);
            return switch (applied) {
                case PAID -> "Payment successful. Order remains PAID.";
                case CANCELLED -> "All payment attempts failed. Order cancelled.";
                case STILL_PENDING -> "Payment is not confirmed as failed. Order remains PENDING.";
                case ALREADY_FINAL -> "Order is already final and remains unchanged.";
            };

        } catch (RazorpayException ex) {
            log.warn("Razorpay status check failed; order remains unchanged: orderId={}", localOrder.getId(), ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to check payment status with Razorpay. "
                            + "Order remains unchanged.");
        }
    }


    @Transactional
    public CreatePaymentOrderResponse createOrder(
            String email,
            CreatePaymentOrderRequest request) {

        if (request == null || request.items() == null
                || request.items().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        log.info("Order creation started for userId={}", user.getId());
        if (request.addressId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a delivery address");
        Address address = addressRepo.findByIdAndUserEmail(request.addressId(), email)
                .orElseThrow(() -> {
                    log.warn("Checkout address ownership check failed: addressId={}, userId={}", request.addressId(), user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery address not found");
                });

        Order order = new Order();
        order.setUser(user);
        order.setCurrency("INR");
        order.setStatus("PENDING");
        order.setDeliveryRecipientName(address.getRecipientName());
        order.setDeliveryPhoneNumber(address.getPhoneNumber());
        order.setDeliveryAddressLine(address.getAddressLine());
        order.setDeliveryLocality(address.getLocality());
        order.setDeliveryLandmark(address.getLandmark());
        order.setDeliveryCity(address.getCity());
        order.setDeliveryState(address.getState());
        order.setDeliveryPinCode(address.getPinCode());
        order.setDeliveryCountry(address.getCountry());

        BigDecimal total = BigDecimal.ZERO;
        Set<Integer> seenProductIds = new HashSet<>();

        for (CreatePaymentOrderRequest.Item item : request.items()) {

            if (item.quantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid quantity");
            }

            if (!seenProductIds.add(item.productId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate product IDs are not allowed");
            }

            Product product = productRepo.findById(item.productId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Product not found"));

            if (!product.isProductAvailable()
                    || product.getStockQuantity() < item.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product unavailable or insufficient stock: "
                                + product.getName());
            }

            BigDecimal price = product.getPrice();

            if (price == null || price.signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid product price");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setUnitPrice(price);

            order.getItems().add(orderItem);

            total = total.add(
                    price.multiply(BigDecimal.valueOf(item.quantity())));
        }

        total = total.setScale(2, RoundingMode.UNNECESSARY);

        if (total.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid order amount");
        }

        order.setTotalAmount(total);

        // Save local pending order and its items.
        order = orderRepo.saveAndFlush(order);

        try {
            long amountPaise = total
                    .movePointRight(2)
                    .longValueExact();

            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", "INR");
            options.put("receipt", "bookverse_" + order.getId());

            com.razorpay.Order razorpayOrder =
                    razorpayClient.orders.create(options);

            String razorpayOrderId =
                    razorpayOrder.get("id").toString();

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setProvider("RAZORPAY");
            payment.setProviderOrderId(razorpayOrderId);
            payment.setAmount(total);
            payment.setCurrency("INR");
            payment.setStatus("CREATED");

            paymentRepo.save(payment);
            log.info("Local order and Razorpay payment order created: orderId={}", order.getId());

            return new CreatePaymentOrderResponse(
                    order.getId(),
                    razorpayOrderId,
                    amountPaise,
                    "INR",
                    keyId);

        } catch (RazorpayException e) {
            log.error("Unable to create Razorpay payment order for orderId={}", order.getId(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create payment order");
        }
    }
}
