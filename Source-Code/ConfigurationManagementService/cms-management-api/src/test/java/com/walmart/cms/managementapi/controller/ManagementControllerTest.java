package com.walmart.cms.managementapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.managementapi.ManagementApiTestApplication;
import com.walmart.cms.managementapi.dto.ManagementDtos.*;
import com.walmart.cms.managementapi.service.ManagementService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = ManagementApiTestApplication.class)
@Import(ManagementController.class)
class ManagementControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    ManagementService service;

    @Test
    void createOrganizationReturns201() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.createOrganization(any()))
                .thenReturn(new OrganizationResponse(id, "Acme"));

        mvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(
                                new CreateOrganizationRequest("Acme"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void createRoleReturns201() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.createRole(any()))
                .thenReturn(new RoleResponse(id, "Admin", "Administrator"));

        mvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(
                                new CreateRoleRequest("Admin", "Administrator"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleId").value(id.toString()));
    }

    @Test
    void invalidOrganizationRequestReturns400() throws Exception {
        mvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}