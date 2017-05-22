package runcom;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.JOptionPane;

import mylogger.MyLogger;

import runcom.activemqcommunication.Sender;
import runcom.activemqcommunication.Receiver;
import runcom.activemqcommunication.DeskDataMessage;
import runcom.activemqcommunication.TextMessage;
import runcom.activemqcommunication.RequestMessage;
import runcom.activemqcommunication.WhiteBoardMessage;

import runcom.deskconfig.DeskConfiguration;
import runcom.deskconfig.DeskConfigurationProcessor;


/**
 * The main class of the RunCom application.
 * RunCom is a singleton class.
 * 
 * @author Zdenek Maxa
 * 
 */
public final class RunCom
{
    // logger - takes care of logging to destinations ...
    private static MyLogger logger = null;
        
    // text file with definition of available subsystem names (desks)
    // XML file contains definition of all relevant initialisation
    // data associated with a desk, e.g. deskName, files names of various
    // XML checklists (displayed by the CheckList application)
    // see comment in the file
    private static final String DESKS_CONFIGURATIONS =
        "desks_configurations.xml";
    
    // HashMap with desks configurations (start up config data) as defined
    // in DESKS_DEFINITION_FILE, key is desk name
    private static HashMap<String, DeskConfiguration> deskConfigurations = null;
    
    
    // CheckList prefixes - used to differentiate after which action
    // RunCom is called back from CheckList
    private static final String SIGNIN_CHECK_LIST_NAME_PREFIX = "signin-";
    private static final String DQ_CHECK_LIST_NAME_PREFIX = "DQcheck-";
    private static final String INJECTION_CHECK_LIST_NAME_PREFIX = "injection-";
    private static final String STABLE_BEAM_CHECK_LIST_NAME_PREFIX = "stablebeam-";
    private static final String START_RUN_CHECK_LIST_NAME_PREFIX = "startrun-";
    
    // names which instance of RunCom uses for logging, etc in supervisor
    // and observer modes
    private static final String SUPERVISOR_MODE_NAME = "supervisor";
    private static final String OBSERVER_MODE_NAME = "observer";
    // this is the only desk for which RunCom behaves as a normal desks but
    // provides supervisor power, i.e. setting all values for everybody, etc
    // this name must be the one defined in the desks.txt file and this hybrid
    // mode works only if --control DESK_ALLOWED_SUPERVISOR_MODE --supervisor
    // in the code then:
    // currentSubSystem = DESK_ALLOWED_SUPERVISOR_MODE && isSupervisor = true
    private static final String DESK_ALLOWED_SUPERVISOR_MODE = "Shift Leader";
    

    // could control all subsystems in the supervisor mode
    private static boolean supervisorMode = false;
    
    // current role (subsystem) of this RunCom application (driven by command
    // line argument)
    private static String activeSubSystem = null;
    
    // reference to the GUI window
    private static RunComGUI gui = null;
    
    // network communication attributes
    
    // first must be CLI configurable, later should use discovery mechanism
    // private static final String BROKER_NAME = "tcp://localhost:52525";
    // private String BROKER_NAME = ActiveMQConnection.DEFAULT_BROKER_URL;
    // private String BROKER_NAME = "tcp://localhost:61616";
    public static final String BROKER_PROTOCOL = "tcp://";
    public static final String BROKER_PORT = ":61616";
    // network name of the Message Broker
    private static String messageBrokerName = null;
    // reference to the message receiver instance
    private static Receiver messageReceiver = null;
    // reference to the message sender instance
    private static Sender messageSender = null;
    
    
    
    // DQ checked status automatic watcher
    private static DQStatusWatcher watcher = null;

    
    
    
    
    
    
