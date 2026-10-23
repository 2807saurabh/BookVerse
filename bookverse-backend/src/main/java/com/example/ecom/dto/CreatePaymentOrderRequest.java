package com.example.ecom.dto;

import java.util.List;

public record CreatePaymentOrderRequest(List<Item> items, Long addressId) {
    public record Item(int productId, int quantity) {}
}
