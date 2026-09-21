package com.stevenmoriasi.insurance.workflows.shared;

public record HumanTaskView(
        String taskType, String candidateRole, long dueAtEpochMillis, boolean overdue) {}
