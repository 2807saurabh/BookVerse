package com.example.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 120) String recipientName,
        @NotBlank @Pattern(regexp = "^[+]?[0-9\\s()-]{10,16}$", message = "Enter a valid phone number") String phoneNumber,
        @NotBlank @Size(max = 180) String addressLine,
        @NotBlank @Size(max = 120) String locality,
        @Size(max = 120) String landmark,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid 6-digit PIN code") String pinCode,
        @NotBlank @Size(max = 80) String country,
        boolean defaultAddress) {}
