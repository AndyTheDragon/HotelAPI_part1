package dat.controllers;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.dto.TestEntityDTO;
import dat.entities.Hotel;
import dat.routes.Routes;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestResourceTest
{

    private static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();;
    ObjectMapper objectMapper = new ObjectMapper();
    Hotel t1, t2;


    @BeforeAll
    static void setUpAll()
    {
        ApplicationConfig.getInstance()
                .initiateServer()
                .setRoute(Routes.getRoutes(emf))
                .handleException()
                .startServer(7078);
        RestAssured.baseURI = "http://localhost:7078/api";
    }

    @BeforeEach
    void setUp()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            //TestEntity[] entities = EntityPopulator.populate(genericDAO);
            t1 = new Hotel("TestEntityA");
            t2 = new Hotel("TestEntityB");
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Hotel ").executeUpdate();
            em.createNativeQuery("ALTER SEQUENCE entity_id_seq RESTART WITH 1");
            em.persist(t1);
            em.persist(t2);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    void getAll()
    {
        given().when().get("/entity").then().statusCode(200).body("size()", equalTo(2));
    }

    @Test
    void getById()
    {
        given().when().get("/entity/1").then().statusCode(200).body("id", equalTo(1));
    }

    @Test
    void create()
    {
        Hotel entity = new Hotel("New entity");
        try
        {
            String json = objectMapper.writeValueAsString(new TestEntityDTO(entity));
            given().when()
                    .contentType("application/json")
                    .accept("application/json")
                    .body(json)
                    .post("/entity")
                    .then()
                    .statusCode(200)
                    .body("title", equalTo("New entity")); // check here??
        } catch (JsonProcessingException e)
        {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void update()
    {
        Hotel entity = new Hotel("New entity2");
        try
        {
            String json = objectMapper.writeValueAsString(new TestEntityDTO(entity));
            given().when()
                    .contentType("application/json")
                    .accept("application/json")
                    .body(json)
                    .put("/entity/1") // double check id
                    .then()
                    .statusCode(200)
                    .body("title", equalTo("New entity2"));
        } catch (JsonProcessingException e)
        {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void delete()
    {
    }
}