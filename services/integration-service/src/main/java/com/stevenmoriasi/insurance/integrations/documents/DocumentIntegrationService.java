package com.stevenmoriasi.insurance.integrations.documents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService;
import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService.WorkItemView;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEvent;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEventRepository;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationConflictException;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationNotFoundException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentIntegrationService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");

    private final DocumentObjectRepository documents;
    private final DocumentObjectStore objectStore;
    private final AutomationQueueService automationQueue;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DocumentIntegrationService(
            DocumentObjectRepository documents,
            DocumentObjectStore objectStore,
            AutomationQueueService automationQueue,
            OutboxEventRepository outbox,
            ObjectMapper objectMapper) {
        this.documents = documents;
        this.objectStore = objectStore;
        this.automationQueue = automationQueue;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public DocumentRegistration register(
            String idempotencyKey,
            String claimReference,
            String fileName,
            String contentType,
            String expectedSha256,
            long maximumBytes) {
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Document content type is not supported");
        }
        DocumentObject existing = documents.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getClaimReference().equals(claimReference)
                    || !existing.getFileName().equals(fileName)
                    || !existing.getContentType().equals(contentType)
                    || !existing.getExpectedSha256().equalsIgnoreCase(expectedSha256)
                    || existing.getMaximumBytes() != maximumBytes) {
                throw new IntegrationConflictException(
                        "Idempotency key was already used for a different document");
            }
            return new DocumentRegistration(
                    DocumentView.from(existing),
                    existing.getScanWorkItemId(),
                    objectStore.uploadUrl(existing.getObjectKey(), existing.getContentType()));
        }
        UUID documentId = UUID.randomUUID();
        String objectKey = "claims/" + claimReference + "/evidence/" + documentId;
        DocumentObject document =
                documents.save(
                        new DocumentObject(
                                documentId,
                                idempotencyKey,
                                claimReference,
                                fileName,
                                objectKey,
                                contentType,
                                expectedSha256.toLowerCase(),
                                maximumBytes,
                                clock.instant()));
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("documentId", documentId.toString());
        payload.put("objectKey", objectKey);
        payload.put("contentType", contentType);
        payload.put("expectedSha256", expectedSha256.toLowerCase());
        payload.put("maximumBytes", maximumBytes);
        WorkItemView scanWork =
                automationQueue.enqueue(
                        "document-scan-" + documentId,
                        "DOCUMENT_SCAN",
                        documentId.toString(),
                        payload,
                        4);
        document.assignScanWorkItem(scanWork.id());
        return new DocumentRegistration(
                DocumentView.from(document),
                scanWork.id(),
                objectStore.uploadUrl(objectKey, contentType));
    }

    @Transactional(readOnly = true)
    public URI downloadUrl(UUID documentId) {
        return objectStore.downloadUrl(find(documentId).getObjectKey());
    }

    @Transactional
    public DocumentView recordScan(
            UUID documentId,
            UUID workItemId,
            UUID leaseToken,
            String observedSha256,
            long observedBytes,
            boolean clean,
            String scannerSummary) {
        DocumentObject document = find(documentId);
        document.recordScan(observedSha256, observedBytes, clean, clock.instant());
        ObjectNode result = objectMapper.createObjectNode();
        result.put("documentId", documentId.toString());
        result.put("status", document.getStatus().name());
        result.put("scannerSummary", scannerSummary);
        automationQueue.completeFor(
                workItemId, leaseToken, "DOCUMENT_SCAN", documentId.toString(), result);
        writeEvent(document);
        return DocumentView.from(document);
    }

    private DocumentObject find(UUID documentId) {
        return documents
                .findById(documentId)
                .orElseThrow(
                        () -> new IntegrationNotFoundException("Document object was not found"));
    }

    private void writeEvent(DocumentObject document) {
        DocumentView view = DocumentView.from(document);
        try {
            outbox.save(
                    new OutboxEvent(
                            UUID.randomUUID(),
                            "DOCUMENT",
                            document.getId().toString(),
                            document.getStatus() == DocumentObject.DocumentStatus.AVAILABLE
                                    ? "insurance.document.available"
                                    : "insurance.document.rejected",
                            objectMapper.writeValueAsString(view),
                            clock.instant()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Document event cannot be serialized", exception);
        }
    }

    public record DocumentRegistration(DocumentView document, UUID scanWorkItemId, URI uploadUrl) {}

    public record DocumentView(
            UUID id,
            String claimReference,
            String fileName,
            String contentType,
            String expectedSha256,
            long maximumBytes,
            Long observedBytes,
            String status,
            String rejectionReason,
            Instant createdAt,
            Instant scannedAt) {

        static DocumentView from(DocumentObject document) {
            return new DocumentView(
                    document.getId(),
                    document.getClaimReference(),
                    document.getFileName(),
                    document.getContentType(),
                    document.getExpectedSha256(),
                    document.getMaximumBytes(),
                    document.getObservedBytes(),
                    document.getStatus().name(),
                    document.getRejectionReason(),
                    document.getCreatedAt(),
                    document.getScannedAt());
        }
    }
}
