package runcom;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JButton;

import runcom.deskconfig.DeskConfiguration;

import mylogger.MyLogger;



/**
 * Class of a desk sub-system panel having the observer functionality in
 * which it shows all information but doesn't permit to change status
 * or reset, etc. Changing status is possible when this instance is 'enabled'.
 * 
 * @author Zdenek Maxa
 */
@SuppressWarnings("serial")
public class DeskPanel extends JPanel implements IDeskPanel
{
    
    private static MyLogger logger = MyLogger.getLogger(DeskPanel.class);    
    
    // GUI components within the desk panel exposed to changes
    private JLabel stateLabel = null;
    private JButton newShifterButton = null;
    private JButton signOffButton = null;
    private JButton errorButton = null;
    private JButton readyButton = null;
    private JTextField personNameTextField = null;   
    private JPanel mainPanel = null;
    protected JSpinner dqCheckPeriodSpinner = null;
    protected JCheckBox dqCheckedStatusCheckBox = null;
    private JLabel dqLastCheckedLabel = null;
    private JLabel notAcknowledgedCounterLabel = null;
    
    // date format
    public static final SimpleDateFormat DATE_FORMAT =
    	new SimpleDateFormat("HH:mm:ss dd/MM/yy");
    
    private static final Font FONT = new Font("SansSerif", Font.BOLD, 10);

    public static final String NEW_SHIFTER_ACTION = "new shifter";
    public static final String SIGN_OFF_ACTION = "sign off";
    public static final String READY_ACTION = "ready";
    public static final String ERROR_ACTION = "error";
    
    
    
    /**
     * 
     * @param name - name of the desk / subsystem
     */
    public DeskPanel(String name)
    {
    	// main panel with desk title, status buttons, etc
    	createTitleAndStatusPanel(name);
        
        // data quality panel
        JPanel dqPanel = this.createDQPanel(name);
        this.add(dqPanel);
        
        JPanel messagesAckPanel = this.createMessageAcknowledgePanel();
        this.add(messagesAckPanel);
        
    } // DeskPanel() --------------------------------------------------------

    
    
    private JPanel createMessageAcknowledgePanel()
    {
    	JPanel panel = new JPanel();
    	
    	panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        
        JLabel label = new JLabel("Number of messages to acknowledge: ");
        label.setFont(FONT);
        panel.add(label, BorderLayout.WEST);
        notAcknowledgedCounterLabel = new JLabel("0");
        notAcknowledgedCounterLabel.setFont(FONT);
        panel.add(notAcknowledgedCounterLabel, BorderLayout.CENTER);
    	
    	return panel;
    	
    } // createMessageAcknowledgePanel() ------------------------------------
    
    
    
    private void createTitleAndStatusPanel(String name)
    {
    	this.setLayout(new FlowLayout());
    	
        // border:  top, left, bottom, right border around panel
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // main panel
        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // title panel
        JPanel titlePanel = new JPanel();
        // FlowLayout will center the component
        titlePanel.setLayout(new FlowLayout());
        //titlePanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JPanel stateTitlePanel = new JPanel();
        stateTitlePanel.setLayout(new FlowLayout());
        JLabel titleLabel = new JLabel(name);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titlePanel.add(titleLabel);
        stateLabel = new JLabel(DeskData.UNASSIGNED);
        stateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        stateTitlePanel.add(stateLabel);
        mainPanel.add(titlePanel);
        mainPanel.add(stateTitlePanel);
        
        // state buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(5, 5));
        newShifterButton = new JButton(NEW_SHIFTER_ACTION);
        newShifterButton.setActionCommand(NEW_SHIFTER_ACTION);
        newShifterButton.addActionListener(new StateButtonsActionListener(name));
        newShifterButton.setEnabled(false);
        newShifterButton.setToolTipText("gets system into " + DeskData.ASSIGNED +
                                        " state (or assign a new shifter) - system " +
                                        "part of the global run and shifter " +
                                        "present");
        leftPanel.add(newShifterButton, BorderLayout.NORTH);
        errorButton = new JButton(ERROR_ACTION);
        errorButton.setActionCommand(ERROR_ACTION);
        errorButton.addActionListener(new StateButtonsActionListener(name));
        errorButton.setEnabled(false);
        errorButton.setToolTipText("gets the system into " + DeskData.ERROR +
                                   " state - part of the global run but has a problem");
        leftPanel.add(errorButton, BorderLayout.SOUTH);
        
        buttonsPanel.add(leftPanel);
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(5, 5));
        
