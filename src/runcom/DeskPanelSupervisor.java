package runcom;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;

import mylogger.MyLogger;


/** 
 * Desk sub-system GUI panel class allowing supervisor mode, i.e. possibility
 * to change status for all, reset button, etc.
 * @author Zdenek Maxa
 */
@SuppressWarnings("serial")
public final class DeskPanelSupervisor extends DeskPanel
{
    
	/**
	 * 
	 * @param name - name of the desk / subsystem
	 */
    public DeskPanelSupervisor(String name)
    {
        super(name);
        
        super.setDeskPanelEnabled(true);
        
        // enable DQ period spinner - only for supervisor
        this.dqCheckPeriodSpinner.setEnabled(true);
        
        // add DQ reset button - reset the subsystem desk status by supervisor
        JButton resetDQButton = new JButton("Set DQ");
        resetDQButton.setName(name);
        SetDQButtonListener listener = new SetDQButtonListener(this);
        resetDQButton.addActionListener(listener);
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.add(resetDQButton);
        
        this.add(panel);
                
    } // SubSystemPanelSupervisor() -----------------------------------------
        

} // class DeskPanelSupervisor ==============================================




/**
 * Handler of supervisor set DQ button.
 */
class SetDQButtonListener implements ActionListener
{
    private static MyLogger logger = MyLogger.getLogger(SetDQButtonListener.class);
    
    private DeskPanelSupervisor adaptee = null;
    
    
    public SetDQButtonListener(DeskPanelSupervisor adaptee)
    {
    	this.adaptee = adaptee;
    	
    } // SetDQButtonListener() ----------------------------------------------
    
    
	public void actionPerformed(ActionEvent event)
	{
		JButton button = (JButton) event.getSource();
		String deskName = button.getName();

		boolean dqChecked = adaptee.dqCheckedStatusCheckBox.isSelected();
		Integer dqPeriod = (Integer) adaptee.dqCheckPeriodSpinner.getValue();
		
		Date now = null;
		if(dqChecked)
		{
			// the DQ was set as checked by supervisor now - set the time
			now = new Date(System.currentTimeMillis());
		}
		
		logger.debug("Action event on Set DQ button event for \"" +
				     deskName + "\", updating the system ...");
	
		// update in the system
		DeskData deskData = RunCom.getGUI().getDeskData(deskName);
		deskData.setDqCheckPeriodMinutes(dqPeriod.shortValue());
		deskData.setDqStatusChecked(dqChecked);
    	deskData.setDqLastChecked(now);
    	
		logger.debug("DQ values for \"" + deskName + "\" updated, going " +
				     "to distribute ...");
		RunCom.sendMessage(deskData); // distribute		
				
	} // actionPerformed() --------------------------------------------------
	
	
} // class SetDQButtonListener ==============================================