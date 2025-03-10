package dat.dao;

import dat.entities.Hotel;
import dat.entities.Room;

import java.util.List;

public class HotelDAO implements IHotelDAO
{
    public List<Hotel> getAllHotels()
    {
        return List.of();
    }

    public Hotel getHotelById(Long id)
    {
        return null;
    }

    public Hotel createHotel(Hotel hotel)
    {
        return null;
    }

    public Hotel updateHotel(Hotel hotel)
    {
        return null;
    }

    public void deleteHotel(Long id)
    {
    }

    @Override
    public Hotel addRoom(Hotel hotel, Room room)
    {
        return null;
    }

    @Override
    public Hotel removeRoom(Hotel hotel, Room room)
    {
        return null;
    }

    @Override
    public List<Room> getRoomsForHotel(Hotel hotel)
    {
        return List.of();
    }


}
