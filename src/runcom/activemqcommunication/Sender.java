package runcom.activemqcommunication;

import java.util.Date;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.ObjectMessage;
import javax.swing.JOptionPane;

import org.apache.activemq.ActiveMQConnectionFactory;

import mylogger.MyLogger;

import runcom.RunComException;
import runcom.activemqcommunication.Receiver;
import runcom.DeskData;



/**
 * Message sender communication class using Apache ActiveMQ.
 * @author Zdenek Maxa
 */
public final class Sender
{
    private static MyLogger logger = MyLogger.getLogger(Sender.class);
    
    private static Sender instance = null;
    
    // communication attributes
    private Connection connection = null;
    private Session session = null;
    private Destination destination = null;
    private MessageProducer producer = null;
    
    
    
    
    
    private Sender(String messageBrokerName) throws RunComException
    {
                
        try
        {
        	
            ActiveMQConnectionFactory connectionFactory = null;
            connectionFactory = new ActiveMQConnectionFactory(messageBrokerName);                  
            logger.info("Connecting message sender to broker: " + messageBrokerName + " ...");
        	
            connection = connectionFactory.createConnection();
            connection.start();
            
            // create the session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // destination = session.createQueue(subject); // one-to-one
            destination = session.createTopic(Receiver.SUBJECT); // one-to-many
            
            // create the producer
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            // producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            // producer.setTimeToLive(timeToLive);
            
            logger.info("Message sender initialised.");
            
            // Use the ActiveMQConnection interface to dump the connection
            // stats. (?)
            // ActiveMQConnection c = (ActiveMQConnection)connection;
            // c.getConnectionStats().dump(new IndentPrinter());
            
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while message sender " +
                       "initialisation, exit. Reason: " + jmse.getMessage();
            logger.debug(m, jmse);
            throw new RunComException(m);
        }
                
    } // Sender() -----------------------------------------------------------

    
    
    private void sendMessage(ObjectMessage objectMsg)
    {
    	try
    	{
    		logger.debug("Sending ObjectMessage ...");
            producer.send(objectMsg);

            logger.info("Message sucessfully sent.");
            // if (transacted) then session.commit()    		
    	}
        catch(JMSException jmse)
        {
            String m = "JMSException occured while sending message. " +
                       "Reason: " + jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }
    	
    } // sendMessage() ------------------------------------------------------

    
    
    public void sendMessage(TextMessage tm)
    {
        try
        {
            ObjectMessage objectMsg = session.createObjectMessage(tm);
            this.sendMessage(objectMsg);
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while creating ObjectMessage for " +
                       "sending. Reason: " + jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }
    	
    } // sendMessage() ------------------------------------------------------
    

    
    public void sendMessage(RequestMessage rm)
    {
        try
        {
            ObjectMessage objectMsg = session.createObjectMessage(rm);
            this.sendMessage(objectMsg);
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while creating ObjectMessage for " +
                       "sending. Reason: " + jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }
        
    } // sendMessage() ------------------------------------------------------

    
    
    public void sendMessage(WhiteBoardMessage wbm)
    {
        try
        {
            ObjectMessage objectMsg = session.createObjectMessage(wbm);
            this.sendMessage(objectMsg);
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while creating ObjectMessage for " +
                       "sending. Reason: " + jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }
        
    } // sendMessage() ------------------------------------------------------

    

    public void sendMessage(DeskData deskData)
    {
        DeskDataMessage ddm = createDeskDataMessage(deskData);
        logger.info("Sending message: " + ddm.toString());
                
        try
        {
            ObjectMessage objectMsg = session.createObjectMessage(ddm);
            this.sendMessage(objectMsg);
        }
        catch(JMSException jmse)
        {
            String m = "JMSException occured while creating ObjectMessage for " +
                       "sending. Reason: " + jmse.getMessage();
            logger.error(m);
            logger.debug(m, jmse);
        }   
        
    } // sendMessage() ------------------------------------------------------
    
    
    
    private DeskDataMessage createDeskDataMessage(DeskData deskData)
    {
        DeskDataMessage ddm = new DeskDataMessage();
        
        ddm.setTimeOfMessage(new Date(System.currentTimeMillis()));
        
        ddm.setDeskName(deskData.getDeskName());
        ddm.setCurrentStatus(deskData.getStatus());
        ddm.setAssignedUserName(deskData.getAssignedUserName());
        
        ddm.setDqCheckPeriodMinutes(deskData.getDqCheckPeriodMinutes());
        ddm.setDqLastChecked(deskData.getDqLastChecked());
        ddm.setDqStatusChecked(deskData.getDqStatusChecked());
        
        ddm.setNotAckMessagesCounter(deskData.getNotAckMessagesCounter());
        
        ddm.setPendingInjectionRequestsCounter(deskData.getPendingInjectionRequestsCounter());
        ddm.setPendingStableBeamRequestsCounter(deskData.getPendingStableBeamRequestsCounter());
        ddm.setPendingStartRunRequestsCounter(deskData.getPendingStartRunRequestsCounter());
        
        return ddm;
        
    } // createDeskDataMessage() --------------------------------------------
    
    
   
    public static Sender getInstance(String messageBrokerName) throws RunComException
    {
        if(instance == null)
        {
            instance = new Sender(messageBrokerName);
        }
        
        return instance;
        
    } // getInstance() ------------------------------------------------------
    
    
    
    public void shutdownSender()
    {
        logger.info("Shutting down the message sender ...");

        try
        {
            session.close();
            producer.close();
            connection.close();
            logger.info("Message sender was successfully closed.");
        }
        catch(JMSException jmse)
        {
            logger.error("Error while closing the message sender.");            
        }
        
    } // shutdownSender() ---------------------------------------------------
    
    
    
} // class Sender ===========================================================