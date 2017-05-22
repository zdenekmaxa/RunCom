package runcom;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import runcom.deskconfig.DeskConfiguration;

import mylogger.MyLogger;

/**
 * Handles events triggered on the data quality status checked check box.
 * Going from unticked to ticked, i.e. DQ status unchecked to checked
 * requires completing a DQ check checklist. Otherwise the status stays
 * at not ticked.
 * @author Zdenek Maxa
 */
public final class DQStatusCheckBoxListener implements ActionListener
{
    private static MyLogger logger =
    	MyLogger.getLogger(DQStatusCheckBoxListener.class);
    
    // reference to the desk data instance it handles events on
    private DeskData deskData = null;

    
    
    public DQStatusCheckBoxListener(DeskData deskData)
    {
        this.deskData = deskData;

    } // DQStatusCheckBoxListener() -----------------------------------------
    
    
    
    synchronized public void actionPerformed(ActionEvent ae)
    {
        JCheckBox checkBox = (JCheckBox) ae.getSource();
        boolean newStatus = checkBox.isSelected();
        String deskName = this.deskData.getDeskName();
        boolean prevStatus = this.deskData.getDqStatusChecked();
        
        
        logger.debug("Action event on DQ-status-checked CheckBox \"" + deskName +
                     "\" status: " + prevStatus + " -> " + newStatus + " ...");
                
        // this method is also called when programatically setting state
        // on the checkbox (if checklist doesn't get completed or when message
        // arrives instructing to tick/untick the checkbox
        // statuses are equal, then don't do any event processing
        if(newStatus == prevStatus)
        {
            logger.debug("Action event on DQ-status-checked CheckBox, previous " +
            		     "and new statuses are equal, quit.");    
            return;
        }

        
        // change back the actual state of checkbox box for the moment
        // if status change gets validated (e.g. if CheckList is completed),
        // it will be changed as desired (in the deskData.setDQStatusChecked())
        checkBox.setSelected(prevStatus);
        
        // check status of the desk
        if(newStatus && ! DeskData.READY.equals(deskData.getStatus()))
        {
            String m = "DQ check can only be performed if in the " +
                       DeskData.READY + " state.";
            JOptionPane.showMessageDialog((JFrame) RunCom.getGUI(), m,
                                          "RunCom warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        

        if(newStatus)
        {
        	// want to change to DQ status checked, present CheckList
        	
        	logger.debug("New status on DQ checked CheckBox is \"" +
        			     newStatus + "\", show DQ CheckList ...");
        	
            DeskConfiguration dc = RunCom.getDeskConfiguration(deskName);
            String dqCheckListFileName = dc.getDqCheckListFileName();
            logger.debug("Calling CheckList to dislay: \"" +
                         dqCheckListFileName + "\" ...");
            CheckListCaller.call(dqCheckListFileName, deskName);
            
            // upon completed checklist, CheckList application will call back
            // RunCom to change status on an appropriate desk
        }
        else
        {
        	// new status goes from checked to unchecked, perform change
        	// and notify the others
        	
        	logger.debug("New status on DQ checked CheckBox is \"" +
        			     newStatus + "\", going to change state and notify others ...");
        			
            this.deskData.setDqStatusChecked(newStatus);
            RunCom.sendMessage(this.deskData); // distribute
        }
                
    } // actionPerformed() --------------------------------------------------


    
} // class DQStatusCheckBoxListener =========================================