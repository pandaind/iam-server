package com.vibes.iam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Controller
@Tag(name = "OAuth2", description = "OAuth2 Authorization Server endpoints")
public class OAuth2Controller {

    private final OAuth2AuthorizationService authorizationService;

    public OAuth2Controller(OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/login")
    @Operation(summary = "Login page", description = "OAuth2 login page")
    public String login() {
        return "login";
    }

    @GetMapping("/oauth2/consent")
    @Operation(summary = "Consent page", description = "OAuth2 consent page")
    public String consent(Principal principal, Model model,
                         @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                         @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
                         @RequestParam(OAuth2ParameterNames.STATE) String state) {
        
        Set<String> scopesToApprove = Set.of(scope.split(" "));
        Set<String> previouslyApprovedScopes = Collections.emptySet();
        
        OAuth2Authorization authorization = this.authorizationService.findByToken(state, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization != null) {
            previouslyApprovedScopes = authorization.getAuthorizedScopes();
        }

        Set<String> scopesToRequest = Set.copyOf(scopesToApprove);
        scopesToRequest.removeAll(previouslyApprovedScopes);

        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state);
        model.addAttribute("scopes", withDescription(scopesToRequest));
        model.addAttribute("previouslyApprovedScopes", withDescription(previouslyApprovedScopes));
        model.addAttribute("principalName", principal.getName());

        return "consent";
    }

    @PostMapping("/oauth2/revoke")
    @ResponseBody
    @Operation(summary = "Revoke token", description = "Revoke OAuth2 token")
    public Map<String, Object> revokeToken(@RequestParam("token") String token,
                                          @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
                                          Principal principal) {
        
        OAuth2TokenType tokenType = OAuth2TokenType.ACCESS_TOKEN;
        if ("refresh_token".equals(tokenTypeHint)) {
            tokenType = OAuth2TokenType.REFRESH_TOKEN;
        }

        OAuth2Authorization authorization = this.authorizationService.findByToken(token, tokenType);
        if (authorization != null) {
            this.authorizationService.remove(authorization);
            return Map.of("revoked", true, "message", "Token revoked successfully");
        }

        return Map.of("revoked", false, "message", "Token not found or already revoked");
    }

    @GetMapping("/oauth2/introspect")
    @ResponseBody
    @Operation(summary = "Introspect token", description = "Introspect OAuth2 token")
    public Map<String, Object> introspectToken(@RequestParam("token") String token,
                                              @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {
        
        OAuth2TokenType tokenType = OAuth2TokenType.ACCESS_TOKEN;
        if ("refresh_token".equals(tokenTypeHint)) {
            tokenType = OAuth2TokenType.REFRESH_TOKEN;
        }

        OAuth2Authorization authorization = this.authorizationService.findByToken(token, tokenType);
        if (authorization != null
                && authorization.getAccessToken() != null
                && !authorization.getAccessToken().isExpired()) {
            Map<String, Object> introspection = new HashMap<>();
            introspection.put("active", true);
            introspection.put("client_id", authorization.getRegisteredClientId());
            introspection.put("username", authorization.getPrincipalName());
            introspection.put("scope", String.join(" ", authorization.getAuthorizedScopes()));
            introspection.put("exp", authorization.getAccessToken().getToken().getExpiresAt().getEpochSecond());
            introspection.put("iat", authorization.getAccessToken().getToken().getIssuedAt().getEpochSecond());
            
            return introspection;
        }

        return Map.of("active", false);
    }

    private static Set<ScopeWithDescription> withDescription(Set<String> scopes) {
        Set<ScopeWithDescription> scopeWithDescriptions = new java.util.HashSet<>();
        for (String scope : scopes) {
            scopeWithDescriptions.add(new ScopeWithDescription(scope));
        }
        return scopeWithDescriptions;
    }

    public static class ScopeWithDescription {
        public final String scope;
        public final String description;

        ScopeWithDescription(String scope) {
            this.scope = scope;
            this.description = switch (scope) {
                case "openid" -> "OpenID Connect scope for authentication";
                case "profile" -> "Access to user profile information";
                case "read" -> "Read access to resources";
                case "write" -> "Write access to resources";
                case "service" -> "Service-to-service communication";
                default -> "Unknown scope: " + scope;
            };
        }
    }
}