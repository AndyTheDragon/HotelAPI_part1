package dat.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import dat.dto.RoomDTO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Room
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "hotel_id")
    @JsonManagedReference
    private Hotel hotel;
    private String roomNumber;
    private double price;

    public Room(String roomNumber)
    {
        this.roomNumber = roomNumber;
    }

    public Room(RoomDTO roomDTO)
    {
        this.id = roomDTO.getId();
        this.roomNumber = roomDTO.getRoomNumber();
        this.price = roomDTO.getPrice();
    }


}
