package com.example.ecom.Service;

import com.example.ecom.Model.*;
import com.example.ecom.Reposetory.*;
import com.example.ecom.dto.CartResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class CartService {
    private static final int MAX_QUANTITY = 99;
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepo products;
    private final UserRepo users;
    public CartService(CartRepository carts, CartItemRepository items, ProductRepo products, UserRepo users) {
        this.carts = carts; this.items = items; this.products = products; this.users = users;
    }
    @Transactional(readOnly = true)
    public CartResponse get(String email) { return response(carts.findForUser(email).orElse(null)); }
    @Transactional
    public CartResponse add(String email, int productId, int quantity) {
        validateQuantity(quantity);
        Cart cart = cart(email);
        Product product = products.findById(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        ensureAvailable(product, quantity);
        CartItem item = items.findByCartIdAndProductId(cart.getId(), productId).orElseGet(() -> {
            CartItem created = new CartItem(); created.setCart(cart); created.setProduct(product); created.setQuantity(0); cart.getItems().add(created); return created;
        });
        int updated = item.getQuantity() + quantity;
        ensureAvailable(product, updated); item.setQuantity(updated); items.save(item);
        return response(cart);
    }
    @Transactional
    public CartResponse update(String email, int productId, int quantity) {
        validateQuantity(quantity);
        Cart cart = carts.findForUserUpdate(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        CartItem item = items.findByCartIdAndProductId(cart.getId(), productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        ensureAvailable(item.getProduct(), quantity); item.setQuantity(quantity); return response(cart);
    }
    @Transactional
    public CartResponse remove(String email, int productId) {
        Cart cart = carts.findForUserUpdate(email).orElse(null);
        if (cart != null) items.findByCartIdAndProductId(cart.getId(), productId).ifPresent(item -> cart.getItems().remove(item));
        return response(cart);
    }
    @Transactional
    public CartResponse clear(String email) {
        Cart cart = carts.findForUserUpdate(email).orElse(null);
        if (cart != null) { cart.getItems().clear(); carts.save(cart); }
        return response(cart);
    }
    private Cart cart(String email) {
        return carts.findForUserUpdate(email).orElseGet(() -> {
            User user = users.findByEmailForUpdate(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            Cart cart = new Cart(); cart.setUser(user); cart.getItems().addAll(new ArrayList<>()); return carts.saveAndFlush(cart);
        });
    }
    private void validateQuantity(int q) { if (q < 1 || q > MAX_QUANTITY) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be between 1 and 99"); }
    private void ensureAvailable(Product p, int q) { if (!p.isProductAvailable() || p.getStockQuantity() < q) throw new ResponseStatusException(HttpStatus.CONFLICT, "Product unavailable or insufficient stock: " + p.getName()); }
    private CartResponse response(Cart cart) {
        var values = cart == null ? java.util.List.<CartResponse.Item>of() : cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            return new CartResponse.Item(p.getId(), p.getName(), p.getAuthor(), p.getImageName(), p.getImageType(), p.getPrice(), p.getStockQuantity(), p.isProductAvailable(), i.getQuantity(), p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }).toList();
        BigDecimal total = values.stream().map(CartResponse.Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(values, total, values.stream().mapToInt(CartResponse.Item::quantity).sum());
    }
}
