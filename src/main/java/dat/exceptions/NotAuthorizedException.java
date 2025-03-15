package dat.exceptions;

public class NotAuthorizedException extends RuntimeException
{
    private  int code;
    public NotAuthorizedException(int code, String message)
    {
      super(message);
      this.code = code;
    }
    public NotAuthorizedException(int code, String message, Exception e)
    {
      super(message, e);
      this.code = code;
    }
    public int getCode()
    {
      return code;
    }
}
