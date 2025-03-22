package dat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dat.config.HibernateConfig;
import dat.dao.ISecurityDAO;
import dat.dao.SecurityDAO;
import dat.dto.ErrorMessage;
import dat.entities.UserAccount;
import dat.enums.Roles;
import dat.exceptions.ApiException;
import dat.exceptions.DaoException;
import dat.exceptions.ValidationException;
import dat.utils.PropertyReader;
import dk.bugelhartmann.*;
import io.javalin.http.*;
import io.javalin.security.RouteRole;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

public class SecurityController implements ISecurityController
{
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ITokenSecurity tokenSecurity = new TokenSecurity();
    private final ISecurityDAO securityDAO;
    private final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    public SecurityController()
    {
        this.securityDAO = new SecurityDAO(HibernateConfig.getEntityManagerFactory());
    }

    public SecurityController(EntityManagerFactory emf)
    {
        this.securityDAO = new SecurityDAO(emf);
    }

    public SecurityController(ISecurityDAO securityDAO)
    {
        this.securityDAO = securityDAO;
    }

    // Health check for the API. Used in deployment
    public void healthCheck(@NotNull Context ctx) {
        ctx.status(200).json("{\"msg\": \"API is up and running\"}");
    }


    @Override
    public void login(Context ctx)
    {
        ObjectNode returnJson = objectMapper.createObjectNode();
        try {
            UserDTO userInput = ctx.bodyAsClass(UserDTO.class);
            UserDTO verifiedUser = securityDAO.getVerifiedUser(userInput.getUsername(),  userInput.getPassword());
            String token = createToken(verifiedUser);
            returnJson.put("token", token)
                    .put("username", verifiedUser.getUsername());

            ctx.status(HttpStatus.OK).json(returnJson);
        }
        catch (EntityNotFoundException | ValidationException | DaoException e) {
            logger.error("Error logging in user", e);
            throw new ApiException(401, "Could not verify user", e);
            //ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorMessage("Could not verify user " + e.getMessage()));
        }
    }

    @Override
    public void register(Context ctx)
    {
        ObjectNode returnJson = objectMapper.createObjectNode();
        try {
            UserDTO userInput = ctx.bodyAsClass(UserDTO.class);
            UserAccount createdUserAccount = securityDAO.createUser(userInput.getUsername(), userInput.getPassword());
            String token = createToken(new UserDTO(createdUserAccount.getUsername(), Set.of("USER")));
            returnJson.put("token", token)
                    .put("username", createdUserAccount.getUsername());

            ctx.status(HttpStatus.CREATED).json(returnJson);
        }
        catch (EntityExistsException e) {
            logger.error("Error registering user", e);
            //throw new APIException(422, "Could not register user: User already exists", e);
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).json(new ErrorMessage("User already exists " + e.getMessage()));
        }
    }

    public void accessHandler(Context ctx)
    {
        // This is a preflight request => no need for authentication
        if (ctx.method().toString().equals("OPTIONS")) {
            ctx.status(200);
            return;
        }

        // 1. Check if endpoint is open to all
        // If the endpoint is not protected with roles or is open to ANYONE role, then skip
        Set<RouteRole> permittedRoles = ctx.routeRoles();
        if (permittedRoles.isEmpty() || permittedRoles.contains(Roles.ANYONE)){
            return;
        }

        // Check that token is present and not malformed, and get the User from the token
        UserDTO verifiedTokenUser = getUserFromToken(ctx);
        if (verifiedTokenUser == null) {
            throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
//                throw new dat.exceptions.APIException(401, "Invalid user or token");
        }
        ctx.attribute("user", verifiedTokenUser);

        if (!userHasAllowedRole(verifiedTokenUser, permittedRoles)) {
            throw new ForbiddenResponse("User does not have the required role to access this endpoint");
            // throw new APIException(403, "User does not have the required role to access this endpoint");
        }

    }

    private UserDTO getUserFromToken(Context ctx)
    {
        String header = ctx.header("Authorization");
        if (header == null)
        {
            throw new UnauthorizedResponse("Authorization header is missing");
        }
        String token = header.split(" ")[1];
        if (token == null)
        {
            throw new UnauthorizedResponse("Authorization header is malformed");
        }
        return verifyToken(token);
    }

    @Override
    public void verify(Context ctx)
    {

        ObjectNode returnJson = objectMapper.createObjectNode();
        UserDTO verifiedTokenUser = getUserFromToken(ctx);
        if (verifiedTokenUser == null)
        {
            throw new UnauthorizedResponse("Invalid user or token");
        }
        returnJson.put("msg", "Token is valid");
        ctx.status(HttpStatus.OK).json(returnJson);

    }

    @Override
    public void timeToLive(Context ctx)
    {

        ObjectNode returnJson = objectMapper.createObjectNode();

        UserDTO verifiedTokenUser = getUserFromToken(ctx);
        if (verifiedTokenUser == null)
        {
            throw new UnauthorizedResponse("Invalid user or token");
        }

        String token = Objects.requireNonNull(ctx.header("Authorization")).split(" ")[1];
        int timeToLive;
        try
        {
            timeToLive = tokenSecurity.timeToExpire(token);
        } catch (ParseException e)
        {
            throw new UnauthorizedResponse("Token could not be parsed. Invalid token");
        }
        logger.info("Time to live: {}", timeToLive);
        LocalDateTime expireTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeToLive), ZoneId.systemDefault());
        ZonedDateTime zTime = expireTime.atZone(ZoneId.systemDefault());
        Long difference = zTime.toEpochSecond() - ZonedDateTime.now().toEpochSecond();
        returnJson.put("msg", "Token is valid until " + zTime)
                .put("expireTime", zTime.toOffsetDateTime().toString())
                .put("secondsToLive", difference);
        ctx.status(HttpStatus.OK).json(returnJson);

    }


    private boolean userHasAllowedRole(UserDTO user, Set<RouteRole> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(Roles.valueOf(role.toUpperCase())));
    }

    private String createToken(UserDTO user) {
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = PropertyReader.getPropertyValue("ISSUER", "config.properties");
                TOKEN_EXPIRE_TIME = PropertyReader.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties");
                SECRET_KEY = PropertyReader.getPropertyValue("SECRET_KEY", "config.properties");
            }
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch (Exception e) {
            logger.error("Error creating token", e);
            throw new ApiException(500, "Could not create token");
        }
    }

    private UserDTO verifyToken(String token) {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : PropertyReader.getPropertyValue("SECRET_KEY", "config.properties");

        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                throw new ApiException(403, "Token is not valid");
            }
        } catch (ParseException | ApiException | TokenVerificationException e) {
            logger.error("Error verifying token", e);
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
    }

}
