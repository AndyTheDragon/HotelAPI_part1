package dat.entities;

import dat.enums.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserAccount
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

    public void removeRole(String roleName)
    {
        //roles.remove(Roles.valueOf(roleName.toUpperCase()));
        roles.removeIf(r -> r.toString().equals(roleName));
    }


}