        signOffButton = new JButton(SIGN_OFF_ACTION);
        signOffButton.setActionCommand(SIGN_OFF_ACTION);
        signOffButton.addActionListener(new StateButtonsActionListener(name));
        signOffButton.setEnabled(false);
        signOffButton.setToolTipText("gets the system into " + DeskData.UNASSIGNED +
                                     " state - not part of the global run");
        rightPanel.add(signOffButton, BorderLayout.NORTH);
        
        
        readyButton = new JButton(READY_ACTION);
        readyButton.setActionCommand(READY_ACTION);
        readyButton.addActionListener(new StateButtonsActionListener(name));
        readyButton.setEnabled(false);
        readyButton.setToolTipText("gets the system into " + DeskData.READY +
                                   " state - ready to take part in global run " +
                                   "(in general, not for a specific run)");
        rightPanel.add(readyButton, BorderLayout.SOUTH);
        buttonsPanel.add(rightPanel);
        mainPanel.add(buttonsPanel);
                
        // text field panel
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        JLabel textLabel = new JLabel("signed in shifter");
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        textPanel.add(textLabel);
        personNameTextField = new JTextField("", 6);
        personNameTextField.setBorder(BorderFactory.createLineBorder(Color.black));
        personNameTextField.setEditable(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(3, 6, 6, 6));
        textPanel.add(personNameTextField);
        mainPanel.add(textPanel);
        
        this.add(mainPanel);
    	
    } // createTitleAndStatusPanel() ----------------------------------------
    
    
    
    /**
     * Create read/only data quality (DQ) subpanel. In the supervisor mode,
     * there will also be a reset button and check period editable
     * @return
     */
    private JPanel createDQPanel(String name)
    {
        String dqToolTip = "Click to check DQ, must be in " + DeskData.READY +
                           " state first";
    	JPanel dqPanel = new JPanel();
    	
    	dqPanel.setLayout(new BoxLayout(dqPanel, BoxLayout.Y_AXIS));
        dqPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        
        JPanel dqPeriodPanel = new JPanel();
        dqPeriodPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel dqPeriodLabel = new JLabel("DQ check period [min]");
        dqPeriodLabel.setFont(FONT);
        
        SpinnerModel model = new SpinnerNumberModel(60, 1, 120, 1);
        dqCheckPeriodSpinner = new JSpinner(model);
        dqCheckPeriodSpinner.setEnabled(false);
        
        dqPeriodPanel.add(dqPeriodLabel);
        dqPeriodPanel.add(dqCheckPeriodSpinner);
        
        dqPanel.add(dqPeriodPanel);
        
        JPanel dqCheckedPanel = new JPanel();
        dqCheckedPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel dqCheckedLabel = new JLabel("DQ checked status");
        dqCheckedLabel.setFont(FONT);
        dqCheckedStatusCheckBox = new JCheckBox();
        dqCheckedStatusCheckBox.setEnabled(false);
        dqCheckedStatusCheckBox.setSelected(false);
        dqCheckedStatusCheckBox.setName(name);
        dqCheckedLabel.setToolTipText(dqToolTip);
        dqCheckedStatusCheckBox.setToolTipText(dqToolTip);
        dqCheckedPanel.add(dqCheckedLabel);
        dqCheckedPanel.add(dqCheckedStatusCheckBox);
        
        dqPanel.add(dqCheckedPanel);
        
        JPanel dqLastPanel = new JPanel();
        dqLastPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel dqLastLabel =  new JLabel("DQ last checked");
        dqLastLabel.setFont(FONT);
        dqLastCheckedLabel = new JLabel("n/a");
        dqLastCheckedLabel.setFont(FONT);
        dqLastPanel.add(dqLastLabel);
        dqLastPanel.add(dqLastCheckedLabel);
        
        dqPanel.add(dqLastPanel);
            	
    	return dqPanel;
    	
    } // createDQPanel() ----------------------------------------------------
    
    
    
    public void setDeskPanelEnabled(boolean enabled)
    {
        newShifterButton.setEnabled(true);
        dqCheckedStatusCheckBox.setEnabled(enabled);
        
    } // setSubSystemStatus() -----------------------------------------------
       
            
    
    public void setDQCheckedStatusActionListener(ActionListener listener)
    {
    	this.dqCheckedStatusCheckBox.addActionListener(listener);
    	
    } // setDQCheckedStatusActionListener() ---------------------------------
    
    
    
    /**
     * Set colour and state title. 
     * if setCompomentsState is true - set how buttons are enabled / disabled
     * between transitions of states.
     */
    synchronized public void setStatus(final String state, final Color color,
                                       final boolean setCompomentsState)
    {        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                logger.debug("Setting state of graphical compoments ...");
                mainPanel.setBackground(color);
                stateLabel.setText(state);
                stateLabel.setForeground(color);
                
                // correct combination of button being enabled / disabled
                // but changing only if running its own desk
                if(setCompomentsState)
                {
                    if(DeskData.UNASSIGNED.equals(state))
                    {
                        newShifterButton.setEnabled(true);
                        signOffButton.setEnabled(false);
                        errorButton.setEnabled(false);
                        readyButton.setEnabled(false);
                    }
                    else if(DeskData.ASSIGNED.equals(state))
                    {
                        newShifterButton.setEnabled(false);
                        signOffButton.setEnabled(true);
                        errorButton.setEnabled(true);
                        readyButton.setEnabled(true);
                    }
                    else if(DeskData.READY.equals(state))
                    {
                        newShifterButton.setEnabled(true);
                        signOffButton.setEnabled(true);
                        errorButton.setEnabled(true);
                        readyButton.setEnabled(false);
                    }
                    else if(DeskData.ERROR.equals(state))
                    {
                        newShifterButton.setEnabled(false);
                        signOffButton.setEnabled(true);
                        errorButton.setEnabled(false);
                        readyButton.setEnabled(true);
                    }                    
                }
            }
        });
        
    } // setStatus() --------------------------------------------------------
    
    
    
    synchronized public void setPersonNameTextField(String text)
    {
        this.personNameTextField.setText(text);
        
    } // setPersonNameTextField() -------------------------------------------

    
    
    synchronized public void setNotAcknowledgedCounterLabel(String counter)
    {
    	this.notAcknowledgedCounterLabel.setText(counter);
    	
    } // setNotAcknowledgedCounterLabel() -----------------------------------
    
    
    
    synchronized public void setDQLastChecked(Date date)
    {
    	
    	String sDate = "n/a";
    	if(date != null)
    	{
    		sDate = DATE_FORMAT.format(date);
    	}
    	this.dqLastCheckedLabel.setText(sDate);
  
    } // setDQLastChecked() -------------------------------------------------
    
    
    
    synchronized public void setDQCheckPeriodMinutes(int minutes)
    {
    	this.dqCheckPeriodSpinner.setValue(minutes);
    	
    } // setDQCheckPeriodMinutes() ------------------------------------------
    
    
    
    synchronized public void setDQStatusChecked(boolean status)
    {
    	this.dqCheckedStatusCheckBox.setSelected(status);
    	
    } // setDQStatusChecked() -----------------------------------------------

    
} // class DeskPanel ========================================================



