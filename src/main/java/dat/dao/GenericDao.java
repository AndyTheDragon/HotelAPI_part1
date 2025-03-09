package dat.dao;

import dat.exceptions.DaoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class GenericDao implements crudDAO
{
    private static EntityManagerFactory emf;
    private static GenericDao instance;

    private GenericDao(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    public static GenericDao getInstance(EntityManagerFactory emf)
    {
        if (instance == null)
        {
            instance = new GenericDao(emf);
        }
        return instance;
    }

    public <T> T create(T object)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(object);
            em.getTransaction().commit();
            return object;
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error persisting object to db. ", e);
        }
    }

    public <T> List<T> create(List<T> objects)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            for (T object : objects)
            {
                em.persist(object);
            }
            em.getTransaction().commit();
            return objects;
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error persisting object to db. ", e);
        }
    }

    public <T> T read(Class<T> type, Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.find(type, id);
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error reading object from db", e);
        }
    }

    public <T> List<T> findAll(Class<T> type)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT t FROM " + type.getSimpleName() + " t", type).getResultList();
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error reading objects from db", e);
        }
    }

    public <T> T update(T object)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            T updatedEntity = em.merge(object);
            em.getTransaction().commit();
            return updatedEntity;
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error updating object. ", e);
        }
    }

    public <T> List<T> update(List<T> objects)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            List<T> updatedObjects = new ArrayList<>();
            em.getTransaction().begin();
            for (T object : objects)
            {
                updatedObjects.add(em.merge(object));
            }
            em.getTransaction().commit();
            return updatedObjects;
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error updating object. ", e);
        }
    }

    public <T> void delete(T object)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.remove(object);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error deleting object. ", e);
        }
    }

    public <T> void delete(Class<T> type, Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            T object = em.find(type, id);
            em.remove(object);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            throw new DaoException(401, "Error deleting object. ", e);
        }
    }
}
