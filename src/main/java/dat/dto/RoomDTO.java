package dat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dat.entities.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RoomDTO
{
    @JsonIgnore
    private Long id;
    private Long hotelId;
    private String roomNumber;
    private double price;

    public RoomDTO(Room room)
    {
        this.id = room.getId();
        this.hotelId = room.getHotel().getId();
        this.roomNumber = room.getRoomNumber();
        this.price = room.getPrice();
    }
}
