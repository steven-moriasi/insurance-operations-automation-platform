package com.stevenmoriasi.insurance.integrations.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationAttemptRepository extends JpaRepository<IntegrationAttempt, UUID> {

    Optional<IntegrationAttempt> findByIdempotencyKey(String idempotencyKey);
}
