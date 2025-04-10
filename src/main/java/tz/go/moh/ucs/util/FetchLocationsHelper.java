package tz.go.moh.ucs.util;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.service.OpenmrsService;

import java.util.ArrayList;
import java.util.List;

import static tz.go.moh.ucs.Main.LOGGER;

public class FetchLocationsHelper extends OpenmrsService {
    public static final String LOCATION_URL = "ws/rest/v1/location";

    public synchronized List<Location> getAllOpenMRSlocations() {
        List<Location> allLocationsList = new ArrayList<>();
        return getAllLocations(allLocationsList, 0);
    }

    /**
     * New helper to fetch the raw JSON response from OpenMRS for the given start index.
     */
    protected String fetchLocationResponse(int startIndex) throws Exception {
        String url = HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/" + LOCATION_URL
                + "?v=custom:(uuid,display,name,attributes,tags:(uuid,display),parentLocation:(uuid,display))"
                + "&limit=100&startIndex=" + startIndex;
        return HttpUtil.getURL(url, OPENMRS_USER, OPENMRS_PWD);
    }

    /**
     * New helper to parse the response and accumulate locations into the provided list.
     */
    protected List<Location> parseLocationsFromResponse(String response, List<Location> locationList) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);
        if (StringUtils.isNotBlank(response) && jsonObject.has(ConnectorConstants.RESULTS)) {
            JSONArray results = jsonObject.getJSONArray(ConnectorConstants.RESULTS);
            for (int i = 0; i < results.length(); i++) {
                locationList.add(makeLocation(results.getJSONObject(i)));
            }
        }
        return locationList;
    }

    /**
     * New helper to check if the response indicates that there is a next page of results.
     */
    protected boolean hasNextPage(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);
        if (jsonObject.has("links")) {
            JSONArray links = jsonObject.getJSONArray("links");
            // Check if any link has the 'next' relation
            for (int i = 0; i < links.length(); i++) {
                if ("next".equalsIgnoreCase(links.getJSONObject(i).optString("rel"))) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Location> getAllLocations(List<Location> locationList, int startIndex) throws JSONException {
        try {
            LOGGER.info("Fetching locations from OpenMRS Starting index = " + startIndex);
            String response = fetchLocationResponse(startIndex);
            if (StringUtils.isNotBlank(response)) {
                List<Location> updatedLocationList = parseLocationsFromResponse(response, locationList);
                if (hasNextPage(response)) {
                    return getAllLocations(updatedLocationList, startIndex + 100);
                }
                return updatedLocationList;
            }
        } catch (Exception e) {
            LOGGER.severe("Exception occurred, retrying fetch for start index: " + e.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return getAllLocations(locationList, startIndex);
        }
        return locationList;
    }

    public Location makeLocation(String locationJson) throws JSONException {
        JSONObject locationsJsonObject = new JSONObject(locationJson);
        Location parentLocation = getParent(locationsJsonObject);
        Location location = new Location(locationsJsonObject.getString(ConnectorConstants.UUID),
                locationsJsonObject.getString(ConnectorConstants.NAME), null, null, parentLocation, null, null);
        JSONArray tags = locationsJsonObject.getJSONArray(ConnectorConstants.TAGS);

        for (int i = 0; i < tags.length(); i++) {
            location.addTag(tags.getJSONObject(i).getString(ConnectorConstants.DISPLAY));
        }

        if (locationsJsonObject.has(ConnectorConstants.ATTRIBUTES)) {
            JSONArray attributes = locationsJsonObject.getJSONArray(ConnectorConstants.ATTRIBUTES);
            for (int i = 0; i < attributes.length(); i++) {
                JSONObject attribute = attributes.getJSONObject(i);
                boolean voided = attribute.optBoolean(ConnectorConstants.VOIDED);
                if (!voided) {
                    String ad = attribute.getString(ConnectorConstants.DISPLAY);
                    location.addAttribute(ad.substring(0, ad.indexOf(":")), ad.substring(ad.indexOf(":") + 2));
                }
            }
        }

        return location;
    }

    public Location makeLocation(JSONObject location) throws JSONException {
        return makeLocation(location.toString());
    }

    public Location getParent(JSONObject locobj) throws JSONException {
        JSONObject parentL = (locobj.has(ConnectorConstants.PARENT_LOCATION)
                && !locobj.isNull(ConnectorConstants.PARENT_LOCATION))
                ? locobj.getJSONObject(ConnectorConstants.PARENT_LOCATION)
                : null;

        if (parentL != null) {
            return new Location(parentL.getString(ConnectorConstants.UUID), parentL.getString(ConnectorConstants.DISPLAY),
                    null, getParent(parentL));
        }
        return null;
    }


}
