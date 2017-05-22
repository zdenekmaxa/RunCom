package runcom.activemqcommunication.broker;

import java.io.File;
import java.io.IOException;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

// import org.apache.activemq.broker.BrokerFactory;
// import org.apache.activemq.network.DiscoveryNetworkConnector;
// import org.apache.activemq.transport.discovery.rendezvous.RendezvousDiscoveryAgent;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import mylogger.MyLogger;


/**
 * Broker class creates an embedded BrokerService.
 * @author Zdenek Maxa
 */
public final class Broker
{
    // private static final String BROKER_CONNECTOR = "tcp://localhost:52525";
    private static final String BROKER_NAME = "tcp://localhost:61616";
    
    private static final String BROKER_DIRECTORY = "/tmp/RunComBroker";
    private static final String RUNTIME_DIRECTORY =
        BROKER_DIRECTORY + File.separator + "runtime";
    
    private static String persistentDataDirectory = "/tmp/RunComBrokerPersistent";
        
    // directory into which LAST_WHITE_BOARD_FILENAME goes is CLI configurable
    private static String lastWhiteBoardMessageFile = null;
    private static final String LAST_WHITE_BOARD_FILENAME =
        "last_white_board_message.txt";
    
    // directory into which HTML_OVERVIEW_FILE goes is CLI configurable
    private static String htmlOverviewFile = null;
    private static final String HTML_OVERVIEW_FILE = "html_overview.html";
    
    private static MyLogger logger = null;

    
    // components
    private static BrokerService broker = null;
    private static BrokerReceiver receiver = null;
    private static PersistentRepository repository = null;
    
    
    
    private static void startBroker()
    {
        logger.info("JMS Broker initialization.");
        
        try
        {
            broker = new BrokerService();
            // whether or not the Broker's services should be exposed into JMX
            broker.setUseJmx(true);
            
            // use clean up methods during shutdown
            broker.setUseShutdownHook(true);
            
            // set the tmp directory and directory for persistent data
            logger.info("Setting data and temporary directories to: " +
                      RUNTIME_DIRECTORY);
            File dataDir = new File(RUNTIME_DIRECTORY);
            broker.setTmpDataDirectory(dataDir);
            broker.setDataDirectory(RUNTIME_DIRECTORY);
            
            logger.info("Adding Broker connector: " + BROKER_NAME + " ...");
            broker.addConnector(BROKER_NAME);
            

            /* start of discovery - at least started up on brokers's side ...
            DiscoveryNetworkConnector networkConnector = null;
            networkConnector = new DiscoveryNetworkConnector();
            networkConnector.setDiscoveryAgent(new RendezvousDiscoveryAgent());
            broker.addNetworkConnector(networkConnector);
            */
            
            logger.info("Starting Broker Service ...");
            broker.start();        
            logger.info("BrokerService " + BROKER_NAME + " started.");
            logger.info("Waiting ...");
            
        }
        catch(Throwable t)
        {
            String m = "RunCom Broker: exception while BrokerService " +
                       "initialization, exit. Reason: " + t.getMessage();
            logger.fatal(m, t);
            System.exit(1);
        }
        
    } // startBroker() ------------------------------------------------------

    
    
    public static BrokerService getBroker()
    {
        return broker;
        
    } // getBroker() --------------------------------------------------------
    
    
    
    public static BrokerReceiver getReceiver()
    {
    	return receiver;
    	
    } // getReceiver() ------------------------------------------------------
    
    
    
    public static String getRuntimeDirectory()
    {
        return RUNTIME_DIRECTORY;
        
    } // getRuntimeDirectory() ----------------------------------------------
    
    
    
    public static String getBrokerDirectory()
    {
        return BROKER_DIRECTORY;
        
    } // getBrokerDirectory() -----------------------------------------------
    

    
    /**
     * Method starts message receiver which silently listens for all messages
     * send amongst RunCom instances and stores a copy into the local message
     * repository. Message repository is queried when an instance of RunCom
     * start to get last actual states of all subsystems.
     * 
     * There basically are two receivers behind the Receiver class. First,
     * described above listens for messages and stores them, the second
     * listens for updates queries from starting-up RunCom instances and
     * replies with the last message for each subsystem.
     * 
     */
    private static void startUpdatesReceiver()
    {
    	logger.debug("Starting updates receiver and updates query receiver ...");
    	
    	// runs actually two receivers (message listeners) within this
    	// BrokerReceiver
    	receiver = new BrokerReceiver(BROKER_NAME, repository);
    	
    } // startUpdatesReceiver() ---------------------------------------------
        
  
   
