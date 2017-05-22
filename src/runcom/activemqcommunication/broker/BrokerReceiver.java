package runcom.activemqcommunication.broker;

import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.ObjectMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import runcom.activemqcommunication.DeskDataMessage;
import runcom.activemqcommunication.TextMessage;
import runcom.activemqcommunication.WhiteBoardMessage;

import mylogger.MyLogger;



/**
 * Message receiver at Broker
 * 1) stores message copies for later updates, etc.
 * 2) listens for request for subsystems status updates and sends response
 * 
 * @author Zdenek Maxa
 */
public final class BrokerReceiver implements ExceptionListener
{
	
    private static MyLogger logger = MyLogger.getLogger(BrokerReceiver.class);
    
    private static final String UPDATE_LISTENER_SUBJECT = "RunComMessageSubject";
    private static final String
    	UPDATE_QUERY_LISTENER_SUBJECT = "RunComMessageUpdateQuerySubject";    
    
    // single connection within this Receiver, is shared for multiple sessions
    private Connection connection = null;
    
    // there will be two sessions within this connection
    private Session updateSession = null;
    private Session updateQuerySession = null;
    
    // each session has its destination
    private Destination updateDestination = null;
    private Destination updateQueryDestination = null;
    
    // updates query replies to requests via reply producer
    private MessageProducer replyProducer = null;
    
    // two consumers
    private MessageConsumer updateConsumer = null;
    private MessageConsumer updateQueryConsumer = null;
    
    
    
    public BrokerReceiver(String brokerName, PersistentRepository repository)
    {    	
    	logger.info("Starting Message Receiver (broker: " + brokerName + ") ...");
    	
        try
        {
        	
            ActiveMQConnectionFactory connectionFactory = null;
            connectionFactory = new ActiveMQConnectionFactory(brokerName);        
        	
            connection = connectionFactory.createConnection();
            
            // if using durable subscriptions, then specify clientID for
            // connection.setClientID( ... );

            connection.setExceptionListener(this);
            connection.start();
            
            logger.debug("Receiver connection started. Going to create " +
            		     "listeners ...");
            
            // create two sessions within the connection
            updateSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            updateQuerySession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            logger.debug("Receiver sessions created.");
            
            // two destinations, one listens for everything (all updates), that
            // is topic - one-to-many scenario. other listens for updates
            // queries, that is queue one-to-one delivery mode
            updateDestination = updateSession.createTopic(UPDATE_LISTENER_SUBJECT);
            updateQueryDestination =
            	updateSession.createQueue(UPDATE_QUERY_LISTENER_SUBJECT);
            
            logger.debug("Receiver destinations created.");

            // create replyProducer for updates query listener by which a
            // query will get reply
            // message destination is set to null, it will be provided by the
            // requester in the query message
            replyProducer = updateQuerySession.createProducer(null);
            replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            
            // create two message consumers
            updateConsumer = updateSession.createConsumer(updateDestination);
            updateQueryConsumer =
            	updateQuerySession.createConsumer(updateQueryDestination);
            
            logger.debug("Receiver message consumers created.");

            // finally set message listeners for create message consumers
            StatusUpdatesListener sul = new StatusUpdatesListener(repository);
            updateConsumer.setMessageListener(sul);
            StatusUpdatesQueryListener suql = 
            	new StatusUpdatesQueryListener(updateQuerySession,
            	                               replyProducer, repository);
            updateQueryConsumer.setMessageListener(suql);
            
            logger.info("Message Receiver started (two message listeners).");
            
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while message receiver " +
                       "initialisation, exit. Reason: " +
                       jmse.getMessage();
            logger.fatal(m);
            logger.debug(m, jmse);
            System.exit(1);
        }
        catch(Throwable t)
        {
        	String m = "Unknown exception occured while message receiver " +
        	           "initialisation, exit. Reason: " + t.getMessage();
        	logger.fatal(m);
        	logger.debug(m, t);
        	System.exit(1);
        }
        
    } // BrokerReceiver() ---------------------------------------------------

    
    
