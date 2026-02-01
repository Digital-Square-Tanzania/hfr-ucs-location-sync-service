package tz.go.moh.ucs.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import tz.go.moh.ucs.Main;
import tz.go.moh.ucs.domain.HfrCodeCorrectionRow;
import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.domain.LocationCSVRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        Main.codeCache.clear();
        Main.locationCache.clear();
        Main.allLocations = new ArrayList<>();
        deleteLogFiles();

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

    @Test
    void testApplyHamletCodeCorrectionsUpdatesCodeAndLogsSuccess() throws IOException {
        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode("OLD.CODE");
        row.setCorrectHfrCode("NEWCODE");
        row.setLocationName("Test Location");
        row.setLocationType("Village");

        Location location = new Location();
        location.setLocationId("loc-uuid");
        location.setName("Test Location");
        location.setTags(new HashSet<>());
        location.getTags().add("Village");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Code", "OLD.CODE");
        location.setAttributes(attributes);
        Main.codeCache.put("old.code", location);

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class);
             MockedStatic<OpenMrsCallsUtils> mockOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockReader.when(() -> CSVReaderUtil.readHfrCodeCorrectionsFromResources("test.csv"))
                    .thenReturn(List.of(row));
            mockOpenMrs.when(() -> OpenMrsCallsUtils.getLocationAttributeUuid(anyString(), anyString()))
                    .thenReturn("attr-uuid");
            mockOpenMrs.when(() -> OpenMrsCallsUtils.updateLocationAttribute(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> null);

            Utils.applyHamletCodeCorrectionsFromCSV("test.csv");

            assertEquals("NEWCODE", location.getAttributes().get("Code"));
            assertNull(Main.codeCache.get("old.code"));
            assertEquals(location, Main.codeCache.get("newcode"));
        }

        JSONArray logEntries = readLogArray("src/main/resources/hamlets_updates_success.json");
        assertEquals(1, logEntries.length());
        JSONObject entry = logEntries.getJSONObject(0);
        assertEquals("OLD.CODE", entry.getString("incorrectCode"));
        assertEquals("NEWCODE", entry.getString("correctCode"));
    }

    @Test
    void testApplyHamletCodeCorrectionsSetsParentFromCorrectCode() {
        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode("TZ.WRONG.1");
        row.setCorrectHfrCode("TZ.PARENT.1");

        Location parent = new Location();
        parent.setLocationId("parent-uuid");
        parent.setName("Parent");
        parent.setTags(new HashSet<>());
        parent.getTags().add("Ward");
        Map<String, String> parentAttributes = new HashMap<>();
        parentAttributes.put("Code", "TZ.PARENT");
        parent.setAttributes(parentAttributes);

        Location child = new Location();
        child.setLocationId("child-uuid");
        child.setName("Child");
        child.setTags(new HashSet<>());
        child.getTags().add("Village");
        Map<String, String> childAttributes = new HashMap<>();
        childAttributes.put("Code", "TZ.WRONG.1");
        child.setAttributes(childAttributes);

        Main.codeCache.put("tz.parent", parent);
        Main.codeCache.put("tz.wrong.1", child);

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class);
             MockedStatic<OpenMrsCallsUtils> mockOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockReader.when(() -> CSVReaderUtil.readHfrCodeCorrectionsFromResources("test.csv"))
                    .thenReturn(List.of(row));
            mockOpenMrs.when(() -> OpenMrsCallsUtils.getLocationAttributeUuid(anyString(), anyString()))
                    .thenReturn("attr-uuid");
            mockOpenMrs.when(() -> OpenMrsCallsUtils.updateLocationAttribute(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> null);
            mockOpenMrs.when(() -> OpenMrsCallsUtils.updateChildLocationParent(child, "parent-uuid"))
                    .thenAnswer(invocation -> null);

            Utils.applyHamletCodeCorrectionsFromCSV("test.csv");

            mockOpenMrs.verify(() -> OpenMrsCallsUtils.updateChildLocationParent(child, "parent-uuid"));
            assertEquals(parent, child.getParentLocation());
        }
    }

    @Test
    void testApplyHamletCodeCorrectionsMissingLocationLogsMissing() throws IOException {
        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode("MISSING.CODE");
        row.setCorrectHfrCode("NEW.CODE");

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class)) {
            mockReader.when(() -> CSVReaderUtil.readHfrCodeCorrectionsFromResources("test.csv"))
                    .thenReturn(List.of(row));

            Utils.applyHamletCodeCorrectionsFromCSV("test.csv");
        }

        JSONArray logEntries = readLogArray("src/main/resources/hamlets_updates_missing.json");
        assertEquals(1, logEntries.length());
        assertEquals("MISSING.CODE", logEntries.getJSONObject(0).getString("incorrectCode"));
    }

    @Test
    void testApplyHamletCodeCorrectionsMissingParentDoesNotUpdateParent() throws IOException {
        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode("TZ.OLD.1");
        row.setCorrectHfrCode("TZ.NEW.1");

        Location child = new Location();
        child.setLocationId("child-uuid");
        child.setName("Child");
        child.setTags(new HashSet<>());
        child.getTags().add("Village");
        Map<String, String> childAttributes = new HashMap<>();
        childAttributes.put("Code", "TZ.OLD.1");
        child.setAttributes(childAttributes);

        Main.codeCache.put("tz.old.1", child);

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class);
             MockedStatic<OpenMrsCallsUtils> mockOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockReader.when(() -> CSVReaderUtil.readHfrCodeCorrectionsFromResources("test.csv"))
                    .thenReturn(List.of(row));
            mockOpenMrs.when(() -> OpenMrsCallsUtils.getLocationAttributeUuid(anyString(), anyString()))
                    .thenReturn("attr-uuid");
            mockOpenMrs.when(() -> OpenMrsCallsUtils.updateLocationAttribute(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> null);

            Utils.applyHamletCodeCorrectionsFromCSV("test.csv");

            mockOpenMrs.verify(() -> OpenMrsCallsUtils.updateChildLocationParent(any(), anyString()), never());
            assertNull(child.getParentLocation());
        }

        JSONArray logEntries = readLogArray("src/main/resources/hamlets_updates_success.json");
        assertEquals(1, logEntries.length());
        assertTrue(logEntries.getJSONObject(0).getString("message").contains("Parent location not found"));
    }

    @Test
    void testApplyHamletCodeCorrectionsLogsFailureOnException() throws IOException {
        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode("OLD.CODE");
        row.setCorrectHfrCode("NEW.CODE");

        Location location = new Location();
        location.setLocationId("loc-uuid");
        location.setName("Test Location");
        location.setTags(new HashSet<>());
        location.getTags().add("Village");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Code", "OLD.CODE");
        location.setAttributes(attributes);
        Main.codeCache.put("old.code", location);

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class);
             MockedStatic<OpenMrsCallsUtils> mockOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockReader.when(() -> CSVReaderUtil.readHfrCodeCorrectionsFromResources("test.csv"))
                    .thenReturn(List.of(row));
            mockOpenMrs.when(() -> OpenMrsCallsUtils.getLocationAttributeUuid(anyString(), anyString()))
                    .thenThrow(new RuntimeException("boom"));

            Utils.applyHamletCodeCorrectionsFromCSV("test.csv");
        }

        JSONArray logEntries = readLogArray("src/main/resources/hamlets_updates_failed.json");
        assertEquals(1, logEntries.length());
        assertEquals("OLD.CODE", logEntries.getJSONObject(0).getString("incorrectCode"));
    }

    @Test
    void testApplyHamletCodeCorrectionsLogsDuplicate() throws IOException {
        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode("DUP.OLD");
        row.setCorrectHfrCode("DUP.NEW");
        row.setLocationName("Duplicate Location");

        Location incorrectLocation = new Location();
        incorrectLocation.setLocationId("incorrect-uuid");
        incorrectLocation.setName("Incorrect");
        incorrectLocation.setTags(new HashSet<>());
        incorrectLocation.getTags().add("Village");
        Map<String, String> incorrectAttributes = new HashMap<>();
        incorrectAttributes.put("Code", "DUP.OLD");
        incorrectLocation.setAttributes(incorrectAttributes);

        Location correctLocation = new Location();
        correctLocation.setLocationId("correct-uuid");
        correctLocation.setName("Correct");
        correctLocation.setTags(new HashSet<>());
        correctLocation.getTags().add("Village");
        Map<String, String> correctAttributes = new HashMap<>();
        correctAttributes.put("Code", "DUP.NEW");
        correctLocation.setAttributes(correctAttributes);

        Main.codeCache.put("dup.old", incorrectLocation);
        Main.codeCache.put("dup.new", correctLocation);

        try (MockedStatic<CSVReaderUtil> mockReader = mockStatic(CSVReaderUtil.class);
             MockedStatic<OpenMrsCallsUtils> mockOpenMrs = mockStatic(OpenMrsCallsUtils.class)) {
            mockReader.when(() -> CSVReaderUtil.readHfrCodeCorrectionsFromResources("test.csv"))
                    .thenReturn(List.of(row));

            Utils.applyHamletCodeCorrectionsFromCSV("test.csv");

            mockOpenMrs.verify(() -> OpenMrsCallsUtils.updateLocationAttribute(anyString(), anyString(), anyString(), anyString()), never());
            mockOpenMrs.verify(() -> OpenMrsCallsUtils.updateChildLocationParent(any(), anyString()), never());
        }

        List<String> lines = readCsvLines("src/main/resources/hamlets_updates_duplicates.csv");
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("DUP.OLD"));
        assertTrue(lines.get(1).contains("DUP.NEW"));
        assertTrue(lines.get(1).contains("incorrect-uuid"));
        assertTrue(lines.get(1).contains("correct-uuid"));
    }

    private static void deleteLogFiles() {
        try {
            resetLogFile(Paths.get("src/main/resources/hamlets_updates_success.json"));
            resetLogFile(Paths.get("src/main/resources/hamlets_updates_failed.json"));
            resetLogFile(Paths.get("src/main/resources/hamlets_updates_missing.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/hamlets_updates_duplicates.csv"));
        } catch (IOException ignored) {
        }
    }

    private static void resetLogFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "[]", StandardCharsets.UTF_8);
    }

    private static JSONArray readLogArray(String path) throws IOException {
        Path filePath = Paths.get(path);
        assertTrue(Files.exists(filePath), "Expected log file to exist at " + path);
        String content = Files.readString(filePath, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return new JSONArray();
        }
        return new JSONArray(content);
    }

    private static List<String> readCsvLines(String path) throws IOException {
        Path filePath = Paths.get(path);
        assertTrue(Files.exists(filePath), "Expected CSV file to exist at " + path);
        return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    }
}
