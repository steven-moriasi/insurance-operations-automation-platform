package com.stevenmoriasi.insurance.bff.workflows;

import com.fasterxml.jackson.databind.JsonNode;
import com.stevenmoriasi.insurance.bff.cases.UserAccessTokenProvider;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workflows/reference")
public class ReferenceWorkflowController {

    private static final String INTERNAL_BASE = "/internal/api/v1/workflows/";
    private static final Set<String> JOURNEYS = Set.of("renewals", "broker-onboarding");

    private final WorkflowServiceClient workflowService;
    private final UserAccessTokenProvider tokens;

    public ReferenceWorkflowController(
            WorkflowServiceClient workflowService, UserAccessTokenProvider tokens) {
        this.workflowService = workflowService;
        this.tokens = tokens;
    }

    @PostMapping("/{journey}")
    public ResponseEntity<JsonNode> start(
            @PathVariable String journey,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(HttpMethod.POST, journeyPath(journey), request, authentication);
    }

    @GetMapping("/{journey}/{businessReference}")
    public ResponseEntity<JsonNode> status(
            @PathVariable String journey,
            @PathVariable String businessReference,
            Authentication authentication) {
        return forward(
                HttpMethod.GET,
                journeyPath(journey) + "/" + businessReference,
                null,
                authentication);
    }

    @PostMapping("/{journey}/{businessReference}/signals/{signal}")
    public ResponseEntity<JsonNode> signal(
            @PathVariable String journey,
            @PathVariable String businessReference,
            @PathVariable String signal,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                journeyPath(journey) + "/" + businessReference + "/signals/" + signal,
                request,
                authentication);
    }

    @PostMapping("/{journey}/{businessReference}/cancel")
    public ResponseEntity<JsonNode> cancel(
            @PathVariable String journey,
            @PathVariable String businessReference,
            @RequestBody JsonNode request,
            Authentication authentication) {
        return forward(
                HttpMethod.POST,
                journeyPath(journey) + "/" + businessReference + "/cancel",
                request,
                authentication);
    }

    private ResponseEntity<JsonNode> forward(
            HttpMethod method, String path, JsonNode body, Authentication authentication) {
        return workflowService.exchange(method, path, body, tokens.currentToken(authentication));
    }

    private static String journeyPath(String journey) {
        if (!JOURNEYS.contains(journey)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported reference workflow journey");
        }
        return INTERNAL_BASE + journey;
    }
}
