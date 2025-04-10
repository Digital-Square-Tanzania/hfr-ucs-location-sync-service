package tz.go.moh.ucs.service;

import tz.go.moh.ucs.util.FetchLocationsHelper;
import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.util.HttpUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenmrsLocationService extends OpenmrsService {

    public static final String LOCATION_URL = "ws/rest/v1/location";
    private FetchLocationsHelper fetchLocationsHelper;

    public OpenmrsLocationService(FetchLocationsHelper fetchLocationsHelper) {
        this.fetchLocationsHelper = fetchLocationsHelper;
    }

    public OpenmrsLocationService(String openmrsUrl, String user, String password) {
        super(openmrsUrl, user, password);
    }

    private String getURL(String url) throws IOException {
        return HttpUtils.getURL(url, OPENMRS_USER, OPENMRS_PWD);
    }

    /**
     * This method is used on OpenSRP web as a wrapper around implementation of fetching location by
     * level and tags
     *
     * @param uuid                unique id of the location
     * @param locationTopLevel    highest level of location hierarchy to begin querying from
     * @param locationTagsQueried OpenMRS tags to use when filtering the locations
     * @return List of locations matching the specified tags
     * @throws JSONException
     */
    public List<Location> getLocationsByLevelAndTags(String uuid, String locationTopLevel, JSONArray locationTagsQueried)
            throws JSONException {
        List<Location> allLocationsList = fetchLocationsHelper.getAllOpenMRSlocations();
        return getLocationsByLevelAndTagsFromAllLocationsList(uuid, allLocationsList, locationTopLevel, locationTagsQueried);
    }

    ;

    /**
     * This method is used to obtain locations within a hierarchy level by passing the following
     * parameters
     *
     * @param uuid                a uuid of a location within the hierarchy level, this is used for transversing to
     *                            the top location level stated below
     * @param allLocations        this is a list of all locations obtained from OpenMRS
     * @param locationTopLevel    this defines the top most level that locations to be querried from
     *                            e.g 1. for obtaining all locations within a district this value would contain the
     *                            tag name district locations 2. for obtaining all locations within a region this
     *                            value would contain the tag name for region locations
     * @param locationTagsQueried this defines the tags of all the locations to be returned e.g for
     *                            obtaining all villages this json array would contain the tag name for village
     *                            locations for villages and health facilities, this json array would contain both
     *                            tag names
     * @return returns a list of all locations matching the above criteria
     */
    public List<Location> getLocationsByLevelAndTagsFromAllLocationsList(String uuid, List<Location> allLocations,
                                                                         String locationTopLevel,
                                                                         JSONArray locationTagsQueried) {
        Location location = null;
        for (Location allLocation : allLocations) {
            if (allLocation.getLocationId().equals(uuid)) {
                location = allLocation;
                break;
            }
        }

        if (location == null) {
            return new ArrayList<>();
        }

        if (!location.getTags().contains(locationTopLevel)) {
            return getLocationsByLevelAndTagsFromAllLocationsList(location.getParentLocation().getLocationId(), allLocations,
                    locationTopLevel, locationTagsQueried);
        }

        if (location.getTags().contains(locationTopLevel)) {
            return getChildLocationsTreeByTagsAndParentLocationUUID(location.getLocationId(), allLocations,
                    locationTagsQueried);
        } else {
            return new ArrayList<>();
        }
    }

    private List<Location> getChildLocationsTreeByTagsAndParentLocationUUID(String parentUUID, List<Location> allLocations,
                                                                            JSONArray locationTagsQueried) {
        List<Location> obtainedLocations = new ArrayList<>();
        for (Location location : allLocations) {
            for (int i = 0; i < locationTagsQueried.length(); i++) {
                try {
                    if (location.getParentLocation() != null
                            && location.getParentLocation().getLocationId().equals(parentUUID)
                            && location.getTags().contains(locationTagsQueried.getString(i))) {
                        obtainedLocations.add(location);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (location.getParentLocation() != null && location.getParentLocation().getLocationId().equals(parentUUID)) {
                obtainedLocations.addAll(getChildLocationsTreeByTagsAndParentLocationUUID(location.getLocationId(),
                        allLocations, locationTagsQueried));
            }
        }
        return obtainedLocations;
    }

    public enum AllowedLevels {
        COUNTRY("Country"), PROVINCE("Province"), DISTRICT("District"), COUNTY("County"), SUB_COUNTY(
                "Sub-county"), HEALTH_FACILITY("Health Facility");

        private final String display;

        private AllowedLevels(final String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }

    }
}
