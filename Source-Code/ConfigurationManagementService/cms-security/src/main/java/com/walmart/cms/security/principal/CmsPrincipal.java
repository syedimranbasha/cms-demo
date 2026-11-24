package com.walmart.cms.security.principal;

/**
 * Stable application-level identity extracted from a validated JWT.
 *
 * externalId is the identity-provider subject (JWT "sub"), which should be
 * mapped to principals.external_id in the CMS database.
 */
public record CmsPrincipal(
        String externalId,
        String username
) {
}
