package com.walmart.cms.managementapi.service;

import com.walmart.cms.dal.entity.*;
import com.walmart.cms.dal.repository.*;
import com.walmart.cms.managementapi.dto.ManagementDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagementServiceTest {
    @Mock OrganizationRepository organizations; @Mock ProjectRepository projects; @Mock ApplicationRepository applications;
    @Mock PrincipalRepository principals; @Mock RoleRepository roles; @Mock PermissionRepository permissions;
    @Mock RolePermissionRepository rolePermissions; @Mock RoleBindingRepository roleBindings;
    ManagementService service;
    @BeforeEach void setUp(){service=new ManagementService(organizations,projects,applications,principals,roles,permissions,rolePermissions,roleBindings);}

    @Test void createsOrganization(){when(organizations.findByName("Acme")).thenReturn(Optional.empty()); when(organizations.save(any())).thenAnswer(i->i.getArgument(0));
        var result=service.createOrganization(new CreateOrganizationRequest("Acme")); assertNotNull(result.organizationId()); assertEquals("Acme",result.name());}
    @Test void rejectsDuplicateOrganization(){when(organizations.findByName("Acme")).thenReturn(Optional.of(new OrganizationEntity())); assertThrows(ManagementService.ConflictException.class,()->service.createOrganization(new CreateOrganizationRequest("Acme"))); verify(organizations,never()).save(any());}
    @Test void createsProjectOnlyUnderExistingOrganization(){UUID org=UUID.randomUUID(); when(organizations.findById(org)).thenReturn(Optional.of(orgEntity(org))); when(projects.save(any())).thenAnswer(i->i.getArgument(0)); var result=service.createProject(org,new CreateProjectRequest("Payments")); assertEquals(org,result.organizationId());}
    @Test void rejectsProjectForUnknownOrganization(){UUID org=UUID.randomUUID(); when(organizations.findById(org)).thenReturn(Optional.empty()); assertThrows(ManagementService.ResourceNotFoundException.class,()->service.createProject(org,new CreateProjectRequest("Payments")));}
    @Test void createsApplicationOnlyUnderProjectInOrganization(){UUID org=UUID.randomUUID(), project=UUID.randomUUID(); when(organizations.findById(org)).thenReturn(Optional.of(orgEntity(org))); when(projects.findByProjectIdAndOrganizationId(project,org)).thenReturn(Optional.of(projectEntity(project,org))); when(applications.save(any())).thenAnswer(i->i.getArgument(0)); var result=service.createApplication(org,project,new CreateApplicationRequest("Checkout")); assertEquals(project,result.projectId());}
    @Test void createsPrincipal(){when(principals.findByPrincipalTypeAndExternalId("USER","user-1")).thenReturn(Optional.empty()); when(principals.save(any())).thenAnswer(i->i.getArgument(0)); var result=service.createPrincipal(new CreatePrincipalRequest("USER","user-1","Alice")); assertEquals("user-1",result.externalId());}
    @Test void createsRole(){when(roles.findByRoleName("Admin")).thenReturn(Optional.empty()); when(roles.save(any())).thenAnswer(i->i.getArgument(0)); assertEquals("Admin",service.createRole(new CreateRoleRequest("Admin","Administrator")).roleName());}
    @Test void createsPermission(){when(permissions.findByPermissionName("config:read")).thenReturn(Optional.empty()); when(permissions.save(any())).thenAnswer(i->i.getArgument(0)); assertEquals("config:read",service.createPermission(new CreatePermissionRequest("config:read","Read config")).permissionName());}
    @Test void addsPermissionToRole(){UUID role=UUID.randomUUID(),permission=UUID.randomUUID(); when(roles.findById(role)).thenReturn(Optional.of(new RoleEntity())); when(permissions.findById(permission)).thenReturn(Optional.of(new PermissionEntity())); when(rolePermissions.existsById(any())).thenReturn(false); var r=service.addPermissionToRole(role,new CreateRolePermissionRequest(permission)); assertEquals(role,r.roleId()); verify(rolePermissions).save(any());}
    @Test void createsOrganizationScopedRoleBinding(){UUID org=UUID.randomUUID(),principal=UUID.randomUUID(),role=UUID.randomUUID(); when(principals.findById(principal)).thenReturn(Optional.of(new PrincipalEntity())); when(roles.findById(role)).thenReturn(Optional.of(new RoleEntity())); when(organizations.findById(org)).thenReturn(Optional.of(orgEntity(org))); when(roleBindings.save(any())).thenAnswer(i->i.getArgument(0)); var r=service.createRoleBinding(new CreateRoleBindingRequest(principal,role,org,null,null)); assertEquals(org,r.organizationId());}
    @Test void rejectsApplicationBindingWithoutProject(){
        UUID principal=UUID.randomUUID(), role=UUID.randomUUID(), org=UUID.randomUUID();
        when(principals.findById(principal)).thenReturn(Optional.of(new PrincipalEntity()));
        when(roles.findById(role)).thenReturn(Optional.of(new RoleEntity()));
        when(organizations.findById(org)).thenReturn(Optional.of(orgEntity(org)));
        var request=new CreateRoleBindingRequest(principal,role,org,null,UUID.randomUUID());
        assertThrows(IllegalArgumentException.class,()->service.createRoleBinding(request));
        verify(roleBindings,never()).save(any());
    }
    @Test void rejectsDuplicateRolePermission(){
        UUID role=UUID.randomUUID(), permission=UUID.randomUUID();
        when(roles.findById(role)).thenReturn(Optional.of(new RoleEntity()));
        when(permissions.findById(permission)).thenReturn(Optional.of(new PermissionEntity()));
        when(rolePermissions.existsById(any())).thenReturn(true);
        assertThrows(ManagementService.ConflictException.class,()->service.addPermissionToRole(role,new CreateRolePermissionRequest(permission)));
        verify(rolePermissions,never()).save(any());
    }

    private OrganizationEntity orgEntity(UUID id){var e=new OrganizationEntity();e.setOrganizationId(id);e.setName("Acme");return e;}
    private ProjectEntity projectEntity(UUID id,UUID org){var e=new ProjectEntity();e.setProjectId(id);e.setOrganizationId(org);e.setName("Project");return e;}
}
