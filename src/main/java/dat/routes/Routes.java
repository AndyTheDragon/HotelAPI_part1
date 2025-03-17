package dat.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import dat.controllers.HotelController;
import dat.controllers.SecurityController;
import dat.enums.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes
{
    private static HotelController hotelController;
    private static SecurityController securityController;
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static EndpointGroup getRoutes(EntityManagerFactory emf)
    {
        hotelController = new HotelController(emf);
        securityController = new SecurityController(emf);
        return () -> {
            path("hotel", hotelRoutes());
            path("auth", authRoutes());
            path("protected", protectedRoutes());
        };
    }

    private static EndpointGroup hotelRoutes()
    {
        return () -> {
            get(hotelController::getAll);
            post(hotelController::create);
            get("/{id}", hotelController::getById);
            put("/{id}", hotelController::update);
            delete("/{id}", hotelController::delete);
            get("{id}/rooms", hotelController::getRooms);
        };
    }

    private static EndpointGroup authRoutes()
    {
        return () -> {
            get("/test", ctx->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from Open")), Role.ANYONE);
            post("/login", securityController.login(), Role.ANYONE);
            post("/register", securityController.register(), Role.ANYONE);
            get("/verify", securityController.verify() , Role.ANYONE);
            get("/tokenlifespan", securityController.timeToLive() , Role.ANYONE);
        };
    }

    private static EndpointGroup protectedRoutes()
    {
        return () -> {
            get("/user_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from USER Protected")), Role.USER);
            get("/admin_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from ADMIN Protected")), Role.ADMIN);
        };
    }

}
