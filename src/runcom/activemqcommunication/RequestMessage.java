package runcom.activemqcommunication;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;


public class RequestMessage implements Serializable
{
    private static final long serialVersionUID = -7840812262010938015L;
    
    // request message attributes
    
    // operation type - e.g. injection, serves to distinguish type of request
    private String requestType = null;
    
    private Date timeStamp = null;
        
    // date format
    public static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    
    
    
    
    public RequestMessage(String requestType)
    {
        this.requestType = requestType;
        this.timeStamp = new Date(System.currentTimeMillis());
        
    } // RequestMessage() ---------------------------------------------------

    
    
    public String getRequestType()
    {
        return requestType;
        
    } // getRequestType() ---------------------------------------------------

    
    
    public String toString()
    {
        String r = "";
        String sDate = DATE_FORMAT.format(timeStamp); 
        r += "request type: \"" + requestType + "\" " + 
             "timestamp of this message: \"" +
             sDate + "\"";
        return r;
        
    } // toString() ---------------------------------------------------------
    

} // class RequestMessage ===================================================
