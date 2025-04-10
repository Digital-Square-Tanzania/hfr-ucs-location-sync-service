package tz.go.moh.ucs.util;

import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.domain.LocationCSVRow;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tz.go.moh.ucs.Main.*;
import static tz.go.moh.ucs.util.CSVReaderUtil.readCsvFromResources;
import static tz.go.moh.ucs.util.CapitalizeUtil.capitalizeWords;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateChildLocationParent;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateLocationName;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    /**
     * Ensures a location exists; updates its name if needed or creates a new one.
     */
    public static Location ensureLocationExists(Location parentLocation, String name, String code, String tag) throws Exception {
        if (code == null || code.isEmpty()) {
            return null;
        }

        // First, try to find an existing location by its unique code.
        Location existing = findLocationByCode(code);
        if (existing != null) {
            // Check if the location already has the expected tag.
            if (existing.hasTag(tag)) {
                if (!existing.getName().trim().equals(name.trim())) {
                    updateLocationName(existing, name);
                }
                if (existing.getParentLocation() == null && parentLocation != null) {
                    LOGGER.warning("Location found by code " + code + " name = " + name + " but has no parent (expected: " + parentLocation.getLocationId() + " | " + parentLocation.getName() + "). Updating the location parent.");
                    updateChildLocationParent(existing, parentLocation.getLocationId());
                } else if (existing.getParentLocation() != null && parentLocation != null && !existing.getParentLocation().getLocationId().equals(parentLocation.getLocationId())) {
                    LOGGER.warning("Location found by code " + code + " name = " + name + " but parent mismatch (expected: " + parentLocation.getLocationId() + " | " + parentLocation.getName() + "). Updating the location parent.");
                    updateChildLocationParent(existing, parentLocation.getLocationId());
                }
            } else {
                // Optionally, add the tag to the existing location if appropriate:
                // existing.getTags().add(tag);
                // return existing;
                // Or log a warning and continue to create a new location.
                LOGGER.warning("Location found by code " + code + " name = " + name + " but tag mismatch (expected: " + tag + "). Creating a new location.");
            }
            return existing;
        }

        if (parentLocation == null && !tag.equalsIgnoreCase("Region")) {
            LOGGER.warning("Location Parent is NULL");
            return null;
        }

        // No matching location was found; create a new location.
        Map<String, String> attributes = new HashMap<>();

        if (tag.equalsIgnoreCase("facility"))
            attributes.put(HFR_CODE_LOCATION_ATTRIBUTE_UUID, code);
        else
            attributes.put(CODE_LOCATION_ATTRIBUTE_UUID, code);

        Set<String> tags = new HashSet<>(Collections.singletonList(tag));
        String parentUuid = (parentLocation != null) ? parentLocation.getLocationId() : null;
        Location newLoc = createNewLocation(name, parentUuid, tags, attributes);

        if (newLoc != null) {
            LOGGER.info("Created new " + tag + ": " + name);
            allLocations.add(newLoc);
            if (newLoc.getLocationId() != null) {
                locationCache.put(newLoc.getLocationId().toLowerCase(), newLoc);
            }
            if (newLoc.getAttributes() != null && newLoc.getAttributes().get("Code") != null) {
                codeCache.put(((String) newLoc.getAttributes().get("Code")).toLowerCase(), newLoc);
            } else if (newLoc.getAttributes() != null && newLoc.getAttributes().get("HFR Code") != null) {
                codeCache.put(((String) newLoc.getAttributes().get("HFR Code")).toLowerCase(), newLoc);
            }
        } else {
            LOGGER.info("Failed Creating new " + tag + ": " + name);
        }
        return newLoc;
    }


    public static Location findLocationByCode(String code) {
        if (code == null) return null;
        return codeCache.get(code.toLowerCase());
    }


    public static Location findLocationByUuid(String uuid) {
        if (uuid == null) return null;
        return locationCache.get(uuid.toLowerCase());
    }

    /**
     * Imports hamlet locations from a CSV file.
     */
    public static void importHamletLocationsFromCSV(String resourceName) {
        List<LocationCSVRow> rows = null;
        try {
            rows = readCsvFromResources(resourceName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading Hamlets CSV file", e);
        }
        if (rows != null) {
            for (LocationCSVRow row : rows) {
                try {
                    Location villageLocation = null;
                    if (row.getVillageCode() != null && !row.getVillageCode().isEmpty()) {
                        villageLocation = findLocationByCode(row.getVillageCode());
                    }

                    if (villageLocation != null && row.getHamlet() != null && !row.getHamlet().isEmpty()) {
                        ensureLocationExists(villageLocation, capitalizeWords(row.getHamlet() + " - " + row.getVillage() + " - " + row.getWard()), row.getHamletCode(), "Hamlet");
                    } else {
                        LOGGER.info("Village not found");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error importing location row", e);
                }
            }
        }
    }
}
