package runcom.activemqcommunication;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.ObjectMessage;
import javax.swing.JOptionPane;

import org.apache.activemq.ActiveMQConnectionFactory;

import runcom.RunCom;
import runcom.activemqcommunication.DeskDataMessage;


import mylogger.MyLogger;


/**
 * Class is called at the application startup, request is sent for
 * up-to-date subsystem states. Synchronously waits for reply from
 * BrokerReceiver. Once the reply is received, the subsystems are 
 * updated accordingly.
 * @author Zdenek Maxa
 */
public class StartupSynchronizer implements ExceptionListener
{
	
    private static MyLogger logger = MyLogger.getLogger(StartupSynchronizer.class);
    
    private static StartupSynchronizer instance = null;
    
    private static final String
    	UPDATE_QUERY_SUBJECT = "RunComMessageUpdateQuerySubject";
    private static final String
    	UPDATE_RESPONSE_SUBJECT = "RunComMessageUpdateResponseSubject";
    
    private static final long RESPONSE_WAITING_TIMEOUT = 6000; // 6 seconds
        
    private Connection connection = null;    
    private Session querySession = null; // to send request
    private Session responseSession = null; // to receive query response    
    private Destination queryDestination = null;
    private Destination responseDestination = null;
    private MessageProducer producer = null;
    private MessageConsumer consumer = null;

  
    
    
    
