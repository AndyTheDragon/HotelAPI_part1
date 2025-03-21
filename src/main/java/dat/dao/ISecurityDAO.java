package dat.dao;

import dat.enums.Roles;
import dk.bugelhartmann.UserDTO;
import dat.entities.UserAccount;
import dat.exceptions.ValidationException;


public interface ISecurityDAO
{
    UserDTO getVerifiedUser(String username, String password) throws ValidationException;
    UserAccount createUser(String username, String password);
    UserAccount addRoleToUser(String username, Roles role);
    UserAccount removeRoleFromUser(String username, Roles role);
}
