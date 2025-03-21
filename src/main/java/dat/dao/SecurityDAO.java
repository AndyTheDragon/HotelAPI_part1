package dat.dao;

import dat.entities.UserAccount;
import dat.enums.Roles;
import dat.exceptions.ApiException;
import dat.exceptions.ValidationException;
import dat.entities.Role;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class SecurityDAO extends GenericDAO implements ISecurityDAO
{
    private final Logger logger = LoggerFactory.getLogger(SecurityDAO.class);

    public SecurityDAO(EntityManagerFactory emf)
    {
        super(emf);
    }

    @Override
    public UserDTO getVerifiedUser(String username, String password) throws ValidationException
    {

        UserAccount userAccount = super.getById(UserAccount.class, username); //Throws DaoException if user not found
        if (!userAccount.verifyPassword(password))
        {
            logger.error(userAccount.getUsername() + " " + userAccount.getPassword());
            throw new ValidationException("Password does not match");
        }
        return new UserDTO(userAccount.getUsername(), userAccount.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()));

    }

    @Override
    public UserAccount createUser(String username, String password)
    {
        UserAccount userAccount = new UserAccount(username, password);
        userAccount.addRole(Roles.USER);
        try
        {
            userAccount = super.create(userAccount);
            logger.info("User created (username " + username + ")");
            return userAccount;
        }
        catch (Exception e)
        {
            logger.error("Error creating user", e);
            throw new EntityExistsException("Error creating user", e);
        }
    }

    @Override
    public UserAccount addRoleToUser(String username, String role)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            UserAccount foundUserAccount = em.find(UserAccount.class, username);
            if (foundUserAccount == null)
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
            foundUserAccount.addRole(foundRole);
            em.getTransaction().commit();
            logger.info("Role added to user (username " + username + ", role " + role + ")");
            return foundUserAccount;
        }
        catch (Exception e)
        {
            logger.error("Error adding role to user", e);
            throw new ApiException(400, "Error adding role to user", e);
        }
    }

    @Override
    public UserAccount removeRoleFromUser(String username, String role)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            UserAccount foundUserAccount = em.find(UserAccount.class, username);
            if (foundUserAccount == null)
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
            foundUserAccount.removeRole(foundRole);
            em.getTransaction().commit();
            logger.info("Role removed from user (username " + username + ", role " + role + ")");
            return foundUserAccount;
        }
        catch (Exception e)
        {
            logger.error("Error removing role from user", e);
            throw new ApiException(400, "Error removing role from user", e);
        }
    }
}
