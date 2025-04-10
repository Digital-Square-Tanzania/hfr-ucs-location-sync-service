package tz.go.moh.ucs.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class LocationCSVRowTest {

    @Test
    public void testGettersAndSetters() {
        LocationCSVRow row = new LocationCSVRow();
        
        String region = "Region1";
        String regionCode = "R001";
        String council = "Council1";
        String councilCode = "C001";
        String ward = "Ward1";
        String wardCode = "W001";
        String street = "Street1";
        String streetCode = "S001";
        String village = "Village1";
        String villageCode = "V001";
        String hamlet = "Hamlet1";
        String hamletCode = "H001";
        
        row.setRegion(region);
        row.setRegionCode(regionCode);
        row.setCouncil(council);
        row.setCouncilCode(councilCode);
        row.setWard(ward);
        row.setWardCode(wardCode);
        row.setStreet(street);
        row.setStreetCode(streetCode);
        row.setVillage(village);
        row.setVillageCode(villageCode);
        row.setHamlet(hamlet);
        row.setHamletCode(hamletCode);
        
        assertEquals(region, row.getRegion(), "Region should match");
        assertEquals(regionCode, row.getRegionCode(), "Region code should match");
        assertEquals(council, row.getCouncil(), "Council should match");
        assertEquals(councilCode, row.getCouncilCode(), "Council code should match");
        assertEquals(ward, row.getWard(), "Ward should match");
        assertEquals(wardCode, row.getWardCode(), "Ward code should match");
        assertEquals(street, row.getStreet(), "Street should match");
        assertEquals(streetCode, row.getStreetCode(), "Street code should match");
        assertEquals(village, row.getVillage(), "Village should match");
        assertEquals(villageCode, row.getVillageCode(), "Village code should match");
        assertEquals(hamlet, row.getHamlet(), "Hamlet should match");
        assertEquals(hamletCode, row.getHamletCode(), "Hamlet code should match");
    }

    // Additional tests can be added as needed
}