    /**
     * The method checks validity of the subsystem provided as a command line
     * argument.
     * role is either number or subsystem (role) name as defined in the
     * file DESKS_CONFIGURATIONS
     * If role is correct and valid, the currentSubSystem attribute will remain
     * set and contains the role name. Otherwise exception is thrown.
     * @throws RunComException
     */
    private static void checkSubSystemValidity(String role,
                        ArrayList<DeskConfiguration> desks)
    throws RunComException
    {
        try
        {
            // assume the role is a number, check if it is a integer number
            int roleInt = Integer.valueOf(role);
            
            // role seemed to be correct integer, check if its value is correct
            try
            {
                activeSubSystem = desks.get(roleInt).getDeskName();
                // currentRole is now properly initialised with the role name
            }
            catch(IndexOutOfBoundsException ioobe)
            {
                String m = "argument " + role + " is incorrect for -c, --control";
                throw new RunComException(m);
            }
        }
        catch(NumberFormatException nfe)
        {
            // role is not integer, so it must be String with a subsystem name
            boolean roleOk = false;
            for(DeskConfiguration desk : desks)
            {
                if(desk.getDeskName().equals(role))
                {
                    roleOk = true;
                    break;
                }
            }
            
            if(roleOk)
            {
                activeSubSystem = role;
            }
            else
            {
                String m = "argument " + role + " is incorrect for -c, --control";
                throw new RunComException(m);
            }
        }

        
        // if running supervisor mode, check if it running with allowed
        // subsystem desk, otherwise exit
        if(supervisorMode &&
           ! DESK_ALLOWED_SUPERVISOR_MODE.equals(activeSubSystem))
        {
        	// running supervisor mode for some other desk than allowed
        	String m = "Supervisor mode is supported without any desk " +
        	           "assigned or with \"" +
        	           DESK_ALLOWED_SUPERVISOR_MODE + "\" desk only, exit.";
        	throw new RunComException(m);
        }
    
    } // checkSubSystemValidity() -------------------------------------------
    
    

    private static void processCommandLineParameters(String[] args)
                        throws RunComException
    {
    
        // command line options definition
        HelpFormatter formatter = new HelpFormatter();
        String helpTitle = "RunCom application help";
        String helpHeaderMsg = "";
        String helpFooterMsg = "";
        
        Options options = new Options();
   
        
        // define help option (-h, --help)
        Option help = new Option("h", "help", false,
                                 "display this help message");
        options.addOption(help);

        // define debug option (-d, --debug)
        String debugDescr = "log debug messages of severity level to " +
                             "stdout, if destination (filename) is set " +
                             "then also to a file. severity levels are " +
                             MyLogger.getStringLevels() +
                             " (INFO is default)";
        Option debug = OptionBuilder.hasArgs(2)
                                    .withArgName("severity destination")
                                    .withDescription(debugDescr)
                                    .withLongOpt("debug").create('d');
        options.addOption(debug);
        
        // define control role option (-c, --control <role>)
        // note: 2008-07-04 - put .hasArgs() since otherwise doesn't get
        // the whole name of the subsystem if it consists of more words
        // e.g. "Run Control"
        String controlDescr = "RunCom controls particular subsystem tabpane. " +
                              "Example usage: --control 1 or -c Tile  " +
                              "If neither this argument or --supervisor is not " +
                              "requested, then RunCom runs observer mode in which " +
                              "no setting changes are allowed." +
                              "The subsystem names are defined in file: " +
                              DESKS_CONFIGURATIONS;
        Option control = OptionBuilder.hasArgs()
                         .withArgName("subsystem")
                         .withDescription(controlDescr)
                         .withLongOpt("control").create('c');
        options.addOption(control);
        
        // define supervisor option (-s, --supervisor)
        String superDescr = "Supervisor mode in which all subsytems are " +
                            "controlled." +
                            "If neither this argument or --control <role> " +
                            "requested, then RunCom runs observer mode in which " +
                            "no setting changes are allowed. It is possible to " +
                            "combine --supervisor and --control " +
                            DESK_ALLOWED_SUPERVISOR_MODE + " only.";
        Option supervisor = OptionBuilder.withDescription(superDescr)
                            .withLongOpt("supervisor").create('s');
        options.addOption(supervisor);
        
        // define broker name option (-b, --broker)
        String brokerDescr = "Message broker network name. This argument is mandatory.";
        Option broker = OptionBuilder.hasArgs(1).withDescription(brokerDescr)
                                     .withLongOpt("broker").create('b');
        options.addOption(broker);


        
        // process command line options
        CommandLineParser parser = new GnuParser();
        try
        {        	
            CommandLine l = parser.parse(options, args);
            
            // --help option
            if(l.hasOption('h'))
            {
                formatter.printHelp(helpTitle, helpHeaderMsg, options,
                                    helpFooterMsg);
                System.exit(0);
            }
            // end of help option

            // --debug option
            if(l.hasOption('d'))
            {
                String[] vals = l.getOptionValues('d');
                // vals[0] - severity level
                // vals[1] - destination to write logs to
                try
                {
                    // MyLogger.setConsoleLoggingLayout(MyLogger.FILE_LOGGING_LAYOUT_FULL);
                    MyLogger.initialize(vals); // need to check vals
                }
                catch(Exception ex)
                {
                    String m = debug.getLongOpt() + " incorrect options: " +
                               ex.getMessage();
                    throw new ParseException(m);
                }
            } 
            // end of debug option
                        
            // --control option
            if(l.hasOption('c'))
            {
            	// problems working as two worded-arguments (e.g. "Muon 2")
            	// from bash script, forwarding the command line arguments
            	// must have .hasArgs() at the argument definition
                // String arg = l.getOptionValue('c');
                // currentSubSystem = arg;
      	
            	String arg[] = l.getOptionValues('c');
            	for(int i = 0; i < arg.length; i++)
            	{
            		if(activeSubSystem == null)
            		{
            			activeSubSystem = "";
            		}
            		else
            		{
            			activeSubSystem += " ";
            		}
            		activeSubSystem += arg[i];
            	}
            }
            // end of control option
            
            // --supervisor option
            if(l.hasOption('s'))
            {
                supervisorMode = true;
            }
            // end of supervisor option
            
            // --broker option
            if(l.hasOption('b'))
            {
                String arg = l.getOptionValue('b');
                messageBrokerName = BROKER_PROTOCOL + arg + BROKER_PORT;
            }
            // end of broker option
            
            
            // check some parameters provided
            
            if(messageBrokerName == null)
            {
                throw new ParseException("Message Broker is a mandatory argument.");
            }

            // later also need to check whether supervisor is not run with
            // other than DESK_ALLOWED_SUPERVISOR_MODE, need to load desk
            // names first, this will be done in checkSubSystemValidity()            
            
        } // try
        catch(ParseException pe)
        {
            formatter.printHelp(helpTitle, helpHeaderMsg, options, helpFooterMsg);
            throw new RunComException(pe.getMessage());
        }
    
    } // processCommandLineParameters() -------------------------------------
    
        
     
