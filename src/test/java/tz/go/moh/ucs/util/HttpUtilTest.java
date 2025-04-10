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
    void testRemoveEndingSlashRemovesSlash() {
        String result = HttpUtil.removeEndingSlash("http://example.com/");
        assertEquals("http://example.com", result);
    }

    @Test
    void testRemoveEndingSlashNoSlash() {
        String result = HttpUtil.removeEndingSlash("http://example.com");
        assertEquals("http://example.com", result);
    }

    @Test
    void testRemoveEndingSlashWithEmptyString() {
        assertEquals("", HttpUtil.removeEndingSlash(""));
    }

    @Test
    void testRemoveTrailingSlashRemovesLeadingSlash() {
        String result = HttpUtil.removeTrailingSlash("/path");
        assertEquals("path", result);
    }

    @Test
    void testRemoveTrailingSlashNoLeadingSlash() {
        String result = HttpUtil.removeTrailingSlash("path");
        assertEquals("path", result);
    }

    @Test
    void testRemoveTrailingSlashWithEmptyString() {
        assertEquals("", HttpUtil.removeTrailingSlash(""));
    }

    @Test
    void testCheckSuccessBasedOnHttpCode() {
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(404));
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(500));
        assertTrue(HttpUtil.checkSuccessBasedOnHttpCode(200));
    }

    @Test
    void testCheckSuccessBasedOnHttpCodeEdgeCases() {
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(400));
        assertTrue(HttpUtil.checkSuccessBasedOnHttpCode(399));
    }

    @Test
    void testMakeConnectionGetNoAuth() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.GET, HttpUtil.AuthType.NONE, "");
        assertNotNull(request);
        assertTrue(request.getURI().toString().contains("http://example.com/api"));
    }

    @Test
    void testMakeConnectionWithBasicAuth() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", "q=test", HttpMethod.GET, HttpUtil.AuthType.BASIC, "user:pass");
        assertNotNull(request.getFirstHeader("Authorization"));
        assertTrue(request.getFirstHeader("Authorization").getValue().startsWith("Basic"));
    }

    @Test
    void testMakeConnectionWithTokenAuth() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.GET, HttpUtil.AuthType.TOKEN, "sometoken");
        assertNotNull(request.getFirstHeader("Authorization"));
        assertTrue(request.getFirstHeader("Authorization").getValue().startsWith("Token"));
    }

    @Test
    void testMakeConnectionPOST() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.POST, HttpUtil.AuthType.NONE, "");
        assertEquals("POST", request.getMethod());
    }

    @Test
    void testMakeConnectionPUT() throws URISyntaxException {
        HttpRequestBase request = HttpUtil.makeConnection("http://example.com/api", null, HttpMethod.PUT, HttpUtil.AuthType.NONE, "");
        assertEquals("PUT", request.getMethod());
    }

    @Test
    void testMakeConnectionDELETE() throws URISyntaxException {
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
    void testGetURLWithValidResponseShouldReturnResponseBody() throws IOException {
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
    void testGetURLWithEmptyResponseShouldReturnNull() throws IOException{
        mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .setResponseCode(200));

        String baseUrl = mockWebServer.url("/empty").toString();
        String response = HttpUtil.getURL(baseUrl, "user", "pass");

        assertNull(response);
    }

    @Test
    void testGetURLWithErrorResponseShouldReturnErrorBody() throws IOException {
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
    void testPostWithoutAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.post("http://invalid-url", null, "{\"key\":\"value\"}")
        );
    }

    @Test
    void testPostWithBasicAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.post("http://invalid-url", null, "{\"key\":\"value\"}", "application/json", HttpUtil.AuthType.BASIC, "user:pass")
        );
    }

    @Test
    void testGetWithoutAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.get("http://invalid-url", null)
        );
    }

    @Test
    void testGetWithToken() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.getWithToken("http://invalid-url", null, "token")
        );
    }

    @Test
    void testDeleteWithTokenAuth() {
        assertThrows(RuntimeException.class, () ->
            HttpUtil.delete("http://invalid-url", null, HttpUtil.AuthType.TOKEN, "token")
        );
    }
}