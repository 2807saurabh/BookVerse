package com.example.ecom.Controller;

import com.example.ecom.Service.AddressService;
import com.example.ecom.dto.AddressRequest;
import com.example.ecom.dto.AddressResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final AddressService service;
    public AddressController(AddressService service) { this.service = service; }
    @GetMapping public List<AddressResponse> list(Authentication auth) { log.info("Address list requested"); return service.list(auth.getName()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@Valid @RequestBody AddressRequest request, Authentication auth) { log.info("Address creation requested"); return service.create(auth.getName(), request); }
    @PutMapping("/{id}") public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest request, Authentication auth) { log.info("Address update requested: addressId={}", id); return service.update(auth.getName(), id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) { log.info("Address deletion requested: addressId={}", id); service.delete(auth.getName(), id); }
    @PatchMapping("/{id}/default") public AddressResponse setDefault(@PathVariable Long id, Authentication auth) { log.info("Default address change requested: addressId={}", id); return service.setDefault(auth.getName(), id); }
}
