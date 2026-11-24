package com.walmart.cms.coreapi.controller;
import com.walmart.cms.coreapi.service.CoreConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
class CoreConfigurationControllerTest {
 @Test void effectiveConfigurationRouteDelegatesToService() throws Exception {
  CoreConfigurationService service=mock(CoreConfigurationService.class);
  MockMvc mvc=standaloneSetup(new CoreConfigurationController(service)).build();
  mvc.perform(get("/api/v1/organizations/{o}/applications/{a}/config",UUID.randomUUID(),UUID.randomUUID())
    .param("environment","PROD").param("region","us-central1").header("X-Instance-Id","instance-1"))
    .andExpect(status().isOk());
  verify(service).retrieveEffectiveConfiguration(any(),any(),eq("PROD"),eq("us-central1"),eq("instance-1"));
 }
}
