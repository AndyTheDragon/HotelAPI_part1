# API Security Guide (authentication & authorization)
Vi kan implementere authorization og authentication security i en Javalin applikation ved at implementere en `Handler`, 
som kaldes før hvert request bliver sendt ud til den `Controller`, som håndterer endpointet.

Vi kan bruge `JWT` (JSON Web Token) til at implementere authentication. Når en bruger logger ind, genererer serveren et
JWT, som sendes tilbage til klienten. Klienten gemmer JWT'en og sender den med i headeren af alle fremtidige requests.
Serveren kan så verificere JWT'en og tillade eller afvise requestet.

For at implementere authorization, kan vi bruge Javalins `RouteRole`. Vi kan gemme brugerens rolle i JWT'en og bruge
den til at bestemme, om brugeren har adgang til en given endpoint. Vi kan også gemme brugerens `Role` i den entitet som
repræsenterer brugeren.

## Roles
For hvert endpoint, definerer vi hvilken `Role` der har adgang til endpointet. Dette gøres ved at oprette en `enum` som
implementerer `RouteRole` interfacet. Du kan tilføje alle de værdier du har brug for, husk at navngive dem med nogle 
sigende navne.

```java
import io.javalin.security.RouteRole;

enum Role implements RouteRole 
{
    ANYONE,
    USER_READ, 
    USER_WRITE 
}
```
For hvert endpoint, kan vi definere hvilken `Role` der har adgang til endpointet. Dette gøres ved at tilføje `Role`'en 
som en parameter til `get`, `post`, `put` eller `delete` metoden.
```java
get("/", ctx -> ctx.redirect("/users"), Role.ANYONE);
path("users", () -> {
    get(UserController::getAllUserIds, Role.ANYONE);
    post(UserController::createUser, Role.USER_WRITE);
    path("{userId}", () -> {
        get(UserController::getUser, Role.USER_READ);
        patch(UserController::updateUser, Role.USER_WRITE);
        delete(UserController::deleteUser, Role.USER_WRITE);
    });
});
```
Bemærk at man godt kan tilføje mere end én `Role` til hvert endpoint.

## UserAccount
Vi er selvfølgelig nødt til at have en entitet som repræsenterer en bruger. Denne entitet skal have en `Role` som 
attribut, så vi kan gemme brugerens rolle i databasen. Man kan forestille sig et system hvor hver bruger kan have flere 
roller, og vi derfor har et `Set<Role>` i stedet for en enkelt `Role`. Vi får også brug for en metode til at tilføje 
(eller sette) en `Role`, og en metode til at fjerne en `Role`. Man kunne derfor forestille sig at have et interface 
`ISecurityUser` som ser således ud:

```java
public interface ISecurityUser {
    Set<Role> getRoles();
    boolean verifyPassword(String pw);
    void addRole(Role role);
    void removeRole(Role role);
}
```
Det er lidt et spørgsmål om personlig stil, hvorvidt man vil bruge `Role role` som parameter, eller om man vil bruge 
`String role`, og så oversætte `String` til enum-typen `Role`. Hvis man bruger `String`s vil man typisk også få brug for
en `Set<String> getRolesAsStrings()` metode.

Som minimum har vores `UserAccount` brug for felterne `String username` (kan bruges som ID, eller du kan tilføje et 
`Long userId`),  `String password` og `Set<Role> roles`.

### Password hashing
Husk at bruge bcrypt til at hashe kodeordet før det gemmes i databasen.
* bcrypt link: https://www.mindrot.org/projects/jBCrypt/
* tilføj til `pom.xml` en property: `<jbcrypt.version>0.4</jbcrypt.version>`
* og en dependency:
```xml
    <dependency>
      <groupId>org.mindrot</groupId>
      <artifactId>jbcrypt</artifactId>
      <version>${jbcrypt.version}</version>
    </dependency>
```
* Hash kodeordet i constructoren: `BCrypt.hashpw(userPass, BCrypt.gensalt());`
* Verify kodeordet: `BCrypt.checkpw(pw, this.password);`
<details>
  <summary>Eksempel UserAccount entitet</summary>

  ```java
  @Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserAccount implements ISecurityUser
{
    @Id
    private String username;
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Roles> roles = new HashSet<>();

    public UserAccount(String userName, String userPass)
    {
        this.username = userName;
        this.password = BCrypt.hashpw(userPass, BCrypt.gensalt());
    }

    public UserAccount(String userName, Set<Roles> roleEntityList)
    {
        this.username = userName;
        this.roles = roleEntityList;
    }

    public Set<String> getRolesAsString()
    {
        return roles.stream().map(Roles::toString).collect(Collectors.toSet());
    }

    public boolean verifyPassword(String pw)
    {
        return BCrypt.checkpw(pw, this.password);
    }

    public void addRole(Roles role)
    {
        if (role != null)
        {
            roles.add(role);
        }
    }

    public void removeRole(Roles role)
    {
        roles.remove(role);
    }
}
  
  ```

