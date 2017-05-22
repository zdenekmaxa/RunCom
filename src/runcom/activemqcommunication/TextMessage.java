package runcom.activemqcommunication;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;



/**
 * Representation of a text message.
 * Broker stores all these messages.
 * 
 * @author Zdenek Maxa
 *
 */
public class TextMessage implements Serializable
{
	private static final long serialVersionUID = 5540353370735801203L;
	
    // date format
    public static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("dd/MM/yy HH:mm:ss");
   
	private String message = null;
	private Date timeStamp = null;
	private String from = null;
	private boolean toAcknowledge = false;
	

 
	
    public static String getClassName()
    {
        return "TextMessage";
        
    } // getClassName() -----------------------------------------------------

 
	
	public void setMessage(String msg)
	{
	    this.message = msg;
	    
	} // setMessage() -------------------------------------------------------
	
	
   	
	public String toString()
	{
		String r = "";
		String sDate = DATE_FORMAT.format(this.timeStamp); 
		r += "on \"" + sDate + "\" \"" + this.from + "\" wrote: \"" +
		     this.message + "\" to acknowledge: \"" + this.toAcknowledge + "\"";
		return r;
		
	} // toString() ---------------------------------------------------------
	

	
	public String getMessage()
	{
		return message;
		
	} // getMessage() -------------------------------------------------------


	
	public String getHeader()
	{
		String r = "";
		String sDate = DATE_FORMAT.format(this.timeStamp); 
		r += "on " + sDate + " " + this.from + " wrote:";
		return r;
		
	} // getHeader() --------------------------------------------------------

	
	
	public Date getTimeStamp()
	{
		return timeStamp;
		
	} // getTimeStamp() -----------------------------------------------------

	

	public void setTimeStamp(Date ts)
	{
	    this.timeStamp = ts;
	    
	} // setTimeStamp() -----------------------------------------------------
	
	

	public String getFrom() 
	{
		return from;
		
	} // getFrom() ----------------------------------------------------------
	
	
	
	public void setFrom(String from)
	{
	    this.from = from;
	    
	} // setFrom() ----------------------------------------------------------

	
	
	public boolean getToAcknowledge()
	{
		return toAcknowledge;
		
	} // getToAcknowledge() -------------------------------------------------

	
	
	public void setToAcknowledge(boolean status)
	{
		toAcknowledge = status;
		
	} // setToAcknowledge() -------------------------------------------------

	
	
} // class TextMessage ======================================================