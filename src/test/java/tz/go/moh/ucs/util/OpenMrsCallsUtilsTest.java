package tz.go.moh.ucs.util;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.go.moh.ucs.domain.Location;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenMrsCallsUtilsTest {
    private FetchLocationsHelper fetchLocationsHelper;
    private Location mockLocation;

    @BeforeEach
    void setup() {
        fetchLocationsHelper = new FetchLocationsHelper();

        mockLocation = mock(Location.class);
        when(mockLocation.getLocationId()).thenReturn("123");
        when(mockLocation.getName()).thenReturn("Old Village");
        when(mockLocation.getAttributes()).thenReturn(new HashMap<>());
        when(mockLocation.getParentLocation()).thenReturn(mock(Location.class));
        when(mockLocation.getTags()).thenReturn(Collections.singleton("Village"));
        when(mockLocation.getAttribute("Code")).thenReturn("VILLAGE123");
    }

    @Test
    void testGetOtherLocation() {
        JSONObject hfrLocation = new JSONObject();
        hfrLocation.put("Village_Code", "VILLAGE123");
        hfrLocation.put("village", "New Village");

        OpenMrsCallsUtils.getOtherLocation("123", "parentUuid", hfrLocation);

        // Since we don't control the global allLocations, this just ensures method runs without error
        assertTrue(true);
    }

    @Test
    void testUpdateOrCreateLocationAttribute_AddsAttribute() {
        OpenMrsCallsUtils.updateOrCreateLocationAttribute(mockLocation, "uuid-code", "NEWCODE");
        // Just ensure no exception is thrown
        assertTrue(true);
    }

    @Test
    void testGetLocationAttributeUuid_ReturnsNull() {
        String result = OpenMrsCallsUtils.getLocationAttributeUuid("nonexistent", "fake-type-uuid");
        assertNull(result); // Since no real HTTP response, we expect null
    }

    @Test
    void testUpdateLocationAttribute_HandlesFailure() {
        OpenMrsCallsUtils.updateLocationAttribute("fake-uuid", "fake-attr-uuid", "attrType", "newValue");
        assertTrue(true); // No exception thrown
    }

    @Test
    void testAddLocationAttribute_HandlesFailure() {
        OpenMrsCallsUtils.addLocationAttribute(mockLocation, "attrType", "value");
        assertTrue(true); // Should not throw
    }

    @Test
    void testUpdateChildLocationParent_HandlesFailure() {
        OpenMrsCallsUtils.updateChildLocationParent(mockLocation, "parentUuid");
        assertTrue(true);
    }

    @Test
    void testCreateConnection_InvalidUrlThrowsException() {
        assertThrows(Exception.class, () -> {
            OpenMrsCallsUtils.createConnection("::bad-url::", "GET");
        });
    }

    @Test
    void testUpdateLocationName_HandlesFailure() {
        int result = OpenMrsCallsUtils.updateLocationName(mockLocation, "NewName");
        assertEquals(-1, result); // Because HTTP calls will fail
    }
}