package runcom;

import java.util.LinkedHashMap;
import java.util.ArrayList;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;

import runcom.activemqcommunication.StartupSynchronizer;
import runcom.activemqcommunication.TextMessage;
import runcom.activemqcommunication.WhiteBoardMessage;

import runcom.deskconfig.DeskConfiguration;

import mylogger.MyLogger;


/**
 * GUI window of the RunCom application.
 * RunComGUI is a singleton class.
 * @author Zdenek Maxa
 */
@SuppressWarnings("serial")
public class RunComGUI extends JFrame implements Runnable 
{
    private static MyLogger logger = MyLogger.getLogger(RunComGUI.class);
    
    private static RunComGUI instance = null;
    
    private static String mode = null;
    
    // DeskData is an instance holding all data associated with a desk,
    // this is the main data container, keys are desk titles
    // this is run-time data (status, counters, etc)
    private static LinkedHashMap<String, DeskData> deskDataMap = null;
    
    // DeskConfiguration holds configuration data associated with each desk
    // it is configuration data (desk name, checklist names, etc)
    private static ArrayList<DeskConfiguration> deskConfigs = null;
    
    // chat panel reference is not part of the deskDataMap, it is not associated
    // with each desk only with GUI instance of RunCom (class attribute - just
    // one for RunComGUI)
    private static ChatPanel chatPanel = null;
    
    // reference to the overall panel
    private static OverallPanel overallPanel = null;
    
    // white board window component, editable for supervisor
    private static WhiteBoardWindow whiteBoard = null;
    
    // reference to the request panel, is valid only if running supervisor mode
    private static RequestPanelSupervisor requestPanel = null;
    
    
    
    
    public static void createAndShowGUI(String mode,
                                        ArrayList<DeskConfiguration> deskConfigs)
    {
        if(instance == null)
        {
            RunComGUI.mode = mode;
            RunComGUI.deskConfigs = deskConfigs;
            SwingUtilities.invokeLater(new RunComGUI());
        }        
        
    } // createAndShowGUI() -------------------------------------------------
    
    
    
    public void increaseMessagesFontSize()
    {
    	chatPanel.increaseMessagesFontSize();
    	
    } // increaseMessagesFontSize() -----------------------------------------
    
    
    
    public void decreaseMessagesFontSize()
    {
    	chatPanel.decreaseMessagesFontSize();
    	
    } // decreaseMessagesFontSize() -----------------------------------------
    
    
    
    public static void updateOverallPanel(DeskData deskData)
    {
        overallPanel.updateOverallPanel(deskData);
    
    } // updateOverallPanel() -----------------------------------------------
    
    
    
    public static void updateRequestPanel()
    {
        if(requestPanel != null)
        {
            requestPanel.updateRequestPanel();
        }
        
    } // updateRequestPanel() ------------------------------------------------
    
    
    
    public void displayIncomingTextMessage(TextMessage tm)
    {
        chatPanel.displayIncomingTextMessage(tm);
                
    } // displayIncomingTextMessage() ---------------------------------------
    
    
    
    public void updateWhiteBoardMessage(WhiteBoardMessage wbm)
    {
        whiteBoard.processWhiteBoardUpdate(wbm.getMessage(),
                                           wbm.getTimeStampString());
        
    } // updateWhiteBoardMessage() ------------------------------------------
    
    
    
    /**
     * Method called by the Swing event-dispatching thread.
     * Construct the GUI here.
     */
    public void run()
    {
        logger.debug("Creating RunComGUI and its compoments ... ");
        
        whiteBoard = WhiteBoardWindow.getInstance();
        
        this.addWindowListener(new GUIWindowAdapter());
        
        this.setJMenuBar(new MenuBar(this, whiteBoard));
                
        // create GUI content
        this.setTitle("RunCom mode: \"" + mode + "\"");
        
        this.setLayout(new BorderLayout());        
        
        // tabbed panes with all subsystems
        JTabbedPane tabbedPane = createJTabbedPane();
        this.add(tabbedPane, BorderLayout.NORTH);
        
        // messaging panel ...
        chatPanel = new ChatPanel();
        this.add(chatPanel, BorderLayout.CENTER);
         
        this.setSizeAndLocation();
                
        // enable control of the active subsystem
        String activeSubsystem = RunCom.getActiveSubSystem(); 
        if(activeSubsystem != null)
        {
            DeskData deskData = deskDataMap.get(activeSubsystem);
            deskData.enableDeskPanel(true);
        }
        
        // these following (all remaining in this method) lines should remain
        // the last ones in this method
        instance = this;
        logger.debug("RunComGUI is now created and initialised.");
        // GUI registration must happen before StartupSynchronizer is called
        // b/c: synchronize() -> RunCom.updateLocalData(item) -> RunCom.gui
        // must be initialised, otherwise NullPointerException
        RunCom.registerGUI(instance);
        communicationInitializationAndSynchronization();
        RunCom.startDQCheckedStatusWatcher();
        
    } // run() --------------------------------------------------------------


    
    public void disposeWindow()
    {
        logger.debug("Disposing the RunComGUI window.");
        this.dispose();
        instance = null;
        
    } // disposeWindow() ----------------------------------------------------
    
    
    
