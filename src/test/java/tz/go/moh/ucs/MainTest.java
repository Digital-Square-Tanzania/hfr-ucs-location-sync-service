package tz.go.moh.ucs;


import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.service.HfrService;
import tz.go.moh.ucs.util.FetchLocationsHelper;
import tz.go.moh.ucs.util.OpenMrsCallsUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MainTest {


    @Test
    void fetchHfrDataShouldCallHfrServiceStaticMethod() {
        try (MockedStatic<HfrService> mockedStatic = mockStatic(HfrService.class)) {
            // Stub the static method
            mockedStatic.when(() -> HfrService.fetchHealthFacilityData(1)).thenAnswer(invocation -> null);

            // Call the method under test
            Main.fetchHfrData(1);

            // Verify static method was called once with 1
            mockedStatic.verify(() -> HfrService.fetchHealthFacilityData(1), times(1));
        }
    }

    @Test
    void fetchHfrAdminHierarchyDataShouldCallStaticMethodOnce() {
        try (MockedStatic<HfrService> mockedStatic = mockStatic(HfrService.class)) {
            // Stub the static method
            mockedStatic.when(() -> HfrService.fetchAdminHierarchData(1)).thenAnswer(invocation -> null);

            // Run your method
            Main.fetchHfrAdminHierarchyData(1);

            // Verify it was called once
            mockedStatic.verify(() -> HfrService.fetchAdminHierarchData(1), times(1));
        }
    }

    @Test
    void processAdminHierarchyData() {
        try (MockedStatic<OpenMrsCallsUtils> mockedOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockedOpenMrs.when(() -> OpenMrsCallsUtils.updateLocationName(any(Location.class), anyString()))
                    .thenReturn(HttpURLConnection.HTTP_OK);

            mockedOpenMrs.when(() -> OpenMrsCallsUtils.updateLocationAttribute(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> null);
            mockedOpenMrs.when(() -> OpenMrsCallsUtils.addLocationAttribute(any(Location.class), anyString(), anyString()))
                    .thenAnswer(invocation -> null);
            mockedOpenMrs.when(() -> OpenMrsCallsUtils.updateChildLocationParent(any(Location.class), anyString()))
                    .thenAnswer(invocation -> null);

            JSONArray mockResponse = new JSONArray();
            JSONObject mockObject = new JSONObject();
            mockObject.put("region", "Region1");
            mockObject.put("region_code", "R1");
            mockObject.put("district", "District1");
            mockObject.put("district_code", "D1");
            mockObject.put("council", "Council1");
            mockObject.put("council_code", "C1");
            mockObject.put("ward", "Ward1");
            mockObject.put("ward_code", "W1");
            mockObject.put("village_mtaa", "Village1");
            mockObject.put("village_mtaa_code", "V1");
            mockResponse.put(mockObject);

            Main.processAdminHierarchyData(mockResponse);
        }

        assertNotNull(Main.locationCache);
        assertNotNull(Main.codeCache);
    }

    @Test
    void processHfrResponse() {
        JSONArray mockResponse = new JSONArray();
        JSONObject mockObject = new JSONObject();
        mockObject.put("Fac_IDNumber", "F1");
        mockObject.put("region", "Region1");
        mockObject.put("Region_Code", "R1");
        mockObject.put("district", "District1");
        mockObject.put("District_Code", "D1");
        mockObject.put("council", "Council1");
        mockObject.put("Council_Code", "C1");
        mockObject.put("ward", "Ward1");
        mockObject.put("ward_Code", "W1");
        mockObject.put("Name", "Facility1");
        mockResponse.put(mockObject);

        try (MockedStatic<OpenMrsCallsUtils> mockedOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockedOpenMrs.when(() -> OpenMrsCallsUtils.updateLocationName(any(Location.class), anyString()))
                    .thenReturn(HttpURLConnection.HTTP_OK);
            mockedOpenMrs.when(() -> OpenMrsCallsUtils.updateChildLocationParent(any(Location.class), anyString()))
                    .thenAnswer(invocation -> null);
            mockedOpenMrs.when(() -> OpenMrsCallsUtils.addLocationAttribute(any(Location.class), anyString(), anyString()))
                    .thenAnswer(invocation -> null);

            Main.processHfrResponse(mockResponse);
        }


        assertNotNull(Main.locationCache);
        assertNotNull(Main.codeCache);
    }

    @Test
    void createNewLocation() throws Exception {
        String name = "Test Location";
        String parentUuid = "parent-uuid";
        Set<String> tags = new HashSet<>(Collections.singletonList("TestTag"));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("attributeType1", "value1");

        try (MockedStatic<OpenMrsCallsUtils> mockedOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            HttpURLConnection dummyConnection = new HttpURLConnection(new URL("http://dummy-url")) {
                @Override
                public void disconnect() {
                    //Not Required
                }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public void connect() {
                    //Not Required
                }

                @Override
                public int getResponseCode() {
                    return HttpURLConnection.HTTP_CREATED;
                }

                @Override
                public InputStream getInputStream() {
                    String json = "{\"uuid\": \"dummy-uuid\", \"name\": \"" + name + "\"}";
                    return new ByteArrayInputStream(json.getBytes());
                }

                @Override
                public OutputStream getOutputStream() {
                    return new ByteArrayOutputStream();
                }
            };

            mockedOpenMrs.when(() -> OpenMrsCallsUtils.createConnection(anyString(), eq("POST")))
                    .thenReturn(dummyConnection);

            FetchLocationsHelper fetchLocationsHelper = new FetchLocationsHelper();
            Location location = Main.createNewLocation(name, parentUuid, tags, attributes);

            assertNotNull(location);
            assertEquals(name, location.getName());
            assertEquals(tags, location.getTags());
            assertEquals(attributes, location.getAttributes());
        }
    }
}
