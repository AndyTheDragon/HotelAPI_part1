package dat.dto;

public record ErrorMessage(Integer status,String message) {
    public ErrorMessage(String message) {
        this(0, message);
    }
}
