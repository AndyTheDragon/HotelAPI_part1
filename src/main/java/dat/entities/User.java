package dat.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.mindrot.jbcrypt.BCrypt;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@NamedQueries(@NamedQuery(name = "User.deleteAllRows", query = "DELETE from User"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User implements ISecurityUser, Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @Column(name = "username", length = 25)
    private String username;

    @Basic(optional = false)
    @Column(name = "password")
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_name", referencedColumnName = "username"),
            inverseJoinColumns = @JoinColumn(name = "role_name", referencedColumnName = "role_name"))
    private Set<Role> roles = new HashSet<>();

    public User(String userName, String userPass)
    {
        this.username = userName;
        this.password = BCrypt.hashpw(userPass, BCrypt.gensalt());
    }

    public User(String userName, Set<Role> roleEntityList)
    {
        this.username = userName;
        this.roles = roleEntityList;
    }

    public Set<String> getRolesAsString()
    {
        return roles.stream().map(Role::getRoleName).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean verifyPassword(String pw)
    {
        return BCrypt.checkpw(pw, this.password);
    }

    @Override
    public void addRole(Role role)
    {
        if (role != null)
        {
            roles.add(role);
            role.getUsers().add(this);
        }
    }

    public void removeRole(Role role)
    {
        roles.remove(role);
        role.getUsers().remove(this);
    }

    public void removeRole(String roleName)
    {
        roles.removeIf(r -> r.getRoleName().equals(roleName));
        roles.stream()
                .filter(r -> r.getRoleName().equals(roleName))
                .findFirst()
                .ifPresent(r -> r.getUsers().remove(this));
    }


}
