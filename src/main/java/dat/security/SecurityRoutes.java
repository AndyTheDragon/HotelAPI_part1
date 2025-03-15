package dat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dat.utils.Utils;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.security.RouteRole;

import static io.javalin.apibuilder.ApiBuilder.*;
public class SecurityRoutes
{
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final SecurityController securityController = new SecurityController();
    public static EndpointGroup getSecurityRoutes()
    {
        return ()->{
            path("/auth", ()->{
                get("/test", ctx->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from Open")),Role.ANYONE);
                post("/login", securityController.login(),Role.ANYONE);
                post("/register", securityController.register(),Role.ANYONE);
                get("/verify", securityController.verify() ,Role.ANYONE);
                get("/tokenlifespan", securityController.timeToLive() ,Role.ANYONE);
            });
        };
    }
    public static EndpointGroup getSecuredRoutes()
    {
        return ()->{
            path("/protected", ()->{
//              before(securityController.authenticate()); // This is done in ApplicationConfig now
                get("/user_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from USER Protected")),Role.USER);
                get("/admin_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from ADMIN Protected")),Role.ADMIN);
            });
        };
    }
    public enum Role implements RouteRole { ANYONE, USER, ADMIN }
}
