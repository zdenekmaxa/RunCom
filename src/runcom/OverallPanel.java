package runcom;

import java.util.Iterator;
import java.util.LinkedHashMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mylogger.MyLogger;



/**
 * Implementation of panel within the main application's tabbed pane.
 * This panel shows overview of the desk subsystems statuses and further
 * information (e.g. last acknowledged messages, etc) will be added.
 * 
 * @author Zdenek Maxa
 *
 */
@SuppressWarnings("serial")
public final class OverallPanel extends JPanel
{

    private static MyLogger logger = MyLogger.getLogger(OverallPanel.class);
        
    // container with graphical representation of desk data for OverallPanel
    private static LinkedHashMap<String, DeskRepresentation> graphicDeskData = null;
    
    
    
    
    public void createGUIPanel(LinkedHashMap<String, DeskData> deskDataMap)
    {
        graphicDeskData = new LinkedHashMap<String, DeskRepresentation>();
        
        logger.debug("Creating the OverallPanel ...");

        this.setLayout(new FlowLayout());
        // border:  top, left, bottom, right border around panel
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // JPanel mainPanel = new JPanel();
        // GridLayout(int rows, int cols, int hgap, int vgap) 
        // mainPanel.setLayout(new GridLayout(0, 4, 10, 10));
        
        JPanel titleColumn = new JPanel(new GridLayout(0, 1, 0, 13));
        // icons used here, they are a bit higher, thus 11 here
        JPanel statesColumn = new JPanel(new GridLayout(0, 1, 0, 11));
        // icons used here, they are a bit higher, thus 11 here
        JPanel dqColumn = new JPanel(new GridLayout(0, 1, 0, 11));
        // CounterIndicator - numbers in larger font than system titles,
        // thus 12 used as vertical gap
        JPanel ackColumn = new JPanel(new GridLayout(0, 1, 0, 12));
        JPanel reqColumn = new JPanel(new GridLayout(0, 1, 0, 12));
        
        statesColumn.setToolTipText("Status of the system (subsystem desk)");
        dqColumn.setToolTipText("Data Quality status - checked / unchecked.");
        ackColumn.setToolTipText("Number of text messages awaiting acknowledgement.");
        reqColumn.setToolTipText("Counter of pending requests on a system");
        
        // define vertical spaces between columns (top, left, bottom, right)
        titleColumn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        statesColumn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        dqColumn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        // set some space left
        ackColumn.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        reqColumn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        
        // legend row
        titleColumn.add(new GridLabel("system", true));
        statesColumn.add(new GridLabel("state", true));
        dqColumn.add(new GridLabel("DQ", true));
        ackColumn.add(new GridLabel("msg", true));
        reqColumn.add(new GridLabel("req", true));
        
        // desk subsystems data
        for(Iterator<String> iter = deskDataMap.keySet().iterator(); iter.hasNext();)
        {
            String deskName = iter.next();
            DeskData deskData = deskDataMap.get(deskName);
            
            titleColumn.add(new GridLabel(deskName, true));
            
            // create representation instances of particular information about a desk
            DeskStatusIndicator deskStatus = new DeskStatusIndicator();
            TwoStatesInconIndicator dqStatus = new TwoStatesInconIndicator("");
            CounterIndicator numberOfNonAckMsg = new CounterIndicator();
            CounterIndicator numberOfRequests = new CounterIndicator();
        
            // add into column containers
            statesColumn.add(deskStatus);
            dqColumn.add(dqStatus);
            ackColumn.add(numberOfNonAckMsg);
            reqColumn.add(numberOfRequests);

            DeskRepresentation representation =
            	new DeskRepresentation(deskStatus, dqStatus, numberOfNonAckMsg, 
            	                       numberOfRequests);

            graphicDeskData.put(deskName, representation);
            // now set the factual values of indicators, must be after the
            // desk representation was stored into the graphicDeskData container
            updateOverallPanel(deskData); // performs update for a particular desk
            
        } // for
        
        this.add(titleColumn);
        this.add(statesColumn);
        this.add(dqColumn);
        this.add(ackColumn);
        this.add(reqColumn);
        
        logger.debug("OverallPanel created.");
        
    } // createGUIPanel() ---------------------------------------------------
    
    
    
    public void updateOverallPanel(DeskData deskData)
    {
        String deskName = deskData.getDeskName();
        String newStatus = deskData.getStatus();
        boolean dqChecked = deskData.getDqStatusChecked();
        int nonAckMsg = deskData.getNotAckMessagesCounter();
        
        int pendingReq = deskData.getPendingInjectionRequestsCounter() +
                         deskData.getPendingStableBeamRequestsCounter() +
                         deskData.getPendingStartRunRequestsCounter();
        
        logger.debug("Updating OverallPanel for  " +
                     "\""  + deskName + "\", full desk data: " +
                     deskData.toString());
        
        // retrieve reference to corresponding graphical components and update
        DeskRepresentation representation = graphicDeskData.get(deskName);
                
        representation.update(newStatus, dqChecked, nonAckMsg, pendingReq);

    } // updateOverallPanel() -----------------------------------------------

} // class OverallPanel =====================================================



@SuppressWarnings("serial")
class GridLabel extends JLabel
{
	private static final String FONT_FAMILY = "SansSerif";
	private static final int FONT_SIZE = 11;
	
    public GridLabel(String label, boolean bold)
    {
        this.setText(label);
        Font font = null;
        if(bold)
        {
            font = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE);
        }
        else
        {
            font = new Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE);
        }
        this.setFont(font);
   
    } // GridLabel() --------------------------------------------------------
    
    
    
} // class GridLabel ========================================================




class DeskRepresentation
{
	
