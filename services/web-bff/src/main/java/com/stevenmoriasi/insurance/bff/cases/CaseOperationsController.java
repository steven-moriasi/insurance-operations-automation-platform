package com.stevenmoriasi.insurance.bff.cases;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseOperationsController {

    private static final String INTERNAL_BASE = "/internal/api/v1/claims";

    private final CaseServiceClient caseService;
    private final UserAccessTokenProvider tokens;

    public CaseOperationsController(CaseServiceClient caseService, UserAccessTokenProvider tokens) {
        this.caseService = caseService;
        this.tokens = tokens;
    }

    @PostMapping
    public ResponseEntity<JsonNode> report(
            @RequestBody JsonNode request, Authentication authentication) {
        return forward(HttpMethod.POST, INTERNAL_BASE, request, authentication);
    }

    @GetMapping("/mine")
    public ResponseEntity<JsonNode> mine(Authentication authentication) {
        return forward(HttpMethod.GET, INTERNAL_BASE + "/mine", null, authentication);
    }

    @GetMapping("/{claimReference}")
    public ResponseEntity<JsonNode> get(
            @PathVariable String claimReference, Authentication authentication) {
        return forward(HttpMethod.GET, INTERNAL_BASE + "/" + claimReference, null, authentication);
    }

    @PostMapping("/{claimReference}/tasks")
    public ResponseEntity<JsonNode> createTask(
            @PathVariable String claimReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/tasks",
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/tasks/{taskId}/complete")
    public ResponseEntity<JsonNode> completeTask(
            @PathVariable String claimReference,
            @PathVariable UUID taskId,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/tasks/" + taskId + "/complete",
                null,
                authentication);
    }

    @PostMapping("/{claimReference}/evidence")
    public ResponseEntity<JsonNode> recordEvidence(
            @PathVariable String claimReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/evidence",
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/assessments")
    public ResponseEntity<JsonNode> submitAssessment(
            @PathVariable String claimReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/assessments",
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/decisions")
    public ResponseEntity<JsonNode> requestDecision(
            @PathVariable String claimReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/decisions",
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/decisions/{decisionId}/approve")
    public ResponseEntity<JsonNode> approveDecision(
            @PathVariable String claimReference,
            @PathVariable UUID decisionId,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/decisions/" + decisionId + "/approve",
                null,
                authentication);
    }

    @PostMapping("/{claimReference}/settlements")
    public ResponseEntity<JsonNode> createSettlement(
            @PathVariable String claimReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/settlements",
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/settlements/{settlementId}/instruct")
    public ResponseEntity<JsonNode> instructSettlement(
            @PathVariable String claimReference,
            @PathVariable UUID settlementId,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/settlements/" + settlementId + "/instruct",
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/settlements/{settlementId}/reconcile")
    public ResponseEntity<JsonNode> reconcileSettlement(
            @PathVariable String claimReference,
            @PathVariable UUID settlementId,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE
                        + "/"
                        + claimReference
                        + "/settlements/"
                        + settlementId
                        + "/reconcile",
                null,
                authentication);
    }

    private ResponseEntity<JsonNode> forward(
            HttpMethod method, String path, JsonNode body, Authentication authentication) {
        return caseService.exchange(method, path, body, tokens.currentToken(authentication));
    }
}
