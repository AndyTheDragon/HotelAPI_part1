package dat.controllers;

import dat.dao.GenericDao;
import dat.dto.ErrorMessage;
import dat.dto.TestEntityDTO;
import dat.entities.Hotel;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;


public class TestController
{
    private final GenericDao genericDao;


    public TestController(EntityManagerFactory emf)
    {
        genericDao = GenericDao.getInstance(emf);
    }

    public void getAll(Context ctx)
    {
        try
        {
            ctx.json(genericDao.findAll(Hotel.class));
        }
        catch (Exception ex)
        {
            ErrorMessage error = new ErrorMessage("Error getting entities");
            ctx.status(404).json(error);
        }
    }

    public void getById(Context ctx)
    {

        try {
            //long id = Long.parseLong(ctx.pathParam("id"));
            long id = ctx.pathParamAsClass("id", Long.class)
                    .check(i -> i>0, "id must be at least 0")
                    .getOrThrow((validator) -> new BadRequestResponse("Invalid id"));
            TestEntityDTO foundEntity = new TestEntityDTO(genericDao.read(Hotel.class, id));
            ctx.json(foundEntity);

        } catch (Exception ex){
            ErrorMessage error = new ErrorMessage("No entity with that id");
            ctx.status(404).json(error);
        }
    }

    public void create(Context ctx)
    {
        try
        {
            TestEntityDTO incomingTest = ctx.bodyAsClass(TestEntityDTO.class);
            Hotel entity = new Hotel(incomingTest);
            Hotel createdEntity = genericDao.create(entity);
            ctx.json(new TestEntityDTO(createdEntity));
        }
        catch (Exception ex)
        {
            ErrorMessage error = new ErrorMessage("Error creating entity");
            ctx.status(400).json(error);
        }
    }

    public void update(Context ctx)
    {
        try
        {
            //int id = Integer.parseInt(ctx.pathParam("id"));
            long id = ctx.pathParamAsClass("id", Long.class)
                    .check(i -> i>0, "id must be at least 0")
                    .getOrThrow((validator) -> new BadRequestResponse("Invalid id"));
            TestEntityDTO incomingEntity = ctx.bodyAsClass(TestEntityDTO.class);
            Hotel entity = new Hotel(incomingEntity);
            entity.setId(id);
            Hotel updatedEntity = genericDao.update(entity);
            TestEntityDTO returnedEntity = new TestEntityDTO(updatedEntity);
            ctx.json(returnedEntity);
        }
        catch (Exception ex)
        {
            ErrorMessage error = new ErrorMessage("Error updating entity");
            ctx.status(400).json(error);
        }
    }

    public void delete(Context ctx)
    {
        try
        {
            //long id = Long.parseLong(ctx.pathParam("id"));
            long id = ctx.pathParamAsClass("id", Long.class)
                    .check(i -> i>0, "id must be at least 0")
                    .getOrThrow((validator) -> new BadRequestResponse("Invalid id"));
            genericDao.delete(Hotel.class, id);
            ctx.status(204);
        }
        catch (Exception ex)
        {
            ErrorMessage error = new ErrorMessage("Error deleting entity");
            ctx.status(400).json(error);
        }
    }

}