    private static void processCommandLineParameters(String[] args) throws Exception
    {
    	// command line options definition
		HelpFormatter formatter = new HelpFormatter();
		String helpTitle = "Broker (RunCom) application help";
		String helpHeaderMsg = "";
		String helpFooterMsg = "";
		
		Options options = new Options();
		
		// define help option (-h, --help)
		Option help = new Option("h", "help", false, "display this help message");
		options.addOption(help);
		
		// define debug option (-d, --debug)
		String debugDescr = "log debug messages of severity level to " +
		                    "stdout, if destination (filename) is set " +
		                    "then also to a file. severity levels are " +
		                    MyLogger.getStringLevels() + " (INFO is default)";
		Option debug = OptionBuilder.hasArgs(2)
		               .withArgName("severity destination")
		               .withDescription(debugDescr)
		               .withLongOpt("debug").create('d');
		options.addOption(debug);
		
		// define last white board directory option (-w, --whiteboard)
		// (stores a text file with the last white board message)
		String whiteBoardDescr = "directory in which a text file with the " +
		                         "last white board message is written";
		Option whiteBoard = OptionBuilder.hasArgs(1)
                            .withArgName("directory")
                            .withDescription(whiteBoardDescr)
                            .withLongOpt("whiteboard").create('w');
		options.addOption(whiteBoard);
		
		// define persistent data directory option (-p, --persistent)
		String persistentDescr = "directory into which persistent (bin) data " +
		                         "is written (desk status updates, text " +
		                         "messages, white board messages)";
		Option persistent = OptionBuilder.hasArgs(1)
                            .withArgName("directory")
                            .withDescription(persistentDescr)
                            .withLongOpt("persistent").create('p');
		options.addOption(persistent);
		
        // define HTML status overview file directory option (-o, --overview)
        String htmlOverviewDescr = "directory into which HTML status overview file " +
                                   "is written (status of all desks)";
        Option htmlOverview = OptionBuilder.hasArgs(1)
                            .withArgName("directory")
                            .withDescription(htmlOverviewDescr)
                            .withLongOpt("overview").create('o');
        options.addOption(htmlOverview);

        
        
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
			
			// --persistent option
			if(l.hasOption('p'))
			{
			    persistentDataDirectory = l.getOptionValue('p');
			    File dir = new File(persistentDataDirectory);
			    if(! dir.exists())
			    {
			        String m = persistent.getLongOpt() + " specified " +
			                   "directory does not exist: " + dir;
			        throw new ParseException(m);
			    }
			}
			else
			{
			    File dir = new File(persistentDataDirectory);
			    if(! dir.exists())
			    {
			        if(! dir.mkdir())
			        {
                        String m = "Could not create persistent data directory \"" +
                                    persistentDataDirectory + "\", exit.";
                        throw new Exception(m);
			        }
			    }
			}
			
			// --overview option
			if(l.hasOption('o'))
			{
			    String dir = l.getOptionValue('o');
			    if(! new File(dir).exists())
			    {
			        String m = htmlOverview.getLongOpt() + " specified " +
			                   "directory does not exist: " + dir;
			        throw new ParseException(m);			      
			    }
			    htmlOverviewFile = dir + File.separator + HTML_OVERVIEW_FILE;
			}
			else
			{
			    htmlOverviewFile = persistentDataDirectory + File.separator +
			                       HTML_OVERVIEW_FILE;
			}
			
            // --whiteboard option
            if(l.hasOption('w'))
            {
                String dir = l.getOptionValue('w');
                if(! new File(dir).exists())
                {
                    String m = whiteBoard.getLongOpt() + " specified " +
                               "directory does not exist: " + dir;
                    throw new ParseException(m);                  
                }
                lastWhiteBoardMessageFile =
                    dir + File.separator + LAST_WHITE_BOARD_FILENAME;
            }
            else
            {
                lastWhiteBoardMessageFile = persistentDataDirectory +
                                            File.separator +
                                            LAST_WHITE_BOARD_FILENAME;
            }
            
		} // try
		catch(ParseException pe)
		{
			formatter.printHelp(helpTitle, helpHeaderMsg, options, helpFooterMsg);
			throw new Exception(pe.getMessage());
		}
		
	} // processCommandLineParameters() -------------------------------------
		    
    
    
    public static void main(String[] args)
    {
        try
        {
            processCommandLineParameters(args);            
        }
        catch(Exception ex)
        {
            // ex.printStackTrace();
            System.out.println("\n\n" + ex.getMessage());
            System.exit(1);
        }

        
        // logger should either be initialized from
        // processCommandLineParameters() or will be using default settings
        logger = MyLogger.getLogger(Broker.class);      
                
        
        // create and initialize PersistentRepository
        try
        {
            repository =
                new PersistentRepository(lastWhiteBoardMessageFile,
                                         persistentDataDirectory,
                                         htmlOverviewFile);
        }
        catch(Exception ex)
        {
            String m = "Exception during RunComBroker initialisation. Reason: " +
                       ex.getMessage();
            logger.error(m);
            logger.debug(m, ex);
            System.exit(1);
        }
        
        startBroker();

        // install JVM shutdown hook to perform clean-up actions
        CleanupShutdownHook hook = new CleanupShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);
        logger.info("Clean-up shutdown hook installed.");
        
        startUpdatesReceiver();
        
        // now wait forever to avoid the JVM terminating immediately
        // run until shutdown ...        
        Object lock = new Object();
        synchronized(lock)
        {
            try
            {
                lock.wait();
            }
            catch(InterruptedException ie)
            {
                System.out.println("Shutdown");
                logger.warn("CommunicationBroker thread was interrupted.");
            }
        }
        
    } // main() -------------------------------------------------------------

} // class Broker ===========================================================




