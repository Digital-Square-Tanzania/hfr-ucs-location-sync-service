package tz.go.moh.ucs.util;

import tz.go.moh.ucs.domain.HfrCodeCorrectionRow;
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
import static tz.go.moh.ucs.util.CSVReaderUtil.readHfrCodeCorrectionsFromResources;
import static tz.go.moh.ucs.util.CapitalizeUtil.capitalizeWords;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.addLocationAttribute;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.getLocationAttributeUuid;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateChildLocationParent;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateLocationAttribute;
import static tz.go.moh.ucs.util.OpenMrsCallsUtils.updateLocationName;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    private static final Path HAMLET_UPDATE_LOG_PATH = Paths.get("src", "main", "resources", "hamlet_updates_log.json");
    private static final Path HAMLET_CREATION_LOG_PATH = Paths.get("src", "main", "resources", "hamlet_creations_log.json");
    private static final Path HAMLET_CODE_SUCCESS_LOG_PATH = Paths.get("src", "main", "resources", "hamlets_updates_success.csv");
    private static final Path HAMLET_CODE_FAILED_LOG_PATH = Paths.get("src", "main", "resources", "hamlets_updates_failed.csv");
    private static final Path HAMLET_CODE_MISSING_LOG_PATH = Paths.get("src", "main", "resources", "hamlets_updates_missing.csv");
    private static final Path HAMLET_CODE_DUPLICATE_LOG_PATH = Paths.get("src", "main", "resources", "hamlets_updates_duplicates.csv");
    private static final Path RECEIVED_LOCATIONS_LOG_PATH = Paths.get("src", "main", "resources", "received_locations.json");
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
                        String hamletCode = trimToNull(row.getHamletCode());
                        String hamlet = trimToNull(row.getHamlet());
                        if (hamletCode == null || hamlet == null) {
                            LOGGER.warning("Missing hamlet code or name in CSV row");
                            continue;
                        }

                        if (findLocationByCode(hamletCode) != null) {
                            LOGGER.info("Hamlet already exists for code " + hamletCode + ". Skipping import.");
                            continue;
                        }

                        String villageCode = trimToNull(row.getVillageCode());
                        if (villageCode == null) {
                            LOGGER.info("Village code missing for hamlet code " + hamletCode);
                            continue;
                        }

                        Location villageLocation = findLocationByCode(villageCode);
                        if (villageLocation != null) {
                            ensureLocationExists(villageLocation, buildHamletName(row), hamletCode, "Hamlet");
                        } else {
                            LOGGER.info("Village not found for hamlet code " + hamletCode);
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

    /**
     * Applies HFR code corrections from the hamlet list CSV/TSV.
     */
    public static void applyHamletCodeCorrectionsFromCSV(String resourceName) {
        List<HfrCodeCorrectionRow> rows = null;
        try {
            rows = readHfrCodeCorrectionsFromResources(resourceName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading HFR correction CSV file", e);
        }

        if (rows == null || rows.isEmpty()) {
            LOGGER.info("No HFR code corrections found in " + resourceName);
            return;
        }

        for (HfrCodeCorrectionRow row : rows) {
            try {
                processHfrCodeCorrection(row);
            } catch (Exception e) {
                logHamletCodeFailure(row, "Exception updating HFR code", e);
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

    private static void processHfrCodeCorrection(HfrCodeCorrectionRow row) throws Exception {
        String incorrectCode = trimToNull(row.getIncorrectHfrCode());
        String correctCode = trimToNull(row.getCorrectHfrCode());

        if (incorrectCode == null || correctCode == null) {
            String message = "Missing incorrect or correct HFR code";
            LOGGER.warning(message);
            logHamletCodeFailure(row, message, null);
            return;
        }

        Location incorrectLocation = findLocationByCode(incorrectCode);
        Location correctLocation = findLocationByCode(correctCode);

        if (incorrectLocation != null && correctLocation != null && !isSameLocation(incorrectLocation, correctLocation)) {
            logHamletCodeDuplicate(row, incorrectLocation, correctLocation,
                    "Duplicate locations found for incorrect and correct codes");
            return;
        }

        Location location = incorrectLocation != null ? incorrectLocation : correctLocation;
        boolean foundByIncorrect = incorrectLocation != null;
        boolean alreadyCorrect = false;
        if (location == null) {
            LOGGER.warning("Location not found for incorrect code " + incorrectCode);
            logHamletCodeMissing(row, "Location not found for incorrect code");
            return;
        }

        if (correctLocation != null && isSameLocation(location, correctLocation)) {
            alreadyCorrect = true;
        }

        if (!alreadyCorrect) {
            String existingCode = resolveLocationCode(location);
            if (existingCode != null && existingCode.equalsIgnoreCase(correctCode)) {
                alreadyCorrect = true;
            }
        }

        String parentCode = deriveParentCode(correctCode);
        Location parentLocation = parentCode != null ? findLocationByCode(parentCode) : null;
        String parentWarning = null;

        if (!alreadyCorrect) {
            updateLocationCodeAttribute(location, incorrectCode, correctCode);
        } else if (foundByIncorrect) {
            updateCodeCache(location, incorrectCode, correctCode);
        }

        if (parentCode != null) {
            if (parentLocation != null) {
                if (location.getParentLocation() == null ||
                        !parentLocation.getLocationId().equals(location.getParentLocation().getLocationId())) {
                    updateChildLocationParent(location, parentLocation.getLocationId());
                    location.setParentLocation(parentLocation);
                }
            } else {
                parentWarning = "Parent location not found for code " + parentCode;
                LOGGER.warning(parentWarning + " (child code " + correctCode + ")");
            }
        }

        String message = alreadyCorrect ? "Location already updated" : "Location updated";
        if (parentWarning != null) {
            message = message + "; " + parentWarning;
        }
        logHamletCodeSuccess(row, location, parentCode, parentLocation, message);
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

    private static boolean isSameLocation(Location left, Location right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        String leftId = left.getLocationId();
        String rightId = right.getLocationId();
        return leftId != null && rightId != null && leftId.equalsIgnoreCase(rightId);
    }

    private static String resolveLocationCode(Location location) {
        if (location == null || location.getAttributes() == null) {
            return null;
        }
        String code = location.getAttributes().get("Code");
        if (code == null) {
            code = location.getAttributes().get("HFR Code");
        }
        return trimToNull(code);
    }

    private static boolean usesHfrCodeAttribute(Location location) {
        if (location == null) {
            return false;
        }
        if (location.getTags() != null && location.getTags().contains("Facility")) {
            return true;
        }
        return location.getAttributes() != null && location.getAttributes().get("HFR Code") != null;
    }

    private static void updateLocationCodeAttribute(Location location, String previousCode, String correctCode) {
        if (location == null) {
            return;
        }
        boolean hfrAttribute = usesHfrCodeAttribute(location);
        String attributeKey = hfrAttribute ? "HFR Code" : "Code";
        String attributeTypeUuid = hfrAttribute ? HFR_CODE_LOCATION_ATTRIBUTE_UUID : CODE_LOCATION_ATTRIBUTE_UUID;

        String attributeUuid = getLocationAttributeUuid(location.getLocationId(), attributeTypeUuid);
        if (attributeUuid != null) {
            updateLocationAttribute(location.getLocationId(), attributeUuid, attributeTypeUuid, correctCode);
        } else {
            addLocationAttribute(location, attributeTypeUuid, correctCode);
        }

        if (location.getAttributes() == null) {
            location.setAttributes(new HashMap<>());
        }
        location.getAttributes().put(attributeKey, correctCode);
        updateCodeCache(location, previousCode, correctCode);
    }

    private static void updateCodeCache(Location location, String previousCode, String newCode) {
        if (previousCode != null) {
            Location existing = codeCache.get(previousCode.toLowerCase());
            if (existing == location) {
                codeCache.remove(previousCode.toLowerCase());
            }
        }
        if (newCode != null) {
            codeCache.put(newCode.toLowerCase(), location);
        }
    }

    private static String deriveParentCode(String correctCode) {
        if (correctCode == null) {
            return null;
        }
        int lastDot = correctCode.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        return correctCode.substring(0, lastDot);
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

    private static void logHamletCodeSuccess(HfrCodeCorrectionRow row, Location location, String parentCode, Location parentLocation, String message) {
        String line = buildHamletCodeCsvLine(row, location, parentCode, parentLocation, message, null);
        appendHamletCodeCsv(HAMLET_CODE_SUCCESS_LOG_PATH, line);
    }

    private static void logHamletCodeFailure(HfrCodeCorrectionRow row, String message, Exception exception) {
        String exceptionName = exception != null ? exception.getClass().getSimpleName() : null;
        String line = buildHamletCodeCsvLine(row, null, null, null, message, exceptionName);
        appendHamletCodeCsv(HAMLET_CODE_FAILED_LOG_PATH, line);
    }

    private static void logHamletCodeMissing(HfrCodeCorrectionRow row, String message) {
        String line = buildHamletCodeCsvLine(row, null, null, null, message, null);
        appendHamletCodeCsv(HAMLET_CODE_MISSING_LOG_PATH, line);
    }

    private static void logHamletCodeDuplicate(HfrCodeCorrectionRow row, Location incorrectLocation, Location correctLocation, String message) {
        String timestamp = Instant.now().toString();
        String line = String.join(",",
                csvEscape(timestamp),
                csvEscape(row != null ? row.getIncorrectHfrCode() : null),
                csvEscape(row != null ? row.getCorrectHfrCode() : null),
                csvEscape(incorrectLocation != null ? incorrectLocation.getLocationId() : null),
                csvEscape(correctLocation != null ? correctLocation.getLocationId() : null),
                csvEscape(message),
                csvEscape(row != null ? row.getLocationName() : null),
                csvEscape(row != null ? row.getLocationType() : null),
                csvEscape(row != null ? row.getUid() : null)
        );
        appendHamletDuplicateCsv(HAMLET_CODE_DUPLICATE_LOG_PATH, line);
    }


    private static void appendHamletUpdateLog(JSONObject entry) {
        appendHamletLog(HAMLET_UPDATE_LOG_PATH, entry, "update");
    }

    private static void appendHamletCreationLog(JSONObject entry) {
        appendHamletLog(HAMLET_CREATION_LOG_PATH, entry, "creation");
    }

    private static String buildHamletCodeCsvLine(HfrCodeCorrectionRow row, Location location, String parentCode, Location parentLocation, String message, String exceptionName) {
        return String.join(",",
                csvEscape(Instant.now().toString()),
                csvEscape(row != null ? row.getIncorrectHfrCode() : null),
                csvEscape(row != null ? row.getCorrectHfrCode() : null),
                csvEscape(location != null ? location.getLocationId() : null),
                csvEscape(parentCode),
                csvEscape(parentLocation != null ? parentLocation.getLocationId() : null),
                csvEscape(message),
                csvEscape(row != null ? row.getLocationName() : null),
                csvEscape(row != null ? row.getLocationType() : null),
                csvEscape(row != null ? row.getUid() : null),
                csvEscape(exceptionName)
        );
    }

    private static synchronized void appendHamletCodeCsv(Path path, String line) {
        String header = String.join(",",
                "timestamp",
                "incorrectCode",
                "correctCode",
                "locationUuid",
                "parentCode",
                "parentUuid",
                "message",
                "locationName",
                "locationType",
                "uid",
                "exception");
        appendHamletCsv(path, header, line, "hamlet code");
    }

    private static synchronized void appendHamletDuplicateCsv(Path path, String line) {
        String header = String.join(",",
                "timestamp",
                "incorrectCode",
                "correctCode",
                "incorrectUuid",
                "correctUuid",
                "message",
                "locationName",
                "locationType",
                "uid");
        appendHamletCsv(path, header, line, "hamlet duplicate");
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String csvEscape(String value) {
        String safe = value == null ? "" : value;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public static void storeReceivedAdminHierarchy(JSONObject facilityJson) {
        if (facilityJson == null) {
            return;
        }
        JSONObject entry = new JSONObject(facilityJson.toString());
        appendReceivedLocationsLog(RECEIVED_LOCATIONS_LOG_PATH, entry);
    }

    private static synchronized void appendHamletCsv(Path path, String header, String line, String logLabel) {
        try {
            Files.createDirectories(path.getParent());
            boolean needsHeader = !Files.exists(path) || Files.size(path) == 0;
            if (needsHeader) {
                Files.writeString(path, header + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            Files.writeString(path, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write " + logLabel + " log", e);
        }
    }

    private static synchronized void appendReceivedLocationsLog(Path path, JSONObject entry) {
        try {
            JSONArray existingEntries = new JSONArray();
            if (Files.exists(path)) {
                String content = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    try {
                        existingEntries = new JSONArray(content);
                    } catch (Exception parseException) {
                        LOGGER.log(Level.WARNING, "Existing received locations log is invalid. Resetting log file.", parseException);
                        existingEntries = new JSONArray();
                    }
                }
            }

            existingEntries.put(entry);
            Files.createDirectories(path.getParent());
            Files.writeString(path, existingEntries.toString(2), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write received locations log", e);
        }
    }
}
