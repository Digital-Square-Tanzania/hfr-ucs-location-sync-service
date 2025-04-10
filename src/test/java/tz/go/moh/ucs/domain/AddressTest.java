package tz.go.moh.ucs.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddressTest {

    @Test
    public void testDefaultConstructorAndSetters() {
        Address address = new Address();
        address.setAddressType("Home");
        Date startDate = new Date();
        address.setStartDate(startDate);
        Date endDate = new Date(startDate.getTime() + 86400000L); // 1 day later
        address.setEndDate(endDate);
        Map<String, String> fields = new HashMap<>();
        fields.put("City", "Dar es Salaam");
        address.setAddressFields(fields);
        address.setLatitude("6.7924");
        address.setLongitute("39.2083");
        address.setPostalCode("12345");
        address.setState("State");
        address.setCountry("Tanzania");

        assertEquals("Home", address.getAddressType());
        assertEquals(startDate, address.getStartDate());
        assertEquals(endDate, address.getEndDate());
        assertEquals("Dar es Salaam", address.getAddressField("City"));
        assertEquals("6.7924", address.getLatitude());
        assertEquals("39.2083", address.getLongitute());
        assertEquals("12345", address.getPostalCode());
        assertEquals("State", address.getState());
        assertEquals("Tanzania", address.getCountry());
    }

    @Test
    public void testParameterizedConstructor() {
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000L);
        Map<String, String> fields = new HashMap<>();
        fields.put("City", "Arusha");

        Address address = new Address("Office", startDate, endDate, fields, "10.1234", "20.5678", "54321", "AnotherState", "Kenya");

        assertEquals("Office", address.getAddressType());
        assertEquals(startDate, address.getStartDate());
        assertEquals(endDate, address.getEndDate());
        assertEquals("Arusha", address.getAddressField("City"));
        assertEquals("10.1234", address.getLatitude());
        assertEquals("20.5678", address.getLongitute());
        assertEquals("54321", address.getPostalCode());
        assertEquals("AnotherState", address.getState());
        assertEquals("Kenya", address.getCountry());
    }

    @Test
    public void testAddAndGetAddressField() {
        Address address = new Address();
        address.addAddressField("Street", "Main Street");

        // Check case-insensitive lookup
        assertEquals("Main Street", address.getAddressField("street"));
        
        // Test getAddressFieldMatchingRegex: regex matching keys starting with "Str"
        assertEquals("Main Street", address.getAddressFieldMatchingRegex("Str.*"));
    }

    @Test
    public void testAddAndGetAddressFieldEnum() {
        Address address = new Address();
        // Use AddressField enum; assuming enum values exist such as STREET
        address.addAddressField(AddressField.STREET, "Broadway");
        
        // Check retrieval using enum and string lookup
        assertEquals("Broadway", address.getAddressField(AddressField.STREET));
        assertEquals("Broadway", address.getAddressField("street"));
    }

    @Test
    public void testRemoveAddressFieldByEnum() {
        Address address = new Address();
        address.addAddressField(AddressField.REGION, "Mwanza");
        
        // Ensure field exists
        assertEquals("Mwanza", address.getAddressField(AddressField.REGION));

        // Remove field using enum-based removal
        address.removeAddressField(AddressField.REGION);
        
        // Verify the field has been removed
        assertNull(address.getAddressField(AddressField.REGION));
    }

    @Test
    public void testRemoveAddressFieldByString() {
        Address address = new Address();
        address.addAddressField("PostalCode", "98765");
        
        // Ensure field exists
        assertEquals("98765", address.getAddressField("PostalCode"));
        
        // Remove field by string key
        address.removeAddressField("PostalCode");
        
        // Verify removal
        assertNull(address.getAddressField("PostalCode"));
    }

    @Test
    public void testIsActive() {
        Address address = new Address();

        // When endDate is null, address is considered active
        address.setEndDate(null);
        assertTrue(address.isActive());
        
        // When endDate is in the future, address is active
        Date now = new Date();
        Date future = new Date(now.getTime() + 86400000L);
        address.setEndDate(future);
        assertTrue(address.isActive());
        
        // When endDate is in the past, address is inactive
        Date past = new Date(now.getTime() - 86400000L);
        address.setEndDate(past);
        assertFalse(address.isActive());
    }

    @Test
    public void testDurationCalculations() {
        Address address = new Address();
        
        // When startDate is not set, duration methods should return -1
        assertEquals(-1, address.durationInDays());
        
        // Set a known start and end date: one day apart
        Date start = new Date(0);
        Date end = new Date(86400000L);
        address.setStartDate(start);
        address.setEndDate(end);
        
        // Duration in days should be 1
        assertEquals(1, address.durationInDays());
        // Duration in weeks should be 0 (1 day / 7)
        assertEquals(0, address.durationInWeeks());
        // Duration in months should be 0 (1 day / 30)
        assertEquals(0, address.durationInMonths());
        // Duration in years should be 0 (1 day / 365)
        assertEquals(0, address.durationInYears());
    }

    @Test
    public void testChainMethods() {
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000L);
        Address address = new Address()
                .withAddressType("Billing")
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withLatitude("15.0000")
                .withLongitute("35.0000")
                .withPostalCode("11111")
                .withState("SomeState")
                .withCountry("SomeCountry")
                .withAddressField("Building", "Tower");
        
        assertEquals("Billing", address.getAddressType());
        assertEquals(startDate, address.getStartDate());
        assertEquals(endDate, address.getEndDate());
        assertEquals("15.0000", address.getLatitude());
        assertEquals("35.0000", address.getLongitute());
        assertEquals("11111", address.getPostalCode());
        assertEquals("SomeState", address.getState());
        assertEquals("SomeCountry", address.getCountry());
        assertEquals("Tower", address.getAddressField("Building"));
    }

    @Test
    public void testToString() {
        Address address = new Address();
        address.setAddressType("TestType");
        String toStringResult = address.toString();
        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("TestType"));
    }
}