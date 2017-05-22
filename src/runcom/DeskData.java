package runcom;

import java.util.Date;
import java.util.HashMap;

import java.awt.Color;


import mylogger.MyLogger;



/**
 * Instances of this class hold all relevant information of a single subsystem
 * desk, i.e. its status, messages, etc
 * 
 * @author Zdenek Maxa
 */
public class DeskData
{
    private static MyLogger logger = MyLogger.getLogger(DeskData.class);
    
    
    // self-explanatory information fields about a particular subsystem desk
    // run-time desk data (as opposed to desk configuration (start-up) data
    // held in the DeskConfiguration
    private String deskName = null;    
    private String currentStatus = null;
    private String assignedUserName = null;
    private Date dqLastChecked = null; // data quality
    private int dqCheckPeriodMinutes = 60;
    private boolean dqStatusChecked = false; // true, false status flag
    private int notAckMessagesCounter = 0; // number of text messages to acknowledge
    // pending requests counters - each desk has a counter per request type,
    // if desk is in the ready state and Shift Leader issues a request,
    // corresponding pending request counter is increased ; counting requests
    // awaiting reaction - e.g. fulfilling a checklist
    private int pendingInjectionRequestsCounter = 0;
	private int pendingStableBeamRequestsCounter = 0;
    private int pendingStartRunRequestsCounter = 0;

    
    // possible desk subsystem statuses and states values, constants
    // states were renamed on 2008-08-17, before the states were named
    // unassigned, assigned, error, ready as the names of the constants
    // which stayed throughout the code base. it should not be misleading
    // that in the comments, etc the states are still referred via the
    // old names
    public static final String UNASSIGNED = "signed off";
    public static final String ASSIGNED = "signed in";
    public static final String ERROR = "error";
    public static final String READY = "ready";
    
    // colours defined for desk statuses
    private static HashMap<String, Color> stateColors = null;
    static
    {
        stateColors = new HashMap<String, Color>();
        stateColors.clear();
        stateColors.put(UNASSIGNED, Color.darkGray);
        stateColors.put(ASSIGNED, Color.blue);
        stateColors.put(ERROR, Color.red);
        stateColors.put(READY, Color.green.darker());
    }
    
    public static final String DQ_STATUS_CHECKED = "checked";
    public static final String DQ_STATUS_UNCHECKED = "unchecked";
    
    
    // reference to the desk GUI panel, once a change set on the instance,
    // the GUI is properly update from here via this GUI panel reference
    private IDeskPanel guiPanel = null;
    

    
    
    
    public DeskData(String deskName, IDeskPanel guiPanel)
    {
        this.deskName = deskName;
        this.guiPanel = guiPanel;
        this.currentStatus = UNASSIGNED;
        
        updateDeskGUIPanel();
        
    } // DeskData() ---------------------------------------------------------
    
    
    
    private void updateDeskGUIPanel()
    {
        Color color = stateColors.get(this.currentStatus);
        
        // update also enabled / disabled states of components
        // but only if running its own desk here
        // RunCom.getActiveSubSystem() may be null for observer, but
        // that should be all right
        if(this.deskName.equals(RunCom.getActiveSubSystem()) ||
           RunCom.isSupervisor())
        {
            // yes, hence the true flag
            this.guiPanel.setStatus(this.currentStatus, color, true);            
        }
        else
        {
            this.guiPanel.setStatus(this.currentStatus, color, false);
        }
        
    } // updateDeskGUIPanel() -----------------------------------------------
    
    
       
    /**
     * Method is called when local settings change.
     * @param status
     */
    synchronized public void setStatus(String status)
    {
        String prevStatus = this.currentStatus; 
        this.currentStatus = status;
        
        // new state is set, could update GUI panel of the desk
        updateDeskGUIPanel();
        
        logger.info("Desk \"" + deskName + "\" status \"" +
                    status + "\" set (was \"" + prevStatus + "\")");        
        
    } // setStatus() --------------------------------------------------------

    
    
    /**
     * Method is called when local settings change. Information should then
     * be distributed to other running instances.
     * @param name
     */
    synchronized public void setAssignedUserName(String name)
    {
        this.assignedUserName = name;
        
        this.guiPanel.setPersonNameTextField(this.assignedUserName);
        
        logger.info("Desk \"" + this.deskName + "\" set assigned user: \"" +
                    this.assignedUserName + "\"");   
        
    } // setAssignedUserName() ----------------------------------------------
    
        
    
    synchronized public String getAssignedUserName()
    {
        return this.assignedUserName;
        
    } // getAssignedUserName() ----------------------------------------------
    
    
    
