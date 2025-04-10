package tz.go.moh.ucs.util;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilsTest {

    private MockWebServer mockWebServer;

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
        String response = HttpUtils.getURL(baseUrl, "user", "pass");

        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void testGetURL_withEmptyResponse_shouldReturnNull() throws IOException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .setResponseCode(200));

        String baseUrl = mockWebServer.url("/empty").toString();
        String response = HttpUtils.getURL(baseUrl, "user", "pass");

        assertNull(response);
    }

    @Test
    void testGetURL_withErrorResponse_shouldReturnErrorBody() throws IOException {
        String errorResponse = "{\"error\":\"something went wrong\"}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(errorResponse)
                .setResponseCode(500));

        String baseUrl = mockWebServer.url("/error").toString();
        String response = HttpUtils.getURL(baseUrl, "user", "pass");

        assertNotNull(response);
        assertEquals(errorResponse, response);
    }
}