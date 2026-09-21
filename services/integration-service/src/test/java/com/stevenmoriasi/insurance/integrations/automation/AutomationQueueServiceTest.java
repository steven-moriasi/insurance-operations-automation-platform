package com.stevenmoriasi.insurance.integrations.automation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stevenmoriasi.insurance.integrations.automation.AutomationQueueService.WorkItemView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AutomationQueueServiceTest {

    @Autowired private AutomationQueueService queue;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void leasesAndCompletesBoundedAutomationWork() {
        ObjectNode payload = objectMapper.createObjectNode().put("policyNumber", "POL-9001");
        WorkItemView queued =
                queue.enqueue(
                        "legacy-update-POL-9001", "LEGACY_POLICY_UPDATE", "POL-9001", payload, 3);

        WorkItemView leased = queue.claim("python-worker-1").orElseThrow();
        WorkItemView completed =
                queue.complete(
                        leased.id(),
                        leased.leaseToken(),
                        objectMapper.createObjectNode().put("updated", true));

        assertThat(leased.id()).isEqualTo(queued.id());
        assertThat(leased.status()).isEqualTo("LEASED");
        assertThat(leased.attempts()).isEqualTo(1);
        assertThat(completed.status()).isEqualTo("COMPLETED");
    }

    @Test
    void schedulesRetryAfterAWorkerFailure() {
        WorkItemView queued =
                queue.enqueue(
                        "broker-check-BRK-10",
                        "BROKER_PORTAL_CHECK",
                        "BRK-10",
                        objectMapper.createObjectNode().put("brokerReference", "BRK-10"),
                        3);
        WorkItemView leased = queue.claim("rpa-worker-1").orElseThrow();

        WorkItemView failed = queue.fail(queued.id(), leased.leaseToken(), "Portal unavailable");

        assertThat(failed.status()).isEqualTo("READY");
        assertThat(failed.availableAt()).isAfter(leased.availableAt());
    }
}
