package com.stevenmoriasi.insurance.workflows.renewals;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyRenewalWorkflowTest {

    private static final String TASK_QUEUE = "policy-renewal-test";

    private TestWorkflowEnvironment testEnvironment;

    @BeforeEach
    void setUp() {
        testEnvironment =
                TestWorkflowEnvironment.newInstance(
                        TestEnvironmentOptions.newBuilder()
                                .registerSearchAttribute(
                                        "InsuranceWorkflowStage",
                                        IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD)
                                .build());
        Worker worker = testEnvironment.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(PolicyRenewalWorkflowImpl.class);
        testEnvironment.start();
    }

    @AfterEach
    void tearDown() {
        testEnvironment.close();
    }

    @Test
    void completesRenewalWithMakerCheckerAndPaymentSignals() throws Exception {
        PolicyRenewalWorkflow workflow = newWorkflow("renewal-happy");
        CompletableFuture<RenewalResult> result =
                WorkflowClient.execute(
                        workflow::run, new RenewalInput("POL-1001", true, 3600, 3600, 3600));

        awaitStage(workflow, RenewalWorkflowStage.AWAITING_CUSTOMER_DECISION);
        workflow.customerDecision("RENEW");
        awaitStage(workflow, RenewalWorkflowStage.AWAITING_MAKER_CHECKER);
        workflow.makerCheckerDecision(true);
        awaitStage(workflow, RenewalWorkflowStage.AWAITING_PAYMENT);
        workflow.paymentReconciled("PAY-1001");

        RenewalResult completed = result.get(5, TimeUnit.SECONDS);

        assertThat(completed.outcome()).isEqualTo(RenewalWorkflowStage.COMPLETED);
        assertThat(completed.exceptionRoutes()).isEmpty();
    }

    @Test
    void routesAnOverdueCustomerDecisionAndContinues() throws Exception {
        PolicyRenewalWorkflow workflow = newWorkflow("renewal-overdue");
        CompletableFuture<RenewalResult> result =
                WorkflowClient.execute(
                        workflow::run, new RenewalInput("POL-1002", false, 3600, 3600, 3600));

        awaitStage(workflow, RenewalWorkflowStage.AWAITING_CUSTOMER_DECISION);
        testEnvironment.sleep(Duration.ofSeconds(3601));
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                assertThat(workflow.status().exceptionRoutes())
                                        .extracting("reason")
                                        .contains("SLA_BREACH"));
        workflow.customerDecision("RENEW");
        workflow.paymentReconciled("PAY-1002");

        RenewalResult completed = result.get(5, TimeUnit.SECONDS);

        assertThat(completed.outcome()).isEqualTo(RenewalWorkflowStage.COMPLETED);
        assertThat(completed.exceptionRoutes()).hasSize(1);
    }

    private PolicyRenewalWorkflow newWorkflow(String workflowId) {
        return testEnvironment
                .getWorkflowClient()
                .newWorkflowStub(
                        PolicyRenewalWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId)
                                .setTaskQueue(TASK_QUEUE)
                                .build());
    }

    private static void awaitStage(
            PolicyRenewalWorkflow workflow, RenewalWorkflowStage expectedStage) {
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(workflow.status().stage()).isEqualTo(expectedStage));
    }
}
