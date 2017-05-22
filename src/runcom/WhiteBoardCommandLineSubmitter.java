package runcom;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import java.nio.CharBuffer;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.*;

import java.util.Date;

import runcom.activemqcommunication.Sender;
import runcom.activemqcommunication.WhiteBoardMessage;


/**
 * WhiteBoardCommandLineSubmitter class
 * Allows publishing a new white board message non-interactively by reading
 * a text file specified as a command line argument and submitting its
 * whole content as a white board message. Runnable via cron.
 * 
 * No logging or debug output implemented.
 * 
 * @author Zdenek Maxa
 *
 */
public class WhiteBoardCommandLineSubmitter
{
    
    // input text file with new white board message content
    private static String inputTextFileName = null;
    
    // network communication attributes  
    // network name of the Message Broker
    private static String messageBrokerName = null;
    public static final String BROKER_PROTOCOL = "tcp://";
    public static final String BROKER_PORT = ":61616";
    // network name of the Message Broker
    
    // reference to the message sender instance
    private static Sender messageSender = null;
    
    
    
    
    
    private static void processCommandLineParameters(String[] args)
    throws RunComException
    {
        // command line options definition
        HelpFormatter formatter = new HelpFormatter();
        String helpTitle = "RunCom application - White board command line submitter";
        String helpHeaderMsg = "";
        String helpFooterMsg = "";

        Options options = new Options();

        // define help option (-h, --help)
        Option help = new Option("h", "help", false,
        "display this help message");
        options.addOption(help);

        // define broker name option (-b, --broker)
        String brokerDescr = "Message broker network name. This argument is mandatory.";
        Option broker = OptionBuilder.hasArgs(1).withDescription(brokerDescr)
            .withLongOpt("broker").create('b');
        options.addOption(broker);
        
        // define input text file name with new white board message text
        String inputMsgFileDescr = "Input text file with new white board message to submit.";
        Option inputMsg = OptionBuilder.hasArgs(1).withDescription(inputMsgFileDescr)
            .withLongOpt("input").create('i');
        options.addOption(inputMsg);

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


            // --broker option
            if(l.hasOption('b'))
            { 
                String arg = l.getOptionValue('b');
                messageBrokerName = BROKER_PROTOCOL + arg + BROKER_PORT;
            }
            // end of broker option
            
            // --input option
            if(l.hasOption('i'))
            {
                inputTextFileName = l.getOptionValue('i');   
            }   

            // check some parameters provided

            if(messageBrokerName == null)
            {
                throw new ParseException("Message Broker is a mandatory argument.");
            }
            
            if(inputTextFileName == null)
            {
                throw new ParseException("Input text file is a mandatory argument.");
            }

        } // try
        catch(ParseException pe)
        {
            formatter.printHelp(helpTitle, helpHeaderMsg, options, helpFooterMsg);
            throw new RunComException(pe.getMessage());
        }

    } // processCommandLineParameters() -------------------------------------
    
    
    

    public static void main(String[] args)
    {
        System.out.println("RunCom WhiteBoardCommandLineSubmitter running ...");
        
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
        
        // read content of the file
        String newMessage = null;
        BufferedReader input = null;
        try
        {
            StringBuilder contents = new StringBuilder();
            input = new BufferedReader(new FileReader(inputTextFileName));
            String line = null;
            while((line = input.readLine()) != null)
            {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
            newMessage = contents.toString();
        }
        catch(IOException ioe)
        {
            System.out.println("\n\n" + "Could not read file: " + inputTextFileName +
                               "\nreason: " + ioe.getMessage());
            System.exit(1);
        }        
        finally
        {
            try
            {
                input.close();
            }
            catch(IOException ioe)
            {
                System.out.println("Could not close file, reason: " + ioe.getMessage());
            }
        }
        
        // init message sender
        System.out.println("Creating message sender instance ...");        
        try
        {
            messageSender = Sender.getInstance(messageBrokerName);
        }
        catch(RunComException rce)
        {
            String m = rce.getMessage();
            System.out.println(m);
            System.exit(1);            
        }
                    
        // going to send the new message out ...
        Date now = new Date(System.currentTimeMillis());
        WhiteBoardMessage wbm = new WhiteBoardMessage();
        wbm.setMessage(newMessage);
        wbm.setTimeStamp(now);
        System.out.println("Going to send WhiteBoardMessage: " + wbm.toString());
        
        try
        {
            messageSender.sendMessage(wbm); // send to the others
        }
        catch(Exception ex)
        {
            System.out.println("Could not send message, reason: " + ex.getMessage());
        }
        
        messageSender.shutdownSender();
                
        System.out.println("RunCom WhiteBoardCommandLineSubmitter finished normally.");

    } // main ---------------------------------------------------------------

    
} // class WhiteBoardCommandLineSubmitter ===================================