    /**
     * Method is invoked from message Receiver or from start-up synchronizer.
     * It sets values in the local DeskData object which is held for each
     * subsystem (desk) according to values received in ddm argument.
     * @param ddm
     */
    public static void updateLocalData(DeskDataMessage ddm)
    {
        String deskName = ddm.getDeskName();
        DeskData data = gui.getDeskData(deskName);
        if(data == null)
        {
            logger.error("Desk \"" + deskName + "\" does not exist. " +
                         "Nothing changed.");
        }
        else
        {
        	logger.debug("DeskDataMessage content: " + ddm.toString());
            logger.info("Updating local RunCom data of \"" + deskName + "\" ...");
            
            data.setStatus(ddm.getCurrentStatus());
            data.setAssignedUserName(ddm.getAssignedUserName());
            data.setDqLastChecked(ddm.getDqLastChecked());
            data.setDqCheckPeriodMinutes(ddm.getDqCheckPeriodMinutes());
            if(data.getDeskName().equals(activeSubSystem) && watcher != null)
            {
            	watcher.setDelay(data.getDqCheckPeriodMinutes());
            }
            data.setDqStatusChecked(ddm.getDqStatusChecked());
            data.setNotAckMessagesCounter(ddm.getNotAckMessagesCounter());
            data.setPendingInjectionRequestsCounter(ddm.getPendingInjectionRequestsCounter());
            data.setPendingStableBeamRequestsCounter(ddm.getPendingStableBeamRequestsCounter());
            data.setPendingStartRunRequestsCounter(ddm.getPendingStartRunRequestsCounter());

            // updating overall panel only happens here when a RunCom instance
            // receives update message from network, the method
            // updateOverallPanel() is not called from anywhere else
            // (except for init calls in the OverallPanel class itself)
            RunComGUI.updateOverallPanel(data);
            
            // update request panel indicators - will be done only if supervisor
            RunComGUI.updateRequestPanel();
            
            logger.debug("Update finished.");
        }
        
    } // updateLocalData() --------------------------------------------------
    
    
    
