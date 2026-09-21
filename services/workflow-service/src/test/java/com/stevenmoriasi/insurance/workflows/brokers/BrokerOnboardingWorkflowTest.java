package com.stevenmoriasi.insurance.workflows.brokers;

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

class BrokerOnboardingWorkflowTest {

    private static final String TASK_QUEUE = "broker-onboarding-test";

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
        worker.registerWorkflowImplementationTypes(BrokerOnboardingWorkflowImpl.class);
        testEnvironment.start();
    }

    @AfterEach
    void tearDown() {
        testEnvironment.close();
    }

    @Test
    void routesAutomatedReferralsThroughManualReviewAndApproval() throws Exception {
        BrokerOnboardingWorkflow workflow = newWorkflow("broker-manual-review");
        CompletableFuture<BrokerOnboardingResult> result =
                WorkflowClient.execute(
                        workflow::run,
                        new BrokerOnboardingInput("BRK-1001", 3600, 3600, 3600, 3600));

        awaitStage(workflow, BrokerOnboardingStage.AWAITING_DOCUMENTS);
        workflow.documentsReceived("DOC-1001");
        awaitStage(workflow, BrokerOnboardingStage.AWAITING_AUTOMATED_CHECK);
        workflow.automatedCheckCompleted("MANUAL_REVIEW");
        awaitStage(workflow, BrokerOnboardingStage.AWAITING_MANUAL_REVIEW);
        workflow.manualReviewCompleted(true);
        awaitStage(workflow, BrokerOnboardingStage.AWAITING_APPROVAL);
        workflow.approvalRecorded(true);

        BrokerOnboardingResult completed = result.get(5, TimeUnit.SECONDS);

        assertThat(completed.outcome()).isEqualTo(BrokerOnboardingStage.COMPLETED);
        assertThat(completed.exceptionRoutes()).isEmpty();
    }

    @Test
    void recordsRejectionAsAnOperatorVisibleExceptionRoute() throws Exception {
        BrokerOnboardingWorkflow workflow = newWorkflow("broker-rejected");
        CompletableFuture<BrokerOnboardingResult> result =
                WorkflowClient.execute(
                        workflow::run,
                        new BrokerOnboardingInput("BRK-1002", 3600, 3600, 3600, 3600));

        workflow.documentsReceived("DOC-1002");
        workflow.automatedCheckCompleted("REJECT");

        BrokerOnboardingResult rejected = result.get(5, TimeUnit.SECONDS);

        assertThat(rejected.outcome()).isEqualTo(BrokerOnboardingStage.REJECTED);
        assertThat(rejected.exceptionRoutes())
                .extracting("reason")
                .containsExactly("AUTOMATED_CHECK_REJECTED");
    }

    private BrokerOnboardingWorkflow newWorkflow(String workflowId) {
        return testEnvironment
                .getWorkflowClient()
                .newWorkflowStub(
                        BrokerOnboardingWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId)
                                .setTaskQueue(TASK_QUEUE)
                                .build());
    }

    private static void awaitStage(
            BrokerOnboardingWorkflow workflow, BrokerOnboardingStage expectedStage) {
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(workflow.status().stage()).isEqualTo(expectedStage));
    }
}
