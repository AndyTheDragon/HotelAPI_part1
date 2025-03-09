package dat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;

import static io.javalin.apibuilder.ApiBuilder.path;

public class ApplicationConfig
{
    private static ApplicationConfig instance;
    private static Javalin app;
    private static JavalinConfig javalinConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
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
        return instance;
    }

    public ApplicationConfig setRoute(EndpointGroup routes)
    {
        javalinConfig.router.apiBuilder(()-> {
            path("/", routes);
        });
        return instance;
    }

    public ApplicationConfig startServer(int port)
    {
        app.start(port);
        return instance;
    }

    public ApplicationConfig handleException(){
        app.exception(Exception.class, (e,ctx)->{
            ObjectNode node = objectMapper.createObjectNode();
            node.put("msg",e.getMessage());
            ctx.status(500);
            ctx.json(node);
        });
        return instance;
    }


    public ApplicationConfig stopServer()
    {
        app.stop();
        return instance;
    }
}
