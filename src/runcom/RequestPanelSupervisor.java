package runcom;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import runcom.activemqcommunication.RequestMessage;

import mylogger.MyLogger;



/**
 * Request panel containing all requests, for Shift Leader running supervisor
 * mode.
 * 
 * Implementing new request: new graphical panel here with button listeners
 * and then additions in 
 * RunCom.processRequestMessage(), RunCom.setDeskDetailsApprovedByCheckList()
 * and in the OverallPanel if such request affects the pending requests
 * indicator.
 * 
 * If there is going to be another request type, it would be better hold all
 * panels in a container (HashMap accessible via request name), the same with
 * pending request counters within the DeskData - it will either be an array or
 * a HashMap of counters -> then code duplication for each request as well as
 * duplicated reset*RequestCounter() methods will be reduced.
 * 
 * @author Zdenek Maxa
 *
 */
@SuppressWarnings("serial")
public class RequestPanelSupervisor extends JPanel
{
    private static MyLogger logger = MyLogger.getLogger(RequestPanelSupervisor.class);
    
    // reference to all desk data
    private static LinkedHashMap<String, DeskData> deskDataMap = null;
    
    // date format
    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    // command type name associated with each request
    protected static final String COMMAND_REQUEST = "request";
    protected static final String COMMAND_RESET = "reset";
    
    // ready for beam injection request
    public static final String INJECTION = "injection";
    private static RequestSubPanel injectionPanel = null;
    // ready for stable beam request
    public static final String STABLE_BEAM = "stablebeam";
    private static RequestSubPanel stableBeamPanel = null;
    // ready for config / start run sequence request
    public static final String START_RUN = "startrun";
    private static RequestSubPanel startRunPanel = null;
    
    
    
    
    
    
    public RequestPanelSupervisor(LinkedHashMap<String, DeskData> deskDataMap)
    {
        RequestPanelSupervisor.deskDataMap = deskDataMap;
    
        this.setLayout(new FlowLayout());
        
        // note: if there is going to be more panels, they should be made in
        // a loop and stored in a container
        
        String title = "ready for injection";
        injectionPanel = new RequestSubPanel(title, INJECTION);
        this.add(injectionPanel);
        
        title = "ready for stable beam";
        stableBeamPanel = new RequestSubPanel(title, STABLE_BEAM);
        this.add(stableBeamPanel);
        
        title = "ready for config/start run sequence";
        startRunPanel = new RequestSubPanel(title, START_RUN);
        this.add(startRunPanel);
        		
    } // RequestPanelSupervisor() -------------------------------------------

    
        
    // following reset*RequestCounter() methods - code is either duplicated and
    // more efficient (as it is now), all three methods could be merged into
    // one, but then more String comparisons (to find out type of the request
    // will have to be done)

    
    
    private static void resetInjectionRequestCounter()
    {
        for(Iterator<DeskData> iter = deskDataMap.values().iterator(); iter.hasNext();)
        {
            DeskData deskData = iter.next();

            int counter = deskData.getPendingInjectionRequestsCounter();
        	if(counter > 0)
        	{
        		counter = 0;
        		deskData.setPendingInjectionRequestsCounter(counter);
                
                logger.info("Setting pending injection requests counter to " +
                                counter + " for desk \"" + deskData.getDeskName() + "\"");
                
                RunCom.sendMessage(deskData); // distribute

                // overall panel of this instance is only updated when its own
                // message arrives from network, then the overall panel is
                // updated from RunCom.updateLocalData() method
                
                // the same is true for request panel indicator - also updated
                // only after receiving a message from network
            }
        }
    	    	
    } // resetInjectionRequestCounter() ------------------------------------
    
    
    
    private static void resetStableBeamRequestCounter()
    {
        for(Iterator<DeskData> iter = deskDataMap.values().iterator(); iter.hasNext();)
        {
            DeskData deskData = iter.next();

            int counter = deskData.getPendingStableBeamRequestsCounter();
        	if(counter > 0)
        	{
        		counter = 0;
        		deskData.setPendingStableBeamRequestsCounter(counter);
                
                logger.info("Setting pending stable beam requests counter to " +
                                counter + " for desk \"" + deskData.getDeskName() + "\"");
                
                RunCom.sendMessage(deskData); // distribute

                // overall panel of this instance is only updated when its own
                // message arrives from network, then the overall panel is
                // updated from RunCom.updateLocalData() method
                
                // the same is true for request panel indicator - also updated
                // only after receiving a message from network
            }
        }
    	
    } // resetStableBeamRequestCounter() -----------------------------------
    
    
    
    private static void resetStartRunRequestCounter()
    {
        for(Iterator<DeskData> iter = deskDataMap.values().iterator(); iter.hasNext();)
        {
            DeskData deskData = iter.next();

            int counter = deskData.getPendingStartRunRequestsCounter();
        	if(counter > 0)
        	{
        		counter = 0;
        		deskData.setPendingStartRunRequestsCounter(counter);
                
                logger.info("Setting pending start run requests counter to " +
                                counter + " for desk \"" + deskData.getDeskName() + "\"");
                
                RunCom.sendMessage(deskData); // distribute

                // overall panel of this instance is only updated when its own
                // message arrives from network, then the overall panel is
                // updated from RunCom.updateLocalData() method
                
                // the same is true for request panel indicator - also updated
                // only after receiving a message from network
            }
        }
    	
    } // resetStartRunRequestCounter() --------------------------------------
    
    

