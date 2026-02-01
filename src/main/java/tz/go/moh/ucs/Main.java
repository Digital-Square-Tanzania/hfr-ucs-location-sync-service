package tz.go.moh.ucs;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.service.HfrService;
import tz.go.moh.ucs.util.FetchLocationsHelper;
import tz.go.moh.ucs.util.HttpUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tz.go.moh.ucs.service.OpenmrsService.OPENMRS_BASE_URL;
import static tz.go.moh.ucs.util.CapitalizeUtil.capitalizeWords;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.*;
import static tz.go.moh.ucs.util.Utils.*;

public class Main {
    // Caches for quick lookup by location UUID and Code
    public static final Map<String, Location> locationCache = new HashMap<>();
    public static final Map<String, Location> codeCache = new HashMap<>();
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static String CODE_LOCATION_ATTRIBUTE_UUID;
    public static String HFR_CODE_LOCATION_ATTRIBUTE_UUID;
    public static List<Location> allLocations = new ArrayList<>();
    public static String HAMLET_RESOURCE_FILE_NAME;
    private static final String SELECTED_COUNCILS_RESOURCE_NAME = "selected_councils.json";
    private static final Set<String> SELECTED_COUNCILS = loadSelectedCouncils();
    private static final ThreadLocal<Boolean> HAMLET_IMPORT_CONTEXT = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<String> HAMLET_IMPORT_RESOURCE = new ThreadLocal<>();

    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        CODE_LOCATION_ATTRIBUTE_UUID = config.getString("openmrs.code_location_attribute_uuid");
        HFR_CODE_LOCATION_ATTRIBUTE_UUID = config.getString("openmrs.hfr_code_location_attribute_uuid");
        HAMLET_RESOURCE_FILE_NAME = config.getString("hamlet.resource_file_name");

        allLocations = new FetchLocationsHelper().getAllOpenMRSlocations();

        // Initialize caches
        for (Location loc : allLocations) {
            if (loc != null && loc.getLocationId() != null) {
                locationCache.put(loc.getLocationId().toLowerCase(), loc);
                if (loc.getAttributes() != null && loc.getAttributes().get("Code") != null && !loc.hasTag("Facility")) {
                    codeCache.put(((String) loc.getAttributes().get("Code")).toLowerCase(), loc);
                } else if (loc.getAttributes() != null && loc.getAttributes().get("HFR Code") != null && loc.hasTag("Facility")) {
                    codeCache.put(((String) loc.getAttributes().get("HFR Code")).toLowerCase(), loc);
                }
            }
        }
        LOGGER.info("Fetched " + allLocations.size() + " locations.");
//        fetchHfrData(1);
//        fetchHfrAdminHierarchyData(1);
        applyHamletCodeCorrectionsFromCSV(HAMLET_RESOURCE_FILE_NAME);
//        importHamletLocationsFromCSV(HAMLET_RESOURCE_FILE_NAME);

