package dat.dao;

import dat.entities.UserAccount;
import dat.enums.Roles;
import dat.exceptions.DaoException;
import dat.exceptions.ValidationException;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;
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
    public UserDTO getVerifiedUser(String username, String password) throws ValidationException, DaoException
    {

        UserAccount userAccount = super.getById(UserAccount.class, username); //Throws DaoException if user not found
        if (!userAccount.verifyPassword(password))
        {
            logger.error("{} {}", userAccount.getUsername(), userAccount.getPassword());
            throw new ValidationException("Password does not match");
        }
        return new UserDTO(userAccount.getUsername(), userAccount.getRoles()
                                                    .stream()
                                                    .map(Roles::toString)
                                                    .collect(Collectors.toSet()));

    }

    @Override
    public UserAccount createUser(String username, String password)
    {
        UserAccount userAccount = new UserAccount(username, password);
        userAccount.addRole(Roles.USER);
        try
        {
            userAccount = super.create(userAccount);
            logger.info("User created (username {})", username);
            return userAccount;
        }
        catch (Exception e)
        {
            logger.error("Error creating user", e);
            throw new EntityExistsException("Error creating user", e);
        }
    }

    @Override
    public UserAccount addRoleToUser(String username, Roles role)
    {
        UserAccount foundUser = super.getById(UserAccount.class, username);
        foundUser.addRole(role);
        try
        {
            foundUser = super.update(foundUser);
            logger.info("Role added to user (username {}, role {})", username, role);
            return foundUser;
        }
        catch (Exception e)
        {
            logger.error("Error adding role to user", e);
            throw new DaoException("Error adding role to user", e);
        }
    }

    @Override
    public UserAccount removeRoleFromUser(String username, Roles role)
    {
        UserAccount foundUserAccount = super.getById(UserAccount.class, username);
        foundUserAccount.removeRole(role);
        try
        {
            foundUserAccount = super.update(foundUserAccount);
            logger.info("Role removed from user (username {}, role {})", username, role);
            return foundUserAccount;
        }
        catch (Exception e)
        {
            logger.error("Error removing role from user", e);
            throw new DaoException("Error removing role from user", e);
        }
    }
}
