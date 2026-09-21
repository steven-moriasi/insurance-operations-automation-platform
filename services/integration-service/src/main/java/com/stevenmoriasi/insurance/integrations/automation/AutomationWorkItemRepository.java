package com.stevenmoriasi.insurance.integrations.automation;

import com.stevenmoriasi.insurance.integrations.automation.AutomationWorkItem.WorkStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AutomationWorkItemRepository extends JpaRepository<AutomationWorkItem, UUID> {

    Optional<AutomationWorkItem> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AutomationWorkItem> findFirstByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            WorkStatus status, Instant availableAt);

    List<AutomationWorkItem> findByStatusAndLeaseExpiresAtLessThan(
            WorkStatus status, Instant leaseExpiresAt);
}
