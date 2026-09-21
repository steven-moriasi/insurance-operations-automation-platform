package com.stevenmoriasi.insurance.workflows.cases;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class ServiceAccessTokenProvider {

    private final OAuth2AuthorizedClientManager authorizedClients;

    ServiceAccessTokenProvider(OAuth2AuthorizedClientManager authorizedClients) {
        this.authorizedClients = authorizedClients;
    }

    String currentToken() {
        OAuth2AuthorizedClient client =
                authorizedClients.authorize(
                        OAuth2AuthorizeRequest.withClientRegistrationId("case-service")
                                .principal("insurance-workflow-service")
                                .build());
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No case-service workload access token is available");
        }
        return client.getAccessToken().getTokenValue();
    }
}
