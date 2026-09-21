package com.stevenmoriasi.insurance.integrations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService;
import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService.WorkItemView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/integrations/automation")
@Validated
public class AutomationIntegrationController {

    private final AutomationQueueService workQueue;

    public AutomationIntegrationController(AutomationQueueService workQueue) {
        this.workQueue = workQueue;
    }

    @PostMapping("/work-items")
    @PreAuthorize("hasAnyRole('WORKFLOW_OPERATOR','PROCESS_ANALYST','PLATFORM_ADMIN')")
    public WorkItemView enqueue(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody EnqueueWorkRequest request) {
        return workQueue.enqueue(
                idempotencyKey,
                request.workType(),
                request.businessReference(),
                request.payload(),
                request.maximumAttempts());
    }

    @PostMapping("/worker/claim")
    @PreAuthorize("hasAnyRole('AUTOMATION_WORKER','PLATFORM_ADMIN')")
    public ResponseEntity<WorkItemView> claim(
            @RequestHeader("X-Worker-Id") @NotBlank @Size(max = 128) String workerId) {
        Optional<WorkItemView> claimed = workQueue.claim(workerId);
        return claimed.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/worker/complete")
    @PreAuthorize("hasAnyRole('AUTOMATION_WORKER','PLATFORM_ADMIN')")
    public WorkItemView complete(@Valid @RequestBody CompleteWorkRequest request) {
        return workQueue.complete(request.workItemId(), request.leaseToken(), request.result());
    }

    @PostMapping("/worker/fail")
    @PreAuthorize("hasAnyRole('AUTOMATION_WORKER','PLATFORM_ADMIN')")
    public WorkItemView fail(@Valid @RequestBody FailWorkRequest request) {
        return workQueue.fail(request.workItemId(), request.leaseToken(), request.error());
    }

    public record EnqueueWorkRequest(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String workType,
            @NotBlank @Size(max = 128) String businessReference,
            @NotNull JsonNode payload,
            @Min(1) @Max(10) int maximumAttempts) {}

    public record CompleteWorkRequest(
            @NotNull UUID workItemId, @NotNull UUID leaseToken, @NotNull JsonNode result) {}

    public record FailWorkRequest(
            @NotNull UUID workItemId,
            @NotNull UUID leaseToken,
            @NotBlank @Size(max = 512) String error) {}
}