    private StartupSynchronizer(String messageBrokerName)
    {
    	logger.debug("Creating instance of StartupSynchronizer ... ");
    	
    	try
    	{
            ActiveMQConnectionFactory connectionFactory = null;
            connectionFactory = new ActiveMQConnectionFactory(messageBrokerName);        
        	
            connection = connectionFactory.createConnection();
            
            connection.setExceptionListener(this);
            connection.start();
            logger.debug("StartupSynchronizer connection started (broker: " +
                            messageBrokerName + ").");
            
            // create two sessions within the connection
            querySession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            responseSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            logger.debug("StartupSynchronizer sessions created.");
            
            // two destinations, one to which request is send via the other
            // response is received, queue - one-to-one delivery mode
            queryDestination = querySession.createQueue(UPDATE_QUERY_SUBJECT);
            responseDestination = responseSession.createQueue(UPDATE_RESPONSE_SUBJECT);           
            logger.debug("StartupSynchronizer destinations created.");
            
            // create the producer
            producer = querySession.createProducer(queryDestination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);            
            logger.debug("StartupSynchronizer message producer created.");
            
            // create response (update) consumer
            consumer = responseSession.createConsumer(responseDestination);            
            logger.debug("StartupSynchronizer message consumers created.");

    	}
        catch(JMSException jmse)
        {
            String m = "JMSException occured while StartupSynchronizer " +
                       "initialization, exit. Reason: " +
                       jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
            JOptionPane.showMessageDialog(null, m, "RunCom error",
                                          JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        instance = this;
    	
    } // StartupSynchronizer() ----------------------------------------------
    
    
    
    public static StartupSynchronizer getInstance(String messageBrokerName)
    {
    	if(instance == null)
    	{
    		instance = new StartupSynchronizer(messageBrokerName);
    	}
    	
    	return instance;
    	
    } // getInstance() ------------------------------------------------------
    
    
    /**
     * Always expects ArrayList as response.
     * @param reply
     * @throws JMSException
     */
    private void processSynchronizationResponse(Message reply) throws JMSException
    {
	    if(reply == null)
	    {
	    	throw new JMSException("No synchronization received after " +
	    			               RESPONSE_WAITING_TIMEOUT + " ms timeout.");
	    }
	    else
	    {
	    	logger.info("Synchronization response received.");
	    }
	    
	    // process response
        if(reply instanceof ObjectMessage)
        {
            logger.debug("ObjectMessage received, processing ...");
            ObjectMessage oMsg = (ObjectMessage) reply;
            ArrayList<?> last = (ArrayList<?>) oMsg.getObject();
            if(last.isEmpty())
            {
            	logger.info("Synchronization / message response is empty.");
            	return;
            }
            else
            {
            	if(last.get(0) instanceof DeskDataMessage)
            	{
            		logger.info("Received synchronization response " +
            				    "DeskDataMessage(s) for " + last.size() + 
            				    " subsystem(s).");
            		updateLocalData((ArrayList<DeskDataMessage>) last);
            	}
            	else if(last.get(0) instanceof TextMessage)
            	{
            		logger.info("Received " + last.size() + " TextMessage(s).");
            		displayRecentTextMessages((ArrayList<TextMessage>) last);
            	}
            	else if(last.get(0) instanceof WhiteBoardMessage)
            	{
                    // white board update sent as only 1 item (the last one)
                    WhiteBoardMessage wbm = (WhiteBoardMessage) last.get(0);            	    
            	    logger.info("Received last WhiteBoardMessage: \"" +
            	                wbm.getMessage() + "\"");    
            	    RunCom.updateWhiteBoardMessage(wbm);
            	}
            }
        } 
        else 
        {
        	String m = "Message other than ObjectMessage instance " +
            		   "received, message: " + reply;
        	throw new JMSException(m);
        }    	
    	
    } // processSynchronizationResponse() -----------------------------------
    
    
    
    public void synchronize()
    {
    	logger.info("Updating subsystems start at start-up, synchronizing ...");

    	try
    	{
    		// send update query (empty message), destination will be this
    		// instance's responseDestination
    		DeskDataMessage ddm = null;
    		logger.debug("Sending empty message - synchronization update query ...");
    		ObjectMessage objectMsg = querySession.createObjectMessage(ddm);
    		 // to send reply to for this message
    		objectMsg.setJMSReplyTo(responseDestination);
    		// send this message to queryDestination
    		producer.send(queryDestination, objectMsg);
    		logger.info("StartupSynchronizer - synchronization query sent.");
    	    
    	    // get response, expects actually 3 responses:
    		// 1st is last messages with desk status updates (DeskDataMessage)
    		// 2nd with last text messages (TextMessage)
    		// 3rd is with late white board message (WhiteBoardMessage)
    		
    		// getting 1st response
    	    logger.info("StartupSynchronizer - waiting (" + RESPONSE_WAITING_TIMEOUT +
    	    		    " ms) for synchronization/last messages response ...");
    	    Message reply = null;
    	    reply = consumer.receive(RESPONSE_WAITING_TIMEOUT);
    	    processSynchronizationResponse(reply);
    	    
    	    // getting 2nd response
    	    logger.info("StartupSynchronizer - waiting (" + RESPONSE_WAITING_TIMEOUT +
		                " ms) for synchronization/last messages response ...");
    	    reply = null;
    	    reply = consumer.receive(RESPONSE_WAITING_TIMEOUT);
    	    processSynchronizationResponse(reply);
    	    
    	    // getting 3rd response
            logger.info("StartupSynchronizer - waiting (" + RESPONSE_WAITING_TIMEOUT +
                        " ms) for synchronization/last messages response ...");
            reply = null;
            reply = consumer.receive(RESPONSE_WAITING_TIMEOUT);
            processSynchronizationResponse(reply);
    	    
    	}
        catch(JMSException jmse)
        {
            String m = "JMSException occured while sending / receiving " +
                       "synchronization message. Can't synchronize, reason: " +
                       jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }
        finally
        {
            shutdownSynchronizer();
        }
    	
    	logger.info("Startup synchronization finished.");
    	
    } // synchronize() ------------------------------------------------------

    
    
    private void updateLocalData(ArrayList<DeskDataMessage> lastMessages)
    {
    	if(lastMessages == null)
    	{
    		logger.error("Can't update local data, null received (last messages).");
    	}
    	else
    	{
    		logger.debug("Synchronizing local data on subsystems ...");
    		if(lastMessages.isEmpty())
    		{
    			logger.info("Nothing to synchonize, last messages list is " +
    					    "empty, everything should be up-to-date as is.");
    		}
    		for(Iterator<DeskDataMessage> i = lastMessages.iterator(); i.hasNext();)
    		{
    			DeskDataMessage item = i.next();
    			RunCom.updateLocalData(item);
    		}
    	}
    	
    } // updateLocalData() --------------------------------------------------

    
    
    private void displayRecentTextMessages(ArrayList<TextMessage> lastMessages)
    {
    	if(lastMessages == null)
    	{
    		logger.error("Can't display last messages, null received.");
    	}
    	else
    	{
    		logger.debug("Displaying last text messages ...");
    		if(lastMessages.isEmpty())
    		{
    			logger.info("Nothing to display, text message list is empty.");
    		}
    		for(Iterator<TextMessage> i = lastMessages.iterator(); i.hasNext();)
    		{
    			TextMessage item = i.next();
    			RunCom.displayIncomingTextMessage(item, false);
    		}
    	}
    	
    } // displayRecentTextMessages() ----------------------------------------
    
    
    
    private void shutdownSynchronizer()
    {
        logger.debug("Shutting down StartupSynchronizer ...");

        try
        {        	
            consumer.close();
            producer.close();
            
            querySession.close();
            responseSession.close();
            
            connection.close();            
                    
            logger.warn("StartupSynchronizer was successfully closed.");
        }
        catch(JMSException jmse)
        {
            logger.error("Error while closing the StartupSynchronizer, " +
                         "reason: " + jmse.getMessage());           
        }
        
    } // shutdownSynchronizer() -------------------------------------------------

	
    
    public synchronized void onException(JMSException jmse)
	{
	    String m = "StartupUpdatMessage Receiver: JMS exception occured, reason: " +
	               jmse.getMessage();
	    logger.fatal(m);
	    shutdownSynchronizer();
	    
	} // onException() ------------------------------------------------------

    
	
} // class StartupSynchronizer ==============================================