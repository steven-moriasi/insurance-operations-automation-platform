package com.stevenmoriasi.insurance.cases.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEvent {

    @Id private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String actorUsername;
    private Instant occurredAt;
    private String eventData;

    protected AuditEvent() {}

    public AuditEvent(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String actorUsername,
            Instant occurredAt,
            String eventData) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.actorUsername = actorUsername;
        this.occurredAt = occurredAt;
        this.eventData = eventData;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEventData() {
        return eventData;
    }
}
