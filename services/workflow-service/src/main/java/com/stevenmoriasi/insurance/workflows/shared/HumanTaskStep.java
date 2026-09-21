package com.stevenmoriasi.insurance.workflows.shared;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public final class HumanTaskStep {

    private String taskType;
    private String candidateRole;
    private long dueAtEpochMillis;
    private boolean overdue;

    public void open(String taskType, String candidateRole, long slaSeconds) {
        this.taskType = taskType;
        this.candidateRole = candidateRole;
        this.dueAtEpochMillis =
                Workflow.currentTimeMillis() + Duration.ofSeconds(slaSeconds).toMillis();
        this.overdue = false;
    }

    public boolean await(long slaSeconds, BooleanSupplier completed, BooleanSupplier stopped) {
        boolean completedWithinSla =
                Workflow.await(
                        Duration.ofSeconds(slaSeconds),
                        () -> completed.getAsBoolean() || stopped.getAsBoolean());
        if (!completedWithinSla) {
            overdue = true;
        }
        return completedWithinSla;
    }

    public void awaitWithoutTimeout(BooleanSupplier completed, BooleanSupplier stopped) {
        Workflow.await(() -> completed.getAsBoolean() || stopped.getAsBoolean());
    }

    public HumanTaskView view() {
        if (taskType == null) {
            return null;
        }
        return new HumanTaskView(taskType, candidateRole, dueAtEpochMillis, overdue);
    }

    public void close() {
        taskType = null;
        candidateRole = null;
        dueAtEpochMillis = 0;
        overdue = false;
    }
}
