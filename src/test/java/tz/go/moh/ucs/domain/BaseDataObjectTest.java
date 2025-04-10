package tz.go.moh.ucs.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

public class BaseDataObjectTest {

    // Create a dummy implementation of BaseDataObject for testing purposes
    private static class TestBaseDataObject extends BaseDataObject {
        // No additional implementation needed for testing
    }

    @Test
    public void testCreatorAndEditor() {
        TestBaseDataObject obj = new TestBaseDataObject();
        User creator = new User("creatorId");
        User editor = new User("editorId");

        obj.setCreator(creator);
        obj.setEditor(editor);

        assertEquals(creator, obj.getCreator(), "Creator should be set.");
        assertEquals(editor, obj.getEditor(), "Editor should be set.");
    }
    
    @Test
    public void testDates() {
        TestBaseDataObject obj = new TestBaseDataObject();
        Date now = new Date();
        obj.setDateCreated(now);
        obj.setDateEdited(now);
        obj.setDateVoided(now);
        
        assertEquals(now, obj.getDateCreated(), "Date created should match.");
        assertEquals(now, obj.getDateEdited(), "Date edited should match.");
        assertEquals(now, obj.getDateVoided(), "Date voided should match.");
    }
    
    @Test
    public void testVoiding() {
        TestBaseDataObject obj = new TestBaseDataObject();
        obj.setVoided(true);
        User voider = new User("voiderId");
        obj.setVoider(voider);
        obj.setVoidReason("Test void reason");
        
        assertTrue(obj.getVoided(), "Object should be marked as voided.");
        assertEquals(voider, obj.getVoider(), "Voider should be set.");
        assertEquals("Test void reason", obj.getVoidReason(), "Void reason should match.");
    }
    
    @Test
    public void testFluentSetters() {
        TestBaseDataObject obj = new TestBaseDataObject();
        Date now = new Date();
        User creator = new User("creatorId");
        User editor = new User("editorId");
        
        // Testing fluent setters
        obj.withCreator(creator)
           .withDateCreated(now)
           .withEditor(editor)
           .withDateEdited(now)
           .withVoided(true)
           .withDateVoided(now)
           .withVoider(new User("voiderId"))
           .withVoidReason("Fluent void reason");
        
        assertEquals(creator, obj.getCreator(), "Creator should be set using fluent method.");
        assertEquals(editor, obj.getEditor(), "Editor should be set using fluent method.");
        assertEquals(now, obj.getDateCreated(), "Date created should match using fluent method.");
        assertEquals(now, obj.getDateEdited(), "Date edited should match using fluent method.");
        assertTrue(obj.getVoided(), "Object should be marked as voided using fluent method.");
        assertEquals(now, obj.getDateVoided(), "Date voided should match using fluent method.");
        assertEquals("Fluent void reason", obj.getVoidReason(), "Void reason should match fluent method.");
    }
    
    @Test
    public void testToString() {
        TestBaseDataObject obj = new TestBaseDataObject();
        // Ensure that toString doesn't return null
        assertNotNull(obj.toString(), "toString should not return null");
    }
}