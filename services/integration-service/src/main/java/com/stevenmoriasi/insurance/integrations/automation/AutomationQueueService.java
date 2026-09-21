package com.stevenmoriasi.insurance.integrations.automation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stevenmoriasi.insurance.integrations.automation.AutomationWorkItem.WorkStatus;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationConflictException;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomationQueueService {

    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

    private final AutomationWorkItemRepository workItems;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public AutomationQueueService(
            AutomationWorkItemRepository workItems,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.workItems = workItems;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public WorkItemView enqueue(
            String idempotencyKey,
            String workType,
            String businessReference,
            JsonNode payload,
            int maximumAttempts) {
        String payloadJson = write(payload);
        AutomationWorkItem existing = workItems.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getWorkType().equals(workType)
                    || !existing.getBusinessReference().equals(businessReference)
                    || !existing.getPayloadJson().equals(payloadJson)) {
                throw new IntegrationConflictException(
                        "Idempotency key was already used for different automation work");
            }
            return WorkItemView.from(existing, objectMapper);
        }
        AutomationWorkItem workItem =
                workItems.save(
                        new AutomationWorkItem(
                                UUID.randomUUID(),
                                idempotencyKey,
                                workType,
                                businessReference,
                                payloadJson,
                                maximumAttempts,
                                clock.instant()));
        count("enqueued");
        return WorkItemView.from(workItem, objectMapper);
    }

    @Transactional
    public Optional<WorkItemView> claim(String workerId) {
        Instant now = clock.instant();
        var expired = workItems.findByStatusAndLeaseExpiresAtLessThan(WorkStatus.LEASED, now);
        expired.forEach(item -> item.recoverExpiredLease(now));
        if (!expired.isEmpty()) {
            meterRegistry
                    .counter("insurance.automation.work.items", "operation", "lease_recovered")
                    .increment(expired.size());
        }
        return workItems
                .findFirstByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        WorkStatus.READY, now)
                .map(
                        item -> {
                            item.lease(workerId, LEASE_DURATION, now);
                            count("leased");
                            return WorkItemView.from(item, objectMapper);
                        });
    }

    @Transactional
    public WorkItemView complete(UUID workItemId, UUID leaseToken, JsonNode result) {
        AutomationWorkItem workItem = find(workItemId);
        workItem.complete(leaseToken, write(result), clock.instant());
        count("completed");
        return WorkItemView.from(workItem, objectMapper);
    }

    @Transactional
    public WorkItemView fail(UUID workItemId, UUID leaseToken, String error) {
        AutomationWorkItem workItem = find(workItemId);
        workItem.fail(leaseToken, error, clock.instant());
        count(workItem.getStatus() == WorkStatus.DEAD_LETTER ? "dead_lettered" : "retry_scheduled");
        return WorkItemView.from(workItem, objectMapper);
    }

    @Transactional
    public WorkItemView completeFor(
            UUID workItemId,
            UUID leaseToken,
            String expectedType,
            String expectedBusinessReference,
            JsonNode result) {
        AutomationWorkItem workItem = find(workItemId);
        if (!workItem.getWorkType().equals(expectedType)
                || !workItem.getBusinessReference().equals(expectedBusinessReference)) {
            throw new IntegrationConflictException(
                    "Automation work item does not match the supplied business object");
        }
        workItem.complete(leaseToken, write(result), clock.instant());
        count("completed");
        return WorkItemView.from(workItem, objectMapper);
    }

    private AutomationWorkItem find(UUID workItemId) {
        return workItems
                .findById(workItemId)
                .orElseThrow(
                        () ->
                                new IntegrationNotFoundException(
                                        "Automation work item was not found"));
    }

    private String write(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Automation payload is invalid", exception);
        }
    }

    private void count(String operation) {
        meterRegistry
                .counter("insurance.automation.work.items", "operation", operation)
                .increment();
    }

    public record WorkItemView(
            UUID id,
            String workType,
            String businessReference,
            JsonNode payload,
            String status,
            Instant availableAt,
            UUID leaseToken,
            Instant leaseExpiresAt,
            int attempts,
            int maximumAttempts) {

        static WorkItemView from(AutomationWorkItem workItem, ObjectMapper objectMapper) {
            try {
                return new WorkItemView(
                        workItem.getId(),
                        workItem.getWorkType(),
                        workItem.getBusinessReference(),
                        objectMapper.readTree(workItem.getPayloadJson()),
                        workItem.getStatus().name(),
                        workItem.getAvailableAt(),
                        workItem.getLeaseToken(),
                        workItem.getLeaseExpiresAt(),
                        workItem.getAttempts(),
                        workItem.getMaximumAttempts());
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored automation payload is invalid", exception);
            }
        }
    }
}
