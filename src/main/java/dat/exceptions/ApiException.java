package dat.exceptions;

public class ApiException extends RuntimeException
{
    private final int code;

    public ApiException(int code, String msg)
    {
        super(msg);
        this.code = code;
    }

    public ApiException(int code, String msg, Exception e)
    {
        super(msg, e);
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}
