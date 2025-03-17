package dat.controllers;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.entities.Role;
import dat.entities.User;
import dat.routes.Routes;
import dat.routes.SecurityRoutes;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityControllerTest {

    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private final Logger logger = LoggerFactory.getLogger(SecurityControllerTest.class.getName());
    private String token;
    private final String TEST_USER = "testuser";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_ADMIN = "testadmin";

    @BeforeAll
    static void setUpAll() {
        ApplicationConfig.getInstance()
                .initiateServer()
                .setRoute(Routes.getRoutes(emf))
                .setRoute(SecurityRoutes.getSecurityRoutes())
                .setRoute(SecurityRoutes.getSecuredRoutes())
                .handleException()
                .setApiExceptionHandling()
                .startServer(7079)
                .checkSecurityRoles();
        RestAssured.baseURI = "http://localhost:7079/api";
    }

    @BeforeEach
    void setUp() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Clean up existing data
            em.createQuery("DELETE FROM User").executeUpdate();
            em.createQuery("DELETE FROM Role").executeUpdate();

            // Create test roles
            Role userRole = new Role("user");
            Role adminRole = new Role("admin");
            em.persist(userRole);
            em.persist(adminRole);

            // Create test user with user role
            User testUser = new User(TEST_USER, TEST_PASSWORD);
            testUser.addRole(userRole);
            em.persist(testUser);

            // Create test admin with admin role
            User testAdmin = new User(TEST_ADMIN, TEST_PASSWORD);
            testAdmin.addRole(userRole);
            testAdmin.addRole(adminRole);
            em.persist(testAdmin);

            em.getTransaction().commit();
        }
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            System.out.println("EntityManagerFactory closed");
        }
        ApplicationConfig.getInstance().stopServer();
    }

    @Test
    void testLogin_Success() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_USER);
        loginRequest.put("password", TEST_PASSWORD);

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo(TEST_USER));
    }

    @Test
    void testLogin_WrongPassword() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_USER);
        loginRequest.put("password", "wrongpassword");

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(401)
            .body("message", containsString("Could not verify user"));
    }

    @Test
    void testLogin_UserNotFound() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "nonexistentuser");
        loginRequest.put("password", TEST_PASSWORD);

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(401)
            .body("message", containsString("Could not verify user"));
    }

    @Test
    void testRegister_Success() {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "newuser");
        registerRequest.put("password", "newpassword");

        given()
            .contentType(ContentType.JSON)
            .body(registerRequest)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue())
            .body("username", equalTo("newuser"));
    }

    @Test
    void testRegister_UserAlreadyExists() {
        // Count users before the test
        int userCountBefore = countUsers();

        // Try to register an existing user
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", TEST_USER);
        registerRequest.put("password", TEST_PASSWORD);

        try {
            given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
            .when()
                .post("/auth/register");
        } catch (Exception e) {
            // Ignore any exceptions - we expect this to fail
            logger.info("Expected exception: " + e.getMessage());
        }

        // Count users after the test
        int userCountAfter = countUsers();

        // Verify that no new user was created
        assertEquals(userCountBefore, userCountAfter, "User count should not change when trying to register an existing user");
    }

    private int countUsers() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult().intValue();
        }
    }

    @Test
    void testVerify_ValidToken() {
        // First login to get a token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_USER);
        loginRequest.put("password", TEST_PASSWORD);

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .post("/auth/login");

        String token = loginResponse.jsonPath().getString("token");

        // Then verify the token
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/auth/verify")
        .then()
            .statusCode(200)
            .body("msg", equalTo("Token is valid"));
    }

    @Test
    void testVerify_InvalidToken() {
        given()
            .header("Authorization", "Bearer invalidtoken")
        .when()
            .get("/auth/verify")
        .then()
            .statusCode(401);
    }

    @Test
    void testVerify_NoToken() {
        given()
        .when()
            .get("/auth/verify")
        .then()
            .statusCode(401);
    }

    @Test
    void testTokenLifespan() {
        // First login to get a token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_USER);
        loginRequest.put("password", TEST_PASSWORD);

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .post("/auth/login");

        String token = loginResponse.jsonPath().getString("token");

        // Then check token lifespan
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/auth/tokenlifespan")
        .then()
            .statusCode(200)
            .body("msg", containsString("Token is valid until"))
            .body("expireTime", notNullValue())
            .body("secondsToLive", notNullValue());
            // The token might have already expired, so we're just checking that the field exists
    }

    @Test
    void testProtectedUserEndpoint_WithUserRole() {
        // First login to get a token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_USER);
        loginRequest.put("password", TEST_PASSWORD);

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .post("/auth/login");

        String token = loginResponse.jsonPath().getString("token");

        // Then access protected user endpoint
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/protected/user_demo")
        .then()
            .statusCode(200)
            .body("msg", equalTo("Hello from USER Protected"));
    }

    @Test
    void testProtectedAdminEndpoint_WithUserRole() {
        // First login to get a token for a user with only USER role
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_USER);
        loginRequest.put("password", TEST_PASSWORD);

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .post("/auth/login");

        String token = loginResponse.jsonPath().getString("token");

        // Then try to access protected admin endpoint
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/protected/admin_demo")
        .then()
            .statusCode(403); // Forbidden
    }

    @Test
    void testProtectedAdminEndpoint_WithAdminRole() {
        // First login to get a token for a user with ADMIN role
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", TEST_ADMIN);
        loginRequest.put("password", TEST_PASSWORD);

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .post("/auth/login");

        String token = loginResponse.jsonPath().getString("token");

        // Then access protected admin endpoint
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/protected/admin_demo")
        .then()
            .statusCode(200)
            .body("msg", equalTo("Hello from ADMIN Protected"));
    }
}
