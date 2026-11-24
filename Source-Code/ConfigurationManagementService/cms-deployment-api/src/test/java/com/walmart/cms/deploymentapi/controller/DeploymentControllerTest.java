package com.walmart.cms.deploymentapi.controller;
import com.walmart.cms.deploymentapi.service.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class DeploymentControllerTest {
 @Test void fetchDeploymentRouteDelegatesToService() throws Exception {
  DeploymentService service=mock(DeploymentService.class);
  MockMvc mvc=standaloneSetup(new DeploymentController(service)).build();
  mvc.perform(get("/api/v1/organizations/{o}/deployments/{d}",UUID.randomUUID(),UUID.randomUUID()))
   .andExpect(status().isOk());
  verify(service).fetchDeployment(any(),any());
 }
}
