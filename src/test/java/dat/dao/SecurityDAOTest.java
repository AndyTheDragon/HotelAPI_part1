package dat.dao;

import dat.config.HibernateConfig;
import dat.entities.UserAccount;
import dat.enums.Roles;
import dat.exceptions.DaoException;
import dat.exceptions.ValidationException;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityDAOTest {
    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private static final SecurityDAO securityDAO = new SecurityDAO(emf);
    private  UserAccount testUserAccount;

    @BeforeEach
    void setUp() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Clean up existing data
            em.createQuery("DELETE FROM UserAccount").executeUpdate();

            // Create test user with user role
            testUserAccount = new UserAccount("testuser", "password123");
            testUserAccount.addRole(Roles.USER);
            em.persist(testUserAccount);

            em.getTransaction().commit();
        }
    }


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
        assertTrue(result.getRoles().contains(Roles.USER.toString()));
        assertEquals(1, result.getRoles().size());
    }

    @Test
    void testGetVerifiedUser_WrongPassword() {
        // Arrange
        String username = "testuser";
        String wrongPassword = "wrongpassword";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                                                        () -> securityDAO.getVerifiedUser(username, wrongPassword));

        assertEquals("Password does not match", exception.getMessage());
    }

    @Test
    void testGetVerifiedUser_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser";
        String password = "password123";

        // Act & Assert
        DaoException exception = assertThrows(DaoException.class,
                                                () -> securityDAO.getVerifiedUser(nonExistentUsername, password));

        assertTrue(exception.getMessage().contains("Error reading object from db"));
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
            assertTrue(persistedUserAccount.getRolesAsString().contains("USER"));
        }
    }

    @Test
    void testCreateUser_UserAlreadyExists() {
        // Arrange
        String existingUsername = "testuser";
        String password = "newpassword";

        // Act & Assert
        EntityExistsException exception = assertThrows(EntityExistsException.class,
                                                        () -> securityDAO.createUser(existingUsername, password));

        assertTrue(exception.getMessage().contains("Error creating user"));
    }

    @Test
    void testAddRoleToUser_Success() {
        // Arrange
        String username = testUserAccount.getUsername();

        // Act
        UserAccount result = securityDAO.addRoleToUser(username, Roles.ADMIN);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains(Roles.USER));
        assertTrue(result.getRoles().contains(Roles.ADMIN));

        // Verify role was added in the database
        try (EntityManager em = emf.createEntityManager()) {
            UserAccount persistedUserAccount = em.find(UserAccount.class, username);
            assertNotNull(persistedUserAccount);
            assertEquals(2, persistedUserAccount.getRoles().size());
            assertTrue(persistedUserAccount.getRolesAsString().contains("ADMIN"));
        }
    }

    @Test
    void testAddRoleToUser_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser";

        // Act & Assert
        DaoException exception = assertThrows(DaoException.class,
                                                () -> securityDAO.addRoleToUser(nonExistentUsername, Roles.USER));

        assertTrue(exception.getMessage().contains("Error reading object from db"));
    }


    @Test
    void testRemoveRoleFromUser_Success() {
        // First add admin role to test user
        securityDAO.addRoleToUser("testuser", Roles.ADMIN);

        // Arrange
        String username = "testuser";

        // Act
        UserAccount result = securityDAO.removeRoleFromUser(username, Roles.ADMIN);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains(Roles.USER));
        assertFalse(result.getRoles().contains(Roles.ADMIN));

        // Verify role was removed in the database
        try (EntityManager em = emf.createEntityManager()) {
            UserAccount persistedUserAccount = em.find(UserAccount.class, username);
            assertNotNull(persistedUserAccount);
            assertEquals(1, persistedUserAccount.getRoles().size());
            assertFalse(persistedUserAccount.getRoles().contains(Roles.ADMIN));
        }
    }

    @Test
    void testRemoveRoleFromUser_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser";

        // Act & Assert
        DaoException exception = assertThrows(DaoException.class,
                                                () -> securityDAO.removeRoleFromUser(nonExistentUsername, Roles.USER));

        assertTrue(exception.getMessage().contains("Error reading object from db"));
    }

}
