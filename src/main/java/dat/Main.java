package dat;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.routes.Routes;
import dat.routes.SecurityRoutes;
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
                .setRoute(SecurityRoutes.getSecurityRoutes())
                .setRoute(SecurityRoutes.getSecuredRoutes())
                .handleException()
                .setApiExceptionHandling()
                .startServer(7070)
                .checkSecurityRoles();
    }
}