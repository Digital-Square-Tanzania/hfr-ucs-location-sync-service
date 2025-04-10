package tz.go.moh.ucs.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import tz.go.moh.ucs.Main;
import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.domain.LocationCSVRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilsTest {

    Location mockParentLocation;
    Location mockExistingLocation;

    @BeforeEach
    void setUp() {
        Main.CODE_LOCATION_ATTRIBUTE_UUID = "mock-code-uuid";
        Main.HFR_CODE_LOCATION_ATTRIBUTE_UUID = "mock-hfr-code-uuid";

        mockParentLocation = mock(Location.class);
        when(mockParentLocation.getLocationId()).thenReturn("parent-uuid");
        when(mockParentLocation.getName()).thenReturn("Parent Location");

        mockExistingLocation = mock(Location.class);
        when(mockExistingLocation.getName()).thenReturn("Old Name");
        when(mockExistingLocation.hasTag("Village")).thenReturn(true);
        when(mockExistingLocation.getParentLocation()).thenReturn(null);
    }


    @Test
    void testFindLocationByCodeWhenCodeExists() {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class, CALLS_REAL_METHODS)) {
            Location loc = new Location();
            loc.setName("Test Location");

            mockedUtils.when(() -> Utils.findLocationByCode("123")).thenReturn(loc);

            Location result = Utils.findLocationByCode("123");
            assertNotNull(result);
            assertEquals("Test Location", result.getName());
        }
    }

    @Test
    void testFindLocationByUuidWhenUuidExists() {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class, CALLS_REAL_METHODS)) {
            Location loc = new Location();
            loc.setLocationId("uuid-123");

            mockedUtils.when(() -> Utils.findLocationByUuid("uuid-123")).thenReturn(loc);

            Location result = Utils.findLocationByUuid("uuid-123");
            assertNotNull(result);
            assertEquals("uuid-123", result.getLocationId());
        }
    }

    @Test
    void testEnsureLocationExistsUpdatesExistingLocationNameAndParent() throws Exception {
        when(mockExistingLocation.hasTag("Village")).thenReturn(true);
        when(mockExistingLocation.getParentLocation()).thenReturn(null);
        when(mockExistingLocation.getLocationId()).thenReturn("existing-uuid");

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class, CALLS_REAL_METHODS);
             MockedStatic<OpenMrsCallsUtils> mockOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockedUtils.when(() -> Utils.findLocationByCode("CODE123")).thenReturn(mockExistingLocation);

            Location result = Utils.ensureLocationExists(mockParentLocation, "New Name", "CODE123", "Village");

            verify(mockExistingLocation).getName();
            mockOpenMrs.verify(() -> OpenMrsCallsUtils.updateLocationName(mockExistingLocation, "New Name"));
            mockOpenMrs.verify(() -> OpenMrsCallsUtils.updateChildLocationParent(mockExistingLocation, "parent-uuid"));
            assertEquals(mockExistingLocation, result);
        }
    }

    @Test
    void testEnsureLocationExistsCreatesNewLocationWhenNotFound() throws Exception {

        try (MockedStatic<Utils> mockUtils = mockStatic(Utils.class, CALLS_REAL_METHODS);
             MockedStatic<Main> mockMain = mockStatic(Main.class)) {
            // Simulate that no existing location is found
            mockUtils.when(() -> Utils.findLocationByCode("NEWCODE")).thenReturn(null);

            // Set up expected new location details
            Location expectedNewLocation = new Location();
            expectedNewLocation.setName("Test Location");
            expectedNewLocation.setLocationId("generated-uuid");
            Map<String, String> locationAttributes = new HashMap<>();
            // For tag "Village", the else branch is taken in ensureLocationExists:
            locationAttributes.put(Main.CODE_LOCATION_ATTRIBUTE_UUID, "NEWCODE");
            expectedNewLocation.setAttributes(locationAttributes);

            // Mock the creation of a new location by Main.createNewLocation
            mockMain.when(() -> Main.createNewLocation(anyString(), anyString(), anySet(), anyMap()))
                    .thenReturn(expectedNewLocation);

            // Call the method under test
            Location result = Utils.ensureLocationExists(mockParentLocation, "Test Location", "NEWCODE", "Village");

            // Assert that the returned location matches expectations
            assertNotNull(result);
            assertEquals("Test Location", result.getName());
        }
    }

    @Test
    void testEnsureLocationExistsReturnsNullWhenCodeIsNull() throws Exception {
        Location result = Utils.ensureLocationExists(mockParentLocation, "Name", null, "Region");
        assertNull(result);
    }

    @Test
    void testEnsureLocationExistsReturnsNullWhenParentIsNullForNonRegion() throws Exception {
        Location result = Utils.ensureLocationExists(null, "Name", "C123", "Village");
        assertNull(result);
    }

    @Test
    void testImportHamletLocationsFromCSVHandlesCSVCorrectly() {
        List<LocationCSVRow> rows = new ArrayList<>();
        LocationCSVRow row = new LocationCSVRow();
        row.setVillageCode("V123");
        row.setVillage("Village");
        row.setWard("Ward");
        row.setHamlet("Hamlet");
        row.setHamletCode("H123");
        rows.add(row);

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class);
             MockedStatic<Utils> mockUtils = mockStatic(Utils.class, CALLS_REAL_METHODS)) {

            mockReader.when(() -> CSVReaderUtil.readCsvFromResources("test.csv")).thenReturn(rows);

            Location villageLoc = new Location();
            villageLoc.setLocationId("village-uuid");
            mockUtils.when(() -> Utils.findLocationByCode("V123")).thenReturn(villageLoc);
            mockUtils.when(() -> Utils.ensureLocationExists(any(), any(), any(), eq("Hamlet")))
                    .thenReturn(new Location());

            Utils.importHamletLocationsFromCSV("test.csv");

            mockReader.verify(() -> CSVReaderUtil.readCsvFromResources("test.csv"), times(1));
        }
    }
}
