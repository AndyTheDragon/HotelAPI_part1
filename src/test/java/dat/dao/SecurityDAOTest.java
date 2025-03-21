package dat.dao;

import dat.config.HibernateConfig;
import dat.entities.UserAccount;
import dat.exceptions.ApiException;
import dat.exceptions.ValidationException;
import dat.entities.Role;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityDAOTest {
    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private static final SecurityDAO securityDAO = SecurityDAO.getInstance(emf);
    private static UserAccount testUserAccount;
    private static Role userRole, adminRole;

    @BeforeEach
    void setUp() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Clean up existing data
            em.createQuery("DELETE FROM UserAccount").executeUpdate();
            em.createQuery("DELETE FROM Role").executeUpdate();

            // Create test roles
            userRole = new Role("user");
            adminRole = new Role("admin");
            em.persist(userRole);
            em.persist(adminRole);

            // Create test user with user role
            testUserAccount = new UserAccount("testuser", "password123");
            testUserAccount.addRole(userRole);
            em.persist(testUserAccount);

            em.getTransaction().commit();
        }
    }

//    @AfterAll
//    void tearDown() {
//        if (emf != null && emf.isOpen()) {
//            emf.close();
//            System.out.println("EntityManagerFactory closed");
//        }
//    }

    @Test
    void testGetVerifiedUser_Success() throws ValidationException {
        // Arrange
        String username = "testuser";
        String password = "password123";

        // Act
        UserDTO result = securityDAO.getVerifiedUser(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertTrue(result.getRoles().contains("user"));
        assertEquals(1, result.getRoles().size());
    }

    @Test
    void testGetVerifiedUser_WrongPassword() {
        // Arrange
        String username = "testuser";
        String wrongPassword = "wrongpassword";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            securityDAO.getVerifiedUser(username, wrongPassword);
        });

        assertEquals("Password does not match", exception.getMessage());
    }

    @Test
    void testGetVerifiedUser_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser";
        String password = "password123";

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            securityDAO.getVerifiedUser(nonExistentUsername, password);
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        String username = "newuser";
        String password = "newpassword";

        // Act
        UserAccount result = securityDAO.createUser(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());

        // Verify user was persisted with the user role
        try (EntityManager em = emf.createEntityManager()) {
            UserAccount persistedUserAccount = em.find(UserAccount.class, username);
            assertNotNull(persistedUserAccount);
            assertEquals(1, persistedUserAccount.getRoles().size());
            assertTrue(persistedUserAccount.getRolesAsString().contains("user"));
        }
    }

    @Test
    void testCreateUser_UserAlreadyExists() {
        // Arrange
        String existingUsername = "testuser";
        String password = "newpassword";

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            securityDAO.createUser(existingUsername, password);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("Error creating user"));
    }

    @Test
    void testAddRoleToUser_Success() {
        // Arrange
        String username = testUserAccount.getUsername();
        String roleName = adminRole.getRoleName();

        // Act
        UserAccount result = securityDAO.addRoleToUser(username, roleName);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRolesAsString().contains("user"));
        assertTrue(result.getRolesAsString().contains("admin"));

        // Verify role was added in the database
        try (EntityManager em = emf.createEntityManager()) {
            UserAccount persistedUserAccount = em.find(UserAccount.class, username);
            assertNotNull(persistedUserAccount);
            assertEquals(2, persistedUserAccount.getRoles().size());
            assertTrue(persistedUserAccount.getRolesAsString().contains("admin"));
        }
    }

    @Test
    void testAddRoleToUser_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser";
        String roleName = "admin";

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            securityDAO.addRoleToUser(nonExistentUsername, roleName);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("Error adding role to user"));
    }

    @Test
    void testAddRoleToUser_RoleNotFound() {
        // Arrange
        String username = "testuser";
        String nonExistentRole = "nonexistentrole";

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            securityDAO.addRoleToUser(username, nonExistentRole);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("Error adding role to user"));
    }

    @Test
    void testRemoveRoleFromUser_Success() {
        // First add admin role to test user
        securityDAO.addRoleToUser("testuser", "admin");

        // Arrange
        String username = "testuser";
        String roleName = "admin";

        // Act
        UserAccount result = securityDAO.removeRoleFromUser(username, roleName);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRolesAsString().contains("user"));
        assertFalse(result.getRolesAsString().contains("admin"));

        // Verify role was removed in the database
        try (EntityManager em = emf.createEntityManager()) {
            UserAccount persistedUserAccount = em.find(UserAccount.class, username);
            assertNotNull(persistedUserAccount);
            assertEquals(1, persistedUserAccount.getRoles().size());
            assertFalse(persistedUserAccount.getRolesAsString().contains("admin"));
        }
    }

    @Test
    void testRemoveRoleFromUser_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser";
        String roleName = "user";

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            securityDAO.removeRoleFromUser(nonExistentUsername, roleName);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("Error removing role from user"));
    }

    @Test
    void testRemoveRoleFromUser_RoleNotFound() {
        // Arrange
        String username = "testuser";
        String nonExistentRole = "nonexistentrole";

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            securityDAO.removeRoleFromUser(username, nonExistentRole);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("Error removing role from user"));
    }
}
