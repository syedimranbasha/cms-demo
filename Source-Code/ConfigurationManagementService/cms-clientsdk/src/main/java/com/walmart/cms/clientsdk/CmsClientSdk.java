package com.walmart.cms.clientsdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.clientsdk.config.CmsClientSdkConfig;
import com.walmart.cms.clientsdk.model.ConfigurationSnapshot;
import com.walmart.cms.clientsdk.model.OrganizationResponse;
import com.walmart.cms.clientsdk.model.ProjectResponse;
import com.walmart.cms.clientsdk.model.ApplicationResponse;
import com.walmart.cms.clientsdk.model.CreateConfigurationVersionRequest;
import com.walmart.cms.clientsdk.model.CreateConfigurationVersionResponse;
import com.walmart.cms.clientsdk.model.CreateDeploymentRequest;
import com.walmart.cms.clientsdk.model.InitiateStageRequest;
import com.walmart.cms.clientsdk.model.RollbackRequest;
import com.walmart.cms.clientsdk.model.DeploymentResponse;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authenticated CMS SDK. start() retrieves immediately and then polls at a fixed
 * delay (two minutes by default). Failed reads preserve the last-known-good value.
 */
public final class CmsClientSdk implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(CmsClientSdk.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

    private final CmsClientSdkConfig config;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<ConfigurationSnapshot> latest = new AtomicReference<>();
    private final Object tokenLock = new Object();

    private volatile CmsConfigurationCallback callback;
    private volatile AccessToken cachedToken;
    private volatile ScheduledFuture<?> pollingTask;
    private volatile boolean started;

    public CmsClientSdk(CmsClientSdkConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    // Package-private constructor for tests.
    CmsClientSdk(CmsClientSdkConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cms-client-sdk-poller");
            t.setDaemon(true);
            return t;
        });
    }

    /** Register before start(); a subsequent registration replaces the previous callback. */
    public void registerCallback(CmsConfigurationCallback callback) {
        this.callback = callback;
    }

    /** Performs the initial retrieval and schedules polling. Safe to call once. */
    public synchronized void start() {
        if (started) return;
        started = true;
        pollSafely();
        pollingTask = scheduler.scheduleWithFixedDelay(this::pollSafely,
                config.pollingInterval().toMillis(),
                config.pollingInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Trigger an immediate read. Returns true only if a fresh snapshot was installed. */
    public boolean refreshNow() {
        try {
            ConfigurationSnapshot snapshot = retrieveConfiguration();
            latest.set(snapshot);
            invokeCallback(snapshot);
            return true;
        } catch (Exception e) {
            log.warn("CMS configuration refresh failed; retaining last-known-good configuration. organizationId={}, applicationId={}, instanceId={}",
                    config.organizationId(), config.applicationId(), config.instanceId(), e);
            return false;
        }
    }

    public Optional<ConfigurationSnapshot> getConfiguration() {
        return Optional.ofNullable(latest.get());
    }

    public Optional<JsonNode> getEffectiveConfiguration() {
        return getConfiguration().map(ConfigurationSnapshot::effectiveConfig);
    }

    /** Creates an organization (tenant) through POST /api/v1/organizations. */
    public OrganizationResponse createOrganization(String name) {
        requireName(name, "organization name");
        return post("/organizations", Map.of("name", name), OrganizationResponse.class);
    }

    /** Creates a project under an organization. */
    public ProjectResponse createProject(String organizationId, String name) {
        requirePathId(organizationId, "organizationId");
        requireName(name, "project name");
        return post("/organizations/" + encodePath(organizationId) + "/projects",
                Map.of("name", name), ProjectResponse.class);
    }

    /** Creates an application under a project in the specified organization. */
    public ApplicationResponse createApplication(String organizationId, String projectId, String name) {
        requirePathId(organizationId, "organizationId");
        requirePathId(projectId, "projectId");
        requireName(name, "application name");
        return post("/organizations/" + encodePath(organizationId) + "/projects/"
                        + encodePath(projectId) + "/applications",
                Map.of("name", name), ApplicationResponse.class);
    }

    /** Fetches an organization by ID. */
    public OrganizationResponse fetchOrganization(String organizationId) {
        requirePathId(organizationId, "organizationId");
        return get("/organizations/" + encodePath(organizationId), OrganizationResponse.class);
    }

    /** Fetches a project by organization and project IDs. */
    public ProjectResponse fetchProject(String organizationId, String projectId) {
        requirePathId(organizationId, "organizationId");
        requirePathId(projectId, "projectId");
        return get("/organizations/" + encodePath(organizationId) + "/projects/" + encodePath(projectId),
                ProjectResponse.class);
    }

    /** Fetches an application by organization, project, and application IDs. */
    public ApplicationResponse fetchApplication(String organizationId, String projectId, String applicationId) {
        requirePathId(organizationId, "organizationId");
        requirePathId(projectId, "projectId");
        requirePathId(applicationId, "applicationId");
        return get("/organizations/" + encodePath(organizationId) + "/projects/" + encodePath(projectId)
                + "/applications/" + encodePath(applicationId), ApplicationResponse.class);
    }

    /** Creates a deployment and its rollout stages. Creation alone does not start rollout. */
    public DeploymentResponse createDeployment(String organizationId, String applicationId,
                                               CreateDeploymentRequest request) {
        requirePathId(organizationId, "organizationId");
        requirePathId(applicationId, "applicationId");
        if (request == null || request.stages() == null || request.stages().isEmpty())
            throw new IllegalArgumentException("At least one deployment stage is required");
        return post("/organizations/" + encodePath(organizationId) + "/applications/"
                + encodePath(applicationId) + "/deployments", request, DeploymentResponse.class);
    }

    /** Starts a deployment stage. The API derives the actor from the authenticated principal. */
    public DeploymentResponse startDeploymentStage(String organizationId, String deploymentId, String stageId,
                                                   String approvalComment) {
        requirePathId(organizationId, "organizationId");
        requirePathId(deploymentId, "deploymentId");
        requirePathId(stageId, "stageId");
        return post("/organizations/" + encodePath(organizationId) + "/deployments/"
                + encodePath(deploymentId) + "/stages/" + encodePath(stageId) + "/start",
                new InitiateStageRequest(null, approvalComment), DeploymentResponse.class);
    }

    /** Creates a new rollback deployment targeting a previously deployed version set. */
    public DeploymentResponse rollback(String organizationId, String applicationId, String currentDeploymentId,
                                        String targetDeploymentId, String changeSummary) {
        requirePathId(organizationId, "organizationId");
        requirePathId(applicationId, "applicationId");
        requirePathId(currentDeploymentId, "deploymentId");
        requirePathId(targetDeploymentId, "targetDeploymentId");
        requireName(changeSummary, "changeSummary");
        return post("/organizations/" + encodePath(organizationId) + "/applications/"
                + encodePath(applicationId) + "/deployments/" + encodePath(currentDeploymentId) + "/rollback",
                new RollbackRequest(UUID.fromString(targetDeploymentId), changeSummary), DeploymentResponse.class);
    }

    /** Fetches deployment state and rollout progress. */
    public DeploymentResponse fetchDeployment(String organizationId, String deploymentId) {
        requirePathId(organizationId, "organizationId");
        requirePathId(deploymentId, "deploymentId");
        return get("/organizations/" + encodePath(organizationId) + "/deployments/"
                + encodePath(deploymentId), DeploymentResponse.class);
    }

    /** Creates an immutable GLOBAL configuration version for an organization. */
    public CreateConfigurationVersionResponse createGlobalConfigurationVersion(
            String organizationId, Map<String, Object> configDocument,
            String changeSummary, UUID expectedBaseVersionId) {
        requirePathId(organizationId, "organizationId");
        return createConfigurationVersion("/organizations/" + encodePath(organizationId)
                        + "/config/versions", configDocument, changeSummary, expectedBaseVersionId);
    }

    /** Creates an immutable ENVIRONMENT configuration version. */
    public CreateConfigurationVersionResponse createEnvironmentConfigurationVersion(
            String organizationId, String environment, Map<String, Object> configDocument,
            String changeSummary, UUID expectedBaseVersionId) {
        requirePathId(organizationId, "organizationId");
        requireName(environment, "environment");
        return createConfigurationVersion("/organizations/" + encodePath(organizationId)
                        + "/environments/" + encodePath(environment) + "/config/versions",
                configDocument, changeSummary, expectedBaseVersionId);
    }

    /** Creates an immutable APPLICATION configuration version for an application/environment pair. */
    public CreateConfigurationVersionResponse createApplicationConfigurationVersion(
            String organizationId, String applicationId, String environment,
            Map<String, Object> configDocument, String changeSummary, UUID expectedBaseVersionId) {
        requirePathId(organizationId, "organizationId");
        requirePathId(applicationId, "applicationId");
        requireName(environment, "environment");
        return createConfigurationVersion("/organizations/" + encodePath(organizationId)
                        + "/applications/" + encodePath(applicationId) + "/config/versions?environment="
                        + encodeQuery(environment), configDocument, changeSummary, expectedBaseVersionId);
    }

    /** Creates an immutable REGION configuration version for an application/environment/region tuple. */
    public CreateConfigurationVersionResponse createRegionConfigurationVersion(
            String organizationId, String applicationId, String environment, String region,
            Map<String, Object> configDocument, String changeSummary, UUID expectedBaseVersionId) {
        requirePathId(organizationId, "organizationId");
        requirePathId(applicationId, "applicationId");
        requireName(environment, "environment");
        requireName(region, "region");
        return createConfigurationVersion("/organizations/" + encodePath(organizationId)
                        + "/applications/" + encodePath(applicationId) + "/environments/"
                        + encodePath(environment) + "/regions/" + encodePath(region) + "/config/versions",
                configDocument, changeSummary, expectedBaseVersionId);
    }

    private CreateConfigurationVersionResponse createConfigurationVersion(
            String path, Map<String, Object> configDocument, String changeSummary, UUID expectedBaseVersionId) {
        if (configDocument == null) throw new IllegalArgumentException("configDocument must not be null");
        if (changeSummary == null || changeSummary.isBlank())
            throw new IllegalArgumentException("changeSummary must not be blank");
        return post(path, new CreateConfigurationVersionRequest(configDocument, changeSummary, expectedBaseVersionId),
                CreateConfigurationVersionResponse.class);
    }

    private <T> T get(String path, Class<T> responseType) {
        try {
            AccessToken token = getAccessToken();
            HttpRequest request = HttpRequest.newBuilder(apiUri(path))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + token.value())
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                invalidateToken();
                throw new CmsClientSdkException("CMS rejected the access token (HTTP 401)");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CmsClientSdkException("CMS API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            return MAPPER.readValue(response.body(), responseType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CmsClientSdkException("Interrupted while calling CMS API", e);
        } catch (IOException e) {
            throw new CmsClientSdkException("Unable to call CMS API", e);
        }
    }

    private <T> T post(String path, Object requestBody, Class<T> responseType) {
        try {
            AccessToken token = getAccessToken();
            HttpRequest request = HttpRequest.newBuilder(apiUri(path))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + token.value())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                invalidateToken();
                throw new CmsClientSdkException("CMS rejected the access token (HTTP 401)");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CmsClientSdkException("CMS management API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            return MAPPER.readValue(response.body(), responseType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CmsClientSdkException("Interrupted while calling CMS management API", e);
        } catch (IOException e) {
            throw new CmsClientSdkException("Unable to call CMS management API", e);
        }
    }

    private URI apiUri(String path) {
        String base = config.apiBaseUrl().toString().replaceAll("/+$", "");
        return URI.create(base + path);
    }

    private static void requireName(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        if (value.length() > 200) throw new IllegalArgumentException(field + " must not exceed 200 characters");
    }

    private static void requirePathId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }

    /** Reads a JSON Pointer, for example /database/connection_pool_size. */
    public Optional<JsonNode> getValue(String jsonPointer) {
        return getEffectiveConfiguration().map(n -> n.at(jsonPointer))
                .filter(n -> !n.isMissingNode());
    }

    private void pollSafely() {
        refreshNow();
    }

    private ConfigurationSnapshot retrieveConfiguration() throws IOException, InterruptedException {
        AccessToken token = getAccessToken();
        HttpRequest request = HttpRequest.newBuilder(buildConfigurationUri())
                .timeout(config.requestTimeout())
                .header("Authorization", "Bearer " + token.value())
                .header("X-Instance-Id", config.instanceId())
                .header("Accept", "application/json")
                .GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            invalidateToken();
            throw new IOException("CMS rejected the access token (HTTP 401)");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Effective configuration API returned HTTP " + response.statusCode());
        }

        JsonNode root = MAPPER.readTree(response.body());
        // Spring/Jackson serializes the EffectiveConfigurationResponse record using
        // camelCase (effectiveConfig). Accept snake_case as a compatibility alias.
        JsonNode effective = root.has("effectiveConfig")
                ? root.get("effectiveConfig") : root.get("effective_config");
        if (effective == null || !effective.isObject()) {
            throw new IOException("Response is missing an object-valued effectiveConfig field");
        }
        Map<String, String> versions = Map.of();
        JsonNode versionsNode = root.get("versions");
        if (versionsNode != null && versionsNode.isObject()) {
            versions = MAPPER.convertValue(versionsNode, STRING_MAP);
        }
        return new ConfigurationSnapshot(effective, versions, Instant.now());
    }

    private URI buildConfigurationUri() {
        String base = config.apiBaseUrl().toString().replaceAll("/+$", "");
        return URI.create(base + "/organizations/" + encodePath(config.organizationId())
                + "/applications/" + encodePath(config.applicationId())
                + "/config?environment=" + encodeQuery(config.environment())
                + "&region=" + encodeQuery(config.region()));
    }

    private AccessToken getAccessToken() throws IOException, InterruptedException {
        AccessToken token = cachedToken;
        if (token != null && token.usable()) return token;

        synchronized (tokenLock) {
            token = cachedToken;
            if (token != null && token.usable()) return token;

            String form = "grant_type=client_credentials"
                    + "&client_id=" + encodeQuery(config.clientId())
                    + "&client_secret=" + encodeQuery(config.clientSecret());
            HttpRequest request = HttpRequest.newBuilder(config.tokenUri())
                    .timeout(config.requestTimeout())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("OAuth token endpoint returned HTTP " + response.statusCode());
            }
            JsonNode body = MAPPER.readTree(response.body());
            String value = body.path("access_token").asText(null);
            long expiresIn = body.path("expires_in").asLong(300);
            if (value == null || value.isBlank()) throw new IOException("OAuth response did not contain access_token");

            long skew = Math.min(30, Math.max(0, expiresIn / 10));
            cachedToken = new AccessToken(value, Instant.now().plusSeconds(Math.max(1, expiresIn - skew)));
            return cachedToken;
        }
    }

    private void invalidateToken() {
        synchronized (tokenLock) { cachedToken = null; }
    }

    private void invokeCallback(ConfigurationSnapshot snapshot) {
        CmsConfigurationCallback registered = callback;
        if (registered == null) return;

        log.info("Invoking CMS configuration callback. organizationId={}, applicationId={}, instanceId={}, fetchedAt={}",
                config.organizationId(), config.applicationId(), config.instanceId(), snapshot.fetchedAt());
        try {
            registered.onConfigurationUpdated(snapshot);
        } catch (RuntimeException e) {
            log.error("CMS configuration callback failed. organizationId={}, applicationId={}, instanceId={}",
                    config.organizationId(), config.applicationId(), config.instanceId(), e);
        }
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    private static String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    @Override
    public synchronized void close() {
        if (pollingTask != null) pollingTask.cancel(true);
        scheduler.shutdownNow();
        started = false;
    }

    private record AccessToken(String value, Instant expiresAt) {
        boolean usable() { return Instant.now().isBefore(expiresAt); }
    }
}
