package tz.go.moh.ucs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tz.go.moh.ucs.service.HfrService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.*;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HfrServiceTest contains tests for the HfrService static methods.
 * It simulates network responses by using a dummy URLStreamHandlerFactory.
 */
public class HfrServiceTest {

    @BeforeAll
    public static void setupURLStreamHandlerFactory() {
        try {
            // Reset any previously set URLStreamHandlerFactory using reflection
            Field factoryField = URL.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            factoryField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset URLStreamHandlerFactory", e);
        }

        // Set our dummy factory to intercept HTTP requests
        URL.setURLStreamHandlerFactory(new DummyURLStreamHandlerFactory());
    }

    /**
     * Test that fetchHealthFacilityData successfully processes a 200 OK response.
     */
    @Test
    public void testFetchHealthFacilityDataSuccess() {
        try {
            // This should complete without throwing an exception.
            HfrService.fetchHealthFacilityData(1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not be thrown for a successful fetchHealthFacilityData");
        }
    }

    /**
     * Test that fetchHealthFacilityData throws an exception for non-200 responses.
     */
    @Test
    public void testFetchHealthFacilityDataFailure() {
        // Requesting page 999 will simulate a 404 error.
        Exception exception = assertThrows(Exception.class, () -> {
            HfrService.fetchHealthFacilityData(999);
        });

        // Validate the exception message
        assertEquals("Failed to fetch data. HTTP response code: 404", exception.getMessage());
    }

    /**
     * Test that fetchAdminHierarchData successfully processes a 200 OK response.
     */
    @Test
    public void testFetchAdminHierarchyDataSuccess() {
        try {
            HfrService.fetchAdminHierarchData(1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not be thrown for a successful fetchAdminHierarchData");
        }
    }

    /**
     * Test that fetchAdminHierarchData throws an exception for non-200 responses.
     */
    @Test
    public void testFetchAdminHierarchyDataFailure() {
        // Requesting page 999 will simulate a 500 error.
        Exception exception = assertThrows(Exception.class, () -> {
            HfrService.fetchAdminHierarchData(999);
        });

        // Validate the exception message
        assertEquals("Failed to fetch data. HTTP response code: 500", exception.getMessage());
    }

    // DummyHttpURLConnection class to simulate HTTP responses
    public static class DummyHttpURLConnection extends HttpURLConnection {
        private final int responseCode;
        private final String responseData;

        protected DummyHttpURLConnection(URL u, int responseCode, String responseData) {
            super(u);
            this.responseCode = responseCode;
            this.responseData = responseData;
        }

        @Override
        public void disconnect() {
            //NOT required
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
            //NOT required
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(responseData.getBytes());
        }

        @Override
        public int getResponseCode() throws IOException {
            return responseCode;
        }
    }

    // Dummy URLStreamHandlerFactory to intercept connections for testing
    public static class DummyURLStreamHandlerFactory implements URLStreamHandlerFactory {
        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("https".equals(protocol)) {
                return new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        // For fetchHealthFacilityData URLs, simulate responses.
                        if (u.toString().contains("health-facility-list")) {
                            // Simulate failure when the URL contains "page=999"
                            if (u.toString().contains("page=999")) {
                                return new DummyHttpURLConnection(u, 404, "Not Found");
                            } else {
                                // Simulate a 200 OK response with minimal JSON content
                                String json = "{\"metaData\":{\"pageCount\":1,\"currentPage\":1},\"data\":[]}";
                                return new DummyHttpURLConnection(u, 200, json);
                            }
                        }
                        // For fetchAdminHierarchData URLs, simulate responses.
                        else if (u.toString().contains("administrative-hierarchy")) {
                            if (u.toString().contains("page=999")) {
                                return new DummyHttpURLConnection(u, 500, "Internal Server Error");
                            } else {
                                String json = "{\"metaData\":{\"pageCount\":1,\"currentPage\":1},\"data\":[]}";
                                return new DummyHttpURLConnection(u, 200, json);
                            }
                        }
                        throw new IOException("Unexpected URL: " + u);
                    }
                };
            }
            return null; // Use default for other protocols
        }
    }
}