    /**
     * Called from message receiver when a RequestMessage is delivered.
     * Request is processed in this method:
     * 
     * Check if the RunCom instance runs some particular desk (i.e. is
     * not observer)
     * Check that desk is in the ready state
     * Check ignore flag at desk configuration for a given request type
     * Finally proceed if all above is fulfilled, otherwise drop request
     * 
     * This method is another example that requests feature (originally
     * started for one request) can't be implemented for each request
     * type this separately, should be done generally, Request being a
     * class within DeskData with its type, counter, etc. Will simplify
     * implementation of the whole feature.
     * 
     */
    public static void processRequestMessage(RequestMessage rm)
    {        
        logger.info("RequestMessage received, content: " + rm.toString() +
                     " processing ");
        
        String deskName = RunCom.getActiveSubSystem();
        
        if(deskName == null)
        {
            logger.warn("RunCom not running as any system, request dropped.");
            return;
        }
        
        DeskData deskData = gui.getDeskData(deskName);
        DeskConfiguration dc = deskConfigurations.get(deskName);
        String checkListFileName = null;
        
        if(! DeskData.READY.equals(deskData.getStatus()))
        {
            logger.warn("RunCom \"" + deskName + "\" is not in the " +
                        DeskData.READY + " state, request dropped.");
            return;
        }
        
        
        // distinguish which request type arrived
        String requestType = rm.getRequestType();
        boolean ignoreFlag = false;
        
    	if(RequestPanelSupervisor.INJECTION.equals(requestType))
    	{
    	    if(dc.isIgnoreInjectionRequest())
    	    {
    	        ignoreFlag = true;
    	    }
    	    else
    	    {
    	        int c = deskData.getPendingInjectionRequestsCounter();
    	        c++;
    	        deskData.setPendingInjectionRequestsCounter(c);
    	        checkListFileName = dc.getInjectionCheckListFileName();
    	    }
    	}
    	else if(RequestPanelSupervisor.STABLE_BEAM.equals(requestType))
    	{
    	    if(dc.isIgnoreStableBeamRequest())
    	    {
    	        ignoreFlag = true;
    	    }
    	    else
    	    {
        		int c = deskData.getPendingStableBeamRequestsCounter();
        		c++;
        		deskData.setPendingStableBeamRequestsCounter(c);
        		checkListFileName = dc.getStableBeamCheckListFileName();
    	    }
    	}
    	else if(RequestPanelSupervisor.START_RUN.equals(requestType))
    	{
    	    if(dc.isIgnoreStartRunRequest())
    	    {
    	        ignoreFlag = true;
    	    }
    	    else
    	    {
        		int c = deskData.getPendingStartRunRequestsCounter();
        		c++;
        		deskData.setPendingStartRunRequestsCounter(c);
        		checkListFileName = dc.getStartRunCheckListFileName();
    	    }
    	}
    	
    	if(ignoreFlag)
    	{
    	    logger.warn("Desk \"" + deskName + "\" has this request ignore " +
    	                "flag set, request dropped.");
    	    return;
    	    
    	}
    	
    	// request wasn't ignored, distribute changed stated in deskData
        RunCom.sendMessage(deskData);

        // overall panel of this instance is only updated when its own
        // message arrives from network, then the overall panel is
        // updated from RunCom.updateLocalData() method
            
        // display notification dialog and hang here?
        
        // display request checklist
        if(checkListFileName != null)
        {
            logger.warn("Calling CheckList to display: \"" +
                         checkListFileName + "\" ...");
            CheckListCaller.call(checkListFileName, deskName);
        }
        else
        {
            logger.warn("No CheckList to be displayed (checklist file name " +
                        "is null). No action taken on this request.");
        }
        
    } // processRequestMessage() --------------------------------------------
        
 

