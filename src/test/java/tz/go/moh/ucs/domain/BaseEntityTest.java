package tz.go.moh.ucs.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the BaseEntity class.
 */
public class BaseEntityTest {

    @Test
    public void testBaseEntityId() {
        String id = "BE123";
        BaseEntity entity = new BaseEntity(id);
        assertEquals(id, entity.getBaseEntityId(), "The baseEntityId should be set via the constructor.");
    }

    @Test
    public void testIdentifiers() {
        BaseEntity entity = new BaseEntity("BE123");
        Map<String, String> ids = new HashMap<>();
        ids.put("SSN", "123-45-6789");
        entity.setIdentifiers(ids);

        assertEquals("123-45-6789", entity.getIdentifier("SSN"), "Identifier should match the set value.");
    }

    @Test
    public void testGetIdentifierMatchingRegex() {
        BaseEntity entity = new BaseEntity("BE123");
        Map<String, String> ids = new HashMap<>();
        ids.put("ID1", "value1");
        ids.put("id2", "value2");
        entity.setIdentifiers(ids);

        String matched = entity.getIdentifierMatchingRegex("(?i)id2");
        assertEquals("value2", matched, "The identifier matching regex should return the correct value.");
    }

    @Test
    public void testAddresses() {
        BaseEntity entity = new BaseEntity("BE123");
        List<Address> addresses = new ArrayList<>();
        // Assuming Address has a no-argument constructor
        Address address = new Address();
        addresses.add(address);
        entity.setAddresses(addresses);

        assertNotNull(entity.getAddresses(), "Addresses list should not be null.");
        assertEquals(1, entity.getAddresses().size(), "There should be one address in the list.");
    }

    @Test
    public void testAddAddress() {
        BaseEntity entity = new BaseEntity("BE123");
        Address address = new Address();
        entity.addAddress(address);

        assertNotNull(entity.getAddresses(), "Addresses list should be initialized after adding an address.");
        assertTrue(entity.getAddresses().contains(address), "The added address should be present in the list.");
    }

    @Test
    public void testAttributes() {
        BaseEntity entity = new BaseEntity("BE123");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key", "value");
        entity.setAttributes(attrs);

        assertEquals("value", entity.getAttribute("key"), "The attribute should match the set value.");
    }

    @Test
    public void testRemoveIdentifier() {
        BaseEntity entity = new BaseEntity("BE123");
        entity.setIdentifiers(new HashMap<>());
        entity.addIdentifier("ID", "value");
        assertEquals("value", entity.getIdentifier("ID"), "Identifier should be added.");
        
        entity.removeIdentifier("ID");
        assertNull(entity.getIdentifier("ID"), "Identifier should be removed.");
    }

    @Test
    public void testRemoveAttribute() {
        BaseEntity entity = new BaseEntity("BE123");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key", "value");
        entity.setAttributes(attrs);
        
        entity.removeAttribute("key");
        assertNull(entity.getAttribute("key"), "Attribute should be removed.");
    }

    @Test
    public void testWithMethods() {
        // Testing the fluent with methods
        Address dummyAddress = new Address();
        BaseEntity entity = new BaseEntity("initialId")
                .withIdentifier("ID", "value")
                .withAddress(dummyAddress)
                .withAttribute("attr", "val");
        
        assertEquals("initialId", entity.getBaseEntityId(), "The baseEntityId should be preserved.");
        assertEquals("value", entity.getIdentifier("ID"), "Identifier should match the fluent set value.");
        assertEquals("val", entity.getAttribute("attr"), "Attribute should match the fluent set value.");
        assertNotNull(entity.getAddresses(), "Addresses list should be initialized.");
        assertEquals(1, entity.getAddresses().size(), "There should be one address added via withAddress.");
    }
}