package com.stevenmoriasi.insurance.bff.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.stevenmoriasi.insurance.bff.cases.UserAccessTokenProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
public class InsuranceIntegrationController {

    private static final String INTERNAL_BASE = "/internal/api/v1/integrations";

    private final IntegrationServiceClient integrations;
    private final UserAccessTokenProvider tokens;

    public InsuranceIntegrationController(
            IntegrationServiceClient integrations, UserAccessTokenProvider tokens) {
        this.integrations = integrations;
        this.tokens = tokens;
    }

    @PostMapping("/policies/verify")
    public ResponseEntity<JsonNode> verifyPolicy(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/policies/verify",
                request,
                idempotencyKey,
                authentication);
    }

    @PostMapping("/payments")
    public ResponseEntity<JsonNode> instructPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/payments",
                request,
                idempotencyKey,
                authentication);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<JsonNode> getPayment(
            @PathVariable String paymentId, Authentication authentication) {
        return forward(
                HttpMethod.GET,
                INTERNAL_BASE + "/payments/" + paymentId,
                null,
                null,
                authentication);
    }

    @PostMapping("/documents")
    public ResponseEntity<JsonNode> registerDocument(
            @RequestBody JsonNode request, Authentication authentication) {
        return forward(
                HttpMethod.POST, INTERNAL_BASE + "/documents", request, null, authentication);
    }

    @PostMapping("/automation/work-items")
    public ResponseEntity<JsonNode> enqueueAutomation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/automation/work-items",
                request,
                idempotencyKey,
                authentication);
    }

    private ResponseEntity<JsonNode> forward(
            HttpMethod method,
            String path,
            JsonNode body,
            String idempotencyKey,
            Authentication authentication) {
        return integrations.exchange(
                method, path, body, tokens.currentToken(authentication), idempotencyKey);
    }
}