    /**
     * Processes text messages which have pending acknowledgement - 
     * increases the counter of not acknowledged messages for a 
     * desk and displays (annoying) notification dialog.
     * @param tm
     */
    private static void updateNotAckMessagesCounter(TextMessage tm)
    {
    	String deskName = RunCom.getActiveSubSystem();
    	
    	// if running as a desk, must acknowledged. could also run as
    	// Shift Leader + supervisor which will also be fine
    	if(deskName != null)
    	{
    		// do this only if not running observer or supervisor mode
    		if(tm.getToAcknowledge())
    		{
    			// message is intended to be acknowledged by recipients
	    		logger.debug("TextMessage should be acknowledged, updating " +
	    				     "acknowledgement counter ...");
				DeskData deskData = gui.getDeskData(deskName);
				int c = deskData.getNotAckMessagesCounter();
				c++;
				deskData.setNotAckMessagesCounter(c);
				logger.warn("Setting not acknowledged messages counter to " +
						    c + " for desk \"" + deskName + "\"");
				RunCom.sendMessage(deskData); // distribute new state ...
                // overall panel of this instance is only updated when its own
                // message arrives from network, then the overall panel is
                // updated from RunCom.updateLocalData() method
				
				String msg = "Text message requiring acknowledgement\n" +
				             "received (right click on the message).";
				String winTitle = "RunCom warning - message from " + tm.getFrom();
				NotificationDialog notification = new NotificationDialog();
				notification.notify(gui, msg, winTitle);				
    		}
    	}
    	
    } // updateNotAckMessagesCounter() --------------------------------------
    
    
    
    /**
     * This method is called either from Receiver (when a TextMessage) is
     * received during normal running or from StartupSynchronizer at 
     * RunCom instance start-up.
     * At start-up we don't care about the acknowledgement flags of the
     * messages, counter of messages to acknowledge is reset at start-up
     * anyway.
     * Flag considerAck - consider acknowledgement flag of an incoming
     * TextMessage, this is not taken into account at the start-up
     * synchronization, otherwise yes.
     * 
     * @param tm
     * @param considerAck
     */
    public static void displayIncomingTextMessage(TextMessage tm,
    		                                      boolean considerAck)
    {
    	
    	logger.debug("TextMessage received, content: " + tm.toString());
    	logger.info("Displaying incoming text message ...");
    	
		// don't consider ackFlag if the text message was sent by this desk
    	// or when this method is called during start-up
		if(tm.getFrom().equals(RunCom.getActiveSubSystem()) || ! considerAck)
		{
			// ignore whether or not the message should be acknowledge in this case
    		logger.warn("This message was sent by this subsystem, don't " +
    				    "have to acknowledge messages by myself, or we are at " +
    				    "start-up synchronization (ignore ack flag) ...");
    		tm.setToAcknowledge(false);
		}
    	
    	gui.displayIncomingTextMessage(tm);
    	
    	if(considerAck)
    	{
    		updateNotAckMessagesCounter(tm);
    	}
    	
    	logger.info("TextMessage displayed and processed.");
    	
    } // displayIncomingTextMessage() ---------------------------------------    
    
    
    
    public static void updateWhiteBoardMessage(WhiteBoardMessage wbm)
    {
        gui.updateWhiteBoardMessage(wbm);
        
    } // updateWhiteBoardMessage() ------------------------------------------
    
    
    
    public static void closeRunCom()
    {
    	gui.disposeWindow();
    	messageReceiver.shutdownReceiver();
    	messageSender.shutdownSender();
    	if(watcher != null)
    	{
    		watcher.stop();
    	}
    	
        logger.warn("RunCom application closed.");
        
        System.exit(0);
                
    } // closeRunCom() ------------------------------------------------------
    
    
    
    public static DeskConfiguration getDeskConfiguration(String deskName)
    {
        DeskConfiguration dc = deskConfigurations.get(deskName);
        return dc;
        
    } // getDeskConfiguration() --------------------------------------------
    
    
        
    public static String getMessageBrokerName()
    {
        return RunCom.messageBrokerName;
        
    } // getMessageBrokerName() ---------------------------------------------
    
    
    
    public static boolean isSupervisor()
    {
        return supervisorMode;
        
    } // isSupervisor() -----------------------------------------------------
    
    
    
    public static String getActiveSubSystem()
    {
        return activeSubSystem;
        
    } // getActiveSubSystem() -----------------------------------------------
    
    
    
    /**
     * Print ClassPath the RunCom runs with, only in DEBUG logging level.
     */
    public static void printClassPath()
    {
        String currentLevel = logger.getCurrentLevel().toString();
        if("DEBUG".equals(currentLevel))
        {
            ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
            URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();
            for(int i=0; i< urls.length; i++)
            {
                logger.debug("ClassPath item " + i + ": " + urls[i].getFile());
            }
        }
        
    } // printClassPath() ---------------------------------------------------
    
    
    
