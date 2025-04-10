package tz.go.moh.ucs.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class UserTest {

    @Test
    void testConstructorBaseEntityIdOnly() {
        User user = new User("user1");
        assertEquals("user1", user.getBaseEntityId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getSalt());
        assertNull(user.getStatus());
        assertNull(user.getRoles());
        assertNull(user.getPermissions());
        assertNull(user.getPreferredName());
    }

    @Test
    void testConstructorUsernamePasswordSalt() {
        User user = new User("user2", "john_doe", "pass123", "salt123");
        assertEquals("user2", user.getBaseEntityId());
        assertEquals("john_doe", user.getUsername());
        assertEquals("pass123", user.getPassword());
        assertEquals("salt123", user.getSalt());
        // other fields remain null
        assertNull(user.getStatus());
        assertNull(user.getRoles());
        assertNull(user.getPermissions());
        assertNull(user.getPreferredName());
    }

    @Test
    void testConstructorWithStatusRolesPermissions() {
        List<String> roles = new ArrayList<>(Arrays.asList("user", "manager"));
        List<String> permissions = new ArrayList<>(Arrays.asList("read", "write"));
        User user = new User("user3", "jane_doe", "pass456", "salt456", "active", roles, permissions);
        assertEquals("user3", user.getBaseEntityId());
        assertEquals("jane_doe", user.getUsername());
        assertEquals("pass456", user.getPassword());
        assertEquals("salt456", user.getSalt());
        assertEquals("active", user.getStatus());
        assertEquals(roles, user.getRoles());
        assertEquals(permissions, user.getPermissions());
        assertNull(user.getPreferredName());
    }

    @Test
    void testConstructorWithPreferredName() {
        List<String> roles = new ArrayList<>(Arrays.asList("user"));
        List<String> permissions = new ArrayList<>(Arrays.asList("execute"));
        User user = new User("user4", "alex", "pass789", "Alex Smith", "salt789", "inactive", roles, permissions);
        assertEquals("user4", user.getBaseEntityId());
        assertEquals("alex", user.getUsername());
        assertEquals("pass789", user.getPassword());
        assertEquals("salt789", user.getSalt());
        assertEquals("inactive", user.getStatus());
        assertEquals(roles, user.getRoles());
        assertEquals(permissions, user.getPermissions());
        assertEquals("Alex Smith", user.getPreferredName());
    }

    @Test
    void testSettersAndGetters() {
        User user = new User("user5");

        user.setUsername("user5name");
        assertEquals("user5name", user.getUsername());

        user.setPassword("secret");
        assertEquals("secret", user.getPassword());

        user.setSalt("newsalt");
        assertEquals("newsalt", user.getSalt());

        user.setStatus("enabled");
        assertEquals("enabled", user.getStatus());

        user.setPreferredName("User Five");
        assertEquals("User Five", user.getPreferredName());
    }

    @Test
    void testRolesMethods() {
        User user = new User("user6");
        // Initially roles is null
        assertNull(user.getRoles());

        // Test addRole initializes roles list
        user.addRole("admin");
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().contains("admin"));

        // Test setRoles overrides the roles list
        List<String> newRoles = new ArrayList<>();
        newRoles.add("manager");
        user.setRoles(newRoles);
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains("manager"));

        // Test removeRole
        assertTrue(user.removeRole("manager"));
        assertFalse(user.getRoles().contains("manager"));

        // Add multiple roles and test case-insensitive hasRole
        user.addRole("Employee");
        user.addRole("Supervisor");
        assertTrue(user.hasRole("employee"));
        assertTrue(user.hasRole("SUPERVISOR"));
        assertFalse(user.hasRole("manager"));
    }

    @Test
    void testAdminRights() {
        // User with admin username and admin role
        User adminUser = new User("admin1", "admin", "pass", "salt");
        adminUser.addRole("admin");
        assertTrue(adminUser.isDefaultAdmin());
        assertTrue(adminUser.hasAdminRights());

        // User with administrator username and role
        User adminUser2 = new User("admin2", "administrator", "pass", "salt");
        adminUser2.addRole("administrator");
        assertTrue(adminUser2.isDefaultAdmin());
        assertTrue(adminUser2.hasAdminRights());

        // Non-admin user
        User regularUser = new User("user7", "john", "pass", "salt");
        regularUser.addRole("user");
        assertFalse(regularUser.isDefaultAdmin());
        assertFalse(regularUser.hasAdminRights());
    }

    @Test
    void testPermissionsMethods() {
        User user = new User("user8");
        // Initially permissions is null
        assertNull(user.getPermissions());

        // Test addPermission initializes list
        user.addPermission("read");
        assertNotNull(user.getPermissions());
        assertTrue(user.getPermissions().contains("read"));

        // Test setPermissions
        List<String> perms = new ArrayList<>();
        perms.add("write");
        user.setPermissions(perms);
        assertEquals(1, user.getPermissions().size());
        assertTrue(user.getPermissions().contains("write"));

        // Test removePermission
        user.addPermission("execute");
        assertTrue(user.removePermission("execute"));
        assertFalse(user.getPermissions().contains("execute"));

        // Test hasPermission case-insensitive
        user.addPermission("Delete");
        assertTrue(user.hasPermission("delete"));
        assertFalse(user.hasPermission("update"));
    }

    @Test
    void testWithMethods() {
        User user = new User("user9");

        user.withUsername("newName")
            .withPassword("newPass")
            .withSalt("newSalt")
            .withStatus("newStatus");
        assertEquals("newName", user.getUsername());
        assertEquals("newPass", user.getPassword());
        assertEquals("newSalt", user.getSalt());
        assertEquals("newStatus", user.getStatus());

        // Test withRoles and withRole
        List<String> roles = new ArrayList<>();
        roles.add("role1");
        user.withRoles(roles);
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains("role1"));

        user.withRole("role2");
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains("role2"));

        // Test withPermissions and withPermission
        List<String> perms = new ArrayList<>();
        perms.add("perm1");
        user.withPermissions(perms);
        assertEquals(1, user.getPermissions().size());
        assertTrue(user.getPermissions().contains("perm1"));

        user.withPermission("perm2");
        assertEquals(2, user.getPermissions().size());
        assertTrue(user.getPermissions().contains("perm2"));
    }

    @Test
    void testToString() {
        User user = new User("user10", "tester", "pwd", "salt");
        user.addRole("roleTest");
        user.addPermission("permTest");
        String str = user.toString();
        assertNotNull(str);
        // Check that the toString output contains some of the fields
        assertTrue(str.contains("user10"));
        assertTrue(str.contains("tester"));
        assertTrue(str.contains("roleTest"));
        assertTrue(str.contains("permTest"));
    }
}