    /**
     * Will loop over all DeskData instances and if any has pending requests
     * counter > 0, it will be reset to 0 and an update message sent to
     * network for others to get synchronized, no request message is send
     * for reset request
     * @param requestType
     */
    protected static void resetRequestCounters(String requestType)
    {

        logger.debug("Reset of pending requests for type \"" + requestType +
        		     "\" counter on systems.");
            
        if(INJECTION.equals(requestType))
        {
        	resetInjectionRequestCounter();
        }
        else if(STABLE_BEAM.equals(requestType))
        {
        	resetStableBeamRequestCounter();
        }
        else if(START_RUN.equals(requestType))
        {
        	resetStartRunRequestCounter();
        }
    	
    } // resetRequestCounters() ---------------------------------------------
    

        
    /**
     * Method called from RunCom.updateLocalData() when a DeskDataMessage
     * arrives. This method loops over all desks and checks their pending
     * request counters of each request type and sets corresponding graphical
     * representation the the request panel:
     *    - status icon: statusLabel
     *    - fulfilled time stamp: fulfilledAt
     */
    public void updateRequestPanel()
    {
    	// if any of these request status flags remain true, 
    	// corresponding request panel will get its fulfilled time and
    	// status icon indicator updated to true state, otherwise will be
    	// set to false (not yet fulfilled state)
    	boolean injectionStatusFlag = true;
    	boolean stableBeamStatusFlag = true;
    	boolean startRunStatusFlag = true;
    	
    	
        for(Iterator<DeskData> iter = deskDataMap.values().iterator(); iter.hasNext();)
        {
            DeskData deskData = iter.next();
            String deskName = deskData.getDeskName();
            int counter = 0;
            
            // don't test request counter if there wasn't any prior request issued ...
            // check the first request flag
            
            counter = deskData.getPendingInjectionRequestsCounter();
            if(injectionPanel.getFirstRequestFlag() && counter > 0)
            {
            	logger.debug("Desk \"" + deskName + "\" has pending " +
                             "injection request counter " + counter + ", setting " +
                             "request sub panel to false state.");
            	injectionStatusFlag = false;
            }
            
            counter = deskData.getPendingStableBeamRequestsCounter();
            if(stableBeamPanel.getFirstRequestFlag() && counter > 0)
            {
            	logger.debug("Desk \"" + deskName + "\" has pending " +
                             "stable beam request counter " + counter + ", setting " +
                             "request sub panel to false state.");
            	stableBeamStatusFlag = false;
            }
            
            counter = deskData.getPendingStartRunRequestsCounter();
            if(startRunPanel.getFirstRequestFlag() && counter > 0)
            {
            	logger.debug("Desk \"" + deskName + "\" has pending " +
                             "start run request counter " + counter + ", setting " +
                             "request sub panel to false state.");
            	startRunStatusFlag = false;
            }
        } // for
    
        
        Date now = new Date(System.currentTimeMillis());
        String dateTime = DATE_FORMAT.format(now);
        
        
        // don't update anything if there wasn't a corresponding request before

        if(injectionPanel.getFirstRequestFlag())
        {
	        if(injectionStatusFlag)
	        {
	        	injectionPanel.setStatusIndicator(true);
	        	injectionPanel.setFulfilledAt(dateTime);
	        }
	        else
	        {
	        	injectionPanel.setStatusIndicator(false);
	        	injectionPanel.setFulfilledAt("n/a");        	
	        }
        }
        
        if(stableBeamPanel.getFirstRequestFlag())
	        {
	        if(stableBeamStatusFlag)
	        {
	        	stableBeamPanel.setStatusIndicator(true);
	        	stableBeamPanel.setFulfilledAt(dateTime);
	        }
	        else
	        {
	        	stableBeamPanel.setStatusIndicator(false);
	        	stableBeamPanel.setFulfilledAt("n/a");
	        }
        }
                
        if(startRunPanel.getFirstRequestFlag())
        {
	    	if(startRunStatusFlag)
	    	{
	    		startRunPanel.setStatusIndicator(true);
	    		startRunPanel.setFulfilledAt(dateTime);
	    	}
	    	else
	    	{
	    		startRunPanel.setStatusIndicator(false);
	    		startRunPanel.setFulfilledAt("n/a");
	    	}
        }
    	    	
    } // updateRequestPanel() -----------------------------------------------
    

    
} // class RequestPanelSupervisor ===========================================



class CommandButtonsListener implements ActionListener
{
	private static MyLogger logger = MyLogger.getLogger(CommandButtonsListener.class);
	
	private RequestSubPanel adaptee = null;
	

	
	public CommandButtonsListener(RequestSubPanel adaptee)
	{
		this.adaptee = adaptee;
		
	} // CommandButtonsListener() -------------------------------------------
	
	

