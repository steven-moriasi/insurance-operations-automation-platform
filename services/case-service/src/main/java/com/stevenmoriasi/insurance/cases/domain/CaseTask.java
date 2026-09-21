package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.TaskStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_task")
public class CaseTask {

    @Id private UUID id;
    private UUID claimId;
    private String taskType;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private String assignee;
    private Instant dueAt;
    private Instant createdAt;
    private Instant completedAt;

    protected CaseTask() {}

    public CaseTask(
            UUID id,
            UUID claimId,
            String taskType,
            String assignee,
            Instant dueAt,
            Instant createdAt) {
        this.id = id;
        this.claimId = claimId;
        this.taskType = taskType;
        this.status = assignee == null ? TaskStatus.OPEN : TaskStatus.CLAIMED;
        this.assignee = assignee;
        this.dueAt = dueAt;
        this.createdAt = createdAt;
    }

    public void complete(Instant completedAt) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public String getTaskType() {
        return taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
