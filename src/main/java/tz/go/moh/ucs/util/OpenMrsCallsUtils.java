package tz.go.moh.ucs.util;

import org.json.JSONArray;
import org.json.JSONObject;
import tz.go.moh.ucs.domain.Location;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tz.go.moh.ucs.Main.*;
import static tz.go.moh.ucs.service.OpenmrsService.*;
import static tz.go.moh.ucs.util.Utils.findLocationByUuid;

public class OpenMrsCallsUtils {
    private static final Logger LOGGER = Logger.getLogger(OpenMrsCallsUtils.class.getName());

    /**
     * Processes other location types for attribute and name updates.
     */
    public static void getOtherLocation(String uuid, String parentLocationUuid, JSONObject hfrLocation) {
        for (Location location : allLocations) {
            if (location != null && location.getLocationId() != null &&
                    location.getParentLocation() != null &&
                    location.getParentLocation().getLocationId().equalsIgnoreCase(uuid)) {
                LOGGER.info("Processing other location: " + location.getName());
                if (location.getTags() != null && location.getTags().contains("Village") &&
                        location.getAttribute("Code") != null &&
                        location.getAttribute("Code").toString().equalsIgnoreCase(hfrLocation.getString("Village_Code"))) {
                    String expectedVillageName = hfrLocation.getString("village");
                    updateOrCreateLocationAttribute(location, CODE_LOCATION_ATTRIBUTE_UUID, hfrLocation.getString("Village_Code"));
                    if (!location.getName().equalsIgnoreCase(expectedVillageName)) {
                        updateLocationName(location, expectedVillageName);
                    }
                }
            }
        }
    }

    /**
     * Updates or creates a location attribute if not present.
     */
    public static void updateOrCreateLocationAttribute(Location location, String locationAttributeTypeUuid, String newCodeValue) {
        try {
            if (location.getAttributes() == null || location.getAttributes().get("Code") == null) {
                LOGGER.info("Adding location attribute for: " + location.getName());
                addLocationAttribute(location, locationAttributeTypeUuid, newCodeValue);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in updateOrCreateLocationAttribute for location " + location.getName(), e);
        }
    }

    /**
     * Retrieves the UUID of a location attribute.
     */
    public static String getLocationAttributeUuid(String locationUuid, String attributeTypeUuid) {
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/ws/rest/v1/location/" + locationUuid + "?v=full";
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpURLConnection conn = createConnection(url, "GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        JSONObject locationJson = new JSONObject(response.toString());
                        JSONArray attributes = locationJson.getJSONArray("attributes");
                        for (int i = 0; i < attributes.length(); i++) {
                            JSONObject attribute = attributes.getJSONObject(i);
                            if (attribute.getJSONObject("attributeType").getString("uuid").equals(attributeTypeUuid)) {
                                return attribute.getString("uuid");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error fetching location attribute UUID (attempt " + attempt + ")", e);
            }
        }
        return null;
    }


    /**
     * Updates an existing location attribute with retries.
     */
    public static void updateLocationAttribute(String locationUuid, String attributeUuid, String attributeTypeUuid, String newValue) {
        int maxAttempts = 3;
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/ws/rest/v1/location/" + locationUuid;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpURLConnection conn = createConnection(url, "POST");
                conn.setRequestProperty("Content-Type", "application/json");
                JSONObject attribute = new JSONObject();
                attribute.put("uuid", attributeUuid);
                attribute.put("attributeType", attributeTypeUuid);
                attribute.put("value", newValue);
                JSONArray attributesArray = new JSONArray();
                attributesArray.put(attribute);
                JSONObject requestJson = new JSONObject();
                requestJson.put("attributes", attributesArray);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestJson.toString().getBytes());
                }
                int responseCode = conn.getResponseCode();
                LOGGER.info("Update Location Attribute Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    LOGGER.info("Location attribute updated successfully for " + locationUuid);
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating location attribute for " + locationUuid + " (attempt " + attempt + ")", e);
            }
        }
    }

    /**
     * Adds a new attribute to a location.
     */
    public static void addLocationAttribute(Location location, String attributeTypeUuid, String newValue) {
        int maxAttempts = 3;
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/ws/rest/v1/location/" + location.getLocationId();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpURLConnection conn = createConnection(url, "POST");
                conn.setRequestProperty("Content-Type", "application/json");
                JSONObject attribute = new JSONObject();
                attribute.put("attributeType", attributeTypeUuid);
                attribute.put("value", newValue);
                JSONArray attributesArray = new JSONArray();
                attributesArray.put(attribute);
                JSONObject requestJson = new JSONObject();
                requestJson.put("attributes", attributesArray);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestJson.toString().getBytes());
                }
                int responseCode = conn.getResponseCode();
                LOGGER.info("Add Location Attribute Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    LOGGER.info("Location attribute added successfully for " + location.getName());
                    if (location.getAttributes() == null) {
                        location.setAttributes(new HashMap<>());
                    }
                    if (attributeTypeUuid.equalsIgnoreCase(HFR_CODE_LOCATION_ATTRIBUTE_UUID))
                        location.getAttributes().put("HFR Code", newValue);
                    else
                        location.getAttributes().put("Code", newValue);
                    codeCache.put(newValue.toLowerCase(), location);
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error adding location attribute for " + location.getName() + " (attempt " + attempt + ")", e);
            }
        }
    }

    /**
     * Updates a child's parent location.
     */
    public static void updateChildLocationParent(Location child, String newParentUuid) {
        int maxAttempts = 10;
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/ws/rest/v1/location/" + child.getLocationId();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpURLConnection conn = createConnection(url, "POST");
                conn.setRequestProperty("Content-Type", "application/json");
                JSONObject parentLocationJson = new JSONObject();
                parentLocationJson.put("uuid", newParentUuid);
                JSONObject requestJson = new JSONObject();
                requestJson.put("parentLocation", parentLocationJson);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestJson.toString().getBytes());
                }
                int responseCode = conn.getResponseCode();
                LOGGER.info("Update Child Parent Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    LOGGER.info("Child location parent updated successfully for " + child.getName());
                    Location newParent = findLocationByUuid(newParentUuid);
                    child.setParentLocation(newParent);
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating child location parent for " + child.getName(), e);
            }
        }
    }

    /**
     * Creates and configures an HttpURLConnection.
     */
    public static HttpURLConnection createConnection(String url, String method) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod(method);
        String basicAuth = Base64.getEncoder().encodeToString((OPENMRS_USER + ":" + OPENMRS_PWD).getBytes());
        conn.setRequestProperty("Authorization", "Basic " + basicAuth);
        conn.setDoOutput(true);
        return conn;
    }

    /**
     * Updates the name of a location.
     */
    public static int updateLocationName(Location location, String newName) {
        int maxAttempts = 10;
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/ws/rest/v1/location/" + location.getLocationId();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpURLConnection conn = createConnection(url, "POST");
                conn.setRequestProperty("Content-Type", "application/json");
                JSONObject requestJson = new JSONObject();
                requestJson.put("name", newName);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestJson.toString().getBytes());
                }
                int responseCode = conn.getResponseCode();
                LOGGER.info("Update Location Name Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    location.setName(newName);
                    return responseCode;
                } else {
                    LOGGER.info("Failed Update for Location Name: " + location.getName() + " TO = " + newName);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating location name for " + location.getName() + " (attempt " + attempt + ")", e);
            }
        }
        return -1;
    }
}