	public void actionPerformed(ActionEvent ae)
	{
		String command = ae.getActionCommand();
		
		if(RequestPanelSupervisor.COMMAND_REQUEST.equals(command))
		{
			requestCommand();
		}
		else if(RequestPanelSupervisor.COMMAND_RESET.equals(command))
		{
			resetCommand();
		}
		
	} // actionPerformed() --------------------------------------------------

	
	
    /**
     * This command results into creating RequestMessage instance. Listeners
     * which receive such instance call CheckList.
     */   	
	private void requestCommand()
	{
        // do automatic reset before a new request is triggered
        resetCommand(); 
        
        // create RequestMessage instance
        String requestType = adaptee.getRequestType();
        RequestMessage rm = new RequestMessage(requestType);
        
        logger.debug("Going to send request message: " + rm.toString());
        RunCom.sendMessage(rm);
        
        Date now = new Date(System.currentTimeMillis());
        String dateTime = RequestPanelSupervisor.DATE_FORMAT.format(now);
        
        adaptee.setLastRequestAt(dateTime);
        adaptee.setFulfilledAt("n/a");
        adaptee.setFirstRequestFlag(true);
        
        // icon flag will be updated only after DeskData messages return from
        // network - perhaps there is no desk in the ready state, then no
        // request can possibly be performed. in this case, the indicator 
        // would be set true as if fulfilled, thus here is this firstRequestFlag
        
	} // requestCommand() ---------------------------------------------------

	

	private void resetCommand()
	{
		RequestPanelSupervisor.resetRequestCounters(adaptee.getRequestType());
				
	} // resetCommand() -----------------------------------------------------

	
		
} // class CommandButtonsListener ===========================================




@SuppressWarnings("serial")
class RequestSubPanel extends JPanel
{
	
    // icon indicator showing status of the request (done by all desks which
    // are in the ready state or some desks still have this request pending)
    private TwoStatesInconIndicator statusIndicator = null;
    private JLabel lastRequestAt = null;
    private JLabel fulfilledAt = null;
    private String requestType = null;
    // this flag is false until first request of this type is issued
    // until true, don't update indicators and times
    private boolean firstRequestFlag = false;
    
	

    public RequestSubPanel(String title, String requestType)
    {
    	this.requestType = requestType;
    
    	CommandButtonsListener listener = new CommandButtonsListener(this);
    	
    	createSubPanelGUI(listener, title);
        
    } // RequestSubPanel() --------------------------------------------------
    
    
    
    private void createSubPanelGUI(CommandButtonsListener listener, String title)
    {
    	
        this.setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());
        
        Border titleBorder = BorderFactory.createLineBorder(Color.GRAY);
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border innerBorder = BorderFactory.createTitledBorder(titleBorder, title);
        this.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        JButton requestButton = new JButton("request");
        requestButton.setActionCommand(RequestPanelSupervisor.COMMAND_REQUEST);        
        requestButton.addActionListener(listener);
        topPanel.add(requestButton);
        
        JButton resetButton = new JButton("reset");
        resetButton.setActionCommand(RequestPanelSupervisor.COMMAND_RESET);
        resetButton.addActionListener(listener);
        topPanel.add(resetButton);        
        
        topPanel.add(new JLabel("status: "));
        
        statusIndicator = new TwoStatesInconIndicator("n/a");
        statusIndicator.setToolTipText("shows status of pending requests \"" + requestType +
        		                   "\" on desks");
        topPanel.add(statusIndicator);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayout(2, 2, 0, 4));
        // top, left, bottom, right
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        bottomPanel.add(new JLabel("Last request: "));
        lastRequestAt = new JLabel("n/a");
        bottomPanel.add(lastRequestAt);
        bottomPanel.add(new JLabel("Fulfilled at: "));
        fulfilledAt = new JLabel("n/a");
        bottomPanel.add(fulfilledAt);
        
        this.add(topPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);
    	
    	
    } // createSubPanelGUI() ------------------------------------------------
    
    
    
    public void setFirstRequestFlag(boolean value)
    {
    	this.firstRequestFlag = value;
    	
    } // setFirstRequestFlag() ----------------------------------------------
    
    
    
    public boolean getFirstRequestFlag()
    {
    	return this.firstRequestFlag;
    	
    } // getFirstRequestFlag() ----------------------------------------------
    
    
        
    public String getRequestType()
    {
    	return requestType;
    	
    } // getRequestType() ---------------------------------------------------
    
    
    
    public void setLastRequestAt(String when)
    {
    	lastRequestAt.setText(when);
    	
    } // setLastRequestAt() -------------------------------------------------
    
    
    
    public void setFulfilledAt(String when)
    {
    	fulfilledAt.setText(when);
    	
    } // setFulfilledAt() ---------------------------------------------------
 
    
    
    public void setStatusIndicator(boolean state)
    {
    	this.statusIndicator.update(state);
    	
    } // setStatusIndicator() -----------------------------------------------

    
	
} // class RequestSubPanel ==================================================