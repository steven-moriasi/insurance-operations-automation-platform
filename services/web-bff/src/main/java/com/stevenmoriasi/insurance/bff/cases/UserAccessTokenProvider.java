package com.stevenmoriasi.insurance.bff.cases;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserAccessTokenProvider {

    private final OAuth2AuthorizedClientService authorizedClients;

    public UserAccessTokenProvider(OAuth2AuthorizedClientService authorizedClients) {
        this.authorizedClients = authorizedClients;
    }

    public String currentToken(Authentication authentication) {
        OAuth2AuthorizedClient client =
                authorizedClients.loadAuthorizedClient("keycloak", authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No delegated access token is available");
        }
        return client.getAccessToken().getTokenValue();
    }
}
