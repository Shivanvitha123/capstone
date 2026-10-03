package org.example.businessservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.businessservice.model.BusinessStatus;
import org.example.businessservice.model.BusinessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("business_profiles")
public class BusinessProfile {

    @Id
    private Long id;

    @Column("owner_id")
    private Long ownerId;

    @Column("business_name")
    private String businessName;

    @Column("registration_number")
    private String registrationNumber;

    @Column("business_type")
    private BusinessType businessType;

    @Column("industry")
    private String industry;

    @Column("address")
    private String address;

    @Column("city")
    private String city;

    @Column("state")
    private String state;

    @Column("postal_code")
    private String postalCode;

    @Column("country")
    private String country;

    @Column("contact_email")
    private String contactEmail;

    @Column("contact_phone")
    private String contactPhone;

    @Column("annual_revenue")
    private BigDecimal annualRevenue;

    @Column("employee_count")
    private Integer employeeCount;

    @Column("established_date")
    private LocalDate establishedDate;

    // Detailed business information

    @Column("business_description")
    private String businessDescription;

    @Column("website")
    private String website;

    @Column("branch_count")
    private Integer branchCount;

    @Column("premises_type")
    private String premisesType;

    @Column("previous_insurance_claims")
    private Integer previousInsuranceClaims;

    @Column("existing_insurance")
    private Boolean existingInsurance;

    @Column("status")
    private BusinessStatus status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}