</details>

## SecurityDAO
Vi har brug for at kunne oprette og hente brugere fra databasen, at kunne tilføje og fjerne roller fra brugere. Vi kan 
oprette et interface `ISecurityDAO` som ser således ud:

```java
public interface ISecurityDAO
{
    UserAccount getUser(String username, String password); // used for login, also verifies password
    UserAccount createUser(UserAccount user); // used for registration of new users
    UserAccount addRole(UserAccount user, Role role);
    UserAccount removeRole(UserAccount user, Role role);
}
```
I stedet for at sende `UserAccount` objekter med ind i metoderne kunne vi nøjes med at sende `String username` og evt. 
`String password` ind. Det er igen et spørgsmål om personlig stil. Overvej hvordan du gør i dine andre DAO klasser i 
projektet, og gør noget tilsvarende her.
<details>
  <summary>Eksempel SecurityDao </summary>

  ```java
  
public class SecurityDao implements ISecurityDao
{
    private final EntityManagerFactory emf;
    private final Logger logger = LoggerFactory.getLogger(SecurityDao.class);
    
    SecurityDao(EntityManagerFactory emf)
    {
        this.emf = emf;
    }
    
    @Override
    public UserAccount getUser(String username, String password)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            UserAccount foundUser = em.find(UserAccount.class, username);
            if (foundUser == null)
            {
                logger.error("No user found with username: {}", username);
                throw new NoResultException("No user found with username: {}", username);
            }
            if (foundUser.verifyPassword(password)) //does this throw an exception if password is wrong?
            {
                return foundUser;
            }
            logger.error("Wrong password for user: {}", username);
            return null;
        }
    }
    
    @Override
    public UserAccount createUser(UserAccount user)
    {
        // consider checking the Roles in the user object, should we allow users with no roles?
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(user); // throws EntityExistsException if user already exists
            em.getTransaction().commit();
            return user;
        }
    }
    
    @Override
    public UserAccount addRole(UserAccount user, Role role)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            UserAccount foundUser = em.find(UserAccount.class, user.getUsername());
            if (foundUser == null)
            {
                logger.error("No user found with username: {}", user.getUsername());
                throw new NoResultException("No user found with username: {}", user.getUsername());
            }
            em.getTransaction().begin();
            foundUser.addRole(role);
            UserAccount updatedUser = em.merge(foundUser);
            em.getTransaction().commit();
            return updatedUser;
        }
    }
    
    @Override
    public UserAccount removeRole(UserAccount user, Role role)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            UserAccount foundUser = em.find(UserAccount.class, user.getUsername());
            if (foundUser == null)
            {
                logger.error("No user found with username: {}", user.getUsername());
                throw new NoResultException("No user found with username: {}", user.getUsername());
            }
            em.getTransaction().begin();
            foundUser.removeRole(role);
            UserAccount updatedUser = em.merge(foundUser);
            em.getTransaction().commit();
            return updatedUser;
        }
    }
 
    
}
  
  ```
Man kunne vælge at gribe de exceptions der bliver kastet, og lave dem om til en custom exception, f.eks. en 
`DaoException`, eller bruge de i Javalin og Jakarta inkluderede exceptions. Det er igen et spørgsmål om personlig stil. 
Se evt. https://javalin.io/documentation#default-responses
</details>

### Test af DAO
Hvis du ikke allerede har gjort det, så er det nu du skriver test til din SecurityDao klasse.

