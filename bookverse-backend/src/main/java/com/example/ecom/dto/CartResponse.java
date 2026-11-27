package com.example.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<Item> items, BigDecimal total, int itemCount) {
    public record Item(int id, String name, String author, String imageName, String imageType,
                       BigDecimal price, int stockQuantity, boolean productAvailable,
                       int quantity, BigDecimal subtotal) {}
}
