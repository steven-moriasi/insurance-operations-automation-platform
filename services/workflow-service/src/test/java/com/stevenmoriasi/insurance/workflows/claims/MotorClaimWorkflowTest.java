package com.stevenmoriasi.insurance.workflows.claims;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MotorClaimWorkflowTest {

    private static final String TASK_QUEUE = "motor-claim-test";

    private TestWorkflowEnvironment testEnvironment;
    private ClaimActivities activities;

    @BeforeEach
    void setUp() {
        testEnvironment =
                TestWorkflowEnvironment.newInstance(
                        TestEnvironmentOptions.newBuilder()
                                .registerSearchAttribute(
                                        "InsuranceClaimReference",
                                        IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD)
                                .registerSearchAttribute(
                                        "InsuranceWorkflowStage",
                                        IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD)
                                .build());
        activities = new RecordingClaimActivities();

        Worker worker = testEnvironment.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(MotorClaimWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        testEnvironment.start();
    }

    @AfterEach
    void tearDown() {
        testEnvironment.close();
    }

    @Test
    void completesTheClaimJourneyAfterHumanSignals() throws Exception {
        MotorClaimWorkflow workflow = newWorkflow("claim-happy-path");
        CompletableFuture<MotorClaimResult> result =
                WorkflowClient.execute(
                        workflow::run, new MotorClaimInput("CLM-1001", 3600, 3600, 3600, 3600));

        awaitStage(workflow, ClaimWorkflowStage.AWAITING_EVIDENCE);
        workflow.evidenceReceived("evidence-1");
        awaitStage(workflow, ClaimWorkflowStage.AWAITING_ASSESSMENT);
        workflow.assessmentSubmitted("assessment-1");
        awaitStage(workflow, ClaimWorkflowStage.AWAITING_DECISION);
        workflow.decisionRecorded("decision-1");
        awaitStage(workflow, ClaimWorkflowStage.AWAITING_SETTLEMENT);
        workflow.settlementReconciled("settlement-1");

        MotorClaimResult completed = result.get(5, TimeUnit.SECONDS);

        assertThat(completed.outcome()).isEqualTo(ClaimWorkflowStage.COMPLETED);
        assertThat(completed.overdueStages()).isEmpty();
        assertThat(recordingActivities().events).contains("completed:CLM-1001");
    }

    @Test
    void escalatesOverdueEvidenceAndContinuesAfterTheSignal() throws Exception {
        MotorClaimWorkflow workflow = newWorkflow("claim-overdue");
        CompletableFuture<MotorClaimResult> result =
                WorkflowClient.execute(
                        workflow::run, new MotorClaimInput("CLM-1002", 3600, 3600, 3600, 3600));

        awaitStage(workflow, ClaimWorkflowStage.AWAITING_EVIDENCE);
        testEnvironment.sleep(Duration.ofSeconds(3601));
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                assertThat(recordingActivities().events)
                                        .contains(
                                                "escalated:CLM-1002:EVIDENCE:"
                                                        + "task-EVIDENCE_COLLECTION"));

        workflow.evidenceReceived("evidence-2");
        workflow.assessmentSubmitted("assessment-2");
        workflow.decisionRecorded("decision-2");
        workflow.settlementReconciled("settlement-2");

        MotorClaimResult completed = result.get(5, TimeUnit.SECONDS);

        assertThat(completed.outcome()).isEqualTo(ClaimWorkflowStage.COMPLETED);
        assertThat(completed.overdueStages()).containsExactly("EVIDENCE");
    }

    @Test
    void cancelsOutstandingHumanWork() throws Exception {
        MotorClaimWorkflow workflow = newWorkflow("claim-cancelled");
        CompletableFuture<MotorClaimResult> result =
                WorkflowClient.execute(
                        workflow::run, new MotorClaimInput("CLM-1003", 3600, 3600, 3600, 3600));

        awaitStage(workflow, ClaimWorkflowStage.AWAITING_EVIDENCE);
        workflow.cancel("Claim withdrawn");

        MotorClaimResult cancelled = result.get(5, TimeUnit.SECONDS);

        assertThat(cancelled.outcome()).isEqualTo(ClaimWorkflowStage.CANCELLED);
        assertThat(recordingActivities().events)
                .contains("cancelled:CLM-1003:task-EVIDENCE_COLLECTION:Claim withdrawn");
    }

    private MotorClaimWorkflow newWorkflow(String workflowId) {
        return testEnvironment
                .getWorkflowClient()
                .newWorkflowStub(
                        MotorClaimWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId)
                                .setTaskQueue(TASK_QUEUE)
                                .build());
    }

    private static void awaitStage(MotorClaimWorkflow workflow, ClaimWorkflowStage expectedStage) {
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(workflow.status().stage()).isEqualTo(expectedStage));
    }

    private RecordingClaimActivities recordingActivities() {
        return (RecordingClaimActivities) activities;
    }

    private static final class RecordingClaimActivities implements ClaimActivities {

        private final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void verifyCoverage(String claimReference) {
            events.add("coverage:" + claimReference);
        }

        @Override
        public String createTask(
                String claimReference,
                String taskType,
                String assigneeRole,
                long dueAtEpochMillis) {
            events.add("task:" + claimReference + ":" + taskType + ":" + assigneeRole);
            return "task-" + taskType;
        }

        @Override
        public void escalate(String claimReference, String stage, String taskId) {
            events.add("escalated:" + claimReference + ":" + stage + ":" + taskId);
        }

        @Override
        public void cancelTask(String claimReference, String taskId, String reason) {
            events.add("cancelled:" + claimReference + ":" + taskId + ":" + reason);
        }

        @Override
        public void recordCompletion(String claimReference) {
            events.add("completed:" + claimReference);
        }
    }
}