<details>
  <summary>Eksempel skelet til SecurityDao test</summary>

  ```java
  class SecurityDAOTest {
    private static final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryForTest();
    private static final SecurityDAO securityDAO = new SecurityDAO(emf);
    private  UserAccount testUserAccount;

    @BeforeEach
    void setUp() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Clean up existing data
            em.createQuery("DELETE FROM UserAccount").executeUpdate();

            // Create test user with user role
            testUserAccount = new UserAccount("testuser", "password123");
            testUserAccount.addRole(Roles.USER);
            em.persist(testUserAccount);

            em.getTransaction().commit();
        }
    }


    @Test
    void testGetVerifiedUser_Success() throws ValidationException {
        // Arrange
        String username = testUserAccount.getUsername();
        String password = testUserAccount.getPassword();

        // Act
        UserAccount result = securityDAO.getVerifiedUser(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertTrue(result.getRoles().contains(Roles.USER));
        assertEquals(1, result.getRoles().size());
    }

    @Test
    void testGetVerifiedUser_WrongPassword() {
        fail();
    }

    @Test
    void testGetVerifiedUser_UserNotFound() {
        fail();
    }

    @Test
    void testCreateUser_Success() {
        // Arrange

        // Act

        // Assert
        
        // Verify user was persisted with the user role
        
    }

    @Test
    void testCreateUser_UserAlreadyExists() {
        // Arrange
        String existingUsername = "testuser";
        String password = "newpassword";

        // Act & Assert
        EntityExistsException exception = assertThrows(EntityExistsException.class,
                () -> securityDAO.createUser(existingUsername, password));

        assertTrue(exception.getMessage().contains("Error creating user"));
    }

    @Test
    void testAddRoleToUser_Success() {
        // Arrange
        String username = testUserAccount.getUsername();

        // Act
        UserAccount result = securityDAO.addRoleToUser(username, Roles.ADMIN);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains(Roles.USER));
        assertTrue(result.getRoles().contains(Roles.ADMIN));

        // Verify role was added in the database
        try (EntityManager em = emf.createEntityManager()) {
            UserAccount persistedUserAccount = em.find(UserAccount.class, username);
            assertNotNull(persistedUserAccount);
            assertEquals(2, persistedUserAccount.getRoles().size());
            assertTrue(persistedUserAccount.getRolesAsString().contains("ADMIN"));
        }
    }

    @Test
    void testAddRoleToUser_UserNotFound() {
        fail();
    }


    @Test
    void testRemoveRoleFromUser_Success() {
        // Arrange

        // Act

        // Assert

        // Verify role was removed in the database
        try (EntityManager em = emf.createEntityManager()) {
            fail();
        }
    }

    @Test
    void testRemoveRoleFromUser_UserNotFound() {
        fail();
    }

}

  ```

</details>

## Endpoints og SecurityController
Vi har brug for at definere to endpoints:
* `POST /register` der lader os oprette en nye UserAccount og returnerer et JWT
* `POST /login` der også returnerer et JWT

```java
path("auth", () -> {
    post("/register", SecurityController::register, Role.ANYONE);
    post("/login", SecurityController::login, Role.ANYONE)
});
```

Ud over de to åbenlyse metoder `register()` og `login()` skal vi også have en metode til at authentikere og autorisere 
brugerem, vi kan kalde den `authenticateAndAuthorize()`. Og så får vi brug for nogle hjælpemetoder til at generere og
verificere JWTs. De to sidste metoder kan med fordel være private, men nu tager jeg dem med i interfacet alligevel. 

```java
public interface ISecurityController
{
    void register(Context ctx);
    void login(Context ctx);
    void authenticateAndAuthorize(Context ctx, Set<Role> roles);
    String generateToken(UserDTO user);
    UserDTO verifyToken(String token);
}
```

Det frarådes at forsøge at implementere JWT selv, da det kræver at man bruger en kryptografisk sikker algoritme. Derfor 
bruger vi et bibliotek `TokenSecurity`
* Tilføj dette repository til din `pom.xml` fil (under `</properties>`):
```xml
    <repositories>
      <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
      </repository>
    </repositories>
```

* Tilføj denne property til din `pom.xml` fil:
```xml
    <token.security.version>1.0.4</token.security.version>
```

* Tilføj dette dependency til din `pom.xml` fil:
```xml
    <dependency>
        <groupId>com.github.Hartmannsolution</groupId>
        <artifactId>TokenSecurity</artifactId>
        <version>${token.security.version}</version>
    </dependency>

```
* Tilføj følgende til din `config.properties` eller til dine miljøvariabler:
```properties
SECRET_KEY="hemmeligmindst32charactererlang"
ISSUER="dit navn"
TOKEN_EXPIRE_TIME=3600000
```
Den indeholder en `UserDTO` klasse, og en klasse `TokenSecurity` med metoderne `generateToken()` og `verifyToken()`, 
som vi vil bruge.

### Implementering af SecurityController
#### Register

1. The user sends a register (POST) request to the server with a username and password.
2. The server checks if the username is taken.
3. If the username is taken, the server returns an error.
4. If the username is not taken, the server creates a user and returns a JWT token.

#### Login

5. The user sends a login (POST) request to the server with a username and password.
6. The server checks if the username and password are valid.
7. If the username and password are valid, the server returns a JWT token.
8. If the username and password are invalid, the server returns an error.

