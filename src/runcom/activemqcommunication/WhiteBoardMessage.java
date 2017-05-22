package runcom.activemqcommunication;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;



/**
 * Representation of the white board text message
 * 
 * @author Zdenek Maxa
 */
public class WhiteBoardMessage implements Serializable
{
    private static final long serialVersionUID = 1654453682440286430L;

    // date format
    public static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private String message = null;
    private Date timeStamp = null;


    
    
  
    public static String getClassName()
    {
        return "WhiteBoardMessage";
        
    } // getClassName() -----------------------------------------------------
        

    
    public String toString()
    {
        String r = "";
         
        r += "\"" + message + "\" ";
        r += "on \"" + getTimeStampString() + "\"";
        return r;
        
    } // toString() ---------------------------------------------------------
    

    
    public String getMessage()
    {
        return message;
        
    } // getMessage() -------------------------------------------------------

    
    
    public void setMessage(String msg)
    {
        this.message = msg;
        
    } // setMessage() -------------------------------------------------------

    
    
    public Date getTimeStamp()
    {
        return timeStamp;
        
    } // getTimeStamp() -----------------------------------------------------
    
    
    
    public String getTimeStampString()
    {
        String sDate = DATE_FORMAT.format(this.timeStamp);
        return sDate;
        
    } // getTimeStampString() -----------------------------------------------
    
    
    
    public void setTimeStamp(Date ts)
    {
        this.timeStamp = ts;
        
    } // setTimeStamp() -----------------------------------------------------
    

} // class WhiteBoardMessage ================================================