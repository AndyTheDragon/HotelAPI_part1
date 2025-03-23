package dat.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import dat.controllers.HotelController;
import dat.controllers.SecurityController;
import dat.enums.Roles;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes
{
    private final HotelController hotelController;
    private final SecurityController securityController;
    private final ObjectMapper jsonMapper = new ObjectMapper();

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
            get("/test", ctx->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from Open")), Roles.ANYONE);
            get("/healthcheck", securityController::healthCheck, Roles.ANYONE);
            post("/login", securityController::login, Roles.ANYONE);
            post("/register", securityController::register, Roles.ANYONE);
            get("/verify", securityController::verify , Roles.ANYONE);
            get("/tokenlifespan", securityController::timeToLive , Roles.ANYONE);
        };
    }

    private  EndpointGroup protectedRoutes()
    {
        return () -> {
            get("/user_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from USER Protected")), Roles.USER);
            get("/admin_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from ADMIN Protected")), Roles.ADMIN);
        };
    }

}
