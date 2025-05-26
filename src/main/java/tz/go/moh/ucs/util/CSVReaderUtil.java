package tz.go.moh.ucs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import tz.go.moh.ucs.domain.LocationCSVRow;

public class CSVReaderUtil {

    /**
     * Reads a CSV file from the resources folder with the columns:
     * region_code,region,council_code,council,ward_code,ward,street_code,street,village_code,Village,hamlet_code,hamlet
     * and maps each row to a LocationCSVRow.
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

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                boolean isFirstLine = true;

                while ((line = br.readLine()) != null) {
                    // Skip header row
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Split the CSV row into columns
                    String[] values = line.split(",");
                    if (values.length < 9) {
                        // Optionally, log or handle invalid rows
                        continue;
                    }

                    LocationCSVRow row = new LocationCSVRow();
                    // Map CSV columns to LocationCSVRow fields
                    row.setRegionCode(values[0].trim());
                    row.setRegion(values[1].trim());
                    row.setCouncilCode(values[2].trim());
                    row.setCouncil(values[3].trim());
                    row.setWardCode(values[4].trim());
                    row.setWard(values[5].trim());
                    row.setVillageCode(values[6].trim());
                    row.setVillage(values[7].trim());
                    row.setHamletCode(values[8].trim());
                    row.setHamlet(values[9].trim());

                    locations.add(row);
                }
            }
        }

        return locations;
    }
}
