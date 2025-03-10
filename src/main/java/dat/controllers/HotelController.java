package dat.controllers;

import dat.dao.GenericDao;
import dat.dto.ErrorMessage;
import dat.dto.HotelDTO;
import dat.entities.Hotel;
import dat.entities.Room;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;


public class HotelController implements IController
{
    private final GenericDao genericDao;


    public HotelController(EntityManagerFactory emf)
    {
        genericDao = GenericDao.getInstance(emf);
    }

    @Override
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

    @Override
    public void getById(Context ctx)
    {

        try {
            //long id = Long.parseLong(ctx.pathParam("id"));
            long id = ctx.pathParamAsClass("id", Long.class)
                    .check(i -> i>0, "id must be at least 0")
                    .getOrThrow((validator) -> new BadRequestResponse("Invalid id"));
            HotelDTO foundEntity = new HotelDTO(genericDao.read(Hotel.class, id));
            ctx.json(foundEntity);

        } catch (Exception ex){
            ErrorMessage error = new ErrorMessage("No entity with that id");
            ctx.status(404).json(error);
        }
    }

    @Override
    public void create(Context ctx)
    {
        try
        {
            HotelDTO incomingTest = ctx.bodyAsClass(HotelDTO.class);
            Hotel entity = new Hotel(incomingTest);
            Hotel createdEntity = genericDao.create(entity);
            for (Room room : entity.getRooms())
            {
                room.setHotel(createdEntity);
                genericDao.update(room);
            }
            ctx.json(new HotelDTO(createdEntity));
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
            HotelDTO incomingEntity = ctx.bodyAsClass(HotelDTO.class);
            Hotel hotelToUpdate = genericDao.read(Hotel.class, id);
            if (incomingEntity.getName() != null)
            {
                hotelToUpdate.setName(incomingEntity.getName());
            }
            if (incomingEntity.getAddress() != null)
            {
                hotelToUpdate.setAddress(incomingEntity.getAddress());
            }
            Hotel updatedEntity = genericDao.update(hotelToUpdate);
            HotelDTO returnedEntity = new HotelDTO(updatedEntity);
            ctx.json(returnedEntity);
        }
        catch (Exception ex)
        {
            ErrorMessage error = new ErrorMessage("Error updating entity. " + ex.getMessage());
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

    public void getRooms(@NotNull Context context)
    {
        try
        {
            long id = context.pathParamAsClass("id", Long.class)
                    .check(i -> i>0, "id must be at least 0")
                    .getOrThrow((validator) -> new BadRequestResponse("Invalid id"));
            Hotel hotel = genericDao.read(Hotel.class, id);
            context.json(hotel.getRooms());
        }
        catch (Exception ex)
        {
            ErrorMessage error = new ErrorMessage("Error getting rooms");
            context.status(404).json(error);
        }
    }
}
