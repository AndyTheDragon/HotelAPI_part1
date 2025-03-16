package dat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dat.security.*;
import dat.exceptions.ApiException;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;

public class ApplicationConfig
{
    private static ApplicationConfig instance;
    private static Javalin app;
    private static JavalinConfig javalinConfig;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final ISecurityController securityController = new SecurityController();

    private ApplicationConfig() {}

    public static ApplicationConfig getInstance()
    {
        if (instance == null)
        {
            instance = new ApplicationConfig();
        }
        return instance;
    }

    public ApplicationConfig initiateServer()
    {
        app = Javalin.create(config -> {
            javalinConfig = config;
            config.showJavalinBanner = false;
            config.http.defaultContentType = "application/json";
            config.router.contextPath = "/api";
            config.bundledPlugins.enableRouteOverview("/routes");
            config.bundledPlugins.enableDevLogging();
        });
        logger.info("Server initiated");
        return instance;
    }

    public ApplicationConfig setRoute(EndpointGroup routes)
    {
        javalinConfig.router.apiBuilder(()-> {
            path("/", routes);
        });
        logger.info("Routes set");
        return instance;
    }

    public ApplicationConfig startServer(int port)
    {
        app.start(port);
        logger.info("Server started on port: " + port);
        return instance;
    }

    public ApplicationConfig checkSecurityRoles() {
        app.beforeMatched(securityController.authenticate()); // check if there is a valid token in the header
        app.beforeMatched(securityController.authorize()); // check if the user has the required role
        return instance;
    }

    public ApplicationConfig setApiExceptionHandling()
    {
        // Might be overruled by the setErrorHandling method
        app.exception(ApiException.class, (e, ctx) -> {
            logger.error("ApiException: " + e.getMessage());
            int statusCode = e.getCode();
            ObjectNode on = objectMapper
                    .createObjectNode()
                    .put("status", statusCode)
                    .put("msg", e.getMessage());
            ctx.json(on);
            ctx.status(statusCode);
        });
        return instance;
    }

    public ApplicationConfig handleException(){
        app.exception(Exception.class, (e,ctx)->{
            logger.error("Exception: " + e.getMessage());
            ObjectNode node = objectMapper.createObjectNode();
            node.put("msg",e.getMessage());
            ctx.status(500).json(node);
        });
        logger.info("ExceptionHandler initiated");
        return instance;
    }


    public ApplicationConfig stopServer()
    {
        app.stop();
        logger.info("Server stopped");
        return instance;
    }
}
