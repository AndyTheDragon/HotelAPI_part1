package dat.dao;

import dat.exceptions.ApiException;
import dat.exceptions.ValidationException;
import dat.entities.Role;
import dat.entities.User;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class SecurityDAO implements ISecurityDAO
{
    private static SecurityDAO instance;
    private final EntityManagerFactory emf;
    private final Logger logger = LoggerFactory.getLogger(SecurityDAO.class);

    public SecurityDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    public static SecurityDAO getInstance(EntityManagerFactory emf)
    {
        if (instance == null)
        {
            instance = new SecurityDAO(emf);
        }
        return instance;
    }


    @Override
    public UserDTO getVerifiedUser(String username, String password) throws ValidationException
    {
        try (EntityManager em = emf.createEntityManager())
        {
            User user = em.find(User.class, username);
            if (user == null)
            {
                logger.error("User not found (username " + username + ")");
                throw new EntityNotFoundException("User not found (username " + username + ")");
            }
            //user.getRoles().size();
            if (!user.verifyPassword(password))
            {
                logger.error(user.getUsername() + " " + user.getPassword());
                throw new ValidationException("Password does not match");
            }
            return new UserDTO(user.getUsername(), user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()));
        }
    }

    @Override
    public User createUser(String username, String password)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            // check if user already exists
            User user = em.find(User.class, username);
            if (user != null)
            {
                logger.error("User already exists (username " + username + ")");
                throw new EntityExistsException("User already exists (username " + username + ")");
            }
            user = new User(username, password);
            em.getTransaction().begin();
            // check if role user already exists
            Role userRole = em.find(Role.class, "user");
            if (userRole == null)
            {
                logger.info("Role user not found, creating it");
                userRole = new Role("user");
                em.persist(userRole);
            }
            user.addRole(userRole);
            em.persist(user);
            em.getTransaction().commit();
            logger.info("User created (username " + username + ")");
            return user;
        } catch (Exception e)
        {
            logger.error("Error creating user", e);
            throw new ApiException(400, "Error creating user", e);
        }
    }

    @Override
    public User addRoleToUser(String username, String role)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            User foundUser = em.find(User.class, username);
            if (foundUser == null)
            {
                logger.error("User not found (username " + username + ")");
                throw new EntityNotFoundException("User not found (username " + username + ")");
            }
            Role foundRole = em.find(Role.class, role);
            if (foundRole == null)
            {
                logger.error("Role not found (role " + role + ")");
                throw new EntityNotFoundException("Role not found (role " + role + ")");
            }
            em.getTransaction().begin();
            foundUser.addRole(foundRole);
            em.getTransaction().commit();
            logger.info("Role added to user (username " + username + ", role " + role + ")");
            return foundUser;
        }
        catch (Exception e)
        {
            logger.error("Error adding role to user", e);
            throw new ApiException(400, "Error adding role to user", e);
        }
    }

    @Override
    public User removeRoleFromUser(String username, String role)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            User foundUser = em.find(User.class, username);
            if (foundUser == null)
            {
                logger.error("User not found (username " + username + ")");
                throw new EntityNotFoundException("User not found (username " + username + ")");
            }
            Role foundRole = em.find(Role.class, role);
            if (foundRole == null)
            {
                logger.error("Role not found (role " + role + ")");
                throw new EntityNotFoundException("Role not found (role " + role + ")");
            }
            em.getTransaction().begin();
            foundUser.removeRole(foundRole);
            em.getTransaction().commit();
            logger.info("Role removed from user (username " + username + ", role " + role + ")");
            return foundUser;
        }
        catch (Exception e)
        {
            logger.error("Error removing role from user", e);
            throw new ApiException(400, "Error removing role from user", e);
        }
    }
}
