package com.stevenmoriasi.insurance.integrations.api;

import com.stevenmoriasi.insurance.integrations.documents.DocumentIntegrationService;
import com.stevenmoriasi.insurance.integrations.documents.DocumentIntegrationService.DocumentRegistration;
import com.stevenmoriasi.insurance.integrations.documents.DocumentIntegrationService.DocumentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/integrations/documents")
@Validated
public class DocumentIntegrationController {

    private final DocumentIntegrationService documents;

    public DocumentIntegrationController(DocumentIntegrationService documents) {
        this.documents = documents;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('WORKFLOW_OPERATOR','CLAIMS_OFFICER','CLAIMS_ASSESSOR',"
                    + "'PLATFORM_ADMIN')")
    public DocumentRegistration register(@Valid @RequestBody RegisterDocumentRequest request) {
        return documents.register(
                request.idempotencyKey(),
                request.claimReference(),
                request.fileName(),
                request.contentType(),
                request.sha256(),
                request.maximumBytes());
    }

    @GetMapping("/{documentId}/worker-download")
    @PreAuthorize("hasAnyRole('AUTOMATION_WORKER','PLATFORM_ADMIN')")
    public DownloadView download(@PathVariable UUID documentId) {
        return new DownloadView(documents.downloadUrl(documentId));
    }

    @PostMapping("/{documentId}/scan-results")
    @PreAuthorize("hasAnyRole('AUTOMATION_WORKER','PLATFORM_ADMIN')")
    public DocumentView scanResult(
            @PathVariable UUID documentId, @Valid @RequestBody ScanResultRequest request) {
        return documents.recordScan(
                documentId,
                request.workItemId(),
                request.leaseToken(),
                request.observedSha256(),
                request.observedBytes(),
                request.clean(),
                request.scannerSummary());
    }

    public record RegisterDocumentRequest(
            @NotBlank @Size(max = 128) String idempotencyKey,
            @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}") String claimReference,
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 128) String contentType,
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String sha256,
            @Min(1) @Max(25_000_000) long maximumBytes) {}

    public record ScanResultRequest(
            @NotNull UUID workItemId,
            @NotNull UUID leaseToken,
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String observedSha256,
            @Min(1) @Max(25_000_000) long observedBytes,
            boolean clean,
            @NotBlank @Size(max = 256) String scannerSummary) {}

    public record DownloadView(URI downloadUrl) {}
}
