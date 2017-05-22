package runcom.activemqcommunication;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Communication representation of the DeskData information
 * associated with each desk.
 * 
 * @author Zdenek Maxa
 *
 */
public final class DeskDataMessage implements Serializable
{ 
	private static final long serialVersionUID = 6920279653637467355L;
    
    public static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   
	
	// self-explanatory information fields about a particular subsystem desk
	
	private Date timeOfMessage = null; // time when this message was sent
	
    private String deskName = null;
    private String currentStatus = null;
    private String assignedUserName = null;
    
    private Date dqLastChecked = null; // data quality last checked time
	private int dqCheckPeriodMinutes = 0;
    private boolean dqStatusChecked = false;
    
    private int notAckMessagesCounter = 0; // number of text messages to acknowledge
    
    // counters of requests awaiting reaction
    private int pendingInjectionRequestsCounter = 0; 
	private int pendingStableBeamRequestsCounter = 0;
    private int pendingStartRunRequestsCounter = 0;
    
      
    
    
    public static String getClassName()
    {
        return "DeskDataMessage";
        
    } // getClassName() -----------------------------------------------------

    
    
    public void setTimeOfMessage(Date timeOfMessage)
    {
        this.timeOfMessage = timeOfMessage;
        
    } // setTimeOfMessage() -------------------------------------------------
    
    

    public Date getTimeOfMessage()
    {
        return this.timeOfMessage;
        
    } // setTimeOfMessage() -------------------------------------------------
    
    
    
    public Date getDqLastChecked() 
    {
		return dqLastChecked;
		
	} // getDqLastChecked() -------------------------------------------------
    


	public void setDqLastChecked(Date dqLastChecked) 
	{
		this.dqLastChecked = dqLastChecked;
		
	} // setDqLastChecked() ------------------------------------------------



	public int getDqCheckPeriodMinutes() 
	{
		return dqCheckPeriodMinutes;
		
	} // getDqCheckPeriodMinutes() ------------------------------------------



	public void setDqCheckPeriodMinutes(int dqCheckPeriodMinutes)
	{
		this.dqCheckPeriodMinutes = dqCheckPeriodMinutes;
		
	} // setDqCheckPeriodMinutes() ------------------------------------------

	

	public void setDqStatusChecked(boolean dqStatusChecked) 
	{
		this.dqStatusChecked = dqStatusChecked;
		
	} // setDqStatusChecked() -----------------------------------------------

	
	
	public boolean getDqStatusChecked() 
	{
		return this.dqStatusChecked;
		
	} // getDqStatusChecked() -----------------------------------------------

	

    public String getDeskName()
    {
        return deskName;
        
    } // getDeskName() ------------------------------------------------------

    

    public void setDeskName(String deskName)
    {
        this.deskName = deskName;
        
    } // setDeskName() ------------------------------------------------------

    

    public String getCurrentStatus()
    {
        return currentStatus;
        
    } // getCurrentStatus() -------------------------------------------------

    

    public void setCurrentStatus(String currentStatus)
    {
        this.currentStatus = currentStatus;
        
    } // setCurrentStatus() -------------------------------------------------

    

    public String getAssignedUserName()
    {
        return assignedUserName;
        
    } // getAssignedUserName() ----------------------------------------------

    

    public void setAssignedUserName(String assignedUserName)
    {
        this.assignedUserName = assignedUserName;
        
    } // setAssignedUserName() ----------------------------------------------
    
    
    
    public void setNotAckMessagesCounter(int counter)
    {
    	this.notAckMessagesCounter = counter;
    	
    } // setNotAckMessagesCounter() -----------------------------------------
    
    
    
    public int getNotAckMessagesCounter()
    {
    	return this.notAckMessagesCounter;
    	
    } // getNotAckMessagesCounter() ----------------------------------------- 
    

    
    public int getPendingInjectionRequestsCounter()
    {
        return this.pendingInjectionRequestsCounter;
        
    } // getPendingInjectionRequestsCounter() -------------------------------
    
    
    
    public void setPendingInjectionRequestsCounter(int counter)
    {
        this.pendingInjectionRequestsCounter = counter;
        
    } // setPendingInjectionRequestsCounter() -------------------------------

    

    public int getPendingStableBeamRequestsCounter()
    {
		return this.pendingStableBeamRequestsCounter;
		
	} // getPendingStableBeamRequestsCounter() ------------------------------



	public void setPendingStableBeamRequestsCounter(int counter)
	{
		this.pendingStableBeamRequestsCounter = counter;
		
	} // setPendingStableBeamRequestsCounter() ------------------------------



	public int getPendingStartRunRequestsCounter()
	{
		return this.pendingStartRunRequestsCounter;
		
	} // getPendingStartRunRequestsCounter() --------------------------------



	public void setPendingStartRunRequestsCounter(int counter)
	{
		this.pendingStartRunRequestsCounter = counter;
		
	} // setPendingStartRunRequestsCounter() --------------------------------
    
    
    
    public String toString()
    {
    	String sDate = DATE_FORMAT.format(this.timeOfMessage);
    	
    	StringBuffer r = new StringBuffer("");
    	r.append("desk name: \"" + deskName + "\"  ");
    	r.append("status: \"" + currentStatus +  "\"  ");
    	r.append("assigned person: \"" + assignedUserName + "\"  ");
    	r.append("DQ last checked: \"" + dqLastChecked + "\"  ");
    	r.append("DQ check period: \"" + dqCheckPeriodMinutes + "\"  ");
    	r.append("DQ status checked: \"" + dqStatusChecked + "\"  ");
		r.append("messages awaiting acknowledgement: " +
				 "\"" + notAckMessagesCounter + "\"  ");    	
		r.append("pending injection requests: \"" +
				 pendingInjectionRequestsCounter + "\" ");
		r.append("pending stable beam requests: \"" +
				 pendingStableBeamRequestsCounter + "\" ");
		r.append("pending config/start sequence requests: \"" +
				 pendingStartRunRequestsCounter + "\" ");
    	r.append("timestamp of this message: \"" + sDate + "\"");
    	
        return r.toString();
        
    } // toString() ---------------------------------------------------------
    
    
    
} // class DeskDataMessage ==================================================