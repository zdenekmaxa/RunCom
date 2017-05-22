package runcom;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.Timer;

import runcom.deskconfig.DeskConfiguration;

import mylogger.MyLogger;



/**
 * This automatic watch-dog class periodically checks (timer) whether period
 * within which a subsystem must re-check their data quality status elapsed
 * (DQ last checked time + period must be < than now). If elapsed, the
 * DQ checked status is automatically turned off (and new values distributed). 
 * @author Zdenek Maxa
 *
 */
public class DQStatusWatcher implements ActionListener
{
	private static MyLogger logger = MyLogger.getLogger(DQStatusWatcher.class);
	 
	private String deskToWatch = null;
	private Timer timer = null;
	
	// flag which prevents many DQ checklist appearing if the previous
	// ones haven't been fulfilled or closed without fulfilling
	// when notification dialog gets clicked away, this flag gets set to false,
	// so in fact there might be more DQ checklists present if only the notification
	// gets closed, but that is fine - if notification gets closed, something
	// should be done about the checklist as well ; this is to solve long unmanned
	// periods when if system left in ready, many checklists could pop up
	private static boolean dqCheckListAlreadyShown = false;
	
	
	
	/**
	 * @param deskToWatch
	 * @param dqPeriod - number of minutes, period after which DQ should be re-checked
	 */
	public DQStatusWatcher(String deskToWatch, int dqPeriod)
	{
		this.deskToWatch = deskToWatch;
		this.timer = new Timer(dqPeriod * 60 * 1000, this); // delay [m] - > [ms]
		
	} // DQStatusWatcher() --------------------------------------------------

	
	
	public void fireDQCheck()
	{
		logger.debug("Force DQ check ...");
		
		DeskData data = RunCom.getGUI().getDeskData(deskToWatch);
		boolean currentDQStatus = data.getDqStatusChecked();
		if(currentDQStatus)
		{
			logger.debug("System already has DQ checked, quit.");
			return;
		}
		
		// display DQ checklist
		showMessageAndPopUpCheckList(data);
		
	} // fireDQCheck() ------------------------------------------------------
	
	
	
	public void actionPerformed(ActionEvent ae)
	{
		logger.debug("Watcher DQ checked status action fired, desk: \"" +
				     deskToWatch + "\", checking DQ status ...");
	    
		DeskData data = RunCom.getGUI().getDeskData(deskToWatch);

		// if the system is not in ready, then we care if the DQ is not
		// checked, otherwise not (if not ready)
		String deskStatus = data.getStatus();
		boolean currentDQStatus = data.getDqStatusChecked();
		if(! DeskData.READY.equals(deskStatus) && ! currentDQStatus)
		{
			logger.debug("System is not " + DeskData.READY + " and " +
					     "DQ checked status is false (unchecked), quit now.");
			return;
		}
		
		Date lastChecked = data.getDqLastChecked();
        if(lastChecked == null)
        {
            // when system is ready (then don't care whether DQ was
            // checked or not), force DQ to be checked since if lastChecked
            // is null, it has never been checked
            showMessageAndPopUpCheckList(data);
            // must return here, otherwise will fail when referencing lastChecked
            return;
        }

        
        // now finally check whether the period during which the DQ should
        // have been re-checked elapsed
        
        // if firing only once after the whole period, it's enough to 
        // set the status to false, distribute the update DeskData and
        // invoke DQ checklist without any further checking
        // keep the commented out code for now
        /*
		long periodMS = data.getDQCheckPeriodMinutes() * 60 * 1000; // [ms]
		long lastCheckedMS = lastChecked.getTime();
		long currentMS = System.currentTimeMillis();
		
		logger.debug("Watcher DQ on \"" + deskToWatch + "\": DQ last checked: " +
				     DeskPanel.DATE_FORMAT.format(lastChecked) +
				     " DQ check period min: " + data.getDQCheckPeriodMinutes());
		
		if((lastCheckedMS + periodMS) > currentMS)
		{
			logger.debug("Watcher DQ still valid, period not elepsed yet.");
			return;
		}
		else
		{
		    showMessageAndPopUpCheckList(data);
			return;
		}
		*/

		logger.debug("Watcher DQ on \"" + deskToWatch + "\": DQ last checked: " +
			     DeskPanel.DATE_FORMAT.format(lastChecked) +
			     " DQ check period min: " + data.getDqCheckPeriodMinutes());
		showMessageAndPopUpCheckList(data);
		
	} // actionPerformed() --------------------------------------------------

	
	