	public synchronized void onException(JMSException jmse)
	{
	    String m = "Message Receiver: JMS exception occured, reason: " +
	               jmse.getMessage();
	    logger.error(m);
	    
	    // perhaps should shutdown receiver: 
	    // this.shutDownReceiver()
	                            
	} // onException() ------------------------------------------------------
    
	
        
    public void shutdownReceiver()
    {
        logger.warn("Shutting down the message receiver ...");

        try
        {
            updateConsumer.close();
            updateQueryConsumer.close();

            replyProducer.close();

            updateSession.close();
            updateQuerySession.close();
            
            connection.close();            
                    
            logger.warn("Message receiver was successfully closed.");
        }
        catch(JMSException jmse)
        {
            String m = "Error while closing the message receiver.";
            logger.error(m);            
            logger.debug(m, jmse);
        }
        catch(Throwable t)
        {
        	String m = "Unknown exception occured while closing message " +
        	           "receiver, reason: " + t.getMessage();
        	logger.error(m);
        	logger.debug(m, t);
        }
        
    } // shutdownReceiver() -------------------------------------------------
	
	
} // class BrokerReceiver ===================================================




/**
 * Quietly listens for updates of subsystems status and invokes Broker message
 * repository updating. 
 */
final class StatusUpdatesListener implements MessageListener
{	
    private static MyLogger logger =
    	MyLogger.getLogger(StatusUpdatesListener.class); 

    // persistent repository reference
    private PersistentRepository repository = null;

    
    
    public StatusUpdatesListener(PersistentRepository repository)
    {
        this.repository = repository;
        
    } // StatusUpdatesListener() --------------------------------------------
    
    
    
	public void onMessage(Message msg) 
	{
	    try
	    {
	        if(msg instanceof ObjectMessage)
	        {
	            logger.debug("ObjectMessage received (update listener), " +
	            		     "processing ...");
	            ObjectMessage oMsg = (ObjectMessage) msg;
	            if(oMsg.getObject() instanceof DeskDataMessage)
	            {
		            DeskDataMessage ddm = (DeskDataMessage) oMsg.getObject(); 
		            logger.info("Extracted DeskDataMessage, storing ...");
		            repository.storeDeskDataMessage(ddm);	            	
	            }
	            else if(oMsg.getObject() instanceof TextMessage)
	            {
	            	TextMessage tm = (TextMessage) oMsg.getObject();
	            	logger.info("Extracted TextMessage, storing ...");
	            	repository.storeTextMessage(tm);
	            }
                else if(oMsg.getObject() instanceof WhiteBoardMessage)
                {
                    WhiteBoardMessage wbm = (WhiteBoardMessage) oMsg.getObject();
                    logger.info("Extracted WhiteBoardMessage, storing ...");
                    repository.storeWhiteBoardMessage(wbm);
                }
	        } 
	        else 
	        {
	            logger.error("Message other than ObjectMessage instance " +
	            		     "received? message: " + msg);
	        }	
	    }
	    catch(JMSException jmse)
	    {
	        String m = "JMS exception, internal error. Reason: " +
	                   jmse.getMessage();
	        logger.error(m);
	        logger.debug(m, jmse);
	    }
        catch(Throwable t)
        {
        	String m = "Unknown exception occured while receiving message, " +
        	           "reason: " + t.getMessage();
        	logger.error(m);
        	logger.debug(m, t);
        }
	
	} // onMessage() --------------------------------------------------------

} // class StatusUpdatesListener ============================================ 




/**
 * Listens for queries from subsystems RunCom instance which are starting-up
 * and want to update status of subsystems according to current states.
 * This listener responses with last messages from the local message
 * repository.
 */
final class StatusUpdatesQueryListener implements MessageListener
{	
    private static MyLogger logger =
    	MyLogger.getLogger(StatusUpdatesQueryListener.class); 

    
    private Session session = null;
    private MessageProducer replyProducer = null;
    // persistent repository reference
    private PersistentRepository repository = null;
    
    
    
