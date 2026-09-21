package com.stevenmoriasi.insurance.bff.workflows;

import com.fasterxml.jackson.databind.JsonNode;
import com.stevenmoriasi.insurance.bff.cases.UserAccessTokenProvider;
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
@RequestMapping("/api/workflows/claims")
public class ClaimWorkflowController {

    private static final String INTERNAL_BASE = "/internal/api/v1/workflows/claims";

    private final WorkflowServiceClient workflowService;
    private final UserAccessTokenProvider tokens;

    public ClaimWorkflowController(
            WorkflowServiceClient workflowService, UserAccessTokenProvider tokens) {
        this.workflowService = workflowService;
        this.tokens = tokens;
    }

    @PostMapping
    public ResponseEntity<JsonNode> start(
            @RequestBody JsonNode request, Authentication authentication) {
        return forward(HttpMethod.POST, INTERNAL_BASE, request, authentication);
    }

    @GetMapping("/{claimReference}")
    public ResponseEntity<JsonNode> status(
            @PathVariable String claimReference, Authentication authentication) {
        return forward(HttpMethod.GET, INTERNAL_BASE + "/" + claimReference, null, authentication);
    }

    @PostMapping("/{claimReference}/signals/{signal}")
    public ResponseEntity<JsonNode> signal(
            @PathVariable String claimReference,
            @PathVariable String signal,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/signals/" + signal,
                request,
                authentication);
    }

    @PostMapping("/{claimReference}/cancel")
    public ResponseEntity<JsonNode> cancel(
            @PathVariable String claimReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                INTERNAL_BASE + "/" + claimReference + "/cancel",
                request,
                authentication);
    }

    private ResponseEntity<JsonNode> forward(
            HttpMethod method, String path, JsonNode body, Authentication authentication) {
        return workflowService.exchange(method, path, body, tokens.currentToken(authentication));
    }
}