	private void showMessageAndPopUpCheckList(DeskData data)
	{
        String m1 = "Data Quality checklist should be completed now.";
        String m2 = "Setting DQ checked status to " +
                    DeskData.DQ_STATUS_UNCHECKED + " ...";
            
        logger.warn("Watcher DQ - " + m1 + " " + m2);
        
        // if the system's DQ check is false already, no need to re-set it
        // it to false and re-distribute a DeskMessage with identical
        // content
        boolean dqStatus = data.getDqStatusChecked();
        if(dqStatus)
        {
            data.setDqStatusChecked(false);
            RunCom.sendMessage(data); // distribute
        }
        else
        {
            logger.debug("Desk \"" + data.getDeskName() + "\" DQ checked " +
                         "status is already false, no need to redistribute " +
                         "the DeskData message.");
        }
        
        if(dqCheckListAlreadyShown)
        {
            logger.warn("DQ Checklist appears to be already shown, no need to " +
                        "pop up another one, CheckList not called.");
            return;
        }
        
        // display notification message that DQ should be re-checked
        String title = "DQ watcher";
        String m = m1 + "\n" + m2;
        NotificationDialog notification = new NotificationDialog(this);
        notification.notify(RunCom.getGUI(), m, title);
        
        // pop up automatically DQ check checklist
        String deskName = data.getDeskName();
        DeskConfiguration dc = RunCom.getDeskConfiguration(deskName);
        String dqCheckListFileName = dc.getDqCheckListFileName();
        logger.debug("Calling CheckList to display: \"" +
                     dqCheckListFileName + "\" ...");
        CheckListCaller.call(dqCheckListFileName, deskName);
        
        // flag to indicate that DQ checklist has been shown
        dqCheckListAlreadyShown = true;
           
       // upon completed checklist, CheckList application will call back
       // RunCom to change status on an appropriate desk
	    
	} // showMessageAndPopUpCheckList() -------------------------------------
	
	
	
	synchronized protected void resetDqCheckListAlreadyShown()
	{
	    dqCheckListAlreadyShown = false;
	    logger.debug("DQStatusWatcher: flag dqCheckListAlreadyShown was set " +
	                 " to false.");
	    
	} // resetDqCheckListAlreadyShown() -------------------------------------
	
	
	
	public void start()
	{
		timer.start();
		logger.info("Starting DQ checked status watcher timer, delay " +
			     timer.getDelay() / 1000 / 60 + "min for desk: \"" +
			     deskToWatch + "\" ...");
		
	} // start() ------------------------------------------------------------
	

	
	public void stop()
	{
		timer.stop();
		timer = null;
		logger.info("DQ checked status watcher timer for desk \"" +
			        deskToWatch + "\" stopped.");
		
	} // stop() -------------------------------------------------------------
	
	
	
	synchronized public void setDelay(int dqPeriod)
	{
		this.timer.stop();
		this.timer = null;
		this.timer = new Timer(dqPeriod * 60 * 1000, this); // delay [m] - > [ms]
		this.timer.start();
		logger.info("Setting new delay for DQ status watcher " +
			     timer.getDelay() / 1000 / 60 + "min for desk: \"" +
			     deskToWatch + "\" ...");
		
	} // setDelay() ---------------------------------------------------------

	
} // class DQStatusWatcher ==================================================