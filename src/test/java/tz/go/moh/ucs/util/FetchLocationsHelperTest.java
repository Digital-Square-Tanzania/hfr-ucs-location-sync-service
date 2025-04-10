package tz.go.moh.ucs.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import tz.go.moh.ucs.domain.Location;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Note: This test class assumes that the ConnectorConstants keys used in FetchLocationsHelper
// correspond to the following string values:
// UUID -> "uuid"
// NAME -> "name"
// TAGS -> "tags"
// DISPLAY -> "display"
// ATTRIBUTES -> "attributes"
// VOIDED -> "voided"
// PARENT_LOCATION -> "parentLocation"
// RESULTS -> "results"

public class FetchLocationsHelperTest {

    private static final String UUID = "uuid";
    private static final String NAME = "name";
    private static final String TAGS = "tags";
    private static final String DISPLAY = "display";
    private static final String ATTRIBUTES = "attributes";
    private static final String VOIDED = "voided";
    private static final String PARENT_LOCATION = "parentLocation";

    // Test makeLocation(String) without attributes and parent
    @Test
    public void testMakeLocationWithoutAttributesAndParent() throws JSONException {
        String json = "{" +
                "\"" + UUID + "\": \"test-uuid\"," +
                "\"" + NAME + "\": \"Test Location\"," +
                "\"" + TAGS + "\": []" +
                "}";
        FetchLocationsHelper helper = new FetchLocationsHelper();
        Location loc = helper.makeLocation(json);
        assertEquals("test-uuid", loc.getLocationId());
        assertEquals("Test Location", loc.getName());
        assertNull(loc.getTags());
        // Parent should be null
        assertNull(loc.getParentLocation());
    }

    // Test makeLocation(String) with tags and attributes
    @Test
    public void testMakeLocationWithTagsAndAttributes() throws JSONException {
        String json = "{" +
                "\"" + UUID + "\": \"test-uuid\"," +
                "\"" + NAME + "\": \"Test Location\"," +
                "\"" + TAGS + "\": [{\"" + DISPLAY + "\": \"Tag1\"}]," +
                "\"" + ATTRIBUTES + "\": [{\"" + DISPLAY + "\": \"key: value\", \"" + VOIDED + "\": false}]" +
                "}";
        FetchLocationsHelper helper = new FetchLocationsHelper();
        Location loc = helper.makeLocation(json);
        assertEquals("test-uuid", loc.getLocationId());
        assertEquals("Test Location", loc.getName());
        // Verify that the tag is added
        assertEquals(1, loc.getTags().size());
        assertTrue(loc.getTags().contains("Tag1"));
        // If Location stores attributes, you could verify that here; for now we just ensure no exception
    }

    // Test makeLocation(String) with parent location
    @Test
    public void testMakeLocationWithParent() throws JSONException {
        String json = "{" +
                "\"" + UUID + "\": \"child-uuid\"," +
                "\"" + NAME + "\": \"Child Location\"," +
                "\"" + TAGS + "\": []," +
                "\"" + PARENT_LOCATION + "\": {\"" + UUID + "\": \"parent-uuid\", \"" + DISPLAY + "\": \"Parent Location\"}" +
                "}";
        FetchLocationsHelper helper = new FetchLocationsHelper();
        Location loc = helper.makeLocation(json);
        assertEquals("child-uuid", loc.getLocationId());
        assertNotNull(loc.getParentLocation());
        assertEquals("parent-uuid", loc.getParentLocation().getLocationId());
        // Assuming display maps to name
        assertEquals("Parent Location", loc.getParentLocation().getName());
    }

    // Test makeLocation(JSONObject) using a JSONObject
    @Test
    public void testMakeLocationWithJSONObject() throws JSONException {
        JSONObject parent = new JSONObject();
        parent.put(UUID, "parent-uuid");
        parent.put(DISPLAY, "Parent Location");

        JSONObject jsonObj = new JSONObject();
        jsonObj.put(UUID, "child-uuid");
        jsonObj.put(NAME, "Child Location");
        jsonObj.put(TAGS, new JSONArray());
        jsonObj.put(PARENT_LOCATION, parent);

        FetchLocationsHelper helper = new FetchLocationsHelper();
        Location loc = helper.makeLocation(jsonObj);
        assertEquals("child-uuid", loc.getLocationId());
        assertNotNull(loc.getParentLocation());
        assertEquals("parent-uuid", loc.getParentLocation().getLocationId());
    }

    // Test getParent when there is no parent location in the JSON
    @Test
    public void testGetParentNoParent() throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put(UUID, "test-uuid");
        jsonObj.put(NAME, "Test Location");
        jsonObj.put(TAGS, new JSONArray());
        // No parentLocation key
        FetchLocationsHelper helper = new FetchLocationsHelper();
        Location parent = helper.getParent(jsonObj);
        assertNull(parent);
    }

    // Test getParent with recursive parent (nested parent locations)
    @Test
    public void testGetParentWithRecursiveParent() throws JSONException {
        JSONObject grandParent = new JSONObject();
        grandParent.put(UUID, "grand-uuid");
        grandParent.put(DISPLAY, "Grandparent Location");

        JSONObject parent = new JSONObject();
        parent.put(UUID, "parent-uuid");
        parent.put(DISPLAY, "Parent Location");
        parent.put(PARENT_LOCATION, grandParent);

        JSONObject child = new JSONObject();
        child.put(UUID, "child-uuid");
        child.put(NAME, "Child Location");
        child.put(TAGS, new JSONArray());
        child.put(PARENT_LOCATION, parent);

        FetchLocationsHelper helper = new FetchLocationsHelper();
        Location parentLocation = helper.getParent(child);
        assertNotNull(parentLocation);
        assertEquals("parent-uuid", parentLocation.getLocationId());
        // Verify recursion: parent's parent should be grandparent
        assertNotNull(parentLocation.getParentLocation());
        assertEquals("grand-uuid", parentLocation.getParentLocation().getLocationId());
    }

    // Test getAllOpenMRSlocations by overriding getAllLocations to return a fixed list
    @Test
    public void testGetAllOpenMRSlocations() throws JSONException {
        class TestFetchLocationsHelper extends FetchLocationsHelper {
            @Override
            public List<Location> getAllLocations(List<Location> locationList, int startIndex) throws JSONException {
                // Simulate two locations without recursion
                locationList.add(new Location("uuid1", "Location1", null, null, null, null, null));
                locationList.add(new Location("uuid2", "Location2", null, null, null, null, null));
                return locationList;
            }
        }
        FetchLocationsHelper helper = new TestFetchLocationsHelper();
        List<Location> locations = helper.getAllOpenMRSlocations();
        assertEquals(2, locations.size());
        assertEquals("uuid1", locations.get(0).getLocationId());
        assertEquals("uuid2", locations.get(1).getLocationId());
    }
}