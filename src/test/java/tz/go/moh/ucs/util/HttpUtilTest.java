package tz.go.moh.ucs.util;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.go.moh.ucs.domain.HttpMethod;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilTest {

    private MockWebServer mockWebServer;

    @Test
    void testRemoveEndingSlash_RemovesSlash() {
        String result = HttpUtil.removeEndingSlash("http://example.com/");
        assertEquals("http://example.com", result);
    }

    @Test
    void testRemoveEndingSlash_NoSlash() {
        String result = HttpUtil.removeEndingSlash("http://example.com");
        assertEquals("http://example.com", result);
    }

    @Test
    void testRemoveEndingSlash_withEmptyString() {
        assertEquals("", HttpUtil.removeEndingSlash(""));
    }

    @Test
    void testRemoveTrailingSlash_RemovesLeadingSlash() {
        String result = HttpUtil.removeTrailingSlash("/path");
        assertEquals("path", result);
    }

    @Test
    void testRemoveTrailingSlash_NoLeadingSlash() {
        String result = HttpUtil.removeTrailingSlash("path");
        assertEquals("path", result);
    }

    @Test
    void testRemoveTrailingSlash_withEmptyString() {
        assertEquals("", HttpUtil.removeTrailingSlash(""));
    }

    @Test
    void testCheckSuccessBasedOnHttpCode() {
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(404));
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(500));
        assertTrue(HttpUtil.checkSuccessBasedOnHttpCode(200));
    }

    @Test
    void testCheckSuccessBasedOnHttpCode_EdgeCases() {
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(400));
        assertTrue(HttpUtil.checkSuccessBasedOnHttpCode(399));
    }

    @Test
    void testMakeConnection_GET_NoAuth() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.GET, HttpUtil.AuthType.NONE, "");
        assertNotNull(request);
        assertTrue(request.getURI().toString().contains("http://example.com/api"));
    }

    @Test
    void testMakeConnection_WithBasicAuth() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", "q=test", HttpMethod.GET, HttpUtil.AuthType.BASIC, "user:pass");
        assertNotNull(request.getFirstHeader("Authorization"));
        assertTrue(request.getFirstHeader("Authorization").getValue().startsWith("Basic"));
    }

    @Test
    void testMakeConnection_WithTokenAuth() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.GET, HttpUtil.AuthType.TOKEN, "sometoken");
        assertNotNull(request.getFirstHeader("Authorization"));
        assertTrue(request.getFirstHeader("Authorization").getValue().startsWith("Token"));
    }

    @Test
    void testMakeConnection_POST() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.POST, HttpUtil.AuthType.NONE, "");
        assertEquals("POST", request.getMethod());
    }

    @Test
    void testMakeConnection_PUT() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.PUT, HttpUtil.AuthType.NONE, "");
        assertEquals("PUT", request.getMethod());
    }

    @Test
    void testMakeConnection_DELETE() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.DELETE, HttpUtil.AuthType.NONE, "");
        assertEquals("DELETE", request.getMethod());
    }

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetURL_withValidResponse_shouldReturnResponseBody() throws IOException {
        String expectedResponse = "{\"message\":\"success\"}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(expectedResponse)
                .setResponseCode(200));

        String baseUrl = mockWebServer.url("/test").toString();
        String response = HttpUtil.getURL(baseUrl, "user", "pass");

        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void testGetURL_withEmptyResponse_shouldReturnNull() throws IOException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .setResponseCode(200));

        String baseUrl = mockWebServer.url("/empty").toString();
        String response = HttpUtil.getURL(baseUrl, "user", "pass");

        assertNull(response);
    }

    @Test
    void testGetURL_withErrorResponse_shouldReturnErrorBody() throws IOException {
        String errorResponse = "{\"error\":\"something went wrong\"}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(errorResponse)
                .setResponseCode(500));

        String baseUrl = mockWebServer.url("/error").toString();
        String response = HttpUtil.getURL(baseUrl, "user", "pass");

        assertNotNull(response);
        assertEquals(errorResponse, response);
    }

    @Test
    void testPost_withoutAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.post("http://invalid-url", null, "{\"key\":\"value\"}")
        );
    }

    @Test
    void testPost_withBasicAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.post("http://invalid-url", null, "{\"key\":\"value\"}", "application/json", HttpUtil.AuthType.BASIC, "user:pass")
        );
    }

    @Test
    void testGet_withoutAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.get("http://invalid-url", null)
        );
    }

    @Test
    void testGet_withToken() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.getWithToken("http://invalid-url", null, "token")
        );
    }

    @Test
    void testDelete_withTokenAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.delete("http://invalid-url", null, HttpUtil.AuthType.TOKEN, "token")
        );
    }
}