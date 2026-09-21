package com.stevenmoriasi.insurance.cases.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "policy_context")
public class PolicyContext {

    @Id private UUID id;
    private String policyNumber;
    private String productCode;
    private String status;
    private LocalDate coverStartDate;
    private LocalDate coverEndDate;
    private String currency;

    protected PolicyContext() {}

    public PolicyContext(
            UUID id,
            String policyNumber,
            String productCode,
            String status,
            LocalDate coverStartDate,
            LocalDate coverEndDate,
            String currency) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.productCode = productCode;
        this.status = status;
        this.coverStartDate = coverStartDate;
        this.coverEndDate = coverEndDate;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getCoverStartDate() {
        return coverStartDate;
    }

    public LocalDate getCoverEndDate() {
        return coverEndDate;
    }

    public String getCurrency() {
        return currency;
    }
}