#### Authenticate

9. The user sends a request to a protected endpoint with the JWT token in the header ('Authorization': 'Bearer ' + token).
10. A `before` filter calls the `authenticate` method in the SecurityController and check if the token is valid
11. If it is valid the user is added to the context as an attribute and the request is passed on to the endpoint.
11. If the JWT token is valid, the server checks if the user has the required roles to access the endpoint.
12. If the JWT token is invalid or missing, the server returns an error.
13. If the user doesn't have the required roles, the server returns an error.

<details>
  <summary>Eksempel SecurityController</summary>
  
  ```java
public class SecurityController implements ISecurityController
{
    private final ISecurityDAO securityDAO;
    private final ITokenSecurity tokenSecurity = new TokenSecurity();
    private final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    public SecurityController(ISecurityDAO securityDAO)
    {
        this.securityDAO = securityDAO;
    }

    @Override
    public void register(Context ctx)
    {
        UserDTO userDTO = ctx.bodyAsClass(UserDTO.class);
        UserAccount user = new UserAccount(userDTO.getUsername(), userDTO.getPassword());
        UserAccount createdUser = securityDAO.createUser(user);
        ctx.json(new UserDTO(createdUser.getUsername(), createdUser.getRolesAsString()));
    }

    @Override
    public void login(Context ctx)
    {
        UserDTO userDTO = ctx.bodyAsClass(UserDTO.class);
        UserAccount user = securityDAO.getUser(userDTO.getUsername(), userDTO.getPassword());
        if (user != null)
        {
            String token = generateToken(new UserDTO(user.getUsername(), user.getRolesAsString()));
            ctx.json(new UserDTO(user.getUsername(), user.getRolesAsString(), token));
        } else
        {
            ctx.status(401).result("Invalid username or password");
        }
    }

    @Override
    public void authenticateAndAuthorize(Context ctx, Set<Role> roles)
    {
        if (ctx.method().toString().equals("OPTIONS")) {
            ctx.status(200);
            return;
        }
        // 1. Check if endpoint is open to all
        // If the endpoint is not protected with roles or is open to ANYONE role, then skip auth
        Set<RouteRole> permittedRoles = ctx.routeRoles();
        if (permittedRoles.isEmpty() || permittedRoles.contains(Roles.ANYONE)){
            return;
        }
        // Authenticate user
        // Check that token is present and not malformed
        String header = ctx.header("Authorization");
        if (header == null)
        {
            throw new UnauthorizedResponse("Missing Authorization header");
        }
        String token = header.split(" ")[1];
        if (token == null)
        {
            throw new UnauthorizedResponse("Authorization header is malformend");
        }
        // Get UserDTO from token
        UserDTO user = verifyToken(token);
        if (user == null)
        {
            throw new UnauthorizedResponse("Invalid token");
        }
        // Authorize user
        // Check if user has the required roles
        if (!userHasAllowedRole(verifiedTokenUser, permittedRoles))
        {
            throw new ForbiddenResponse("Insufficient permissions");
        }
        ctx.attribute("user", user);
    }
    private boolean userHasAllowedRole(UserDTO user, Set<RouteRole> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(Roles.valueOf(role.toUpperCase())));
    }

    @Override
    public String generateToken(UserDTO user)
    {
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
            throw new Exception("Could not create token");
        }
    }

    @Override
    public UserDTO verifyToken(String token)
    {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : PropertyReader.getPropertyValue("SECRET_KEY", "config.properties");

        try
        {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token))
            {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else
            {
                throw new TokenException("Token is not valid or has expired");
            }
        } catch (ParseException | TokenException | TokenVerificationException e)
        {
            logger.error("Token is not valid or has expired", e);
            throw new UnauthorizedResponse("Token is not valid or has expired");
        }
    }
}
  ```
Man kan lave rigtigt mange overvejelser omkring brug af Logger og Exceptions her. Husk at skrive noget kode som 
stilmæssigt passer sammen med den kode du allerede har skrevet i projektet.
</details>

### Kald af `authenticateAndAuthorize()` metoden i `before` filter
```java
app.beforeMatched(SecurityController::authenticateAndAuthorize);
```
Dette tilføjes til din Javalin app, enten i din `ApplicationConfig` klasse (hvis du bruger sådan en), eller der hvor du 
definerer dine routes. (Lige før eller lige efter dine routes.)

### Test af SecurityController
Hvis du ikke allerede har gjort det, så er det nu du skriver test til din SecurityController klasse.