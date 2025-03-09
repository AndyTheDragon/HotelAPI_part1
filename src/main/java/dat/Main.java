package dat;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.routes.Routes;
import jakarta.persistence.EntityManagerFactory;


public class Main
{
    final static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

    public static void main(String[] args)
    {
        ApplicationConfig
                .getInstance()
                .initiateServer()
                .setRoute(Routes.getRoutes(emf))
                .handleException()
                .startServer(7070);
    }
}