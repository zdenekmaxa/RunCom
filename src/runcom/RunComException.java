package runcom;

/**
 * @author Zdenek Maxa
 * RunComException class 
 */
@SuppressWarnings("serial")
public final class RunComException extends Exception
{
  //private static MyLogger logger = MyLogger.getLogger(RunComException.class);

    public RunComException(String msg)
    {
        super(msg);
        
    } // RunComException() --------------------------------------------------

} // class RunComException ==================================================
