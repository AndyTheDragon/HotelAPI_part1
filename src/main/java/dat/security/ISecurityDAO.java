package dat.security;

import dk.bugelhartmann.UserDTO;
import dat.security.entities.User;
import dat.exceptions.ValidationException;


public interface ISecurityDAO
{
    UserDTO getVerifiedUser(String username, String password) throws ValidationException;
    User createUser(String username, String password);
    User addRoleToUser(String username, String role);
    User removeRoleFromUser(String username, String role);
}
