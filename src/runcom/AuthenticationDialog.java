package runcom;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import runcom.deskconfig.DeskConfiguration;

import mylogger.MyLogger;

/**
 * Authentication dialog used when changing status from unassigned to assigned
 * @author Zdenek Maxa
 */
@SuppressWarnings("serial")
public final class AuthenticationDialog extends JFrame
                                        implements Runnable, ActionListener
{
    private static MyLogger logger = MyLogger.getLogger(AuthenticationDialog.class);
    
    private static AuthenticationDialog instance = null;
    
    private static DeskData deskData = null;
    
    // the state for which a desk is aiming when using the authentication dialog
    private static String targetState = null;

    private JTextField userNameTextField = null;
    // private JPasswordField passwordField = null; - should be used in the future

    

    
    
    public static void createAndShowGUI(DeskData deskData, String targetState)
    {
        if(instance == null)
        {
            AuthenticationDialog.deskData = deskData;
            AuthenticationDialog.targetState = targetState;
            SwingUtilities.invokeLater(new AuthenticationDialog());
        }        
        
    } // createAndShowGUI() -------------------------------------------------
    
    
    
    /**
     * Method called by the Swing event-dispatching thread.
     * Construct the GUI here.
     */
    public void run()
    {
        logger.debug("Creating AuthenticationDialog GUI ... ");
        
        this.setLayout(new BorderLayout());
        
        this.setTitle("\"" + deskData.getDeskName() + "\" authentication");
        
        this.setResizable(false);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        // panel with username
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new FlowLayout());
        JLabel nameLabel = new JLabel("your name: ");
        userNameTextField = new JTextField(14);
        namePanel.add(nameLabel);
        namePanel.add(userNameTextField);
        panel.add(namePanel, BorderLayout.NORTH);
        
        // panel with password
        /*
        JPanel passPanel = new JPanel();
        passPanel.setLayout(new FlowLayout());
        JLabel passLabel = new JLabel("password: ");
        passField = new JPasswordField(14);
        // password authentication not implemented at the moment
        passField.setEnabled(false);
        passPanel.add(passLabel);
        passPanel.add(passField);
        panel.add(passPanel, BorderLayout.CENTER);
        */
        
        // panel with buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        this.add(panel);
        
        this.setSizeAndLocation();
        
        this.setAlwaysOnTop(true);
        
        this.addWindowListener(new AuthenticationDialogWindowAdapter(this));
        
        instance = this;
        
    } // run() --------------------------------------------------------------

    
    
    /**
     * Handle button events
     */
    public void actionPerformed(ActionEvent ae)
    {
        String actionCommand = ae.getActionCommand();
        
        if("ok".equals(actionCommand))
        {
            // ok button clicked
            String userName = this.userNameTextField.getText();
            // later will also retrieve the password and perform proper
            // authentication
            if(userName != null && ! "".equals(userName.trim()))
            {
                // all right, set the persons name and set state
                deskData.setAssignedUserName(userName.trim());
                
                // consult target state into which the desk should be brought
                // from this dialog
                if(DeskData.ASSIGNED.equals(targetState))
                {
                    // normal assigned (signed in) state
                    deskData.setStatus(DeskData.ASSIGNED);
                    logger.debug("Setting the desk \"" + deskData.getDeskName() + "\" " +
                                 "temporarility to " + DeskData.ASSIGNED);   
                    RunCom.sendMessage(deskData); // distribute
                }
                else if(DeskData.READY.equals(targetState))
                {
                    // set the assigned temporarily, if the checklist gets
                    // completed the state will be changed back to ready
                    // otherwise stays only assigned
                    deskData.setStatus(DeskData.ASSIGNED);
                    RunCom.sendMessage(deskData); // distribute
                    
                    String deskName = deskData.getDeskName();
                    DeskConfiguration dc = RunCom.getDeskConfiguration(deskName);
                    String checkListFileName = dc.getSignInCheckListFileName();
                    logger.warn("Calling CheckList to display: \"" +
                                    checkListFileName + "\" ...");
                    CheckListCaller.call(checkListFileName, deskName);
                }
            }
            else
            {
                JOptionPane.showMessageDialog(this, "Username not entered.",
                                              "RunCom error",
                                              JOptionPane.ERROR_MESSAGE);
                logger.debug("Authentication not done, no username provided.");
            }
        }
        else if("cancel".equals(actionCommand))
        {
            logger.debug("Authentication not done, cancel button pressed.");
        }
        
        this.destroyDialog();
        
    } // actionPerformed() --------------------------------------------------
    


    private void setSizeAndLocation()
    {
        // display the window centered on the RunCom window
    	RunComGUI gui = RunCom.getGUI();
        Dimension windowSize = gui.getSize();
        Point upLeft = gui.getLocationOnScreen();        
        this.setLocation((int) upLeft.getX() + (windowSize.width / 2),
                         (int) upLeft.getY() + (windowSize.height / 2));
        this.pack();
        this.setVisible(true);
        
    } // setSizeAndLocation() -----------------------------------------------
    
    
    
    public void destroyDialog()
    {
        AuthenticationDialog.instance = null;
        AuthenticationDialog.deskData = null;
        this.dispose();

    } // destroyDialog() ----------------------------------------------------



} // class AuthenticationDialog =============================================



class AuthenticationDialogWindowAdapter extends WindowAdapter
{
    AuthenticationDialog dialog = null;
        
    
    public AuthenticationDialogWindowAdapter(AuthenticationDialog dialog)
    {
        this.dialog = dialog;
        
    } // AuthenticationDialogWindowAdapter() --------------------------------
    
    
    
    public void windowClosing(WindowEvent we)
    {
        dialog.destroyDialog();
        
    } // windowClosing() ----------------------------------------------------
    
} // class AuthenticationDialogWindowAdapter ================================ 