    public StatusUpdatesQueryListener(Session session,
    		                          MessageProducer replyProducer,
    		                          PersistentRepository repository)
    {
    	this.session = session;
    	this.replyProducer = replyProducer;
    	this.repository = repository;
    	
    } // StatusUpdatesQueryListener() ---------------------------------------

    
    
    private void sendUpdateResponse(Message msg, ObjectMessage response)
    throws Throwable
    {
        // destination for the update response should have been set
        // in the query message by the requester (by setJMSReplyTo())
        if(msg.getJMSReplyTo() != null)
        {
        	logger.debug("Reply to be sent to destination: " +
        			     msg.getJMSReplyTo().toString());
            replyProducer.send(msg.getJMSReplyTo(), response);
        }
        else
        {
        	logger.fatal("Reply destination not set for reply producer, " +
        			     "subsystems updates query response not sent.");
        }
    	
    } // sendUpdateResponse() -----------------------------------------------
    
    
    
    /**
     * Is supposed to receive an empty message as a subsystems updates
     * request (thus doesn't care about incoming Message msg). Sends
     * last message for each desk (status, DQ data, etc),  and last
     * text messages that were distributed and last white board message. 
     * Message msg must have correctly set JMSReplyTo (destination).
     */
	public void onMessage(Message msg) 
	{
	    try
	    {
	    	// this listener doesn't care about type of incoming message
	    	// it should be an empty message anyway, a starting up
	    	// RunCom instance is asking for subsystems updates (i.e.
	    	// last message for each subsystem)
	    		    	
            logger.info("ObjectMessage received (update query listener), " +
            		    "processing ...");
            
            // send desk status updates
            ArrayList<DeskDataMessage> currentStates = null;
            currentStates = repository.getLastDeskMessages();
            
            logger.debug("Sending update (DeskDataMessage) for " + currentStates.size() +
            		     " subsystem(s).");
            
            ObjectMessage deskUpdateObjectMsg =
            	session.createObjectMessage(currentStates);
            
            sendUpdateResponse(msg, deskUpdateObjectMsg);
            
            logger.info("Sending of DeskDataMessage(s) finished.");
          

            // send text messages update
            ArrayList<TextMessage> lastTextMessages = null;
            lastTextMessages = repository.getLastTextMessages();
            
            logger.debug("Sending update (TextMessage), messages: " +
            		     lastTextMessages.size());
            
            ObjectMessage textUpdateObjectMsg =
            	session.createObjectMessage(lastTextMessages);
            
            sendUpdateResponse(msg, textUpdateObjectMsg);
            
            logger.info("Sending LastTextMessage(s) finished.");
            
            
            
            // send last WhiteBoardMessage update
            ArrayList<WhiteBoardMessage> lastWhiteBoardMsg = null;
            lastWhiteBoardMsg = repository.getLastWhiteBoardMessage();
            
            logger.debug("Sending update (WhiteBoardMessage).");
            
            ObjectMessage whiteBoardUpdateObjectMsg =
                session.createObjectMessage(lastWhiteBoardMsg);
            
            sendUpdateResponse(msg, whiteBoardUpdateObjectMsg);
            
            logger.info("Sending WhiteBoardMessage(s) finished.");
            
	        // acknowledge?
	        // if acknowledge mode == Session.CLIENT_ACKNOWLEDGE
	        // then msg.acknowledge()
	            
	    }
	    catch(JMSException jmse)
	    {
	        String m = "JMS exception while receiving update request or " +
	                   "sending update response, reason: " + jmse.getMessage();
	        logger.error(m);
	        logger.debug(m, jmse);
	    }
        catch(Throwable t)
        {
        	String m = "Unknown exception occured while receiving update request " +
        	           "reason: " + t.getMessage();
        	logger.error(m);
        	logger.debug(m, t);
        }
	   
	} // onMessage() --------------------------------------------------------

} // class StatusUpdatesQueryListener =======================================