        LOGGER.info("COMPLETED SYNCHRONIZATION OF HFR LOCATIONS");
    }

    /**
     * Fetches HFR data with a maximum number of retry attempts.
     */
    public static void fetchHfrData(int currentPage) {
        int maxAttempts = 200;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOGGER.info("Fetching HFR data for page " + currentPage + ", attempt " + attempt);
                HfrService.fetchHealthFacilityData(currentPage);
                break;  // success, break out of the loop
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error fetching HFR data for page " + currentPage + " (attempt " + attempt + ")", e);
                if (attempt == maxAttempts) {
                    LOGGER.severe("Max attempts reached for page " + currentPage + ". Aborting fetch.");
                }
            }
        }
    }

    /**
     * Fetches HFR data with a maximum number of retry attempts.
     */
    public static void fetchHfrAdminHierarchyData(int currentPage) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOGGER.info("Fetching HFR data for page " + currentPage + ", attempt " + attempt);
                HfrService.fetchAdminHierarchData(currentPage);
                break;  // success, break out of the loop
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error fetching HFR data for page " + currentPage + " (attempt " + attempt + ")", e);
                if (attempt == maxAttempts) {
                    LOGGER.severe("Max attempts reached for page " + currentPage + ". Aborting fetch.");
                }
            }
        }
    }

    private static Set<String> loadSelectedCouncils() {
        Set<String> councils = new HashSet<>();
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream(SELECTED_COUNCILS_RESOURCE_NAME)) {
            if (is == null) {
                LOGGER.warning("Selected councils resource not found: " + SELECTED_COUNCILS_RESOURCE_NAME);
                return Collections.emptySet();
            }
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }
            String json = jsonBuilder.toString().trim();
            if (json.isEmpty()) {
                LOGGER.warning("Selected councils resource is empty: " + SELECTED_COUNCILS_RESOURCE_NAME);
                return Collections.emptySet();
            }

            JSONArray councilArray;
            if (json.startsWith("[")) {
                councilArray = new JSONArray(json);
            } else if (json.startsWith("{")) {
                JSONObject root = new JSONObject(json);
                if (root.has("councils")) {
                    councilArray = root.getJSONArray("councils");
                } else if (root.has("selectedCouncils")) {
                    councilArray = root.getJSONArray("selectedCouncils");
                } else if (root.has("selected_councils")) {
                    councilArray = root.getJSONArray("selected_councils");
                } else {
                    LOGGER.warning("Selected councils resource missing councils array: " + SELECTED_COUNCILS_RESOURCE_NAME);
                    return Collections.emptySet();
                }
            } else {
                LOGGER.warning("Selected councils resource has unexpected format: " + SELECTED_COUNCILS_RESOURCE_NAME);
                return Collections.emptySet();
            }

            for (int i = 0; i < councilArray.length(); i++) {
                String councilName = councilArray.optString(i, "").trim();
                if (!councilName.isEmpty()) {
                    councils.add(normalizeCouncilName(councilName));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load selected councils from " + SELECTED_COUNCILS_RESOURCE_NAME, e);
        }

        if (councils.isEmpty()) {
            LOGGER.warning("No selected councils found in " + SELECTED_COUNCILS_RESOURCE_NAME);
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(councils);
    }

    private static String normalizeCouncilName(String councilName) {
        if (councilName == null) {
            return "";
        }
        return councilName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean isSelectedCouncil(String councilName) {
        if (SELECTED_COUNCILS.isEmpty()) {
            return false;
        }
        return SELECTED_COUNCILS.contains(normalizeCouncilName(councilName));
    }


    /**
     * Process HFR Admin Hierarchy
     */
    public static void processAdminHierarchyData(JSONArray response) {
        for (int i = 0; i < response.length(); i++) {
            JSONObject facilityJson = response.getJSONObject(i);
            String councilName = facilityJson.optString("council", null);
            if (!isSelectedCouncil(councilName)) {
                continue;
            }
            try {
                Location countryLoc = ensureLocationExists(null, capitalizeWords(facilityJson.getString("country")), "TZ", "Country");
                Location zoneLoc = ensureLocationExists(countryLoc, capitalizeWords(facilityJson.getString("zone")), facilityJson.getString("zone_code"), "Zone");
                ensureLocationExists(zoneLoc, capitalizeWords(facilityJson.getString("region")), facilityJson.getString("region_code"), "Region");
                Location wardLoc = ensureLocationExists(null, capitalizeWords(facilityJson.getString("ward") + " - " + councilName), facilityJson.getString("ward_code"), "Ward");
                ensureLocationExists(wardLoc, capitalizeWords(facilityJson.getString("village_mtaa") + " - " + facilityJson.getString("ward") + " - " + councilName), facilityJson.getString("village_mtaa_code"), "Village");
            } catch (Exception e) {
                LOGGER.severe("Error processing Admin Hierarchy " + e.getMessage());
            }
        }
    }

    /**
     * Processes the HFR JSON response, updating or creating locations.
     */
    public static void processHfrResponse(JSONArray response) {
        for (int i = 0; i < response.length(); i++) {
            JSONObject facilityJson = response.getJSONObject(i);
            String hfrCode = facilityJson.getString("Fac_IDNumber");

            try {
                Location regionLoc = ensureLocationExists(null, capitalizeWords(facilityJson.getString("region")), facilityJson.getString("Region_Code"), "Region");
                Location districtLoc = ensureLocationExists(regionLoc, capitalizeWords(facilityJson.getString("district")), facilityJson.getString("District_Code"), "District");
                Location councilLoc = ensureLocationExists(districtLoc, capitalizeWords(facilityJson.getString("council")), facilityJson.getString("Council_Code"), "Council");
                Location wardLoc = ensureLocationExists(councilLoc, capitalizeWords(facilityJson.getString("ward") + " - " + facilityJson.getString("council")), facilityJson.getString("ward_Code"), "Ward");
                ensureLocationExists(wardLoc, capitalizeWords(facilityJson.getString("Name") + " - " + hfrCode), facilityJson.getString("Fac_IDNumber"), "Facility");
                ensureLocationExists(wardLoc, capitalizeWords(facilityJson.getString("village") + " - " + facilityJson.getString("ward") + " - " + facilityJson.getString("council")), facilityJson.getString("Village_Code"), "Village");
            } catch (Exception e) {
                LOGGER.severe("Error processing HFR Response " + e.getMessage());
            }
        }
    }

    /**
     * Generic helper method to create a new location by POSTing to the /location endpoint.
     */
    public static Location createNewLocation(String name, String parentUuid, Set<String> tags, Map<String, String> attributes) throws Exception {
        int maxAttempts = 10;
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/ws/rest/v1/location";
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOGGER.info("Creating new location: " + name + " " + tags);

                HttpURLConnection conn = createConnection(url, "POST");
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("description", resolveCreationDescription(tags));
                if (parentUuid != null) {
                    json.put("parentLocation", parentUuid);
                }
                if (tags != null && !tags.isEmpty()) {
                    JSONArray tagArray = new JSONArray();
                    for (String tag : tags) {
                        tagArray.put(new JSONObject().put("name", tag));
                    }
                    json.put("tags", tagArray);
                }
                if (attributes != null && !attributes.isEmpty()) {
                    JSONArray attributesArray = new JSONArray();
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        JSONObject attrJson = new JSONObject();
                        attrJson.put("attributeType", entry.getKey());
                        attrJson.put("value", entry.getValue());
                        attributesArray.put(attrJson);
                    }
                    json.put("attributes", attributesArray);
                }
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes());
                }
                int responseCode = conn.getResponseCode();
                LOGGER.info("Create Location Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        JSONObject createdLocation = new JSONObject(response.toString());
                        Location newLocation = new Location();
                        newLocation.setLocationId(createdLocation.getString("uuid"));
                        newLocation.setName(createdLocation.getString("name"));

                        Map<String, String> locAttributes = new HashMap<>();
                        if (attributes != null && !attributes.isEmpty()) {
                            if (attributes.get(CODE_LOCATION_ATTRIBUTE_UUID) != null) {
                                locAttributes.put("Code", attributes.get(CODE_LOCATION_ATTRIBUTE_UUID));
                            } else if (attributes.get(HFR_CODE_LOCATION_ATTRIBUTE_UUID) != null) {
                                locAttributes.put("HFR Code", attributes.get(HFR_CODE_LOCATION_ATTRIBUTE_UUID));
                            } else {
                                locAttributes.putAll(attributes);
                            }
                        }

                        newLocation.setAttributes(locAttributes);
                        newLocation.setTags(tags);
                        if (parentUuid != null) {
                            newLocation.setParentLocation(findLocationByUuid(parentUuid));
                        }
                        return newLocation;
                    }
                } else {
                    LOGGER.severe("Failed to create new location: " + name);
                    return null;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error creating new location " + name, e);
            }
        }
        return null;
    }

    public static void setHamletImportContext(boolean active) {
        HAMLET_IMPORT_CONTEXT.set(active);
    }

    public static void clearHamletImportContext() {
        HAMLET_IMPORT_CONTEXT.remove();
    }

    public static boolean isHamletImportContext() {
        Boolean flag = HAMLET_IMPORT_CONTEXT.get();
        return flag != null && flag;
    }

    public static void setHamletImportResource(String resourceName) {
        HAMLET_IMPORT_RESOURCE.set(resourceName);
    }

    public static String getHamletImportResource() {
        return HAMLET_IMPORT_RESOURCE.get();
    }

    public static void clearHamletImportResource() {
        HAMLET_IMPORT_RESOURCE.remove();
    }

    private static String resolveCreationDescription(Set<String> tags) {
        if (isHamletImportContext() && tags != null) {
            for (String tag : tags) {
                if ("Hamlet".equalsIgnoreCase(tag) || "Village".equalsIgnoreCase(tag)) {
                    return "Created via integration from Update ICCHW villages";
                }
            }
        }
        return "Created via integration";
    }
}
