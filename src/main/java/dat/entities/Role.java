package dat.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name ="roles")
@NamedQueries(@NamedQuery(name = "Role.deleteAllRows", query = "DELETE from Role"))
public class Role implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @Column(name = "role_name", length = 20)
    private String roleName;

    @ManyToMany(mappedBy = "roles")
    private final Set<User> users = new HashSet<>();

    public Role() {}

    public Role(String roleName)
    {
        this.roleName = roleName;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public Set<User> getUsers()
    {
        return users;
    }

    @Override
    public String toString()
    {
        return "Role{" + "name='" + roleName + '\'' + '}';
    }
}
