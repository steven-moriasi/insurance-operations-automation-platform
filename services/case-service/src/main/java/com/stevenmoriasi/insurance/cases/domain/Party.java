package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.PartyType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "party")
public class Party {

    @Id private UUID id;

    @Enumerated(EnumType.STRING)
    private PartyType partyType;

    private String externalReference;
    private String fullName;
    private String phoneNumber;
    private String emailAddress;

    protected Party() {}

    public Party(
            UUID id,
            PartyType partyType,
            String externalReference,
            String fullName,
            String phoneNumber,
            String emailAddress) {
        this.id = id;
        this.partyType = partyType;
        this.externalReference = externalReference;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }

    public UUID getId() {
        return id;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }
}
