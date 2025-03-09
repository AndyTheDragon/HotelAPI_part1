package dat.dto;

import dat.entities.Hotel;
import dat.entities.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HotelDTO
{
    private Long id;
    private String name;
    private String address;
    private List<Room> rooms;

    public HotelDTO(Hotel hotel)
    {
        this.id = hotel.getId();
        this.name = hotel.getName();
        this.address = hotel.getAddress();
        this.rooms = hotel.getRooms();
    }
}
