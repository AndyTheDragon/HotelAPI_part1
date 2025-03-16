package dat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dat.config.HibernateConfig;
import dat.dto.ErrorMessage;
import dat.exceptions.ApiException;
import dat.exceptions.NotAuthorizedException;
import dat.exceptions.ValidationException;
import dat.security.entities.User;
import dat.utils.Utils;
import dk.bugelhartmann.*;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

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


    @Override
    public Handler login()
    {
        return (ctx) -> {
            ObjectNode returnJson = objectMapper.createObjectNode();
            try {
                UserDTO userInput = ctx.bodyAsClass(UserDTO.class);
                UserDTO verifiedUser = securityDAO.getVerifiedUser(userInput.getUsername(),  userInput.getPassword());
                String token = createToken(verifiedUser);
                returnJson.put("token", token)
                        .put("username", verifiedUser.getUsername());

                ctx.status(HttpStatus.OK).json(returnJson);
            }
            catch (EntityNotFoundException | ValidationException e) {
                logger.error("Error logging in user", e);
                //throw new APIException(401, "Could not verify user", e);
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorMessage("Could not verify user " + e.getMessage()));
            }
        };
    }

    @Override
    public Handler register()
    {
        return (ctx) -> {
            ObjectNode returnJson = objectMapper.createObjectNode();
            try {
                UserDTO userInput = ctx.bodyAsClass(UserDTO.class);
                User createdUser = securityDAO.createUser(userInput.getUsername(), userInput.getPassword());
                String token = createToken(new UserDTO(createdUser.getUsername(), Set.of("USER")));
                returnJson.put("token", token)
                        .put("username", createdUser.getUsername());

                ctx.status(HttpStatus.CREATED).json(returnJson);
            }
            catch (EntityExistsException e) {
                logger.error("Error registering user", e);
                //throw new APIException(422, "Could not register user: User already exists", e);
                ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).json(new ErrorMessage("User already exists " + e.getMessage()));
            }
        };
    }

    @Override
    public Handler authenticate()
    {
        return (ctx) -> {
            // This is a preflight request => no need for authentication
            if (ctx.method().toString().equals("OPTIONS")) {
                ctx.status(200);
                return;
            }
            // If the endpoint is not protected with roles or is open to ANYONE role, then skip
            Set<String> allowedRoles = ctx.routeRoles().stream().
                    map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
            if (isOpenEndpoint(allowedRoles))
                return;

            // If there is no token we do not allow entry
            String header = ctx.header("Authorization");
            if (header == null) {
                throw new UnauthorizedResponse("Authorization header is missing"); // UnauthorizedResponse is javalin 6 specific but response is not json!
//                throw new dat.exceptions.APIException(401, "Authorization header is missing");
            }

            // If the Authorization Header was malformed, then no entry
            String token = header.split(" ")[1];
            if (token == null) {
                throw new UnauthorizedResponse("Authorization header is malformed"); // UnauthorizedResponse is javalin 6 specific but response is not json!
//                throw new dat.exceptions.APIException(401, "Authorization header is malformed");

            }
            UserDTO verifiedTokenUser = verifyToken(token);
            if (verifiedTokenUser == null) {
                throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
//                throw new dat.exceptions.APIException(401, "Invalid user or token");
            }
            ctx.attribute("user", verifiedTokenUser); // -> ctx.attribute("user") in ApplicationConfig beforeMatched filter
        };
    }

    @Override
    public Handler authorize()
    {
        return (ctx) -> {
            Set<String> allowedRoles = ctx.routeRoles().stream()
                    .map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());

            // 1. Check if endpoint is open to all
            if (isOpenEndpoint(allowedRoles))
                return;
            // 2. Get user and ensure it is not null
            UserDTO user = ctx.attribute("user");
            if (user == null) {
                throw new ForbiddenResponse("No user was added from the token. Please authenticate first");
                // throw new APIException(401, "No user was added from the token. Please authenticate first");
            }
            // 3. Check if user has the required role
            if (!userHasAllowedRole(user, allowedRoles)) {
                throw new ForbiddenResponse("User does not have the required role to access this endpoint");
                // throw new APIException(403, "User does not have the required role to access this endpoint");
            }
        };
    }

    @Override
    public Handler verify()
    {
        return (ctx) -> {
            ObjectNode returnJson = objectMapper.createObjectNode();
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
            UserDTO verifiedTokenUser = verifyToken(token);
            if (verifiedTokenUser == null)
            {
                throw new UnauthorizedResponse("Invalid user or token");
            }
            returnJson.put("msg", "Token is valid");
            ctx.status(HttpStatus.OK).json(returnJson);
        };
    }

    @Override
    public Handler timeToLive()
    {
        return (ctx) -> {
            ObjectNode returnJson = objectMapper.createObjectNode();
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
            UserDTO verifiedTokenUser = verifyToken(token);
            if (verifiedTokenUser == null)
            {
                throw new UnauthorizedResponse("Invalid user or token");
            }
            int timeToLive = tokenSecurity.timeToExpire(token);
            LocalDateTime expireTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeToLive), ZoneId.systemDefault());
            ZonedDateTime zTime = expireTime.atZone(ZoneId.systemDefault());
            Long difference = zTime.toEpochSecond() - ZonedDateTime.now().toEpochSecond();
            returnJson.put("msg", "Token is valid until " + zTime)
                    .put("expireTime", zTime.toOffsetDateTime().toString())
                    .put("secondsToLive", difference);
            ctx.status(HttpStatus.OK).json(returnJson);
        };
    }

    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }


    private boolean isOpenEndpoint(Set<String> allowedRoles) {
        // If the endpoint is not protected with any roles:
        if (allowedRoles.isEmpty())
            return true;

        // 1. Get permitted roles and Check if the endpoint is open to all with the ANYONE role
        if (allowedRoles.contains("ANYONE")) {
            return true;
        }
        return false;
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
                ISSUER = Utils.getPropertyValue("ISSUER", "config.properties");
                TOKEN_EXPIRE_TIME = Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties");
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            }
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException(500, "Could not create token");
        }
    }

    private UserDTO verifyToken(String token) {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : Utils.getPropertyValue("SECRET_KEY", "config.properties");

        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                throw new NotAuthorizedException(403, "Token is not valid");
            }
        } catch (ParseException | NotAuthorizedException | TokenVerificationException e) {
            e.printStackTrace();
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
    }

}
