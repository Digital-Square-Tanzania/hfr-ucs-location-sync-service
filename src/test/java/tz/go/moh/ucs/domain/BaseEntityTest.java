package tz.go.moh.ucs.domain;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BaseEntity class.
 */
public class BaseEntityTest {

    // DummyAddress class for testing purposes
    private static class DummyAddress extends Address {
        public DummyAddress() {
            super();
        }
    }

    @Test
    public void testDefaultConstructor() {
        BaseEntity entity = new BaseEntity();
        entity.setBaseEntityId("defaultId");
        assertEquals("defaultId", entity.getBaseEntityId());
    }

    @Test
    public void testConstructorWithBaseEntityId() {
        BaseEntity entity = new BaseEntity("entity1");
        assertEquals("entity1", entity.getBaseEntityId());
    }

    @Test
    public void testConstructorWithIdentifiers() {
        Map<String, String> ids = new HashMap<>();
        ids.put("key", "value");
        BaseEntity entity = new BaseEntity("entity1", ids);
        assertEquals("entity1", entity.getBaseEntityId());
        assertEquals("value", entity.getIdentifier("key"));
    }

    @Test
    public void testConstructorWithIdentifiersAndAttributes() {
        Map<String, String> ids = new HashMap<>();
        ids.put("key", "value");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "val");
        BaseEntity entity = new BaseEntity("entity1", ids, attrs);
        assertEquals("entity1", entity.getBaseEntityId());
        assertEquals("value", entity.getIdentifier("key"));
        assertEquals("val", entity.getAttribute("attr"));
    }

    @Test
    public void testConstructorWithAddresses() {
        Map<String, String> ids = new HashMap<>();
        ids.put("key", "value");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "val");
        List<Address> addresses = new ArrayList<>();
        DummyAddress addr = new DummyAddress();
        addresses.add(addr);
        BaseEntity entity = new BaseEntity("entity1", ids, attrs, addresses);
        assertEquals("entity1", entity.getBaseEntityId());
        assertEquals("value", entity.getIdentifier("key"));
        assertEquals("val", entity.getAttribute("attr"));
        assertNotNull(entity.getAddresses());
        assertEquals(1, entity.getAddresses().size());
        assertEquals(addr, entity.getAddresses().get(0));
    }

    @Test
    public void testAddresses() {
        BaseEntity entity = new BaseEntity("entity1");
        assertNull(entity.getAddresses());

        DummyAddress addr1 = new DummyAddress();
        entity.addAddress(addr1);
        assertNotNull(entity.getAddresses());
        assertEquals(1, entity.getAddresses().size());
        assertEquals(addr1, entity.getAddresses().get(0));

        List<Address> newAddresses = new ArrayList<>();
        DummyAddress addr2 = new DummyAddress();
        newAddresses.add(addr2);
        entity.setAddresses(newAddresses);
        assertEquals(1, entity.getAddresses().size());
        assertEquals(addr2, entity.getAddresses().get(0));
    }

    @Test
    public void testAttributes() {
        BaseEntity entity = new BaseEntity("entity1");
        assertNull(entity.getAttribute("attr"));

        entity.addAttribute("attr", "value");
        assertEquals("value", entity.getAttribute("attr"));
        // Test case-insensitive retrieval
        assertEquals("value", entity.getAttribute("ATTR"));

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key", "val");
        entity.setAttributes(attrs);
        assertEquals("val", entity.getAttribute("key"));

        entity.removeAttribute("key");
        assertNull(entity.getAttribute("key"));
    }

    @Test
    public void testIdentifiers() {
        BaseEntity entity = new BaseEntity("entity1");
        // Initially identifiers is null, so getIdentifier should return null
        assertNull(entity.getIdentifier("any"));

        entity.addIdentifier("idType", "value1");
        assertEquals("value1", entity.getIdentifier("idType"));
        assertEquals("value1", entity.getIdentifier("idtype"));

        Map<String, String> idMap = new HashMap<>();
        idMap.put("Test", "123");
        entity.setIdentifiers(idMap);
        assertEquals("123", entity.getIdentifier("test"));

        // Test getIdentifierMatchingRegex when match is found
        assertEquals("123", entity.getIdentifierMatchingRegex("Te.*"));

        // Test getIdentifierMatchingRegex with an empty map
        entity.setIdentifiers(new HashMap<>());
        assertNull(entity.getIdentifierMatchingRegex(".*"));

        // Set again for removal test
        idMap.put("removeKey", "removeVal");
        entity.setIdentifiers(idMap);
        entity.removeIdentifier("removeKey");
        assertNull(entity.getIdentifier("removeKey"));
    }

    @Test
    public void testWithMethods() {
        BaseEntity entity = new BaseEntity("initialId");
        entity.withBaseEntityId("newId");
        assertEquals("newId", entity.getBaseEntityId());

        Map<String, String> idMap = new HashMap<>();
        idMap.put("type", "123");
        entity.withIdentifiers(idMap);
        assertEquals("123", entity.getIdentifier("type"));

        entity.withIdentifier("code", "456");
        assertEquals("456", entity.getIdentifier("code"));

        List<Address> addressList = new ArrayList<>();
        DummyAddress addr = new DummyAddress();
        addressList.add(addr);
        entity.withAddresses(addressList);
        assertNotNull(entity.getAddresses());
        assertEquals(1, entity.getAddresses().size());
        assertEquals(addr, entity.getAddresses().get(0));

        DummyAddress addr2 = new DummyAddress();
        entity.withAddress(addr2);
        assertEquals(2, entity.getAddresses().size());
        assertEquals(addr2, entity.getAddresses().get(1));

        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put("a", "b");
        entity.withAttributes(attrMap);
        assertEquals("b", entity.getAttribute("a"));

        entity.withAttribute("c", "d");
        assertEquals("d", entity.getAttribute("c"));
    }

    @Test
    public void testToString() {
        BaseEntity entity = new BaseEntity("entity1");
        String str = entity.toString();
        assertNotNull(str);
        assertTrue(str.contains("entity1"));
    }
}
