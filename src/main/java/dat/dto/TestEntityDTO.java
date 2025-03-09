package dat.dto;

import dat.entities.Hotel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TestEntityDTO
{
    private Long id;
    private String name;

    public TestEntityDTO(Hotel hotel)
    {
    }
}