    /**
     * Returns colour for the current desk status.
     * @return
     */
    synchronized public Color getCurrentStatusColor()
    {
        return stateColors.get(this.currentStatus);
        
    } // getCurrentStatusColor() --------------------------------------------
    
    
    
    public String getDeskName()
    {
        return this.deskName;
        
    } // getDeskName() ------------------------------------------------------
    
    
    
    synchronized public String getStatus()
    {
        return this.currentStatus;
        
    } // getStatus() --------------------------------------------------------
    

    
    public void enableDeskPanel(boolean enable)
    {
        this.guiPanel.setDeskPanelEnabled(enable);
        
    } // enableDeskPanel() --------------------------------------------------
    
    
    
    // synchronized b/c method is called by DQ status watcher thread
    synchronized  public Date getDqLastChecked()
    {
    	return this.dqLastChecked;
    	
    } // getDqLastChecked() -------------------------------------------------
    
    
    
    // synchronized b/c method is called by DQ status watcher thread
    synchronized public void setDqLastChecked(Date date)
    {
    	this.dqLastChecked = date;
    	this.guiPanel.setDQLastChecked(date);
    	
    } // setDqLastChecked() -------------------------------------------------
    
    
    
    // synchronized b/c method is called by DQ status watcher thread
    synchronized public int getDqCheckPeriodMinutes()
    {
    	return this.dqCheckPeriodMinutes;
    	
    } // getDqCheckPeriodMinutes() ------------------------------------------
    
    
    
    // synchronized b/c method is called by DQ status watcher thread
    synchronized  public void setDqCheckPeriodMinutes(int min)
    {
    	this.dqCheckPeriodMinutes = min;
    	this.guiPanel.setDQCheckPeriodMinutes(min);
    	
    } // setDqCheckPeriodMinutes() ------------------------------------------
    
    
    
    // synchronized b/c method is called by DQ status watcher thread
    synchronized  public boolean getDqStatusChecked()
    {
    	return this.dqStatusChecked;
    	
    } // getDqStatusChecked() -----------------------------------------------
    

    
    // synchronized b/c method is called by DQ status watcher thread
    synchronized public void setDqStatusChecked(boolean status)
    {
    	this.dqStatusChecked = status;
    	this.guiPanel.setDQStatusChecked(status);
    	    	
    } // setDqStatusChecked() -----------------------------------------------

    
    
    synchronized public void setNotAckMessagesCounter(int counter)
    {
    	this.notAckMessagesCounter = counter;
    	this.guiPanel.setNotAcknowledgedCounterLabel(String.valueOf(counter));
    	
    } // setNotAckMessagesCounter() -----------------------------------------
    
    
    
    synchronized public int getNotAckMessagesCounter()
    {
    	return this.notAckMessagesCounter;
    	
    } // getNotAckMessagesCounter() -----------------------------------------
    
    
    
    synchronized public int getPendingInjectionRequestsCounter()
    {
        return this.pendingInjectionRequestsCounter;
        
    } // getPendingInjectionRequestsCounter() -------------------------------
    
    
    
    synchronized public void setPendingInjectionRequestsCounter(int counter)
    {
        this.pendingInjectionRequestsCounter = counter;
        
    } // setPendingInjectionRequestsCounter() -------------------------------
    
    
    
    public synchronized int getPendingStableBeamRequestsCounter()
    {
		return this.pendingStableBeamRequestsCounter;
		
	} // getPendingStableBeamRequestsCounter() ------------------------------



	public synchronized void setPendingStableBeamRequestsCounter(int counter)
	{
		this.pendingStableBeamRequestsCounter = counter;
		
	} // setPendingStableBeamRequestsCounter() ------------------------------



	public synchronized int getPendingStartRunRequestsCounter()
	{
		return this.pendingStartRunRequestsCounter;
		
	} // getPendingStartRunRequestsCounter() --------------------------------



	public synchronized void setPendingStartRunRequestsCounter(int counter)
	{
		this.pendingStartRunRequestsCounter = counter;
		
	} // setPendingStartRunRequestsCounter() --------------------------------

	
	
    synchronized public String toString()
    {
    	
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
				 pendingInjectionRequestsCounter + "\"  ");
		r.append("pending stable beam requests: \"" +
				 pendingStableBeamRequestsCounter + "\"  ");
		r.append("pending config/start sequence requests: \"" +
				 pendingStartRunRequestsCounter + "\"");
		
	    return r.toString();

    } // toString() ---------------------------------------------------------

    
    
} // class DeskData =========================================================