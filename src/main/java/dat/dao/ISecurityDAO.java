package dat.dao;

import dk.bugelhartmann.UserDTO;
import dat.entities.UserAccount;
import dat.exceptions.ValidationException;


public interface ISecurityDAO
{
    UserDTO getVerifiedUser(String username, String password) throws ValidationException;
    UserAccount createUser(String username, String password);
    UserAccount addRoleToUser(String username, String role);
    UserAccount removeRoleFromUser(String username, String role);
}
