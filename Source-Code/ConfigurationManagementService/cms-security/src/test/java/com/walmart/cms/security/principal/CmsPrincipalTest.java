package com.walmart.cms.security.principal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CmsPrincipalTest {

    @Test
    void storesIdentityProviderSubjectAndUsername() {
        CmsPrincipal principal = new CmsPrincipal("keycloak-subject-123", "cms-admin");

        assertEquals("keycloak-subject-123", principal.externalId());
        assertEquals("cms-admin", principal.username());
    }
}
