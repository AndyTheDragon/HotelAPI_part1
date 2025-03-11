package dat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dat.dto.HotelDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "hotel", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JsonBackReference
    @ToString.Exclude
    private List<Room> rooms = new ArrayList<>();

    public Hotel(String name)
    {
        this.name = name;
    }

    public Hotel(String name, String address)
    {
        this.name = name;
        this.address = address;
    }

    public Hotel(HotelDTO hotelDTO)
    {
        this.name = hotelDTO.getName();
        this.address = hotelDTO.getAddress();
        this.rooms = hotelDTO.getRooms().stream().map(Room::new).toList();
    }

    public void addRoom(Room room)
    {
        if (room != null)
        {
            rooms.add(room);
            room.setHotel(this);
        }
    }

    public void removeRoom(Room room)
    {
        if (room != null)
        {
            rooms.remove(room);
            room.setHotel(null);
        }
    }


}
