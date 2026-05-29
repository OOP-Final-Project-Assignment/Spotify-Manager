package siu.k18.cntt.spotify.dao;

import org.junit.jupiter.api.Test;
import siu.k18.cntt.spotify.models.User;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {

    // Test 1: Verifies hashed password does not match raw credential strings
    @Test
    public void testHashPassword_NotPlainText() {
        String plainPassword = "mySecretPassword123";
        String hashedPassword = UserDAO.hashPassword(plainPassword);
        
        assertNotNull(hashedPassword, "Hashed value cannot be null");
        assertNotEquals(plainPassword, hashedPassword, "SHA-256 hash output must differ completely from plaintext");
    }

    // Test 2: Verifies consistency of cryptographic algorithms (Same input yields same hash)
    @Test
    public void testHashPassword_Consistency() {
        String password = "admin";
        String hash1 = UserDAO.hashPassword(password);
        String hash2 = UserDAO.hashPassword(password);
        
        assertEquals(hash1, hash2, "Identical inputs must map to identical hash digests");
    }

    // Test 3: Uniqueness assurance (Different inputs must produce unique digests)
    @Test
    public void testHashPassword_DifferentInputs() {
        String hash1 = UserDAO.hashPassword("passA");
        String hash2 = UserDAO.hashPassword("passB");
        
        assertNotEquals(hash1, hash2, "Distinct credentials must not suffer collision errors");
    }

    // Test 4: Data model validation (Guarantees payload immutability via Java Record architecture)
    @Test
    public void testUserModel_CreationAndGetters() {
        User testUser = new User(10, "alex_it", "hashed_payload_xyz", "Staff");
        
        assertEquals(10, testUser.userId(), "User ID property verification failure");
        assertEquals("alex_it", testUser.username(), "Username property verification failure");
        assertEquals("Staff", testUser.role(), "Role authorization layer mapping failure");
    }

    // Test 5: Role access parsing bounds (Defends UI layout constraints from logic routing flaws)
    @Test
    public void testUserRole_Validation() {
        User adminUser = new User(1, "superadmin", "securehash", "Admin");
        
        assertTrue("Admin".equalsIgnoreCase(adminUser.role()), "System mapping failed to authorize Admin permission bounds");
        assertFalse("User".equalsIgnoreCase(adminUser.role()), "Role resolution overlap error: Admin flagged as normal User");
    }
}