package org.example.businessservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.example.businessservice.model.BusinessType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBusinessProfileRequest(

        @NotBlank(message = "Business name is required")
        @Size(max = 255)
        String businessName,

        @NotBlank(message = "Registration number is required")
        @Size(max = 100)
        String registrationNumber,

        @NotNull(message = "Business type is required")
        BusinessType businessType,

        @NotBlank(message = "Industry is required")
        @Size(max = 255)
        String industry,

        @NotBlank(message = "Address is required")
        @Size(max = 500)
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String country,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 255)
        String contactEmail,

        @NotBlank(message = "Contact phone is required")
        @Size(max = 30)
        String contactPhone,

        @Min(value = 0, message = "Annual revenue cannot be negative")
        BigDecimal annualRevenue,

        @Min(value = 0, message = "Employee count cannot be negative")
        Integer employeeCount,

        @PastOrPresent(message = "Established date cannot be in the future")
        LocalDate establishedDate,

        // Additional business information

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String businessDescription,

        @Size(max = 255, message = "Website cannot exceed 255 characters")
        String website,

        @Min(value = 0, message = "Branch count cannot be negative")
        Integer branchCount,

        @Size(max = 30, message = "Premises type cannot exceed 30 characters")
        String premisesType,

        @Min(value = 0, message = "Previous claims cannot be negative")
        Integer previousInsuranceClaims,

        Boolean existingInsurance
) {
}