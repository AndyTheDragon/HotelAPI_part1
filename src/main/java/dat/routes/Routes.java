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
    private HotelController hotelController;
    private SecurityController securityController;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(Routes.class);

    public Routes(HotelController hotelController, SecurityController securityController)
    {
        this.hotelController = hotelController;
        this.securityController = securityController;
    }

    public  EndpointGroup getRoutes()
    {
        return () -> {
            path("hotel", hotelRoutes());
            path("auth", authRoutes());
            path("protected", protectedRoutes());
        };
    }

    private  EndpointGroup hotelRoutes()
    {
        return () -> {
            get(hotelController::getAll);
            post(hotelController::create);
            get("/{id}", hotelController::getById);
            put("/{id}", hotelController::update);
            delete("/{id}", hotelController::delete);
            get("/{id}/rooms", hotelController::getRooms);
        };
    }

    private  EndpointGroup authRoutes()
    {
        return () -> {
            get("/test", ctx->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from Open")), Role.ANYONE);
            get("/healthcheck", securityController::healthCheck, Role.ANYONE);
            post("/login", securityController::login, Role.ANYONE);
            post("/register", securityController::register, Role.ANYONE);
            get("/verify", securityController::verify , Role.ANYONE);
            get("/tokenlifespan", securityController::timeToLive , Role.ANYONE);
        };
    }

    private  EndpointGroup protectedRoutes()
    {
        return () -> {
            get("/user_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from USER Protected")), Role.USER);
            get("/admin_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from ADMIN Protected")), Role.ADMIN);
        };
    }

}
