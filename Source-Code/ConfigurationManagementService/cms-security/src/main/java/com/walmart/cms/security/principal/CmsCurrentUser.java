package com.walmart.cms.security.principal;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Provides the identity of the caller from Spring Security's validated
 * authentication context. It does not decode or validate tokens itself.
 */
@Component
public class CmsCurrentUser {

    public CmsPrincipal getRequiredPrincipal() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("No authenticated JWT principal is available");
        }

        String subject = jwtAuthentication.getToken().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Authenticated JWT does not contain a subject (sub) claim");
        }

        String username = jwtAuthentication.getToken().getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwtAuthentication.getToken().getClaimAsString("email");
        }
        if (username == null || username.isBlank()) {
            username = subject;
        }

        return new CmsPrincipal(subject, username);
    }
}