/**
 * ShutDownHook is called during JVM shutdown, when application receives
 * terminating signal or on user's ctrl-c.
 * 
 * This ShutdownHook cleans up the temporary / runtime data directory so that
 * another user could start the broker without getting permission denied
 * when broker tries to create runtime/temporary data directory. 
 */
class CleanupShutdownHook extends Thread
{
	
	private static MyLogger logger = MyLogger.getLogger(CleanupShutdownHook.class);
        
	
	
    private void stopBroker()
    {
        logger.warn("Stopping the Broker ...");
        
        BrokerService broker = Broker.getBroker();
        try
        {
            broker.stop();
            broker.waitUntilStopped();
            logger.warn("Broker stopped.");
        }
        catch(Throwable t)
        {
        	logger.error("Could not stop the Broker, reason: " + t.getMessage());
        }
        
    } // stopBroker() -------------------------------------------------------

    
    
    private void stopReceiver()
    {
    	logger.warn("Stopping the Broker Receiver ...");
    	BrokerReceiver receiver = Broker.getReceiver();
    	try
    	{
    		receiver.shutdownReceiver();
    		logger.warn("Broker Receiver stopped.");
    	}
    	catch(Throwable t)
    	{
    		logger.error("Could not stop the Broker Receiver, reason: " +
    				     t.getMessage());
    	}
    	
    } // stopReceiver() -----------------------------------------------------
    
    
    
    private void deleteDataDirectory()
    {
        String dir = Broker.getBrokerDirectory();
        logger.info("Erasing directory: " + dir + " ...");
        
        String command = "rm -fr " + dir;
        try
        {
            Process p = Runtime.getRuntime().exec(command);
            logger.debug("Waiting for command " + command + " to complete ...");
            p.waitFor();
        }
        catch(InterruptedException ie)
        {
            logger.error("Error - process interrupted (exec command: " +
            		     command + ").");
        }
        catch(IOException ioe)
        {
            logger.error("Could not erase directory: " + dir + "reason: " +
            		     ioe.getMessage());
        }
                
    } // deleteDataDirectory() ----------------------------------------------
    
    
    
    private void deleteDirectory()
    {
        deleteDataDirectory();
        
        String dir = Broker.getBrokerDirectory();

        File dirFile = new File(dir);
        if(dirFile.exists())
        {
            logger.error("Directory " + dir + " was not erased, " +
                         "another attempt ...");
            deleteDataDirectory();
            if(dirFile.exists())
            {
                logger.error("Directory " + dir + " was not erased.");
            }
        }
        else
        {
            logger.info("Directory " + dir + " was erased.");
        }
                
    } // deleteDirectory() --------------------------------------------------
        
    
        
    public void run()
    {
        logger.warn("Broker shutdown hook called.");
        
        stopReceiver();
        
        stopBroker();
                
        deleteDirectory();
        
    } // run() --------------------------------------------------------------
    
    
} // class CleanupShutdownHook ==============================================