    /**
     * Just to save a reference to the GUI.
     * @param instanceGUI
     */
    public static void registerGUI(RunComGUI instanceGUI)
    {
        gui = instanceGUI;
        
    } // registerGUI() ------------------------------------------------------
    
    
    
    /**
     * Unchecked getInstance() method to get quickly reference to the
     * main GUI window frame, often just to have a reference to the parent
     * GUI when showing error and/or confirmation JOptionPane dialogs.
     * Obviously, could only be called when the application is running and
     * GUI created.
     * TODO: this is not ideal, every time, reference to a desk data is
     * needed, it goes through RunCom.getGUI().getDeskData(deskName) ...
     */
    public static RunComGUI getGUI()
    {
        if(gui == null)
        {
            logger.fatal("GUI instance is not initialised (null), " +
                         "this will cause NullPointerException somewhere ...");
        }
        return gui;
        
    } // getGUI() -----------------------------------------------------------

    

    /**
     * Method sets deskName desk to ready state - signin checklist was
     * successfully completed.
     * 
     * @param deskName
     * @return
     */
    private static DeskData signInCheckListCompleted(String deskName)
    {
        // once system was declared ready, wait 1 min until automatically
        // pop-up data quality checklist
    	class DQCheckThread extends Thread 
    	{
    		public void run()
    		{
    			logger.debug("DQCheckThread invoked, going to wait 1 minute ...");
    			try
    			{
    				Thread.sleep(1 * 60 * 1000);
    			}
    			catch(InterruptedException ie)
    			{
    				logger.error("DQCheckThread was interrupted ...");
    			}
    			logger.debug("DQCheckThread waiting finished, fire DQ check ....");
    			watcher.fireDQCheck();
    			logger.debug("DQCheckThread finished.");
    		}
    	}
    	
		logger.info("Desk checklist completed, set desk \"" + deskName +
		            "\" to ready.");
		DeskData deskData = gui.getDeskData(deskName);
		
        if(deskData != null)
        {
            deskData.setStatus(DeskData.READY);
            // now should check DQ - fire watcher
            if(watcher != null)
            {
            	// checklist from signin might still be open (insert into
            	// elog part ... etc, so wait a minute before popping-up
            	// the DQ checklist)
            	DQCheckThread dqCheckThread = new DQCheckThread();
            	dqCheckThread.start();
            }
        }
        
        return deskData;
    	
    } // signInCheckListCompleted() -------------------------------------------
    
    
    
