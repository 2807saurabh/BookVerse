package com.example.ecom.dto;

import com.example.ecom.Model.Address;
import java.time.LocalDateTime;

public record AddressResponse(Long id, String recipientName, String phoneNumber,
        String addressLine, String locality, String landmark, String city,
        String state, String pinCode, String country, boolean defaultAddress,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getRecipientName(), a.getPhoneNumber(),
                a.getAddressLine(), a.getLocality(), a.getLandmark(), a.getCity(),
                a.getState(), a.getPinCode(), a.getCountry(), a.isDefaultAddress(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
