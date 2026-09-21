package com.stevenmoriasi.insurance.integrations.documents;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentObjectRepository extends JpaRepository<DocumentObject, UUID> {

    Optional<DocumentObject> findByIdempotencyKey(String idempotencyKey);
}
