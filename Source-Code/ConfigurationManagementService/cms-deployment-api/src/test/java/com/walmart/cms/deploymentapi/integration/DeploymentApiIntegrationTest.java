package com.walmart.cms.deploymentapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.walmart.cms.dal.repository.OrganizationRepository;
import com.walmart.cms.dal.entity.OrganizationEntity;
import com.walmart.cms.security.TestSecurityConfiguration;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DeploymentApiIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Import(TestSecurityConfiguration.class)
class DeploymentApiIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    OrganizationRepository organizations;

    @Test
    void embeddedPersistenceAndDeploymentRouteAreAvailable() throws Exception {
        UUID id = UUID.randomUUID();
        var constructor = OrganizationEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        OrganizationEntity org = constructor.newInstance();
        org.setOrganizationId(id);
        org.setName("integration-" + id);
        organizations.save(org);
        assertTrue(organizations.existsById(id));
        mvc.perform(get("/api/v1/organizations/00000000-0000-0000-0000-000000000001/deployments/00000000-0000-0000-0000-000000000002"))
                .andExpect(status().isNotFound());
    }

    @SpringBootApplication(scanBasePackages = "com.walmart.cms")
    @EntityScan("com.walmart.cms.dal.entity")
    @EnableJpaRepositories("com.walmart.cms.dal.repository")
    static class TestApplication {
    }
}
