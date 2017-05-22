package runcom.activemqcommunication;

import java.io.EOFException;

import javax.swing.JOptionPane;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.ObjectMessage;

// import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import mylogger.MyLogger;

import runcom.RunCom;
import runcom.RunComException;

/**
 * Receiver (listener, producer) communication class using Apache ActiveMQ.
 * Both Receiver and Sender and using the same 'subject' when creating
 * Topic (one sender, multiple consumers scenario). This way, the Receiver
 * will also receive messages sent by itself. If this is perceived as a
 * drawback, multiple topics (one per desk plus supervisor) will be
 * created and a particular RunCom Receiver will only subscribe to each 
 * desk topic but his own and to supervisor topic, otherwise it's fine as is.
 * @author Zdenek Maxa
 */
public final class Receiver implements MessageListener, ExceptionListener
{
    
    private static MyLogger logger = MyLogger.getLogger(Receiver.class);
    
    private static Receiver instance = null;
    
    // communication attributes
    public static final String SUBJECT = "RunComMessageSubject";
        
    private Session session = null;
    private Destination destination = null;
    private Connection connection = null;
    private MessageConsumer consumer = null;
    
    
    
    
    private Receiver(String messageBrokerName) throws RunComException
    {            
        try
        {   	
            ActiveMQConnectionFactory connectionFactory = null;
            connectionFactory = new ActiveMQConnectionFactory(messageBrokerName);
            logger.info("Connecting message receiver to broker: " +
                            messageBrokerName + " ...");        
        	
            connection = connectionFactory.createConnection();
            
            // if using durable subscriptions, then specify clientID for
            // connection.setClientID( ... );

            connection.setExceptionListener(this);
            connection.start();
            
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // destination = session.createQueue(subject); // one-to-one
            destination = session.createTopic(SUBJECT); // one-to-many
            
            consumer = session.createConsumer(destination);
            consumer.setMessageListener(this);
            
            logger.info("Message Receiver successfully started. Listening ...");
            
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while message receiver " +
                       "initialisation, exit. Reason: " + jmse.getMessage();
            logger.debug(m, jmse);
            throw new RunComException(m);
        }
        
    } // Receiver() ---------------------------------------------------------
    
    
        
    /**
     * If there are more types of messages (DeskDataMessage, TextMessage and
     * something else), then should probably use different destinations (?)
     */
    public void onMessage(Message msg) 
    {
        try
        {
            if(msg instanceof ObjectMessage)
            {
                logger.debug("ObjectMessage received, processing ...");
                ObjectMessage oMsg = (ObjectMessage) msg;
                if(oMsg.getObject() instanceof DeskDataMessage)
                {
                    DeskDataMessage ddm = (DeskDataMessage) oMsg.getObject(); 
                    logger.info("Received and extracted DeskDataMessage ...");
                    RunCom.updateLocalData(ddm);          	
                }
                else if(oMsg.getObject() instanceof TextMessage)
                {
                	TextMessage tm = (TextMessage) oMsg.getObject();
                	logger.info("Received and extracted TextMessage ...");
                	// true to consider ack flag - this not at start-up where
                	// the ack flag is ignored
                	RunCom.displayIncomingTextMessage(tm, true);
                }
                else if(oMsg.getObject() instanceof RequestMessage)
                {
                    RequestMessage rm = (RequestMessage) oMsg.getObject();
                    logger.info("Received and extracted RequestMessage ...");
                    RunCom.processRequestMessage(rm);
                }
                else if(oMsg.getObject() instanceof WhiteBoardMessage)
                {
                    WhiteBoardMessage wbm = (WhiteBoardMessage) oMsg.getObject();
                    logger.info("Received and extracted WhiteBoardMessage ...");
                    RunCom.updateWhiteBoardMessage(wbm);
                }
                else
                {
                    
                    logger.warn("Message of some unknown type " +
                                "received, dropped.");
                    // btw interesting to see message content ...
                    // logger.warn("received message: " + o<sg);
                }
            } 
            else 
            {
                logger.error("Message other than ObjectMessage instance " +
                		     "received, message: " + msg);
            }

            // acknowledge?
            // if acknowledge mode == Session.CLIENT_ACKNOWLEDGE
            // then msg.acknowledge()
        }
        catch(JMSException jmse)
        {
            String m = "JMS exception, internal error. Reason: " +
                       jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }

    } // onMessage() --------------------------------------------------------
    
    
    public synchronized void onException(JMSException jmse)
    {
        String m = "Message Receiver: JMS exception occured, reason: " +
                   jmse.getMessage();
        logger.error(m);
        
        // this exception occurs when session is established and
        // broker for instance goes down
        if(jmse.getCause() instanceof EOFException)
        {
            String msg = "Connection with RunCom Broker has probably been lost.\n" +
                         "Please restart RunCom.";
            JOptionPane.showMessageDialog(null, msg, "RunCom error",
                                          JOptionPane.ERROR_MESSAGE);
            String logMsg = "Connection with RunCom Broker has probably been " +
                            "lost (EOFException), have to manually restart RunCom."; 
            logger.error(logMsg);
            logger.debug(logMsg, jmse);
        }
       
        // fancy way of dealing with errors here would be to
        // try periodically re-establish connection with broker

    } // onException() ------------------------------------------------------
    
    
    
    public static Receiver getInstance(String messageBrokerName) throws RunComException
    {
        if(instance == null)
        {
            instance = new Receiver(messageBrokerName);
        }
        
        return instance;
        
    } // getInstance() ------------------------------------------------------
    
    
    
    public void shutdownReceiver()
    {
        logger.warn("Shutting down the message receiver ...");

        try
        {
            consumer.close();
            session.close();
            connection.close();
            
            logger.warn("Message receiver was successfully closed.");
        }
        catch(JMSException jmse)
        {
            logger.error("Error while closing the message receiver.");            
        }
        
    } // shutdownReceiver() -------------------------------------------------

} // class Receiver =========================================================