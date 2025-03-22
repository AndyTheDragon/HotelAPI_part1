package dat.dao;

import dat.config.HibernateConfig;
import dat.entities.*;
import dat.exceptions.DaoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericDAOTest
{
    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private static final GenericDAO genericDAO = new HotelDAO(emf);
    private static Hotel h1, h2;
    private static Room r1, r2, r3, r4;


    @BeforeEach
    void setUp()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            r2 = new Room("A102");
            r3 = new Room("B101");
            r4 = new Room("B102");
            r1 = new Room("A101");
            h1 = new Hotel("Hotel A");
            h2 = new Hotel("Hotel B");
            h1.addRoom(r1);
            h1.addRoom(r2);
            h2.addRoom(r3);
            h2.addRoom(r4);
            em.getTransaction().begin();
                em.createQuery("DELETE FROM Room ").executeUpdate();
                em.createQuery("DELETE FROM Hotel ").executeUpdate();
                em.persist(h1);
                em.persist(h2);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    @Test
    void getInstance()
    {
        assertNotNull(emf);
    }

    @Test
    void create()
    {
        // Arrange
        Hotel h3 = new Hotel("Hotel C");
        Room r5 = new Room("C101");
        r5.setHotel(h3);
        h3.addRoom(r5);
        System.out.println("---- " + h3);


        // Act
        Hotel result = genericDAO.create(h3);
        System.out.println("---- " + result);
        System.out.println("---- " + result.getRooms());
        System.out.println("---- " + r5);

        // Assert
        assertThat(result, samePropertyValuesAs(h3));
        assertNotNull(result);
        try (EntityManager em = emf.createEntityManager())
        {
            Hotel found = em.find(Hotel.class, result.getId());
            assertThat(found, samePropertyValuesAs(h3 ,"rooms"));
            Long amountInDb = em.createQuery("SELECT COUNT(t) FROM Hotel t", Long.class).getSingleResult();
            assertThat(amountInDb, is(3L));
        }

    }

    @Test
    void createMany()
    {
        // Arrange
        Hotel t3 = new Hotel("TestEntityC");
        Hotel t4 = new Hotel("TestEntityD");
        List<Hotel> testEntities = List.of(t3, t4);

        // Act
        List<Hotel> result = genericDAO.create(testEntities);

        // Assert
        assertThat(result.get(0), samePropertyValuesAs(t3, "rooms"));
        assertThat(result.get(1), samePropertyValuesAs(t4, "rooms"));
        assertNotNull(result);
        try (EntityManager em = emf.createEntityManager())
        {
            Long amountInDb = em.createQuery("SELECT COUNT(t) FROM Hotel t", Long.class).getSingleResult();
            assertThat(amountInDb, is(4L));
        }
    }

    @Test
    void read()
    {
        // Arrange
        Hotel expected = h1;

        // Act
        Hotel result = genericDAO.getById(Hotel.class, h1.getId());

        // Assert
        assertThat(result, samePropertyValuesAs(expected, "rooms"));
        //assertThat(result.getRooms(), containsInAnyOrder(expected.getRooms().toArray()));
    }

    @Test
    void read_notFound()
    {


        // Act
        DaoException exception = assertThrows(DaoException.class, () -> genericDAO.getById(Hotel.class, 1000L));
        //Hotel result = genericDAO.read(Hotel.class, 1000L);

        // Assert
        assertThat(exception.getMessage(), is("Error reading object from db"));
    }

    @Test
    void findAll()
    {
        // Arrange
        List<Hotel> expected = List.of(h1, h2);

        // Act
        List<Hotel> result = genericDAO.getAll(Hotel.class);

        // Assert
        assertNotNull(result);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), samePropertyValuesAs(expected.get(0), "rooms"));
        assertThat(result.get(1), samePropertyValuesAs(expected.get(1), "rooms"));
    }

    @Test
    void update()
    {
        // Arrange
        h1.setName("UpdatedName");

        // Act
        Hotel result = genericDAO.update(h1);

        // Assert
        assertThat(result, samePropertyValuesAs(h1, "rooms"));
        //assertThat(result.getRooms(), containsInAnyOrder(h1.getRooms()));

    }

    @Test
    void updateMany()
    {
        // Arrange
        h1.setName("UpdatedName");
        h2.setName("UpdatedName");
        List<Hotel> testEntities = List.of(h1, h2);

        // Act
        List<Hotel> result = genericDAO.update(testEntities);

        // Assert
        assertNotNull(result);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), samePropertyValuesAs(h1, "rooms"));
        assertThat(result.get(1), samePropertyValuesAs(h2, "rooms"));
    }

    @Test
    void delete()
    {
        // Act
        genericDAO.delete(h1);

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            Long amountInDb = em.createQuery("SELECT COUNT(t) FROM Hotel t", Long.class).getSingleResult();
            assertThat(amountInDb, is(1L));
            Hotel found = em.find(Hotel.class, h1.getId());
            assertNull(found);
        }
    }

    @Test
    void delete_byId()
    {
        // Act
        genericDAO.delete(Hotel.class, h2.getId());

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            Long amountInDb = em.createQuery("SELECT COUNT(t) FROM Hotel t", Long.class).getSingleResult();
            assertThat(amountInDb, is(1L));
            Hotel found = em.find(Hotel.class, h2.getId());
            assertNull(found);
        }
    }
}