package dat.dao;

import dat.config.HibernateConfig;
import dat.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.is;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericDaoTest
{
    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private static final GenericDao genericDAO = GenericDao.getInstance(emf);
    private static Hotel t1;
    private static Hotel t2;

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
                em.createNativeQuery("ALTER SEQUENCE testentity_id_seq RESTART WITH 1");
                em.persist(t1);
                em.persist(t2);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @AfterAll
    void tearDown()
    {
        if (emf != null && emf.isOpen())
        {
            emf.close();
            System.out.println("EntityManagerFactory closed");
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
        Hotel t3 = new Hotel("TestEntityC");

        // Act
        Hotel result = genericDAO.create(t3);

        // Assert
        assertThat(result, samePropertyValuesAs(t3));
        assertNotNull(result);
        try (EntityManager em = emf.createEntityManager())
        {
            Hotel found = em.find(Hotel.class, result.getId());
            assertThat(found, samePropertyValuesAs(t3));
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
        assertThat(result.get(0), samePropertyValuesAs(t3));
        assertThat(result.get(1), samePropertyValuesAs(t4));
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
        Hotel expected = t1;

        // Act
        Hotel result = genericDAO.read(Hotel.class, t1.getId());

        // Assert
        assertThat(result, samePropertyValuesAs(expected));
    }

    @Test
    void read_notFound()
    {
        // Act
        Hotel result = genericDAO.read(Hotel.class, 1000L);

        // Assert
        assertNull(result);
    }

    @Test
    void findAll()
    {
        // Arrange
        List<Hotel> expected = List.of(t1, t2);

        // Act
        List<Hotel> result = genericDAO.findAll(Hotel.class);

        // Assert
        assertNotNull(result);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), samePropertyValuesAs(expected.get(0)));
        assertThat(result.get(1), samePropertyValuesAs(expected.get(1)));
    }

    @Test
    void update()
    {
        // Arrange
        t1.setName("UpdatedName");

        // Act
        Hotel result = genericDAO.update(t1);

        // Assert
        assertThat(result, samePropertyValuesAs(t1));

    }

    @Test
    void updateMany()
    {
        // Arrange
        t1.setName("UpdatedName");
        t2.setName("UpdatedName");
        List<Hotel> testEntities = List.of(t1, t2);

        // Act
        List<Hotel> result = genericDAO.update(testEntities);

        // Assert
        assertNotNull(result);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), samePropertyValuesAs(t1));
        assertThat(result.get(1), samePropertyValuesAs(t2));
    }

    @Test
    void delete()
    {
        // Act
        genericDAO.delete(t1);

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            Long amountInDb = em.createQuery("SELECT COUNT(t) FROM Hotel t", Long.class).getSingleResult();
            assertThat(amountInDb, is(1L));
            Hotel found = em.find(Hotel.class, t1.getId());
            assertNull(found);
        }
    }

    @Test
    void delete_byId()
    {
        // Act
        genericDAO.delete(Hotel.class, t2.getId());

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            Long amountInDb = em.createQuery("SELECT COUNT(t) FROM Hotel t", Long.class).getSingleResult();
            assertThat(amountInDb, is(1L));
            Hotel found = em.find(Hotel.class, t2.getId());
            assertNull(found);
        }
    }
}