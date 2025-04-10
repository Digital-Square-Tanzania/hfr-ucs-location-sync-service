package tz.go.moh.ucs.util;

import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;
import tz.go.moh.ucs.domain.HttpMethod;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilTest {

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
    void testCheckSuccessBasedOnHttpCode() {
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(404));
        assertFalse(HttpUtil.checkSuccessBasedOnHttpCode(500));
        assertTrue(HttpUtil.checkSuccessBasedOnHttpCode(200));
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
}