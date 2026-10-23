
package com.example.ecom.Service;

import com.example.ecom.Model.Order;
import com.example.ecom.Reposetory.OrderRepository;
import com.example.ecom.Reposetory.PaymentRepository;
import com.example.ecom.Model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrders(String email) {
        List<Order> orders = orderRepository.findByUserEmailOrderByCreatedAtDesc(email);
        log.info("Retrieved {} orders for authenticated account", orders.size());
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPayments(List<Long> orderIds) {
        return orderIds.isEmpty() ? List.of() : paymentRepository.findByOrderIdIn(orderIds);
    }
}
