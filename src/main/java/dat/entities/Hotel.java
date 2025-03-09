package dat.entities;

import dat.dto.HotelDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Hotel
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    @OneToMany
    private List<Room> rooms;

    public Hotel(String name)
    {
        this.name = name;
    }

    public Hotel(HotelDTO hotelDTO)
    {
        this.id = hotelDTO.getId();
        this.name = hotelDTO.getName();
        this.address = hotelDTO.getAddress();
        this.rooms = hotelDTO.getRooms();
    }

    public void addRoom(Room room)
    {
        if (room != null)
        {
            rooms.add(room);
            room.setHotel(this);
        }
    }


}
