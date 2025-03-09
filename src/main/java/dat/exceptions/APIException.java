package dat.exceptions;

public class APIException extends RuntimeException
{
    private final int code;

    public APIException(int code, String msg)
    {
        super(msg);
        this.code = code;
    }

    public APIException(int code, String msg, Exception e)
    {
        super(msg, e);
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}
