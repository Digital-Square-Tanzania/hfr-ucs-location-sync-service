package tz.go.moh.ucs.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocationTest {

    @Test
    public void testDefaultConstructorAndSetters() {
        Location location = new Location();
        String locationId = "loc-001";
        String name = "Test Location";
        location.setLocationId(locationId);
        location.setName(name);

        // Create a dummy Address object (assumed to be correctly implemented)
        Address address = new Address();
        address.setAddressType("Test Address");
        location.setAddress(address);

        // Set identifiers
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put("code", "ABC123");
        location.setIdentifiers(identifiers);

        // Set parent location
        Location parent = new Location();
        parent.setLocationId("loc-000");
        location.setParentLocation(parent);

        // Set tags
        Set<String> tags = new HashSet<>();
        tags.add("Urban");
        location.setTags(tags);

        // Set attributes
        Map<String, String> attributes = new HashMap<>();
        attributes.put("category", "Commercial");
        location.setAttributes(attributes);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(address, location.getAddress());
        assertEquals("ABC123", location.getIdentifier("code"));
        assertEquals(parent, location.getParentLocation());
        assertTrue(location.getTags().contains("Urban"));
        assertEquals("Commercial", location.getAttribute("category"));
    }

    @Test
    public void testParameterizedConstructor() {
        String locationId = "loc-002";
        String name = "Office";
        Address address = new Address();
        address.setAddressType("Office Address");

        Location parent = new Location();
        parent.setLocationId("loc-parent");

        Map<String, String> identifiers = new HashMap<>();
        identifiers.put("id", "XYZ789");

        Set<String> tags = new HashSet<>();
        tags.add("Business");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("floor", "3");

        Location location = new Location(locationId, name, address, identifiers, parent, tags, attributes);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(address, location.getAddress());
        assertEquals("XYZ789", location.getIdentifier("id"));
        assertEquals(parent, location.getParentLocation());
        assertTrue(location.getTags().contains("Business"));
        assertEquals("3", location.getAttribute("floor"));
    }

    @Test
    public void testIdentifierManagement() {
        Location location = new Location();
        location.addIdentifier("zip", "12345");
        location.addIdentifier("code", "LOC999");

        assertEquals("12345", location.getIdentifier("zip"));
        assertEquals("LOC999", location.getIdentifier("code"));

        // Remove an identifier and verify removal
        location.removeIdentifier("zip");
        assertNull(location.getIdentifier("zip"));
    }

    @Test
    public void testTagManagement() {
        Location location = new Location();
        location.addTag("Educational");
        location.addTag("Rural");

        assertTrue(location.hasTag("Educational"));
        assertTrue(location.getTags().contains("Rural"));

        // Remove tag and verify removal
        boolean removed = location.removeTag("Educational");
        assertTrue(removed);
        assertFalse(location.hasTag("Educational"));
    }

    @Test
    public void testAttributeManagement() {
        Location location = new Location();
        location.addAttribute("type", "residential");
        location.addAttribute("size", "Large");

        assertEquals("residential", location.getAttribute("type"));
        assertEquals("Large", location.getAttribute("size"));

        // Remove attribute and verify removal
        location.removeAttribute("type");
        assertNull(location.getAttribute("type"));
    }

    @Test
    public void testChainMethods() {
        Address address = new Address();
        address.setAddressType("Chain Address");

        Location parent = new Location().withLocationId("loc-parent");

        Location location = new Location()
                .withLocationId("chain-loc")
                .withName("Chain Location")
                .withAddress(address)
                .withIdentifier("chainId", "CH123")
                .withParentLocation(parent)
                .withTag("ChainTag")
                .withAttribute("chainAttr", "value");

        assertEquals("chain-loc", location.getLocationId());
        assertEquals("Chain Location", location.getName());
        assertEquals(address, location.getAddress());
        assertEquals("CH123", location.getIdentifier("chainId"));
        assertEquals(parent, location.getParentLocation());
        assertTrue(location.getTags().contains("ChainTag"));
        assertEquals("value", location.getAttribute("chainAttr"));
    }

    @Test
    public void testToString() {
        Location location = new Location();
        location.setLocationId("loc-str-01");
        location.setName("Location String Test");
        String toStr = location.toString();
        assertNotNull(toStr);
        assertTrue(toStr.contains("loc-str-01"));
        assertTrue(toStr.contains("Location String Test"));
    }
}