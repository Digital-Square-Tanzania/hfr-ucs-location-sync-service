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

    private static String[] splitCsvRow(String line) {
        if (line == null) {
            return new String[0];
        }
        String sanitizedLine = line.replace("\uFEFF", "").replace("\r", "");
        return sanitizedLine.split(",", -1);
    }

    private static String getValue(String[] values, Map<String, Integer> headerIndex, String... keys) {
        for (String key : keys) {
            Integer index = headerIndex.get(key);
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
        return header == null ? "" : header.trim().toLowerCase();
    }
}
