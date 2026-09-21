package com.stevenmoriasi.insurance.workflows.cases;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class CaseServiceGateway {

    private final RestClient restClient;
    private final ServiceAccessTokenProvider tokens;

    CaseServiceGateway(
            RestClient.Builder restClientBuilder,
            ServiceAccessTokenProvider tokens,
            @Value("${insurance.services.case-service-base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.tokens = tokens;
    }

    JsonNode getCase(String claimReference) {
        return restClient
                .get()
                .uri(
                        builder ->
                                builder.pathSegment(
                                                "internal", "api", "v1", "claims", claimReference)
                                        .build())
                .headers(headers -> headers.setBearerAuth(tokens.currentToken()))
                .retrieve()
                .body(JsonNode.class);
    }

    UUID createTask(String claimReference, String taskType, String candidateRole, Instant dueAt) {
        TaskResponse response =
                restClient
                        .post()
                        .uri(
                                builder ->
                                        builder.pathSegment(
                                                        "internal",
                                                        "api",
                                                        "v1",
                                                        "claims",
                                                        claimReference,
                                                        "tasks")
                                                .build())
                        .headers(headers -> headers.setBearerAuth(tokens.currentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new CreateTaskRequest(taskType, candidateRole, null, dueAt))
                        .retrieve()
                        .body(TaskResponse.class);
        if (response == null || response.id() == null) {
            throw new IllegalStateException("Case service returned no task identifier");
        }
        return response.id();
    }

    void cancelTask(String claimReference, UUID taskId, String reason) {
        restClient
                .post()
                .uri(
                        builder ->
                                builder.pathSegment(
                                                "internal",
                                                "api",
                                                "v1",
                                                "claims",
                                                claimReference,
                                                "tasks",
                                                taskId.toString(),
                                                "cancel")
                                        .build())
                .headers(headers -> headers.setBearerAuth(tokens.currentToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CancelTaskRequest(reason))
                .retrieve()
                .toBodilessEntity();
    }

    void recordCompletion(String claimReference) {
        restClient
                .post()
                .uri(
                        builder ->
                                builder.pathSegment(
                                                "internal",
                                                "api",
                                                "v1",
                                                "claims",
                                                claimReference,
                                                "workflow-events",
                                                "completed")
                                        .build())
                .headers(headers -> headers.setBearerAuth(tokens.currentToken()))
                .retrieve()
                .toBodilessEntity();
    }

    private record CreateTaskRequest(
            String taskType, String candidateRole, String assignee, Instant dueAt) {}

    private record CancelTaskRequest(String reason) {}

    private record TaskResponse(UUID id) {}
}