class StateButtonsActionListener implements ActionListener
{
    private static MyLogger logger =
        MyLogger.getLogger(StateButtonsActionListener.class);
    private String deskName = null;
    
    
    
    public StateButtonsActionListener(String deskName)
    {
        this.deskName = deskName;
        
    } // StateButtonsActionListener() ---------------------------------------
    
    
    /**
     * Method defines transitions between states of a desk. Clearer here via
     * buttons than previous solution via ComboBox.
     * 
     */
    public void actionPerformed(ActionEvent ae)
    {
        String action = ae.getActionCommand();
        
        final DeskData deskData = RunCom.getGUI().getDeskData(deskName);
        String status = deskData.getStatus();
        
        logger.debug("\"" + action + "\" button pressed for " +
                     "\"" + deskName + "\", current status " +
                     "\"" + status + "\" ..."); 
        
        if(DeskPanel.NEW_SHIFTER_ACTION.equals(action))
        {
            if(DeskData.UNASSIGNED.equals(status))
            {
                // currently unassigned, show the authentication dialog
                logger.debug("Status change " + DeskData.UNASSIGNED + " -> " +
                             DeskData.ASSIGNED + " calling auth dialog ...");
                                              
               // perform authentication upon a particular desk (later from LDAP)
               // Authentication dialog itself then changes the desk state
               AuthenticationDialog.createAndShowGUI(deskData, DeskData.ASSIGNED);
            }
            else if(DeskData.READY.equals(status))
            {
                // if in the ready state, could sign in a new shifter, but
                // will stay in the ready state only if a checklist is completed,
                // checklist is called from the authentication dialog
                
                // reset the current username and prompt authentication dialog
                deskData.setAssignedUserName("");

                logger.debug("Signing in a new shifter, calling auth dialog ...");
                // moving to target state ready
                AuthenticationDialog.createAndShowGUI(deskData, DeskData.READY);                
            }
        }
        else if(DeskPanel.SIGN_OFF_ACTION.equals(action))
        {
            // set the desk state
            deskData.setStatus(DeskData.UNASSIGNED);
            // if unassigned, reset the assigned person name
            deskData.setAssignedUserName("");
            RunCom.sendMessage(deskData); // distribute
        }
        else if(DeskPanel.ERROR_ACTION.equals(action))
        {
            deskData.setStatus(DeskData.ERROR);
            RunCom.sendMessage(deskData); // distribute
        }
        else if(DeskPanel.READY_ACTION.equals(action))
        {
            DeskConfiguration dc = RunCom.getDeskConfiguration(deskName);
            String checkListFileName = dc.getSignInCheckListFileName();
            logger.warn("Calling CheckList to display: \"" +
                         checkListFileName + "\" ...");
            CheckListCaller.call(checkListFileName, deskName);
            
            // upon completed checklist, the CheckList application will call
            // back RunCom to change status on an appropriate desk
        }
        
    } // actionPerformed() --------------------------------------------------
    
    
} // StateButtonsActionListener() ===========================================