    public DeskData getDeskData(String deskName)
    {
        DeskData deskData = deskDataMap.get(deskName);
        if(deskData == null)
        {
            logger.fatal("Acquiring access to the desk data failed, desk \"" +
                         deskName + "\" does not exist, null returned.");
        }
        
        return deskData;
        
    } // getDeskData() -----------------------------------------------------
    
    
    
    // PRIVATE METHODS ------------------------------------------------------
    
    
    
    private JTabbedPane createJTabbedPane()
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        
        deskDataMap = new LinkedHashMap<String, DeskData>();
        
        String activeSubSystem = RunCom.getActiveSubSystem();
        
        // create overview pane / panel
        overallPanel = new OverallPanel();
        
        // loop over all subsystem desks
        for(DeskConfiguration desk : deskConfigs)
        {
            String deskName = desk.getDeskName();
            
            // create subsystem panel GUI for tabbled panes
            IDeskPanel deskPanel = null;
            if(RunCom.isSupervisor())
            {
            	// supervisor mode
            	deskPanel = new DeskPanelSupervisor(deskName);
            	tabbedPane.addTab(deskName, (DeskPanelSupervisor) deskPanel);
            }
            else
            {
            	deskPanel = new DeskPanel(deskName);
            	tabbedPane.addTab(deskName, (DeskPanel) deskPanel);
                // select tab for this desk control
                if(activeSubSystem != null && deskName.equals(activeSubSystem))
                {
                	tabbedPane.setSelectedComponent((DeskPanel) deskPanel);
                }
            }
            
            // data representation of all subsystem desk information
        	DeskData deskData = new DeskData(deskName, deskPanel);
        	
        	
            deskDataMap.put(deskName, deskData);

            
            // DQStatusCheckBoxListener is created only if there is a desk
            // assigned and if that desk is equal to the desk currently
            // processed in this loop, when running normal desk mode and if
            // running Shift Leader supervisor mode, only his desk will pop-up
            // checklist upon setting DQ status checked (otherwise there
            // is a button for other desks for supervisor). just one tab has
            // this listener in RunCom
            if(activeSubSystem != null && deskName.equals(activeSubSystem))
            {
            	DQStatusCheckBoxListener dqListener =
            		new DQStatusCheckBoxListener(deskData);
            	((DeskPanel) deskPanel).setDQCheckedStatusActionListener(dqListener);
            }
            
        } // for
            
        
        // now fill the overallPanel with data. reference to overallPanel was
        // passed to each DeskData instance when creating it, OverallPanel needs
        // deskDataMap filled with references to all desks
        overallPanel.createGUIPanel(deskDataMap);
        tabbedPane.addTab("Overall", overallPanel);
        
        // select OverallPanel if not running for a particular subsystem desk
        if(activeSubSystem == null)
        {
            tabbedPane.setSelectedComponent(overallPanel);
        }
        
        // create supervisor requests panel (e.g. ready for injection request)
        if(RunCom.isSupervisor())
        {
            requestPanel = new RequestPanelSupervisor(deskDataMap);
            tabbedPane.addTab("requests", requestPanel);            
        }
        
