package dat.exceptions;

public class DaoException extends RuntimeException
{

    public DaoException(String msg)
    {
        super(msg);
    }

    public DaoException(String msg, Exception e)
    {
        super(msg, e);
    }

}
