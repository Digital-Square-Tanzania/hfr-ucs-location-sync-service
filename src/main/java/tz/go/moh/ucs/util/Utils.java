package tz.go.moh.ucs.util;

import tz.go.moh.ucs.domain.Location;
import tz.go.moh.ucs.domain.LocationCSVRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import static tz.go.moh.ucs.Main.*;
import static tz.go.moh.ucs.util.CSVReaderUtil.readCsvFromResources;
import static tz.go.moh.ucs.util.CapitalizeUtil.capitalizeWords;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateChildLocationParent;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateLocationName;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    private static final Path HAMLET_UPDATE_LOG_PATH = Paths.get("src", "main", "resources", "hamlet_updates_log.json");
    private static final Path HAMLET_CREATION_LOG_PATH = Paths.get("src", "main", "resources", "hamlet_creations_log.json");
    private static final Set<String> PRIORITY_WARD_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "TZ.WT.KM.BW.6.16",
            "TZ.WT.KM.BW.6.17",
            "TZ.WT.KM.BW.6.18",
            "TZ.WT.KM.BW.6.19",
            "TZ.WT.KM.BW.6.20",
            "TZ.SH.MB.RG.5.15",
            "TZ.SH.MB.RG.5.16",
            "TZ.SH.MB.RG.5.17",
            "TZ.SH.MB.RG.5.18",
            "TZ.CL.DO.CM.7.21",
            "TZ.CL.DO.CM.7.22",
            "TZ.CL.DO.CM.7.23",
            "TZ.CL.DO.CM.7.24",
            "TZ.CL.DO.CM.7.25",
            "TZ.CL.DO.CM.7.26",
            "TZ.CL.DO.CM.7.27",
            "TZ.ET.PW.KH.7.12",
            "TZ.ET.PW.KH.7.13",
            "TZ.ET.PW.KH.7.14",
            "TZ.ET.PW.KH.7.15",
            "TZ.ST.LI.LI.6.30",
            "TZ.ST.LI.LI.6.31",
            "TZ.ST.LI.LI.6.32",
            "TZ.ST.LI.LI.2.32",
            "TZ.ST.LI.RU.5.25",
            "TZ.NT.TN.TN.4.26",
            "TZ.NT.TN.TN.4.27",
            "TZ.NT.TN.TN.4.28"
    )));

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
            boolean shouldLog = shouldLogHamletChange(tag);
            String previousName = existing.getName();
            Location previousParent = existing.getParentLocation();
            List<String> changeSummary = new ArrayList<>();
            // Check if the location already has the expected tag.
            if (existing.hasTag(tag)) {
                String existingTrimmedName = existing.getName() != null ? existing.getName().trim() : "";
                String desiredTrimmedName = name != null ? name.trim() : "";
                if (!existingTrimmedName.equals(desiredTrimmedName)) {
                    updateLocationName(existing, name);
                    existing.setName(name);
                    if (shouldLog) {
                        changeSummary.add(String.format("Name updated from '%s' to '%s'", previousName, name));
                    }
                }
                if (existing.getParentLocation() == null && parentLocation != null) {
                    LOGGER.warning("Location found by code " + code + " name = " + name + " but has no parent (expected: " + parentLocation.getLocationId() + " | " + parentLocation.getName() + "). Updating the location parent.");
                    updateChildLocationParent(existing, parentLocation.getLocationId());
                    existing.setParentLocation(parentLocation);
                    if (shouldLog) {
                        changeSummary.add(String.format("Parent set to '%s' (%s)", parentLocation.getName(), parentLocation.getLocationId()));
                    }
                } else if (existing.getParentLocation() != null && parentLocation != null && !existing.getParentLocation().getLocationId().equals(parentLocation.getLocationId())) {
                    LOGGER.warning("Location found by code " + code + " name = " + name + " but parent mismatch (expected: " + parentLocation.getLocationId() + " | " + parentLocation.getName() + "). Updating the location parent.");
                    updateChildLocationParent(existing, parentLocation.getLocationId());
                    if (shouldLog) {
                        changeSummary.add(String.format("Parent changed from '%s' (%s) to '%s' (%s)", previousParent != null ? previousParent.getName() : "", previousParent != null ? previousParent.getLocationId() : "", parentLocation.getName(), parentLocation.getLocationId()));
                    }
                    existing.setParentLocation(parentLocation);
                }
            } else {
                // Optionally, add the tag to the existing location if appropriate:
                // existing.getTags().add(tag);
                // return existing;
                // Or log a warning and continue to create a new location.
                LOGGER.warning("Location found by code " + code + " name = " + name + " but tag mismatch (expected: " + tag + "). Creating a new location.");
            }
            if (shouldLog && !changeSummary.isEmpty()) {
                logLocationUpdate(tag, code, previousName, existing.getName(), previousParent, parentLocation, changeSummary);
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
            if (shouldLogHamletCreation(tag)) {
                logHamletCreation(tag, code, newLoc, parentLocation);
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
            setHamletImportContext(true);
            setHamletImportResource(resourceName);
            try {
                for (LocationCSVRow row : rows) {
                    try {
                        Location wardLocation = resolveWardLocation(row);

                        Location villageLocation = null;
                        if (row.getVillageCode() != null && !row.getVillageCode().isEmpty()) {
                            villageLocation = findLocationByCode(row.getVillageCode());
                            if (wardLocation != null && row.getVillage() != null && !row.getVillage().isEmpty()) {
                                villageLocation = ensureLocationExists(
                                        wardLocation,
                                        buildVillageName(row),
                                        row.getVillageCode(),
                                        "Village");
                            } else if (wardLocation == null && villageLocation == null) {
                                LOGGER.warning("Ward not found when creating village for code " + row.getVillageCode());
                            }
                        }

                        if (row.getHamlet() != null && !row.getHamlet().isEmpty()) {
                            if (villageLocation == null && row.getVillageCode() != null && !row.getVillageCode().isEmpty()) {
                                villageLocation = findLocationByCode(row.getVillageCode());
                            }

                            if (villageLocation != null) {
                                ensureLocationExists(villageLocation, buildHamletName(row), row.getHamletCode(), "Hamlet");
                            } else {
                                LOGGER.info("Village not found for hamlet code " + row.getHamletCode());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error importing location row", e);
                    }
                }
            } finally {
                clearHamletImportContext();
                clearHamletImportResource();
            }
        }
    }

    private static Location resolveWardLocation(LocationCSVRow row) throws Exception {
        if (row.getWardCode() == null || row.getWardCode().isEmpty()) {
            return null;
        }

        String wardCode = row.getWardCode();
        if (shouldEnsureWard(wardCode)) {
            Location councilLocation = null;
            if (row.getCouncilCode() != null && !row.getCouncilCode().isEmpty()) {
                councilLocation = findLocationByCode(row.getCouncilCode());
            }

            if (councilLocation != null) {
                Location wardLocation = ensureLocationExists(councilLocation, buildWardName(row), wardCode, "Ward");
                return wardLocation != null ? wardLocation : findLocationByCode(wardCode);
            }

            LOGGER.warning("Council not found when creating ward for code " + wardCode);
        }

        return findLocationByCode(wardCode);
    }

    private static boolean shouldLogHamletChange(String tag) {
        return isHamletImportContext() && tag != null && ("Hamlet".equalsIgnoreCase(tag) || "Village".equalsIgnoreCase(tag));
    }

    private static boolean shouldLogHamletCreation(String tag) {
        return shouldLogHamletChange(tag);
    }

    private static boolean shouldEnsureWard(String wardCode) {
        if (wardCode == null) {
            return false;
        }
        return PRIORITY_WARD_CODES.contains(wardCode.trim());
    }

    private static String buildWardName(LocationCSVRow row) {
        return capitalizeWords(composeName(row.getWard(), row.getCouncil()));
    }

    private static String buildVillageName(LocationCSVRow row) {
        return capitalizeWords(composeName(row.getVillage(), row.getWard(), row.getCouncil()));
    }

    private static String buildHamletName(LocationCSVRow row) {
        return capitalizeWords(composeName(row.getHamlet(), row.getVillage(), row.getWard()));
    }

    private static String composeName(String... segments) {
        List<String> parts = new ArrayList<>();
        if (segments != null) {
            for (String segment : segments) {
                if (segment != null) {
                    String trimmed = segment.trim();
                    if (!trimmed.isEmpty()) {
                        parts.add(trimmed);
                    }
                }
            }
        }
        return String.join(" - ", parts);
    }

    private static void logLocationUpdate(String tag, String code, String previousName, String updatedName, Location previousParent, Location newParent, List<String> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        JSONObject entry = new JSONObject();
        entry.put("timestamp", Instant.now().toString());
        entry.put("tag", tag);
        entry.put("code", code);
        entry.put("previousName", previousName);
        entry.put("updatedName", updatedName);
        entry.put("previousParentUuid", previousParent != null ? previousParent.getLocationId() : JSONObject.NULL);
        entry.put("previousParentName", previousParent != null ? previousParent.getName() : JSONObject.NULL);
        entry.put("newParentUuid", newParent != null ? newParent.getLocationId() : JSONObject.NULL);
        entry.put("newParentName", newParent != null ? newParent.getName() : JSONObject.NULL);
        entry.put("source", getHamletImportResource() != null ? getHamletImportResource() : JSONObject.NULL);
        entry.put("changes", new JSONArray(changes));

        appendHamletUpdateLog(entry);
    }

    private static void logHamletCreation(String tag, String code, Location newLocation, Location parentLocation) {
        JSONObject entry = new JSONObject();
        entry.put("timestamp", Instant.now().toString());
        entry.put("tag", tag);
        entry.put("code", code);
        entry.put("name", newLocation != null ? newLocation.getName() : JSONObject.NULL);
        entry.put("uuid", newLocation != null ? newLocation.getLocationId() : JSONObject.NULL);
        Location parent = newLocation != null && newLocation.getParentLocation() != null ? newLocation.getParentLocation() : parentLocation;
        entry.put("parentUuid", parent != null ? parent.getLocationId() : JSONObject.NULL);
        entry.put("parentName", parent != null ? parent.getName() : JSONObject.NULL);
        entry.put("source", getHamletImportResource() != null ? getHamletImportResource() : JSONObject.NULL);

        appendHamletCreationLog(entry);
    }

    private static void appendHamletUpdateLog(JSONObject entry) {
        appendHamletLog(HAMLET_UPDATE_LOG_PATH, entry, "update");
    }

    private static void appendHamletCreationLog(JSONObject entry) {
        appendHamletLog(HAMLET_CREATION_LOG_PATH, entry, "creation");
    }

    private static synchronized void appendHamletLog(Path path, JSONObject entry, String logLabel) {
        try {
            JSONArray existingEntries = new JSONArray();
            if (Files.exists(path)) {
                String content = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    try {
                        existingEntries = new JSONArray(content);
                    } catch (Exception parseException) {
                        LOGGER.log(Level.WARNING, "Existing hamlet " + logLabel + " log is invalid. Resetting log file.", parseException);
                        existingEntries = new JSONArray();
                    }
                }
            }

            existingEntries.put(entry);
            Files.createDirectories(path.getParent());
            Files.writeString(path, existingEntries.toString(2), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write hamlet " + logLabel + " log", e);
        }
    }
}
