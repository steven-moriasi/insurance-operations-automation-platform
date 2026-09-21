package com.stevenmoriasi.insurance.bff.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class IntegrationServiceClient {

    private final RestClient restClient;
    private final String baseUrl;

    public IntegrationServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${insurance.services.integration-service-base-url}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
    }

    public ResponseEntity<JsonNode> exchange(
            HttpMethod method,
            String path,
            JsonNode body,
            String accessToken,
            String idempotencyKey) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path(path).build().toUri();
        RestClient.RequestBodySpec request =
                restClient
                        .method(method)
                        .uri(uri)
                        .headers(
                                headers -> {
                                    headers.setBearerAuth(accessToken);
                                    if (idempotencyKey != null) {
                                        headers.set("Idempotency-Key", idempotencyKey);
                                    }
                                });
        if (body != null) {
            request.body(body);
        }
        return request.exchange(
                (outboundRequest, response) ->
                        ResponseEntity.status(response.getStatusCode())
                                .headers(response.getHeaders())
                                .body(
                                        response.getStatusCode().value() == 204
                                                ? null
                                                : response.bodyTo(JsonNode.class)));
    }
}
