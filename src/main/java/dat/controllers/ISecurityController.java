package dat.controllers;

import io.javalin.http.Handler;


public interface ISecurityController
{
    Handler login(); // to get a token
    Handler register(); // to get a user
    Handler authenticate(); // to verify roles inside token
    Handler authorize();
    Handler verify(); // to verify a token
    Handler timeToLive(); // to check how long a token is valid
}