        return tabbedPane;
        
    } // createJTabbedPane() ------------------------------------------------
    
    
    
    /**
     * Method start all network communication and calls start-up states
     * synchronization.
     */
    private void communicationInitializationAndSynchronization()
    {
        
        String brokerName = RunCom.getMessageBrokerName(); 
        StartupSynchronizer ss = StartupSynchronizer.getInstance(brokerName);
        // synchronize() method calls references RunCom.gui - it must be
        // initialized / registered before synchronize() call is performed!
        ss.synchronize();

    	RunCom.initMessageSender();
        RunCom.startMessageReceiver();
        
        // even if there were any not acknowledged messages for this subsystem
        // desk before (counter !=0 ), reset the counter for this desk RunCom
        // instance start-up
        // deskName might be null (observer mode)
        String deskName = RunCom.getActiveSubSystem(); 
	    if(deskName != null)
	    {
	    	DeskData deskData = getDeskData(deskName);
	    	// don't bother sending a message if the counter is already zero
	    	if(deskData.getNotAckMessagesCounter() > 0)
	    	{
	    		logger.warn("Setting not acknowledged message counter for " +
	    				    "this desk to 0 at startup.");
	    		deskData.setNotAckMessagesCounter(0);
	    		RunCom.sendMessage(deskData); // distribute to others
	    	}
	    }
	    
    } // communicationInitializationAndSynchronization() --------------------

    

    private void setSizeAndLocation()
    {
        // display the window centered on the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = this.getPreferredSize();
        this.setLocation(screenSize.width / 2 - (labelSize.width / 2),
                         screenSize.height / 2 - (labelSize.height / 2));
    
        this.pack();
        
        // TODO - do something better
        this.setSize(460, 870);
        // this.setSize(this.getPreferredSize().width,
        //              this.getPreferredSize().height);
        
        this.setVisible(true);
        
    } // setSizeAndLocation() -----------------------------------------------
    
       
    
} // class RunComGUI ========================================================

 

/**
 * Easier than WindowListener - doesn't have to implement a number of
 * methods out of which majority would remain empty.
 */
final class GUIWindowAdapter extends WindowAdapter
{
    public void windowClosing(WindowEvent we)
    {
        RunCom.closeRunCom();
        
    } // windowClosing() ----------------------------------------------------
    
} // GUIWindowAdapter =======================================================



@SuppressWarnings("serial")
final class MenuBar extends JMenuBar
{
    
    public MenuBar(RunComGUI gui, WhiteBoardWindow whiteBoard)
    {
        JMenuItem menuItem = null;
        
        // main menu items
        JMenu menuClose = new JMenu("close");
        JMenu menuWhiteBoard = new JMenu("white board");
        JMenu menuPref = new JMenu("preferences");
        JMenu menuDebug = new JMenu("debug");
        
        MenuBarListener listener = new MenuBarListener(gui, whiteBoard);

        // close
        menuItem = new JMenuItem("close");
        menuItem.setActionCommand("close_runcom");
        menuItem.addActionListener(listener);
        menuClose.add(menuItem);
        
        // show debug window sub menu item
        menuItem = new JMenuItem("show debug window");
        menuItem.setActionCommand("debug");
        menuItem.addActionListener(listener);
        menuDebug.add(menuItem);

        /*
        call active subsystem checklist, test calling CheckList application
        menuItem = new JMenuItem("call checklist of current desk");
        menuItem.setActionCommand("call_checklist");
        menuItem.addActionListener(listener);
        menuDebug.add(menuItem);
        */
        
        menuItem = new JMenuItem("show white board");
        menuItem.setActionCommand("show_white_board");
        menuItem.addActionListener(listener);
        menuWhiteBoard.add(menuItem);
        
        menuItem = new JMenuItem("messages font +");
        menuItem.setActionCommand("increase_messages_font");
        menuItem.addActionListener(listener);
        menuPref.add(menuItem);
        
        menuItem = new JMenuItem("messages font -");
        menuItem.setActionCommand("decrease_messages_font");
        menuItem.addActionListener(listener);
        menuPref.add(menuItem);
                
        // close
        this.add(menuClose);
        this.add(menuWhiteBoard);
        this.add(menuPref);
        this.add(menuDebug);
                
    } // MenuBar() ----------------------------------------------------------
        
} // class MenuBar ==========================================================



final class MenuBarListener implements ActionListener
{
    private static MyLogger logger =
        MyLogger.getLogger(MenuBarListener.class);
    
    private RunComGUI gui = null;
    
    private WhiteBoardWindow whiteBoard = null;
    
    
    public MenuBarListener(RunComGUI gui, WhiteBoardWindow whiteBoard)
    {
    	this.gui = gui;
    	this.whiteBoard = whiteBoard;
    	
    } // MenuBarListener() --------------------------------------------------
    
    
    public void actionPerformed(ActionEvent e) 
    {
        String choice = e.getActionCommand();
        
        logger.debug("MenuBar chosen item: \"" + choice + "\"");
        
        if("debug".equals(choice))
        {
            logger.openDebuggingWindow();
            return;
        }
        else if("increase_messages_font".equals(choice))
        {
        	gui.increaseMessagesFontSize();        	
        }
        else if("decrease_messages_font".equals(choice))
        {
        	gui.decreaseMessagesFontSize();
        }
        else if("show_white_board".equals(choice))
        {
            whiteBoard.displayWhiteBoard();
        }
        else if("close_runcom".equals(choice))
        {
            RunCom.closeRunCom();
        }
            
    } // actionPerformed() --------------------------------------------------
        
} // class MenuBarListener ==================================================