	private DeskStatusIndicator deskStatus = null;
	private TwoStatesInconIndicator dqStatus = null;
	private CounterIndicator nonAckMsg = null;
	private CounterIndicator pendingReq = null; 
	
	
	
	public DeskRepresentation(DeskStatusIndicator deskStatus,
			                  TwoStatesInconIndicator dqStatus,
			                  CounterIndicator nonAckMsg,
			                  CounterIndicator pendingReq)			                  
	{
		this.deskStatus = deskStatus;
		this.dqStatus = dqStatus;
		this.nonAckMsg = nonAckMsg;
		this.pendingReq = pendingReq;
		
	} // DeskRepresentation() -----------------------------------------------
	
	
	
	protected void update(String status, boolean dqChecked, int nonAckMsg, 
	                      int pendingReq)
	{
		this.deskStatus.update(status);
		this.dqStatus.update(dqChecked);
		this.nonAckMsg.update(nonAckMsg);
		this.pendingReq.update(pendingReq);
		
	} // update() -----------------------------------------------------------	
	
} // class DeskRepresentation ===============================================




/**
 * This class is a representation of the counter data from a DeskData
 * instance. The class displays a number in the JPanel holding JLabel.
 */
@SuppressWarnings("serial")
class CounterIndicator extends JPanel
{
    private static final String FONT_FAMILY = "SansSerif";
    private static final int FONT_SIZE = 12;
    
	private JLabel counterLabel = null;
	
	
	public CounterIndicator()
	{
		this.setLayout(new BorderLayout());
		this.counterLabel = new JLabel("");
		Font font = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE);
		this.counterLabel.setFont(font);
		this.add(this.counterLabel);
				
	} // CounterIndicator() -------------------------------------------------
	
	
	
	protected void update(int counter)
	{
        this.counterLabel.setText(Integer.toString(counter));
        
        if(counter > 0)
        {
        	this.counterLabel.setForeground(Color.red);
        }
        else
        {
        	this.counterLabel.setForeground(Color.green.darker());
        }
		
	} // update() -----------------------------------------------------------


	
} // class CounterIndicator =================================================



@SuppressWarnings("serial")
class TwoStatesInconIndicator extends JPanel
{
	private static MyLogger logger =
		MyLogger.getLogger(TwoStatesInconIndicator.class);

	private static final String OK_FILE = "green_tick_icon.gif";
	private static final String FAIL_FILE = "red_cross_icon.gif";
	
	private static ImageIcon okIcon = null;
	private static ImageIcon failIcon = null;
	
	// instance data representation
    private JLabel indicator = null;
    
	
	static
	{
		okIcon = new ImageIcon(OK_FILE);
		failIcon = new ImageIcon(FAIL_FILE);
		if(okIcon == null || failIcon == null)
		{
			logger.error("Could not create icons from files: " + OK_FILE +
					     " or " + FAIL_FILE);
		}
	}
	
	
	
	public TwoStatesInconIndicator(String text)
	{
		this.setLayout(new BorderLayout());
		this.indicator = new JLabel(text);
		this.add(this.indicator);
				
	} // TwoStatesInconIndicator() ------------------------------------------
	
	
	
	protected void update(boolean state)
	{
	    // if some text was set before, it will be displayed next to the icon
	    this.indicator.setText("");
	    
		if(state)
		{
			this.indicator.setIcon(okIcon);
		}
		else
		{
			this.indicator.setIcon(failIcon);
		}

	} // update() -----------------------------------------------------------
	
	
	
} // class TwoStatesInconIndicator ==========================================




/**
 * Desk status indicator within the Overall panel
 */
@SuppressWarnings("serial")
class DeskStatusIndicator extends JPanel
{
    private static MyLogger logger = MyLogger.getLogger(DeskStatusIndicator.class);

    private JLabel statusLabel = null;
    
    private static final String UNASSIGNED_FILE = "grey_circle_icon.gif";
    private static final String ASSIGNED_FILE = "blue_square_icon.gif";
    private static final String ERROR_FILE = "red_cross_icon.gif";
    private static final String READY_FILE = "green_tick_icon.gif";

    private static ImageIcon unassignedIcon = null;
    private static ImageIcon assignedIcon = null;
    private static ImageIcon errorIcon = null;
    private static ImageIcon readyIcon = null;
    
    static
    {
        unassignedIcon = new ImageIcon(UNASSIGNED_FILE);
        assignedIcon = new ImageIcon(ASSIGNED_FILE);
        errorIcon = new ImageIcon(ERROR_FILE);
        readyIcon = new ImageIcon(READY_FILE);
        
        if(unassignedIcon == null || assignedIcon == null ||
           errorIcon == null || readyIcon == null)
        {
            logger.error("Could not create icons from files: " + UNASSIGNED_FILE +
                         " or " + ASSIGNED_FILE + " or " + ERROR_FILE + " or " +
                         READY_FILE);
        }
    }
    

    
    public DeskStatusIndicator()
    {
        this.setLayout(new BorderLayout());
        statusLabel = new JLabel();
        this.add(statusLabel);
        
    } // DeskStatusIndicator() ----------------------------------------------
    
    

    protected void update(String status)
    {
        if(DeskData.UNASSIGNED.equals(status))
        {
            statusLabel.setIcon(unassignedIcon);
        }
        else if(DeskData.ASSIGNED.equals(status))
        {
            statusLabel.setIcon(assignedIcon);
        }
        else if(DeskData.ERROR.equals(status))
        {
            statusLabel.setIcon(errorIcon);
        }
        else if(DeskData.READY.equals(status))
        {
            statusLabel.setIcon(readyIcon);
        }
        
    } // update() -----------------------------------------------------------
    
} // class DeskStatusIndicator ==============================================