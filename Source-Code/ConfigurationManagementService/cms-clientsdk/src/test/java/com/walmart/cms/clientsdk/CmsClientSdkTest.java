package com.walmart.cms.clientsdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.walmart.cms.clientsdk.config.CmsClientSdkConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CmsClientSdkTest {
    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final AtomicInteger configRequests = new AtomicInteger();
    private volatile int configStatus = 200;
    private volatile int managementStatus = 200;
    private final List<String> managementRequests = new CopyOnWriteArrayList<>();
    private final List<String> versionRequests = new CopyOnWriteArrayList<>();

    private static final String CONFIG_BODY = """
        {
          "effectiveConfig": {
            "database": {"connection_pool_size": 100, "timeout": 30},
            "logging": {"level": "DEBUG"}
          },
          "versions": {
            "global": "ver-001", "environment": "ver-002",
            "application": "ver-003", "region": "ver-004"
          }
        }
        """;

    @BeforeEach
    void setUp() throws IOException {
        tokenRequests.set(0);
        configRequests.set(0);
        configStatus = 200;
        managementStatus = 200;
        versionRequests.clear();
        managementRequests.clear();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", this::handleToken);
        server.createContext("/api/v1/organizations/org-1/applications/app-1/config", this::handleConfig);
        server.createContext("/api/v1/organizations", this::handleCreateOrganization);
        server.createContext("/api/v1/organizations/11111111-1111-1111-1111-111111111111/config/versions", this::handleCreateVersion);
        server.createContext("/api/v1/organizations/11111111-1111-1111-1111-111111111111/environments/PROD/config/versions", this::handleCreateVersion);
        server.createContext("/api/v1/organizations/11111111-1111-1111-1111-111111111111/applications/33333333-3333-3333-3333-333333333333/config/versions", this::handleCreateVersion);
        server.createContext("/api/v1/organizations/11111111-1111-1111-1111-111111111111/applications/33333333-3333-3333-3333-333333333333/environments/PROD/regions/us-central1/config/versions", this::handleCreateVersion);
        server.createContext("/api/v1/organizations/11111111-1111-1111-1111-111111111111", this::handleManagementAndDeployment);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void retrievesAuthenticatedConfigurationCachesItAndInvokesCallback() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            AtomicReference<Integer> callbackValue = new AtomicReference<>();
            sdk.registerCallback(snapshot -> callbackValue.set(
                    snapshot.effectiveConfig().at("/database/connection_pool_size").asInt()));

            assertTrue(sdk.refreshNow());
            assertEquals(100, sdk.getValue("/database/connection_pool_size").orElseThrow().asInt());
            assertEquals("ver-003", sdk.getConfiguration().orElseThrow().versions().get("application"));
            assertEquals(100, callbackValue.get());
            assertEquals(1, tokenRequests.get());
            assertEquals(1, configRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void failedRefreshPreservesLastKnownGoodConfiguration() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            assertTrue(sdk.refreshNow());
            JsonNode original = sdk.getEffectiveConfiguration().orElseThrow();
            configStatus = 503;
            assertFalse(sdk.refreshNow());
            assertEquals(original, sdk.getEffectiveConfiguration().orElseThrow());
            assertEquals(2, configRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void startPerformsInitialRetrievalAndInvokesCallback() throws Exception {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        CountDownLatch called = new CountDownLatch(1);
        try {
            sdk.registerCallback(snapshot -> called.countDown());
            sdk.start();
            assertTrue(called.await(3, TimeUnit.SECONDS));
            assertTrue(sdk.getConfiguration().isPresent());
            assertEquals(1, configRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void callbackExceptionDoesNotBreakLaterRefreshes() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            sdk.registerCallback(snapshot -> { throw new IllegalStateException("test callback failure"); });
            assertTrue(sdk.refreshNow());
            assertTrue(sdk.refreshNow());
            assertEquals(2, configRequests.get());
            assertTrue(sdk.getConfiguration().isPresent());
        } finally {
            sdk.close();
        }
    }

    @Test
    void rejectsBlankClientSecret() {
        assertThrows(IllegalArgumentException.class, () -> new CmsClientSdkConfig(
                URI.create("http://localhost/api/v1"), URI.create("http://localhost/token"),
                "client", " ", "org", "app", "PROD", "us-central1", "instance",
                Duration.ofMinutes(2), Duration.ofSeconds(5)));
    }


    @Test
    void createsOrganizationProjectAndApplicationUsingAuthenticatedManagementApis() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            var organization = sdk.createOrganization("Acme");
            assertEquals("Acme", organization.name());
            assertEquals("11111111-1111-1111-1111-111111111111", organization.organizationId().toString());

            var project = sdk.createProject(organization.organizationId().toString(), "Payments");
            assertEquals("Payments", project.name());
            assertEquals(organization.organizationId(), project.organizationId());

            var application = sdk.createApplication(organization.organizationId().toString(),
                    project.projectId().toString(), "Checkout");
            assertEquals("Checkout", application.name());
            assertEquals(project.projectId(), application.projectId());
            assertEquals(1, tokenRequests.get()); // access token is reused across SDK calls
        } finally {
            sdk.close();
        }
    }

    @Test
    void createsConfigurationVersionsAtAllFourScopes() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            Map<String, Object> document = Map.of("feature", Map.of("enabled", true));
            var global = sdk.createGlobalConfigurationVersion(
                    "11111111-1111-1111-1111-111111111111", document, "global defaults", null);
            var environment = sdk.createEnvironmentConfigurationVersion(
                    "11111111-1111-1111-1111-111111111111", "PROD", document, "prod override", null);
            var application = sdk.createApplicationConfigurationVersion(
                    "11111111-1111-1111-1111-111111111111",
                    "33333333-3333-3333-3333-333333333333", "PROD", document, "app override", null);
            var region = sdk.createRegionConfigurationVersion(
                    "11111111-1111-1111-1111-111111111111",
                    "33333333-3333-3333-3333-333333333333", "PROD", "us-central1",
                    document, "regional override", null);

            assertEquals(1, global.versionNumber());
            assertEquals(1, environment.versionNumber());
            assertEquals(1, application.versionNumber());
            assertEquals(1, region.versionNumber());
            assertEquals(4, versionRequests.size());
            assertTrue(versionRequests.get(0).contains("/config/versions"));
            assertTrue(versionRequests.get(1).contains("/environments/PROD/config/versions"));
            assertTrue(versionRequests.get(2).contains("/applications/33333333-3333-3333-3333-333333333333/config/versions?environment=PROD"));
            assertTrue(versionRequests.get(3).contains("/environments/PROD/regions/us-central1/config/versions"));
            assertEquals(1, tokenRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void rejectsNullConfigurationDocumentBeforeMakingHttpRequest() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            assertThrows(IllegalArgumentException.class, () -> sdk.createGlobalConfigurationVersion(
                    "11111111-1111-1111-1111-111111111111", null, "summary", null));
            assertEquals(0, tokenRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void rejectsBlankOrganizationNameBeforeMakingHttpRequest() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            assertThrows(IllegalArgumentException.class, () -> sdk.createOrganization(" "));
            assertEquals(0, tokenRequests.get());
        } finally {
            sdk.close();
        }
    }


    @Test
    void fetchesOrganizationProjectAndApplicationUsingAuthenticatedGetApis() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            var organization = sdk.fetchOrganization("11111111-1111-1111-1111-111111111111");
            var project = sdk.fetchProject("11111111-1111-1111-1111-111111111111",
                    "22222222-2222-2222-2222-222222222222");
            var application = sdk.fetchApplication("11111111-1111-1111-1111-111111111111",
                    "22222222-2222-2222-2222-222222222222",
                    "33333333-3333-3333-3333-333333333333");

            assertEquals("Acme", organization.name());
            assertEquals("Payments", project.name());
            assertEquals("Checkout", application.name());
            assertTrue(managementRequests.contains("GET /api/v1/organizations/11111111-1111-1111-1111-111111111111"));
            assertTrue(managementRequests.contains("GET /api/v1/organizations/11111111-1111-1111-1111-111111111111/projects/22222222-2222-2222-2222-222222222222"));
            assertTrue(managementRequests.contains("GET /api/v1/organizations/11111111-1111-1111-1111-111111111111/projects/22222222-2222-2222-2222-222222222222/applications/33333333-3333-3333-3333-333333333333"));
            assertEquals(1, tokenRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void createsDeploymentWithStagesAndDeserializesStageIds() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            var request = new com.walmart.cms.clientsdk.model.CreateDeploymentRequest(
                    "PROD", "us-central1", List.of(
                    new com.walmart.cms.clientsdk.model.CreateDeploymentRequest.StageRequest(1, 10),
                    new com.walmart.cms.clientsdk.model.CreateDeploymentRequest.StageRequest(2, 50),
                    new com.walmart.cms.clientsdk.model.CreateDeploymentRequest.StageRequest(3, 100)));
            var response = sdk.createDeployment("11111111-1111-1111-1111-111111111111",
                    "33333333-3333-3333-3333-333333333333", request);

            assertEquals("PENDING", response.status());
            assertEquals(3, response.stages().size());
            assertEquals(UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    response.stages().get(0).stageId());
            assertTrue(managementRequests.stream().anyMatch(v -> v.startsWith("POST /api/v1/organizations/11111111-1111-1111-1111-111111111111/applications/33333333-3333-3333-3333-333333333333/deployments ")));
            assertEquals(1, tokenRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void startsDeploymentStageWithApprovalCommentAndNoClientSuppliedApprover() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            var response = sdk.startDeploymentStage("11111111-1111-1111-1111-111111111111",
                    "55555555-5555-5555-5555-555555555555",
                    "44444444-4444-4444-4444-444444444444", "Approved for 10 percent rollout");
            assertEquals("IN_PROGRESS", response.status());
            String request = managementRequests.stream().filter(v -> v.contains("/stages/")).findFirst().orElseThrow();
            assertTrue(request.contains("\"approvedBy\":null"));
            assertTrue(request.contains("\"approvalComment\":\"Approved for 10 percent rollout\""));
        } finally {
            sdk.close();
        }
    }

    @Test
    void rollsBackToTargetDeploymentAndFetchesDeploymentStatus() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            var rollback = sdk.rollback("11111111-1111-1111-1111-111111111111",
                    "33333333-3333-3333-3333-333333333333",
                    "55555555-5555-5555-5555-555555555555",
                    "66666666-6666-6666-6666-666666666666", "Rollback due to elevated errors");
            assertEquals("PENDING", rollback.status());
            String rollbackRequest = managementRequests.stream().filter(v -> v.contains("/rollback ")).findFirst().orElseThrow();
            assertTrue(rollbackRequest.contains("\"targetDeploymentId\":\"66666666-6666-6666-6666-666666666666\""));

            var deployment = sdk.fetchDeployment("11111111-1111-1111-1111-111111111111",
                    "55555555-5555-5555-5555-555555555555");
            assertEquals("IN_PROGRESS", deployment.status());
            assertTrue(managementRequests.stream().anyMatch(v -> v.equals("GET /api/v1/organizations/11111111-1111-1111-1111-111111111111/deployments/55555555-5555-5555-5555-555555555555")));
            assertEquals(1, tokenRequests.get());
        } finally {
            sdk.close();
        }
    }

    @Test
    void managementApiErrorsAreReportedAndInvalidInputsAreRejectedBeforeNetworkCalls() {
        CmsClientSdk sdk = new CmsClientSdk(config(Duration.ofHours(1)));
        try {
            assertThrows(IllegalArgumentException.class, () -> sdk.fetchOrganization(" "));
            assertThrows(IllegalArgumentException.class, () -> sdk.createDeployment(
                    "11111111-1111-1111-1111-111111111111", "33333333-3333-3333-3333-333333333333", null));
            assertEquals(0, tokenRequests.get());

            managementStatus = 404;
            var error = assertThrows(CmsClientSdkException.class, () -> sdk.fetchOrganization(
                    "11111111-1111-1111-1111-111111111111"));
            assertTrue(error.getMessage().contains("HTTP 404"));
        } finally {
            sdk.close();
        }
    }

    private CmsClientSdkConfig config(Duration interval) {
        return new CmsClientSdkConfig(URI.create(baseUrl + "/api/v1"), URI.create(baseUrl + "/token"),
                "cms-service-client", "test-only-secret", "org-1", "app-1",
                "PROD", "us-central1", "instance-101", interval, Duration.ofSeconds(3));
    }



    private void handleManagementAndDeployment(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        assertEquals("Bearer test-access-token", exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        managementRequests.add(method + " " + path + (body.isBlank() ? "" : " " + body));
        if (managementStatus != 200) {
            respond(exchange, managementStatus, "{\"error\":\"not found\"}");
            return;
        }
        if (method.equals("POST") && path.endsWith("/projects")) {
            assertTrue(body.contains("\"name\":\"Payments\""));
            respond(exchange, 201, "{\"projectId\":\"22222222-2222-2222-2222-222222222222\",\"organizationId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Payments\"}");
        } else if (method.equals("POST") && path.endsWith("/projects/22222222-2222-2222-2222-222222222222/applications")) {
            assertTrue(body.contains("\"name\":\"Checkout\""));
            respond(exchange, 201, "{\"applicationId\":\"33333333-3333-3333-3333-333333333333\",\"projectId\":\"22222222-2222-2222-2222-222222222222\",\"name\":\"Checkout\"}");
        } else if (method.equals("GET") && path.endsWith("/projects/22222222-2222-2222-2222-222222222222/applications/33333333-3333-3333-3333-333333333333")) {
            respond(exchange, 200, "{\"applicationId\":\"33333333-3333-3333-3333-333333333333\",\"projectId\":\"22222222-2222-2222-2222-222222222222\",\"name\":\"Checkout\"}");
        } else if (method.equals("GET") && path.endsWith("/projects/22222222-2222-2222-2222-222222222222")) {
            respond(exchange, 200, "{\"projectId\":\"22222222-2222-2222-2222-222222222222\",\"organizationId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Payments\"}");
        } else if (method.equals("GET") && path.equals("/api/v1/organizations/11111111-1111-1111-1111-111111111111")) {
            respond(exchange, 200, "{\"organizationId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Acme\"}");
        } else if (method.equals("POST") && path.endsWith("/deployments")) {
            assertTrue(body.contains("\"environmentName\":\"PROD\""));
            assertTrue(body.contains("\"targetPercentage\":100"));
            respond(exchange, 201, deploymentJson("PENDING"));
        } else if (method.equals("POST") && path.endsWith("/start")) {
            respond(exchange, 200, deploymentJson("IN_PROGRESS"));
        } else if (method.equals("POST") && path.endsWith("/rollback")) {
            respond(exchange, 201, deploymentJson("PENDING"));
        } else if (method.equals("GET") && path.endsWith("/deployments/55555555-5555-5555-5555-555555555555")) {
            respond(exchange, 200, deploymentJson("IN_PROGRESS"));
        } else {
            respond(exchange, 404, "{\"error\":\"unexpected route\"}");
        }
    }

    private static String deploymentJson(String status) {
        return "{\"deploymentId\":\"55555555-5555-5555-5555-555555555555\",\"rollbackOfDeploymentId\":null,\"status\":\"" + status + "\","
                + "\"baselineVersionSet\":{\"global\":\"G1\"},\"candidateVersionSet\":{\"global\":\"G2\"},"
                + "\"stages\":[{\"stageId\":\"44444444-4444-4444-4444-444444444444\",\"stageNumber\":1,\"targetPercentage\":10,\"status\":\"" + status + "\"},"
                + "{\"stageId\":\"77777777-7777-7777-7777-777777777777\",\"stageNumber\":2,\"targetPercentage\":50,\"status\":\"PENDING\"},"
                + "{\"stageId\":\"88888888-8888-8888-8888-888888888888\",\"stageNumber\":3,\"targetPercentage\":100,\"status\":\"PENDING\"}]}";
    }

    private void handleCreateVersion(HttpExchange exchange) throws IOException {
        assertEquals("POST", exchange.getRequestMethod());
        assertEquals("Bearer test-access-token", exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"configDocument\""));
        assertTrue(body.contains("\"changeSummary\""));
        versionRequests.add(exchange.getRequestURI().toString());
        respond(exchange, 201, "{\"scopeId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"versionId\":\"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\",\"versionNumber\":1,\"status\":\"CREATED\"}");
    }

    private void handleCreateOrganization(HttpExchange exchange) throws IOException {
        assertEquals("POST", exchange.getRequestMethod());
        assertEquals("Bearer test-access-token", exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"name\":\"Acme\""));
        respond(exchange, 201, "{\"organizationId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Acme\"}");
    }

    private void handleCreateProject(HttpExchange exchange) throws IOException {
        assertEquals("POST", exchange.getRequestMethod());
        assertEquals("Bearer test-access-token", exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"name\":\"Payments\""));
        respond(exchange, 201, "{\"projectId\":\"22222222-2222-2222-2222-222222222222\",\"organizationId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Payments\"}");
    }

    private void handleCreateApplication(HttpExchange exchange) throws IOException {
        assertEquals("POST", exchange.getRequestMethod());
        assertEquals("Bearer test-access-token", exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"name\":\"Checkout\""));
        respond(exchange, 201, "{\"applicationId\":\"33333333-3333-3333-3333-333333333333\",\"projectId\":\"22222222-2222-2222-2222-222222222222\",\"name\":\"Checkout\"}");
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        tokenRequests.incrementAndGet();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("grant_type=client_credentials"));
        assertTrue(body.contains("client_id=cms-service-client"));
        respond(exchange, 200, """
            {"access_token":"test-access-token","token_type":"Bearer","expires_in":300}
            """);
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        configRequests.incrementAndGet();
        assertEquals("Bearer test-access-token", exchange.getRequestHeaders().getFirst("Authorization"));
        assertEquals("instance-101", exchange.getRequestHeaders().getFirst("X-Instance-Id"));
        assertTrue(exchange.getRequestURI().getQuery().contains("environment=PROD"));
        assertTrue(exchange.getRequestURI().getQuery().contains("region=us-central1"));
        respond(exchange, configStatus, CONFIG_BODY);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
