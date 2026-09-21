package com.stevenmoriasi.insurance.integrations.payments;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentInstructionRepository extends JpaRepository<PaymentInstruction, UUID> {

    Optional<PaymentInstruction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentInstruction> findByProviderReference(String providerReference);
}
