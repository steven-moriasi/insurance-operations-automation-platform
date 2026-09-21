package com.stevenmoriasi.insurance.integrations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_outbox")
public class OutboxEvent {

    @Id private UUID id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private int schemaVersion;

    @Column(columnDefinition = "text")
    private String payloadJson;

    private Instant occurredAt;
    private Instant publishedAt;
    private int attempts;

    @Version private long version;

    protected OutboxEvent() {}

    public OutboxEvent(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson,
            Instant occurredAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.schemaVersion = 1;
        this.payloadJson = payloadJson;
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }
}