    /**
     * Method called from CheckList.
     * checkListFileName - it is a completed checklist filename, RunCom
     *                     check the prefix (e.g. signin-) to distinguish which
     *                     action CheckList was called for
     * deskName - is name of RunCom desk from which the CheckList was called.
     */
    public static void setDeskDetailsApprovedByCheckList(String checkListFileName,
                                                         String deskName)
    {
    	DeskData deskData = null;

    	logger.info("RunCom called back from Checklist, checklist file name: " +
    			    "\"" + checkListFileName + "\" desk name: \"" +
    			    deskName + "\"");
    	
    	// differentiate according to check list name received from CheckList
    	if(checkListFileName.startsWith(SIGNIN_CHECK_LIST_NAME_PREFIX))
    	{
    		deskData = signInCheckListCompleted(deskName);
    	}
    	else if(checkListFileName.startsWith(DQ_CHECK_LIST_NAME_PREFIX))
    	{
    		logger.info("DQ checklist completed, set DQ status to checked on " +
    		            "desk \"" + deskName + "\"");
    		deskData = gui.getDeskData(deskName);

            if(deskData != null)
            {
                deskData.setDqStatusChecked(true);
                // DQ last checked time to right now
            	Date now = new Date(System.currentTimeMillis());
            	deskData.setDqLastChecked(now);
            }
    	}
    	else if(checkListFileName.startsWith(INJECTION_CHECK_LIST_NAME_PREFIX))
    	{
    	    logger.info("Injection checklist completed, decrease request " +
    	                "counter on \"" + deskName + "\"");
    	    deskData = gui.getDeskData(deskName);
    	    
    	    if(deskData != null)
    	    {
    	        int c = deskData.getPendingInjectionRequestsCounter();
    	        c--;
    	        deskData.setPendingInjectionRequestsCounter(c);
                logger.warn("Setting pending injection requests counter to " +
                                c + " for desk \"" + deskName + "\"");
    	    }
    	}
    	else if(checkListFileName.startsWith(START_RUN_CHECK_LIST_NAME_PREFIX))
    	{
    		logger.info("Ready for config/start run sequence checklist completed, " +
    				    "decrease counter on \"" + deskName + "\"");
    	    deskData = gui.getDeskData(deskName);
    	    
    	    if(deskData != null)
    	    {
    	    	int c = deskData.getPendingStartRunRequestsCounter();
    	    	c--;
    	    	deskData.setPendingStartRunRequestsCounter(c);
                logger.warn("Setting pending start run requests counter to " +
                		    c + " for desk \"" + deskName + "\"");
    	    }
    	}
    	else if(checkListFileName.startsWith(STABLE_BEAM_CHECK_LIST_NAME_PREFIX))
    	{
    		logger.info("Stable beam checklist completed, decrease counter " +
    		             "on \"" + deskName + "\"");
    		deskData = gui.getDeskData(deskName);
    	    
    	    if(deskData != null)
    	    {
    	    	int c = deskData.getPendingStableBeamRequestsCounter();
    	    	c--;
    	    	deskData.setPendingStableBeamRequestsCounter(c);
                logger.warn("Setting pending stable beam requests counter to " +
                		    c + " for desk \"" + deskName + "\"");
    	    }
    	}
    	else
    	{
    		logger.error("CheckList prefix: \"" + checkListFileName + "\" not recognised.");
    		return;
    	}
    	
    	
    	if(deskData != null)
    	{
            RunCom.sendMessage(deskData); // distribute modified data

	        // overall panel of this instance is only updated when its own
	        // message arrives from network, then the overall panel is
	        // updated from RunCom.updateLocalData() method
            
            // the same is true for RequestPanel indicators (only for Shift Leader
            // running supervisor mode)
    	}
    	else
    	{
            String m1 = "RunCom called from CheckList. ";
            String m2 = "Desk \"" + deskName + "\" does not exist, can't update anything.";
            logger.error(m1 + m2);
            JOptionPane.showMessageDialog(gui, m1 + "\n" + m2, "RunCom error",
                                          JOptionPane.ERROR_MESSAGE);    		
    	}
    	
    } // setDeskDetailsApprovedByCheckList() --------------------------------
  
    
  
    protected static void startMessageReceiver()
    {
        try
        {
            logger.info("Starting message receiver ...");
            messageReceiver = Receiver.getInstance(messageBrokerName);
        }
        catch(RunComException rce)
        {
            String m = rce.getMessage();            
            logger.error(m);
            JOptionPane.showMessageDialog(null, m, "RunCom error",
                            JOptionPane.ERROR_MESSAGE);
            // not ideal to finish this way ...
            System.exit(1);
        }
        
    } // startMessageReceiver() ---------------------------------------------
    
    
    
    protected static void initMessageSender()
    {
        try
        {
            logger.info("Starting message sender ...");
            messageSender = Sender.getInstance(messageBrokerName);
        }
        catch(RunComException rce)
        {
            String m = rce.getMessage();
            logger.error(m);
            JOptionPane.showMessageDialog(null, m, "RunCom error",
                            JOptionPane.ERROR_MESSAGE);
            // not ideal to finish this way ...
            System.exit(1);            
        }
        
    } // initMessageSender() -----------------------------------------------
    
    
    
    public static void sendMessage(TextMessage tm)
    {
    	messageSender.sendMessage(tm);
    	
    } // sendMessage() ------------------------------------------------------
    
    
    
    /**
     * Perhaps exception propagation should be done from Sender.sendMessage()
     * ...
     * @param deskData
     */
    public static void sendMessage(DeskData deskData)
    {
        messageSender.sendMessage(deskData);
        
    } // sendMessage() ------------------------------------------------------
    
    
    
    public static void sendMessage(RequestMessage rm)
    {
        messageSender.sendMessage(rm);
        
    } // sendMessage() ------------------------------------------------------
    

    
    public static void sendMessage(WhiteBoardMessage wbm)
    {
        messageSender.sendMessage(wbm);
        
    } // sendMessage() ------------------------------------------------------

    
        
