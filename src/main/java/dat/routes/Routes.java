package dat.routes;

import dat.controllers.HotelController;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes
{
    private static HotelController hotelController;
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static EndpointGroup getRoutes(EntityManagerFactory emf)
    {
        hotelController = new HotelController(emf);
        return () -> {
            path("/hotel", () -> {
                get(hotelController::getAll);
                post(hotelController::create);
                get("/{id}", hotelController::getById);
                put("/{id}", hotelController::update);
                delete("/{id}", hotelController::delete);
                get("{id}/rooms", hotelController::getRooms);
            });
        };
    }

}
