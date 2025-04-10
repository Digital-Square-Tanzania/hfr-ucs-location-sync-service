package tz.go.moh.ucs.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import tz.go.moh.ucs.domain.LocationCSVRow;

public class CSVReaderUtilTest {

    /*
     * NOTE: For these tests to run correctly, please ensure that the following CSV files
     * are available in your test resources directory (e.g., src/test/resources):
     *
     * 1. valid.csv
     * --------------------------
     * region_code,region,council_code,council,ward_code,ward,street_code,street,village_code,Village,hamlet_code,hamlet
     * 001,Region1,101,Council1,201,Ward1,301,Street1,401,Village1,501,Hamlet1
     * 002,Region2,102,Council2,202,Ward2,302,Street2,402,Village2,502,Hamlet2
     * --------------------------
     *
     * 2. mixed.csv
     * --------------------------
     * region_code,region,council_code,council,ward_code,ward,street_code,street,village_code,Village,hamlet_code,hamlet
     * 001,Region1,101,Council1,201,Ward1,301,Street1,401,Village1,501,Hamlet1
     * invalid,row,with,not,enough,columns
     * 002,Region2,102,Council2,202,Ward2,302,Street2,402,Village2,502,Hamlet2
     * --------------------------
     *
     * 3. whitespace.csv
     * --------------------------
     * region_code,region,council_code,council,ward_code,ward,street_code,street,village_code,Village,hamlet_code,hamlet
     *  001 ,  Region1 ,  101 ,  Council1 ,  201 ,  Ward1 ,  301 ,  Street1 ,  401 ,  Village1 ,  501 ,  Hamlet1
     * --------------------------
     */

    @Test
    public void testReadCsvValidFile() throws IOException {
        List<LocationCSVRow> rows = CSVReaderUtil.readCsvFromResources("valid.csv");
        assertNotNull(rows, "The returned list should not be null");
        assertEquals(2, rows.size(), "There should be exactly 2 valid rows");

        // Verify first row
        LocationCSVRow row1 = rows.get(0);
        assertEquals("001", row1.getRegionCode());
        assertEquals("Region1", row1.getRegion());
        assertEquals("101", row1.getCouncilCode());
        assertEquals("Council1", row1.getCouncil());
        assertEquals("201", row1.getWardCode());
        assertEquals("Ward1", row1.getWard());
        assertEquals("301", row1.getStreetCode());
        assertEquals("Street1", row1.getStreet());
        assertEquals("401", row1.getVillageCode());
        assertEquals("Village1", row1.getVillage());
        assertEquals("501", row1.getHamletCode());
        assertEquals("Hamlet1", row1.getHamlet());

        // Verify second row
        LocationCSVRow row2 = rows.get(1);
        assertEquals("002", row2.getRegionCode());
        assertEquals("Region2", row2.getRegion());
        assertEquals("102", row2.getCouncilCode());
        assertEquals("Council2", row2.getCouncil());
        assertEquals("202", row2.getWardCode());
        assertEquals("Ward2", row2.getWard());
        assertEquals("302", row2.getStreetCode());
        assertEquals("Street2", row2.getStreet());
        assertEquals("402", row2.getVillageCode());
        assertEquals("Village2", row2.getVillage());
        assertEquals("502", row2.getHamletCode());
        assertEquals("Hamlet2", row2.getHamlet());
    }

    @Test
    public void testResourceNotFound() {
        assertThrows(IOException.class, () -> {
            CSVReaderUtil.readCsvFromResources("nonexistent.csv");
        }, "Expected IOException when resource is not found");
    }

    @Test
    public void testInvalidRowsAreSkipped() throws IOException {
        List<LocationCSVRow> rows = CSVReaderUtil.readCsvFromResources("mixed.csv");
        assertNotNull(rows, "The returned list should not be null");
        assertEquals(2, rows.size(), "Invalid rows should be skipped");
    }

    @Test
    public void testTrimmedValues() throws IOException {
        List<LocationCSVRow> rows = CSVReaderUtil.readCsvFromResources("whitespace.csv");
        assertNotNull(rows, "The returned list should not be null");
        assertFalse(rows.isEmpty(), "There should be at least one valid row");

        LocationCSVRow row = rows.get(0);
        assertEquals("001", row.getRegionCode());
        assertEquals("Region1", row.getRegion());
        assertEquals("101", row.getCouncilCode());
        assertEquals("Council1", row.getCouncil());
        assertEquals("201", row.getWardCode());
        assertEquals("Ward1", row.getWard());
        assertEquals("301", row.getStreetCode());
        assertEquals("Street1", row.getStreet());
        assertEquals("401", row.getVillageCode());
        assertEquals("Village1", row.getVillage());
        assertEquals("501", row.getHamletCode());
        assertEquals("Hamlet1", row.getHamlet());
    }
}