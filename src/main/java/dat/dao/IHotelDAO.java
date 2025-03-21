package dat.dao;

import dat.entities.Hotel;
import dat.entities.Room;

import java.util.List;

public interface IHotelDAO
{
    Hotel getHotelById(Long id);
    Hotel addRoom(Hotel hotel, Room room);
    Hotel removeRoom(Hotel hotel, Room room);
    List<Room> getRoomsForHotel(Hotel hotel);
}
