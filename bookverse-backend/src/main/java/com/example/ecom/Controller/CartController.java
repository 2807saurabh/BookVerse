package com.example.ecom.Controller;

import com.example.ecom.Service.CartService;
import com.example.ecom.dto.CartResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService service;
    public CartController(CartService service) { this.service = service; }
    @GetMapping public CartResponse get(java.security.Principal principal) { return service.get(principal.getName()); }
    @PostMapping("/items") @ResponseStatus(HttpStatus.OK)
    public CartResponse add(java.security.Principal principal, @RequestBody Map<String, Integer> body) {
        if (body == null || body.get("productId") == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID is required");
        return service.add(principal.getName(), body.get("productId"), body.getOrDefault("quantity", 1));
    }
    @PutMapping("/items/{productId}") public CartResponse update(java.security.Principal principal, @PathVariable int productId, @RequestBody Map<String, Integer> body) {
        if (body == null || body.get("quantity") == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required");
        return service.update(principal.getName(), productId, body.get("quantity"));
    }
    @DeleteMapping("/items/{productId}") public CartResponse remove(java.security.Principal principal, @PathVariable int productId) { return service.remove(principal.getName(), productId); }
    @DeleteMapping public CartResponse clear(java.security.Principal principal) { return service.clear(principal.getName()); }
}
