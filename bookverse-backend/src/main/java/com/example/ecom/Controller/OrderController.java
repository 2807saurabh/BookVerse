
package com.example.ecom.Controller;

import com.example.ecom.Service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/my-orders")
    public List<Map<String, Object>> getMyOrders(
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Order history requested");

        List<com.example.ecom.Model.Order> orders = orderService.getMyOrders(email);
        Map<Long, String> paymentStatuses = new java.util.HashMap<>();
        orderService.getPayments(orders.stream().map(com.example.ecom.Model.Order::getId).toList())
                .forEach(payment -> paymentStatuses.put(payment.getOrder().getId(), payment.getStatus()));

        return orders
                .stream()
                .map(order -> {
                    Map<String, Object> result = new java.util.LinkedHashMap<>();

                    result.put("orderId", order.getId());
                    result.put("status", order.getStatus());
                    result.put("totalAmount", order.getTotalAmount());
                    result.put("currency", order.getCurrency());
                    result.put("createdAt", order.getCreatedAt());
                    result.put("paymentStatus", paymentStatuses.get(order.getId()));
                    Map<String, Object> delivery = new java.util.LinkedHashMap<>();
                    delivery.put("recipientName", order.getDeliveryRecipientName());
                    delivery.put("phoneNumber", order.getDeliveryPhoneNumber());
                    delivery.put("addressLine", order.getDeliveryAddressLine());
                    delivery.put("locality", order.getDeliveryLocality());
                    delivery.put("landmark", order.getDeliveryLandmark());
                    delivery.put("city", order.getDeliveryCity());
                    delivery.put("state", order.getDeliveryState());
                    delivery.put("pinCode", order.getDeliveryPinCode());
                    delivery.put("country", order.getDeliveryCountry());
                    result.put("deliveryAddress", order.getDeliveryAddressLine() == null ? null : delivery);

                    List<Map<String, Object>> items =
                            order.getItems().stream()
                                    .map(item -> {
                                        Map<String, Object> itemData =
                                                new java.util.LinkedHashMap<>();

                                        itemData.put(
                                                "productId",
                                                item.getProduct().getId()
                                        );
                                        itemData.put(
                                                "productName",
                                                item.getProduct().getName()
                                        );
                                        itemData.put(
                                                "quantity",
                                                item.getQuantity()
                                        );
                                        itemData.put(
                                                "unitPrice",
                                                item.getUnitPrice()
                                        );

                                        return itemData;
                                    })
                                    .toList();

                    result.put("items", items);
                    return result;
                })
                .toList();
    }
}
