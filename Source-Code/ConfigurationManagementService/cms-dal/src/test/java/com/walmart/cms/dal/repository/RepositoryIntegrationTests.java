package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.repository.*;

import com.walmart.cms.dal.config.CmsDalTestApplication;
import com.walmart.cms.dal.config.EmbeddedPostgresConfiguration;
import com.walmart.cms.dal.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = CmsDalTestApplication.class)
@ActiveProfiles("test")
@Import(EmbeddedPostgresConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIntegrationTests {

    @Autowired OrganizationRepository organizationRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ConfigurationScopeRepository configurationScopeRepository;
    @Autowired ConfigurationVersionRepository configurationVersionRepository;
    @Autowired ConfigurationDeploymentRepository configurationDeploymentRepository;
    @Autowired DeploymentStageRepository deploymentStageRepository;
    @Autowired DeploymentInstanceRepository deploymentInstanceRepository;
    @Autowired PrincipalRepository principalRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;
    @Autowired RolePermissionRepository rolePermissionRepository;
    @Autowired RoleBindingRepository roleBindingRepository;


    @Test
    void configurationScopeRepository_validatesAndCreatesScope() {
        OrganizationEntity org = organization("scope-validation-org");
        ProjectEntity project = project(org, "scope-validation-project");
        ApplicationEntity app = application(project, "scope-validation-app");
        applicationRepository.save(app);

        ConfigurationScopeEntity valid = scope(org, "APPLICATION", "PROD", app.getApplicationId(), null);
        ConfigurationScopeEntity created = configurationScopeRepository.createValidated(valid);
        assertThat(configurationScopeRepository.findById(created.getScopeId())).isPresent();

        ConfigurationScopeEntity invalid = scope(org, "REGION", "PROD", UUID.randomUUID(), "us-central1");
        assertThatThrownBy(() -> configurationScopeRepository.createValidated(invalid))
                .isInstanceOf(com.walmart.cms.dal.exception.InvalidConfigurationScopeException.class);
    }

    @Test
    void configurationVersionRepository_createsNextVersionAndRejectsStaleBase() {
        OrganizationEntity org = organization("version-validation-org");
        ConfigurationScopeEntity scope = scope(org, "GLOBAL", null, null, null);
        configurationScopeRepository.save(scope);
        ConfigurationVersionEntity first = version(scope, 0, Map.of("enabled", true));
        first.setVersionNumber(null);
        ConfigurationVersionEntity created = configurationVersionRepository.createNextVersion(
                scope.getScopeId(), null, first);
        assertThat(created.getVersionNumber()).isEqualTo(1);

        ConfigurationVersionEntity second = version(scope, 0, Map.of("enabled", false));
        second.setVersionNumber(null);
        ConfigurationVersionEntity createdSecond = configurationVersionRepository.createNextVersion(
                scope.getScopeId(), created.getVersionId(), second);
        assertThat(createdSecond.getVersionNumber()).isEqualTo(2);

        ConfigurationVersionEntity stale = version(scope, 0, Map.of("enabled", true));
        stale.setVersionNumber(null);
        assertThatThrownBy(() -> configurationVersionRepository.createNextVersion(
                scope.getScopeId(), created.getVersionId(), stale))
                .isInstanceOf(com.walmart.cms.dal.exception.StaleConfigurationVersionException.class);
    }

    @Test
    void organizationRepository_supportsSaveAndFinders() {
        OrganizationEntity org = organization("org-a");
        organizationRepository.save(org);

        assertThat(organizationRepository.findById(org.getOrganizationId())).contains(org);
        assertThat(organizationRepository.findByName("org-a")).contains(org);
        assertThat(organizationRepository.findByName("missing")).isEmpty();
    }

    @Test
    void projectRepository_filtersByOrganizationAndOrdersByName() {
        OrganizationEntity org = organization("org");
        ProjectEntity z = project(org, "Zulu");
        ProjectEntity a = project(org, "Alpha");
        projectRepository.save(z);
        projectRepository.save(a);

        assertThat(projectRepository.findByOrganizationIdOrderByName(org.getOrganizationId()))
                .extracting(ProjectEntity::getName).containsExactly("Alpha", "Zulu");
        assertThat(projectRepository.findByProjectIdAndOrganizationId(a.getProjectId(), org.getOrganizationId()))
                .contains(a);
        assertThat(projectRepository.findByProjectIdAndOrganizationId(a.getProjectId(), UUID.randomUUID())).isEmpty();
        assertThat(projectRepository.findById(z.getProjectId())).contains(z);
    }

    @Test
    void applicationRepository_supportsHierarchyAndTenantIsolationQueries() {
        OrganizationEntity org = organization("org");
        ProjectEntity project = project(org, "project");
        ApplicationEntity app = application(project, "app");
        applicationRepository.save(app);

        assertThat(applicationRepository.findById(app.getApplicationId())).contains(app);
        assertThat(applicationRepository.findByProjectIdOrderByName(project.getProjectId()))
                .extracting(ApplicationEntity::getName).containsExactly("app");
        assertThat(applicationRepository.findByApplicationIdAndProjectId(app.getApplicationId(), project.getProjectId()))
                .contains(app);
        assertThat(projectRepository.existsByProjectIdAndOrganizationId(project.getProjectId(), org.getOrganizationId())).isTrue();
        assertThat(projectRepository.existsByProjectIdAndOrganizationId(project.getProjectId(), UUID.randomUUID())).isFalse();
        assertThat(applicationRepository.existsByApplicationIdAndProjectId(app.getApplicationId(), project.getProjectId())).isTrue();
    }

    @Test
    void configurationScopeRepository_supportsLookupAndPessimisticLockQuery() {
        OrganizationEntity org = organization("org");
        ConfigurationScopeEntity scope = scope(org, "GLOBAL", null, null, null);
        scope = configurationScopeRepository.save(scope);

        assertThat(configurationScopeRepository.findById(scope.getScopeId())).contains(scope);
        assertThat(configurationScopeRepository.findFirstByScopeId(scope.getScopeId())).isPresent();
        assertThat(configurationScopeRepository.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                org.getOrganizationId(), "GLOBAL", null, null, null)).contains(scope);
    }

    @Test
    void configurationVersionRepository_supportsVersionHistoryAndDocumentProjection() {
        OrganizationEntity org = organization("org");
        ConfigurationScopeEntity scope = scope(org, "GLOBAL", null, null, null);
        configurationScopeRepository.save(scope);
        ConfigurationVersionEntity v1 = version(scope, 1, Map.of("feature", true));
        ConfigurationVersionEntity v2 = version(scope, 2, Map.of("feature", false));
        v1 = configurationVersionRepository.save(v1);
        v2 = configurationVersionRepository.save(v2);

        assertThat(configurationVersionRepository.findById(v1.getVersionId())).contains(v1);
        assertThat(configurationVersionRepository.findByVersionIdAndScopeId(v1.getVersionId(), scope.getScopeId())).contains(v1);
        assertThat(configurationVersionRepository.findTopByScopeIdOrderByVersionNumberDesc(scope.getScopeId()))
                .get().extracting(ConfigurationVersionEntity::getVersionNumber).isEqualTo(2);
        List<ConfigurationVersionEntity> docs = configurationVersionRepository.findAllByVersionIdIn(
                List.of(v1.getVersionId(), v2.getVersionId()));
        assertThat(docs).hasSize(2);
        assertThat(docs).extracting(ConfigurationVersionEntity::getVersionId)
                .containsExactlyInAnyOrder(v1.getVersionId(), v2.getVersionId());
    }

    @Test
    void configurationDeploymentRepository_findsDeploymentByIdAndServingContext() {
        OrganizationEntity org = organization("org");
        ProjectEntity project = project(org, "project");
        ApplicationEntity app = application(project, "app");
        ConfigurationDeploymentEntity deployment = deployment(app, "ACTIVE");
        deployment = configurationDeploymentRepository.save(deployment);

        assertThat(configurationDeploymentRepository.findById(deployment.getDeploymentId())).contains(deployment);
        assertThat(configurationDeploymentRepository.findByDeploymentIdAndApplicationId(
                deployment.getDeploymentId(), app.getApplicationId())).contains(deployment);
        assertThat(configurationDeploymentRepository.findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
                app.getApplicationId(), "PROD", "us-central1", "ACTIVE")).contains(deployment);
        assertThat(configurationDeploymentRepository.findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
                app.getApplicationId(), "DEV", "us-central1", "ACTIVE")).isEmpty();
    }

    @Test
    void deploymentStageRepository_savesBatchAndOrdersStages() {
        OrganizationEntity org = organization("org");
        ProjectEntity project = project(org, "project");
        ApplicationEntity app = application(project, "app");
        ConfigurationDeploymentEntity deployment = deployment(app, "ACTIVE");
        configurationDeploymentRepository.save(deployment);

        DeploymentStageEntity stage2 = stage(deployment, 2, 50, "PENDING");
        DeploymentStageEntity stage1 = stage(deployment, 1, 10, "ACTIVE");
        deploymentStageRepository.save(stage2);
        stage1 = deploymentStageRepository.save(stage1);

        assertThat(deploymentStageRepository.findByStageIdAndDeploymentId(stage1.getStageId(), deployment.getDeploymentId()))
                .contains(stage1);
        assertThat(deploymentStageRepository.findFirstByDeploymentIdAndStatusOrderByStageNumber(
                deployment.getDeploymentId(), "ACTIVE")).contains(stage1);
        assertThat(deploymentStageRepository.findByDeploymentIdOrderByStageNumber(deployment.getDeploymentId()))
                .extracting(DeploymentStageEntity::getStageNumber).containsExactly(1, 2);
    }

    @Test
    void deploymentInstanceRepository_supportsCompositeKeyAndStageLookup() {
        OrganizationEntity org = organization("org");
        ProjectEntity project = project(org, "project");
        ApplicationEntity app = application(project, "app");
        ConfigurationDeploymentEntity deployment = deployment(app, "ACTIVE");
        configurationDeploymentRepository.save(deployment);
        DeploymentStageEntity stage = stage(deployment, 1, 10, "ACTIVE");
        deploymentStageRepository.save(stage);

        DeploymentInstanceEntity instance = TestEntities.instance();
        DeploymentInstanceId id = new DeploymentInstanceId(deployment.getDeploymentId(), stage.getStageId(), "instance-1");
        instance.setId(id);
        instance.setVersionSet(Map.of("global", "v1"));
        instance.setStatus("ACKNOWLEDGED");
        instance.setAcknowledgedAt(Instant.parse("2026-01-01T00:00:00Z"));
        instance = deploymentInstanceRepository.save(instance);

        assertThat(deploymentInstanceRepository.findById(id)).contains(instance);
        assertThat(deploymentInstanceRepository.findByIdDeploymentIdAndIdStageIdAndIdInstanceId(
                deployment.getDeploymentId(), stage.getStageId(), "instance-1")).contains(instance);
        assertThat(deploymentInstanceRepository.findByIdDeploymentIdAndIdStageId(
                deployment.getDeploymentId(), stage.getStageId())).containsExactly(instance);
    }

    @Test
    void principalRepository_findsPrincipalByExternalIdentity() {
        PrincipalEntity principal = new PrincipalEntity();
        principal.setPrincipalId(UUID.randomUUID());
        principal.setPrincipalType("USER");
        principal.setExternalId("user-123");
        principal.setName("Test User");
        principal = principalRepository.save(principal);

        assertThat(principalRepository.findById(principal.getPrincipalId())).contains(principal);
        assertThat(principalRepository.findByPrincipalTypeAndExternalId("USER", "user-123")).contains(principal);
        assertThat(principalRepository.findByPrincipalTypeAndExternalId("SERVICE", "user-123")).isEmpty();
    }

    @Test
    void roleRepository_findsRoleByName() {
        RoleEntity role = new RoleEntity();
        role.setRoleId(UUID.randomUUID());
        role.setRoleName("CONFIG_ADMIN");
        role.setDescription("Configuration administrator");
        role = roleRepository.save(role);

        assertThat(roleRepository.findById(role.getRoleId())).contains(role);
        assertThat(roleRepository.findByRoleName("CONFIG_ADMIN")).contains(role);
        assertThat(roleRepository.findByRoleName("UNKNOWN")).isEmpty();
    }

    @Test
    void permissionRepository_findsPermissionByName() {
        PermissionEntity permission = new PermissionEntity();
        permission.setPermissionId(UUID.randomUUID());
        permission.setPermissionName("CONFIG_READ");
        permission.setDescription("Read effective configuration");
        permission = permissionRepository.save(permission);

        assertThat(permissionRepository.findById(permission.getPermissionId())).contains(permission);
        assertThat(permissionRepository.findByPermissionName("CONFIG_READ")).contains(permission);
        assertThat(permissionRepository.findByPermissionName("UNKNOWN")).isEmpty();
    }

    @Test
    void rolePermissionRepository_findsPermissionsAssignedToRole() {
        RoleEntity role = new RoleEntity();
        role.setRoleId(UUID.randomUUID());
        role.setRoleName("READER");
        roleRepository.save(role);
        PermissionEntity p1 = permission("CONFIG_READ");
        PermissionEntity p2 = permission("CONFIG_COMPARE");
        rolePermissionRepository.save(rolePermission(role, p1));
        rolePermissionRepository.save(rolePermission(role, p2));

        assertThat(rolePermissionRepository.findByIdRoleId(role.getRoleId())).hasSize(2)
                .extracting(rp -> rp.getId().getPermissionId())
                .containsExactlyInAnyOrder(p1.getPermissionId(), p2.getPermissionId());
    }

    @Test
    void roleBindingRepository_returnsOrganizationProjectAndApplicationApplicableBindings() {
        OrganizationEntity org = organization("org");
        ProjectEntity project = project(org, "project");
        ApplicationEntity app = application(project, "app");
        PrincipalEntity principal = new PrincipalEntity();
        principal.setPrincipalId(UUID.randomUUID());
        principal.setPrincipalType("USER");
        principal.setExternalId("user-1");
        principalRepository.save(principal);
        RoleEntity role = new RoleEntity();
        role.setRoleId(UUID.randomUUID());
        role.setRoleName("DEPLOYER");
        roleRepository.save(role);

        RoleBindingEntity orgBinding = binding(principal, role, org, null, null);
        RoleBindingEntity projectBinding = binding(principal, role, org, project, null);
        RoleBindingEntity appBinding = binding(principal, role, org, project, app);
        roleBindingRepository.save(orgBinding);
        roleBindingRepository.save(projectBinding);
        roleBindingRepository.save(appBinding);

        assertThat(roleBindingRepository.findApplicableBindings(principal.getPrincipalId(), org.getOrganizationId(),
                project.getProjectId(), app.getApplicationId())).hasSize(3);
        assertThat(roleBindingRepository.findApplicableBindings(UUID.randomUUID(), org.getOrganizationId(),
                project.getProjectId(), app.getApplicationId())).isEmpty();
    }

    private OrganizationEntity organization(String name) {
        OrganizationEntity e = new OrganizationEntity();
        e.setOrganizationId(UUID.randomUUID());
        e.setName(name);
        return organizationRepository.save(e);
    }

    private ProjectEntity project(OrganizationEntity org, String name) {
        ProjectEntity e = new ProjectEntity();
        e.setProjectId(UUID.randomUUID());
        e.setOrganizationId(org.getOrganizationId());
        e.setName(name);
        return projectRepository.save(e);
    }

    private ApplicationEntity application(ProjectEntity project, String name) {
        ApplicationEntity e = new ApplicationEntity();
        e.setApplicationId(UUID.randomUUID());
        e.setProjectId(project.getProjectId());
        e.setName(name);
        return applicationRepository.save(e);
    }

    private ConfigurationScopeEntity scope(OrganizationEntity org, String type, String environment,
                                           UUID applicationId, String region) {
        ConfigurationScopeEntity e = new ConfigurationScopeEntity();
        e.setScopeId(UUID.randomUUID());
        e.setOrganizationId(org.getOrganizationId());
        e.setScopeType(type);
        e.setEnvironmentName(environment);
        e.setApplicationId(applicationId);
        e.setRegionCode(region);
        return e;
    }

    private ConfigurationVersionEntity version(ConfigurationScopeEntity scope, int number, Map<String, Object> document) {
        ConfigurationVersionEntity e = TestEntities.version();
        e.setVersionId(UUID.randomUUID());
        e.setScopeId(scope.getScopeId());
        e.setVersionNumber(number);
        e.setConfigDocument(document);
        e.setChecksum("checksum-" + number);
        e.setChangeSummary("Test version " + number);
        return e;
    }

    private ConfigurationDeploymentEntity deployment(ApplicationEntity app, String status) {
        ConfigurationDeploymentEntity e = TestEntities.deployment();
        e.setDeploymentId(UUID.randomUUID());
        e.setApplicationId(app.getApplicationId());
        e.setEnvironmentName("PROD");
        e.setRegionCode("us-central1");
        e.setBaselineVersionSet(Map.of("global", "v1"));
        e.setCandidateVersionSet(Map.of("global", "v2"));
        e.setStatus(status);
        return e;
    }

    private DeploymentStageEntity stage(ConfigurationDeploymentEntity deployment, int number,
                                        int percentage, String status) {
        DeploymentStageEntity e = TestEntities.stage();
        e.setStageId(UUID.randomUUID());
        e.setDeploymentId(deployment.getDeploymentId());
        e.setStageNumber(number);
        e.setTargetPercentage(percentage);
        e.setStatus(status);
        return e;
    }

    private PermissionEntity permission(String name) {
        PermissionEntity e = new PermissionEntity();
        e.setPermissionId(UUID.randomUUID());
        e.setPermissionName(name);
        permissionRepository.save(e);
        return e;
    }

    private RolePermissionEntity rolePermission(RoleEntity role, PermissionEntity permission) {
        RolePermissionEntity e = new RolePermissionEntity();
        e.setId(new RolePermissionId(role.getRoleId(), permission.getPermissionId()));
        return e;
    }

    private RoleBindingEntity binding(PrincipalEntity principal, RoleEntity role, OrganizationEntity org,
                                      ProjectEntity project, ApplicationEntity app) {
        RoleBindingEntity e = new RoleBindingEntity();
        e.setBindingId(UUID.randomUUID());
        e.setPrincipalId(principal.getPrincipalId());
        e.setRoleId(role.getRoleId());
        e.setOrganizationId(org.getOrganizationId());
        e.setProjectId(project == null ? null : project.getProjectId());
        e.setApplicationId(app == null ? null : app.getApplicationId());
        return e;
    }
}
