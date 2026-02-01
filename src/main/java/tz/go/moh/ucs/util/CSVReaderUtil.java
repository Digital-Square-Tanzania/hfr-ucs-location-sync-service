package tz.go.moh.ucs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tz.go.moh.ucs.domain.HfrCodeCorrectionRow;
import tz.go.moh.ucs.domain.LocationCSVRow;

public class CSVReaderUtil {

    /**
     * Reads a CSV file from the resources folder and maps each row to a LocationCSVRow by
     * aligning column headers (case-insensitive) with known location attributes. Supports
     * both the legacy location exports and the newer hamlet list format.
     *
     * @param resourceName the resource name, e.g. "data.csv"
     * @return a list of LocationCSVRow objects
     * @throws IOException if an I/O error occurs
     */
    public static List<LocationCSVRow> readCsvFromResources(String resourceName) throws IOException {
        List<LocationCSVRow> locations = new ArrayList<>();

        // Load the resource from the classpath
        try (InputStream is = CSVReaderUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String headerLine = br.readLine();
                if (headerLine == null) {
                    return locations;
                }

                Map<String, Integer> headerIndex = buildHeaderIndex(headerLine);
                String line;

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] values = splitCsvRow(line);
                    if (values.length == 0) {
                        continue;
                    }

                    LocationCSVRow row = buildLocationRow(values, headerIndex);
                    if (row != null) {
                        locations.add(row);
                    }
                }
            }
        }

        return locations;
    }

    /**
     * Reads a correction CSV/TSV file from the resources folder and maps each row to a HfrCodeCorrectionRow.
     *
     * @param resourceName the resource name, e.g. "hamlets_list.csv"
     * @return a list of HfrCodeCorrectionRow objects
     * @throws IOException if an I/O error occurs
     */
    public static List<HfrCodeCorrectionRow> readHfrCodeCorrectionsFromResources(String resourceName) throws IOException {
        List<HfrCodeCorrectionRow> rows = new ArrayList<>();

        try (InputStream is = CSVReaderUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String headerLine = br.readLine();
                if (headerLine == null) {
                    return rows;
                }

                Map<String, Integer> headerIndex = buildHeaderIndex(headerLine);
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] values = splitCsvRow(line);
                    if (values.length == 0) {
                        continue;
                    }

                    HfrCodeCorrectionRow row = buildCorrectionRow(values, headerIndex);
                    if (row != null) {
                        rows.add(row);
                    }
                }
            }
        }

        return rows;
    }

    private static Map<String, Integer> buildHeaderIndex(String headerLine) {
        Map<String, Integer> headerIndex = new HashMap<>();
        String[] headers = splitCsvRow(headerLine);
        for (int i = 0; i < headers.length; i++) {
            String key = normalizeHeader(headers[i]);
            if (!key.isEmpty()) {
                headerIndex.putIfAbsent(key, i);
            }
        }
        return headerIndex;
    }

    private static LocationCSVRow buildLocationRow(String[] values, Map<String, Integer> headerIndex) {
        String hamletCode = getValue(values, headerIndex, "hamlet_code");
        String hamlet = getValue(values, headerIndex, "hamlet");
        if (isBlank(hamlet) || isBlank(hamletCode)) {
            return null;
        }

        LocationCSVRow row = new LocationCSVRow();
        row.setRegionCode(getValue(values, headerIndex, "region_code"));
        row.setRegion(getValue(values, headerIndex, "region"));
        row.setCouncilCode(getValue(values, headerIndex, "council_code"));
        row.setCouncil(getValue(values, headerIndex, "council"));
        row.setWardCode(getValue(values, headerIndex, "ward_code"));
        row.setWard(getValue(values, headerIndex, "ward"));
        row.setStreetCode(getValue(values, headerIndex, "street_code"));
        row.setStreet(getValue(values, headerIndex, "street"));
        row.setVillageCode(getValue(values, headerIndex, "village_code"));
        row.setVillage(getValue(values, headerIndex, "village"));
        row.setHamletCode(trimToNull(hamletCode));
        row.setHamlet(trimToNull(hamlet));
        return row;
    }

    private static HfrCodeCorrectionRow buildCorrectionRow(String[] values, Map<String, Integer> headerIndex) {
        String incorrectCode = getValue(values, headerIndex, "incorrect hfr code", "incorrect_hfr_code", "incorrect code", "incorrect_code");
        String correctCode = getValue(values, headerIndex, "correct hfr code", "correct_hfr_code", "correct code", "correct_code");
        String name = getValue(values, headerIndex, "wards/villlages/hamlet", "wards/villages/hamlet", "ward/village/hamlet", "name");
        String locationType = getValue(values, headerIndex, "location type (ward, village, hamlet)", "location type", "location_type");
        String uid = getValue(values, headerIndex, "uid", "uuid");

        if (isBlank(incorrectCode) && isBlank(correctCode) && isBlank(name) && isBlank(uid)) {
            return null;
        }

        HfrCodeCorrectionRow row = new HfrCodeCorrectionRow();
        row.setIncorrectHfrCode(trimToNull(incorrectCode));
        row.setCorrectHfrCode(trimToNull(correctCode));
        row.setLocationName(trimToNull(name));
        row.setLocationType(trimToNull(locationType));
        row.setUid(trimToNull(uid));
        return row;
    }

    private static String[] splitCsvRow(String line) {
        if (line == null) {
            return new String[0];
        }
        String sanitizedLine = line.replace("\uFEFF", "").replace("\r", "");
        String delimiter = sanitizedLine.contains("\t") ? "\t" : ",";
        return sanitizedLine.split(delimiter, -1);
    }

    private static String getValue(String[] values, Map<String, Integer> headerIndex, String... keys) {
        for (String key : keys) {
            String normalizedKey = normalizeHeader(key);
            Integer index = headerIndex.get(normalizedKey);
            if (index != null && index < values.length) {
                return trimToNull(values[index]);
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String normalized = header.trim().toLowerCase();
        normalized = normalized.replace("\uFEFF", "");
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized;
    }
}
