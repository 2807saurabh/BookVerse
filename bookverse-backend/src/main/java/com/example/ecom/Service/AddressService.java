package com.example.ecom.Service;

import com.example.ecom.Model.Address;
import com.example.ecom.Reposetory.AddressRepository;
import com.example.ecom.Reposetory.UserRepo;
import com.example.ecom.dto.AddressRequest;
import com.example.ecom.dto.AddressResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class AddressService {
    private static final Logger log = LoggerFactory.getLogger(AddressService.class);
    private final AddressRepository addresses;
    private final UserRepo users;
    public AddressService(AddressRepository addresses, UserRepo users) { this.addresses = addresses; this.users = users; }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(String email) {
        List<AddressResponse> result = addresses.findByUserEmailOrderByDefaultAddressDescUpdatedAtDesc(email).stream().map(AddressResponse::from).toList();
        log.info("Retrieved {} addresses for userId lookup", result.size());
        return result;
    }

    @Transactional
    public AddressResponse create(String email, AddressRequest request) {
        var user = users.findByEmailForUpdate(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Address address = new Address(); address.setUser(user);
        if (request.defaultAddress() || !addresses.existsByUserEmail(email)) clearDefault(email);
        copy(request, address); address.setDefaultAddress(request.defaultAddress() || !addresses.existsByUserEmail(email));
        address = addresses.save(address); log.info("Created addressId={} for userId={}", address.getId(), user.getId());
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse update(String email, Long id, AddressRequest request) {
        users.findByEmailForUpdate(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Address address = owned(email, id);
        boolean wasDefault = address.isDefaultAddress();
        if (request.defaultAddress()) clearDefault(email);
        copy(request, address); address.setDefaultAddress(request.defaultAddress() || wasDefault);
        log.info("Updated addressId={} for authenticated account", id);
        return AddressResponse.from(addresses.save(address));
    }

    @Transactional
    public void delete(String email, Long id) {
        users.findByEmailForUpdate(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Address address = owned(email, id);
        addresses.delete(address);
        log.info("Deleted addressId={} for authenticated account; no replacement default selected", id);
    }

    @Transactional
    public AddressResponse setDefault(String email, Long id) {
        users.findByEmailForUpdate(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Address address = owned(email, id); clearDefault(email); address.setDefaultAddress(true);
        log.info("Set addressId={} as default", id); return AddressResponse.from(addresses.save(address));
    }

    private Address owned(String email, Long id) {
        return addresses.findByIdAndUserEmail(id, email).orElseThrow(() -> {
            log.warn("Address lookup rejected or missing: addressId={}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found");
        });
    }
    private void clearDefault(String email) {
        addresses.findByUserEmail(email).forEach(a -> {
            if (a.isDefaultAddress()) { a.setDefaultAddress(false); addresses.save(a); }
        });
    }
    private void copy(AddressRequest r, Address a) {
        a.setRecipientName(r.recipientName().trim()); a.setPhoneNumber(r.phoneNumber().trim());
        a.setAddressLine(r.addressLine().trim()); a.setLocality(r.locality().trim()); a.setLandmark(r.landmark() == null ? null : r.landmark().trim());
        a.setCity(r.city().trim()); a.setState(r.state().trim()); a.setPinCode(r.pinCode().trim());
        a.setCountry(r.country().isBlank() ? "India" : r.country().trim());
    }
}
