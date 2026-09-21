package com.stevenmoriasi.insurance.integrations.documents;

import static org.assertj.core.api.Assertions.assertThat;

import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService;
import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService.WorkItemView;
import com.stevenmoriasi.insurance.integrations.documents.DocumentIntegrationService.DocumentRegistration;
import com.stevenmoriasi.insurance.integrations.documents.DocumentIntegrationService.DocumentView;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentIntegrationServiceTest {

    private static final String SHA256 =
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

    @Autowired private DocumentIntegrationService documents;
    @Autowired private AutomationQueueService queue;
    @Autowired private OutboxEventRepository outbox;

    @Test
    void makesEvidenceAvailableOnlyAfterAHashMatchedCleanScan() {
        DocumentRegistration registration =
                documents.register(
                        "document-CLM-7001",
                        "CLM-7001",
                        "police-abstract.pdf",
                        "application/pdf",
                        SHA256,
                        1_000_000);
        DocumentRegistration duplicate =
                documents.register(
                        "document-CLM-7001",
                        "CLM-7001",
                        "police-abstract.pdf",
                        "application/pdf",
                        SHA256,
                        1_000_000);
        WorkItemView scanWork = queue.claim("document-worker-1").orElseThrow();

        DocumentView scanned =
                documents.recordScan(
                        registration.document().id(),
                        scanWork.id(),
                        scanWork.leaseToken(),
                        SHA256,
                        1200,
                        true,
                        "No known malware signature detected");

        assertThat(registration.uploadUrl().getQuery()).contains("X-Amz-Signature");
        assertThat(duplicate.document().id()).isEqualTo(registration.document().id());
        assertThat(registration.scanWorkItemId()).isEqualTo(scanWork.id());
        assertThat(scanned.status()).isEqualTo("AVAILABLE");
        assertThat(outbox.findAll())
                .extracting(event -> event.getEventType())
                .containsExactly("insurance.document.available");
    }

    @Test
    void rejectsEvidenceWhoseDigestDoesNotMatch() {
        DocumentRegistration registration =
                documents.register(
                        "document-CLM-7002",
                        "CLM-7002",
                        "assessment.jpg",
                        "image/jpeg",
                        SHA256,
                        1_000_000);
        WorkItemView scanWork = queue.claim("document-worker-1").orElseThrow();

        DocumentView scanned =
                documents.recordScan(
                        registration.document().id(),
                        scanWork.id(),
                        scanWork.leaseToken(),
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        1200,
                        true,
                        "No known malware signature detected");

        assertThat(scanned.status()).isEqualTo("REJECTED");
        assertThat(scanned.rejectionReason()).contains("digest");
    }
}
