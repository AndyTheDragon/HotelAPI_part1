package dat.controllers;

import io.javalin.http.Context;


public interface ISecurityController
{
    void login(Context ctx); // to get a token
    void register(Context ctx); // to get a user
    void verify(Context ctx); // to verify a token
    void timeToLive(Context ctx); // to check how long a token is valid
    void accessHandler(Context ctx); // to check if a user has access to a route
}
