package dat.enums;

import io.javalin.security.RouteRole;

public enum Roles implements RouteRole
{
    ANYONE,
    USER,
    ADMIN
}