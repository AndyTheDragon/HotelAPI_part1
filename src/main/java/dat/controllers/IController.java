package dat.controllers;

import io.javalin.http.Context;

public interface IController
{
    void getAll(Context ctx);
    void getById(Context ctx);
    void create(Context ctx);
    void update(Context ctx);
    void delete(Context ctx);
}
