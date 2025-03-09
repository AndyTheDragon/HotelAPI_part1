package dat;

import dat.config.HibernateConfig;
import dat.dao.GenericDao;
import dat.entities.TestEntity;
import jakarta.persistence.EntityManagerFactory;

public class Main
{
    final static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
    final static GenericDao genericDao = GenericDao.getInstance(emf);

    public static void main(String[] args)
    {

        System.out.println("Hello, World!");

        TestEntity testEntity = new TestEntity();
        genericDao.create(testEntity);
        System.out.println(testEntity);
        testEntity.setName("Test");
        System.out.println(genericDao.update(testEntity));


        emf.close();

    }
}