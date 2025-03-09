package dat.exceptions;

public class DaoException extends RuntimeException
{
    private final int code;

    public DaoException(int code, String msg)
    {
        super(msg);
        this.code = code;
    }

    public DaoException(int code, String msg, Exception e)
    {
        super(msg, e);
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}