    public static void startDQCheckedStatusWatcher()
    {
    	logger.info("Starting DQ checked status watcher ...");
    	if(activeSubSystem == null)
    	{
    		logger.warn("DQ checked status watcher will not start if there no " +
    				    "desk assigned (observer or plain supervisor)");
    	}
    	else
    	{
    		int dqPeriod = gui.getDeskData(activeSubSystem).getDqCheckPeriodMinutes();
	    	watcher = new DQStatusWatcher(activeSubSystem, dqPeriod);
	    	watcher.start();
    	}
    	
    } // startDQCheckedStatusWatcher() --------------------------------------
    
    
    
    public static void main(String[] args)
    {
        try
        {
            processCommandLineParameters(args);            
        }
        catch(RunComException rce)
        {
            // rce.printStackTrace();
            System.out.println("\n\n" + rce.getMessage());
            System.exit(1);
        }
        
        // logger should either be initialized from
        // processCommandLineParameters() or will be using default settings
        logger = MyLogger.getLogger(RunCom.class);
        
        printClassPath();
        
        // read desks configuration file
        DeskConfigurationProcessor proc =
            new DeskConfigurationProcessor(DESKS_CONFIGURATIONS);
        ArrayList<DeskConfiguration> deskConfigs = null;
        try
        {
            // this ArrayList has information about order of desks - it is
            // needed if -c argument is number (number of a desks) and
            // to keep order or RunCom tabpanes for desks
            deskConfigs = proc.readAndGetDeskConfigurations();
        }
        catch(Exception e)
        {
            logger.debug(e.getMessage(), e);
            logger.fatal(e.getMessage() + ", exit.");
            System.exit(1);            
        }
                
        // if some desks was specified on the command line (either as a
        // number of String - desk name) - check if it is correct
        try
        {
            if(activeSubSystem != null)
            {
                checkSubSystemValidity(activeSubSystem, deskConfigs);
            }
        }
        catch(RunComException rce)
        {
            logger.debug(rce.getMessage(), rce);
            logger.fatal(rce.getMessage() + ", exit.");
            System.exit(1);
        }

        
        // create HashMap with DeskConfigurations, that will be accessed
        // for all details like checklist titles, flags, etc
        deskConfigurations = new HashMap<String, DeskConfiguration>();
        for(DeskConfiguration desk : deskConfigs)
        {
            deskConfigurations.put(desk.getDeskName(), desk);
        }
        
        // in which mode we are running, supervisor mode having the highest priority
        String mode = null;
        if(supervisorMode && activeSubSystem == null)            
        {
        	// plain supervisor mode without a desk assigned
        	mode = SUPERVISOR_MODE_NAME;
        }
        else if(supervisorMode && activeSubSystem != null)
        {
        	// desk plus supervisor mode, should be only for shift leader,
        	// checked already
        	mode = activeSubSystem;
        	logger.debug("Running supervisor mode for desk: \"" + mode + "\"");
        }
        else if(activeSubSystem != null)
        {
        	// normal subsystem desk mode
        	mode = activeSubSystem;
        }
        else
        {
        	mode = OBSERVER_MODE_NAME;
        }
        logger.info("Running mode: \"" + mode + "\"");
        RunComGUI.createAndShowGUI(mode, deskConfigs);
        
        // don't add any other calls here which directly or indirectly access
        // RunCom.gui, since it is 'registered' by an independent thread
        // in the GUI class (run() method), the calls here may be executed
        // before the GUI is created and registered, thus it will result
        // in NullPointerException on RunCom.gui ...
        
        // shutdown hook doesn't seem to work well here. after closing the
        // application, shutdown hook is performed but process still hangs ...
        // RunComShutdownHook hook = new RunComShutdownHook();
        // Runtime.getRuntime().addShutdownHook(hook);
        // logger.info("RunCom shutdown hook installed.");
                
    } // main() -------------------------------------------------------------

} // class RunCom ===========================================================



/**
 * ShutDownHook is called during JVM shutdown, when application receives
 * terminating signal or on user's ctrl-c. 
 */
class RunComShutdownHook extends Thread
{
	
	private static MyLogger logger = MyLogger.getLogger(RunComShutdownHook.class);
               
        
    public void run()
    {
        logger.warn("RunCom shutdown hook called.");
        RunCom.closeRunCom();
                
    } // run() --------------------------------------------------------------
    
    
} // class CleanupShutdownHook ==============================================