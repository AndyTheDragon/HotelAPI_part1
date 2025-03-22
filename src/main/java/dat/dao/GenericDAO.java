package dat.dao;

import dat.exceptions.DaoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GenericDAO implements CrudDAO
{
    protected final EntityManagerFactory emf;
    private final Logger logger = LoggerFactory.getLogger(GenericDAO.class);

    public GenericDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
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
            logger.error("Error persisting object to db", e);
            throw new DaoException("Error persisting object to db. ", e);
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
            logger.error("Error persisting object to db", e);
            throw new DaoException("Error persisting object to db. ", e);
        }
    }

    public <T> T getById(Class<T> type, Object id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            T entity = em.find(type, id);
            if (entity == null)
            {
                throw new EntityNotFoundException("No entity found with id " + id.toString());
            }
            return entity;
        }
        catch (Exception e)
        {
            logger.error("Error reading object from db", e);
            throw new DaoException("Error reading object from db", e);
        }
    }

    @Override
    public <T> List<T> getAll(Class<T> type) throws DaoException
    {
        try (EntityManager em = emf.createEntityManager())
        {
            List<T> entities = em.createQuery("SELECT t FROM " + type.getSimpleName() + " t", type).getResultList();
            if (entities.isEmpty())
            {
                throw new EntityNotFoundException("No entities found in db");
            }
            return em.createQuery("SELECT t FROM " + type.getSimpleName() + " t", type).getResultList();
        }
        catch (Exception e)
        {
            logger.error("Error reading objects from db", e);
            throw new DaoException("Error reading objects from db", e);
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
            logger.error("Error updating object", e);
            throw new DaoException("Error updating object. ", e);
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
            logger.error("Error updating object", e);
            throw new DaoException("Error updating object. ", e);
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
            logger.error("Error deleting object", e);
            throw new DaoException("Error deleting object. ", e);
        }
    }

    public <T> void delete(Class<T> type, Object id)
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
            logger.error("Error deleting object", e);
            throw new DaoException("Error deleting object. ", e);
        }
    }


}
