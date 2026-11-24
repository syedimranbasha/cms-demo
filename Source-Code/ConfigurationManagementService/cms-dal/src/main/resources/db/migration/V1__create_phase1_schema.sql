-- Phase 1 schema: excludes notification_registrations, audit_logs, outbox_events.
CREATE TABLE organizations (
  organization_id UUID PRIMARY KEY,
  name VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE projects (
  project_id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(organization_id),
  name VARCHAR(200) NOT NULL
);
CREATE INDEX idx_projects_organization ON projects(organization_id);

CREATE TABLE applications (
  application_id UUID PRIMARY KEY,
  project_id UUID NOT NULL REFERENCES projects(project_id),
  name VARCHAR(200) NOT NULL
);
CREATE INDEX idx_applications_project ON applications(project_id);

CREATE TABLE config_scopes (
  scope_id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(organization_id),
  scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('GLOBAL','ENVIRONMENT','APPLICATION','REGION')),
  environment_name VARCHAR(100),
  application_id UUID REFERENCES applications(application_id),
  region_code VARCHAR(100),
  active_version_id UUID,
  CHECK (
    (scope_type = 'GLOBAL' AND environment_name IS NULL AND application_id IS NULL AND region_code IS NULL) OR
    (scope_type = 'ENVIRONMENT' AND environment_name IS NOT NULL AND application_id IS NULL AND region_code IS NULL) OR
    (scope_type = 'APPLICATION' AND application_id IS NOT NULL AND region_code IS NULL) OR
    (scope_type = 'REGION' AND application_id IS NOT NULL AND environment_name IS NOT NULL AND region_code IS NOT NULL)
  )
);

CREATE TABLE config_versions (
  version_id UUID PRIMARY KEY,
  scope_id UUID NOT NULL REFERENCES config_scopes(scope_id),
  version_number INTEGER NOT NULL CHECK (version_number > 0),
  config_document JSONB NOT NULL,
  checksum VARCHAR(128) NOT NULL,
  change_summary VARCHAR(1000),
  CONSTRAINT uq_config_version_scope_number UNIQUE(scope_id, version_number)
);
ALTER TABLE config_scopes
  ADD CONSTRAINT fk_config_scopes_active_version
  FOREIGN KEY (active_version_id) REFERENCES config_versions(version_id);

CREATE TABLE config_deployments (
  deployment_id UUID PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES applications(application_id),
  environment_name VARCHAR(100) NOT NULL,
  region_code VARCHAR(100) NOT NULL,
  baseline_version_set JSONB NOT NULL,
  candidate_version_set JSONB NOT NULL,
  status VARCHAR(30) NOT NULL,
  current_stage_id UUID
);
CREATE INDEX idx_config_deployments_lookup
  ON config_deployments(application_id, environment_name, region_code, status);

CREATE TABLE deployment_stages (
  stage_id UUID PRIMARY KEY,
  deployment_id UUID NOT NULL REFERENCES config_deployments(deployment_id),
  stage_number INTEGER NOT NULL CHECK (stage_number > 0),
  target_percentage INTEGER NOT NULL CHECK (target_percentage BETWEEN 1 AND 100),
  status VARCHAR(30) NOT NULL,
  CONSTRAINT uq_deployment_stage_number UNIQUE(deployment_id, stage_number)
);
ALTER TABLE config_deployments
  ADD CONSTRAINT fk_config_deployments_current_stage
  FOREIGN KEY (current_stage_id) REFERENCES deployment_stages(stage_id);

CREATE TABLE deployment_instance (
  deployment_id UUID NOT NULL REFERENCES config_deployments(deployment_id),
  stage_id UUID NOT NULL REFERENCES deployment_stages(stage_id),
  instance_id VARCHAR(200) NOT NULL,
  version_set JSONB NOT NULL,
  status VARCHAR(30) NOT NULL,
  acknowledged_at TIMESTAMPTZ,
  PRIMARY KEY (deployment_id, stage_id, instance_id)
);
CREATE INDEX idx_deployment_instance_stage_status
  ON deployment_instance(deployment_id, stage_id, status);

CREATE TABLE principals (
  principal_id UUID PRIMARY KEY,
  principal_type VARCHAR(30) NOT NULL,
  external_id VARCHAR(300) NOT NULL,
  name VARCHAR(300),
  CONSTRAINT uq_principal_type_external UNIQUE(principal_type, external_id)
);

CREATE TABLE roles (
  role_id UUID PRIMARY KEY,
  role_name VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500)
);

CREATE TABLE permissions (
  permission_id UUID PRIMARY KEY,
  permission_name VARCHAR(150) NOT NULL UNIQUE,
  description VARCHAR(500)
);

CREATE TABLE role_permissions (
  role_id UUID NOT NULL REFERENCES roles(role_id),
  permission_id UUID NOT NULL REFERENCES permissions(permission_id),
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE role_bindings (
  binding_id UUID PRIMARY KEY,
  principal_id UUID NOT NULL REFERENCES principals(principal_id),
  role_id UUID NOT NULL REFERENCES roles(role_id),
  organization_id UUID NOT NULL REFERENCES organizations(organization_id),
  project_id UUID REFERENCES projects(project_id),
  application_id UUID REFERENCES applications(application_id),
  CHECK (application_id IS NULL OR project_id IS NOT NULL)
);
CREATE INDEX idx_role_bindings_principal_org
  ON role_bindings(principal_id, organization_id);
CREATE INDEX idx_role_bindings_project_app
  ON role_bindings(project_id, application_id);
