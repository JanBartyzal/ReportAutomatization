package com.reportplatform.snow.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.reportplatform.snow.model.dto.ServiceNowTableDataDTO;
import com.reportplatform.snow.model.entity.AuthType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServiceNowClientTest {

    private WireMockServer wireMockServer;
    private ServiceNowClient serviceNowClient;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        baseUrl = "http://localhost:" + wireMockServer.port();
        serviceNowClient = new ServiceNowClient(WebClient.builder());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void authenticate_oauth2_shouldReturnAccessToken() {
        wireMockServer.stubFor(post(urlEqualTo("/oauth_token.do"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"test-token-12345\",\"token_type\":\"Bearer\",\"expires_in\":1800}")));

        String token = serviceNowClient.authenticate(baseUrl, AuthType.OAUTH2, "myClientId:myClientSecret");

        assertNotNull(token);
        assertEquals("test-token-12345", token);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/oauth_token.do"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded")));
    }

    @Test
    void authenticate_basic_shouldReturnBase64EncodedCredentials() {
        String token = serviceNowClient.authenticate(baseUrl, AuthType.BASIC, "admin:password123");

        assertNotNull(token);
        // Base64 of "admin:password123"
        assertEquals("YWRtaW46cGFzc3dvcmQxMjM=", token);
    }

    @Test
    void fetchTable_shouldReturnRecords() {
        String responseBody = "{\"result\":[" +
                "{\"sys_id\":\"abc123\",\"number\":\"INC001\",\"short_description\":\"Test incident\"}," +
                "{\"sys_id\":\"def456\",\"number\":\"INC002\",\"short_description\":\"Another incident\"}" +
                "]}";

        wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/incident"))
                .withQueryParam("sysparm_offset", equalTo("0"))
                .withQueryParam("sysparm_limit", equalTo("100"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        List<ServiceNowTableDataDTO> results = serviceNowClient.fetchTable(
                baseUrl, "incident", "test-bearer-token", 0, 100, null);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("abc123", results.get(0).getSysId());
        assertEquals("def456", results.get(1).getSysId());
    }

    @Test
    void fetchTable_withPagination_shouldFetchMultiplePages() {
        // First page - full page of 2 records
        String page1Body = "{\"result\":[" +
                "{\"sys_id\":\"rec1\",\"number\":\"INC001\"}," +
                "{\"sys_id\":\"rec2\",\"number\":\"INC002\"}" +
                "]}";

        // Second page - partial page (last page)
        String page2Body = "{\"result\":[" +
                "{\"sys_id\":\"rec3\",\"number\":\"INC003\"}" +
                "]}";

        wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/incident"))
                .withQueryParam("sysparm_offset", equalTo("0"))
                .withQueryParam("sysparm_limit", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(page1Body)));

        wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/incident"))
                .withQueryParam("sysparm_offset", equalTo("2"))
                .withQueryParam("sysparm_limit", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(page2Body)));

        // Fetch first page
        List<ServiceNowTableDataDTO> page1 = serviceNowClient.fetchTable(
                baseUrl, "incident", "test-token", 0, 2, null);
        assertEquals(2, page1.size());

        // Fetch second page
        List<ServiceNowTableDataDTO> page2 = serviceNowClient.fetchTable(
                baseUrl, "incident", "test-token", 2, 2, null);
        assertEquals(1, page2.size());
        assertEquals("rec3", page2.get(0).getSysId());
    }

    @Test
    void fetchTable_on429_shouldRetryAndSucceed() {
        // First call returns 429 (rate limited)
        wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/incident"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}"))
                .willSetStateTo("retried-once"));

        // Second call succeeds
        wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/incident"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("retried-once")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\":[{\"sys_id\":\"abc123\",\"number\":\"INC001\"}]}")));

        List<ServiceNowTableDataDTO> results = serviceNowClient.fetchTable(
                baseUrl, "incident", "test-token", 0, 100, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("abc123", results.get(0).getSysId());

        // Verify two requests were made (initial + 1 retry)
        wireMockServer.verify(2, getRequestedFor(urlPathEqualTo("/api/now/table/incident")));
    }

    @Test
    void testConnection_shouldReturnInstanceInfo() {
        String responseBody = "{\"result\":[{\"name\":\"glide.buildtag\",\"value\":\"glide-utah-07-08-2023\"}]}";

        wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_properties"))
                .withQueryParam("sysparm_query", equalTo("name=glide.buildtag"))
                .withQueryParam("sysparm_limit", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        Map<String, Object> result = serviceNowClient.testConnection(baseUrl, "test-bearer-token");

        assertNotNull(result);
        assertTrue(result.containsKey("result"));

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/now/table/sys_properties"))
                .withHeader("Authorization", equalTo("Bearer test-bearer-token")));
    }
}
