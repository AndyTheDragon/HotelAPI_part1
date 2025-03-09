package dat.routes;

import dat.controllers.TestController;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes
{
    private static TestController testController;
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static EndpointGroup getRoutes(EntityManagerFactory emf)
    {
        testController = new TestController(emf);
        return () -> {
            path("/poem", () -> {
                get(testController::getAll);
                post(testController::create);
                get("/{id}", testController::getById);
                put("/{id}", testController::update);
                delete("/{id}", testController::delete);
            });
        };
    }

}
