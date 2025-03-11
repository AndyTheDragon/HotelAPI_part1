package dat.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dat.entities.Hotel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
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
    @JsonBackReference
    private List<RoomDTO> rooms = new ArrayList<>();

    public HotelDTO(Hotel hotel)
    {
        this.id = hotel.getId();
        this.name = hotel.getName();
        this.address = hotel.getAddress();
        this.rooms = hotel.getRooms().stream().map(RoomDTO::new).toList();
    }
}
