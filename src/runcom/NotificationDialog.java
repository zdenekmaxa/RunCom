package runcom;

import java.util.Iterator;
import java.util.ArrayList;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.JTextPane;
import javax.swing.BorderFactory;
import javax.swing.Timer;

import mylogger.MyLogger;



/**
 * Notification dialog window. Should popup on all virtual desktops (screens)
 * of a system (not done yet - doesn't work reliably on an ACR machine).
 * Raises RunCom application to front and stay on top until closed.
 * Currently is only used incoming text message notification, but might
 * replace currently used plain JOptionPane on some other occasions.
 * @author Zdenek Maxa
 *
 */
public class NotificationDialog
{
	private static MyLogger logger = MyLogger.getLogger(NotificationDialog.class);
	
	// container holding dialogs for all virtual screens
	private ArrayList<DialogFrame> dc = null;
	
	// timer controls blinking of the notification dialog
	private Timer timer = null;
	
	private static int DELAY = 600; // [ms]
	
	// just for call back purpose to prevent many checklist appearing
	// placing this class here is not ideal design, though
	private DQStatusWatcher dqStatusWatcher = null;
	
	

	
	public NotificationDialog(DQStatusWatcher dqStatusWatcher)
	{
	    this.dqStatusWatcher = dqStatusWatcher;
	    
	} // NotificationDialog() -----------------------------------------------

	

    public NotificationDialog()
    {        
        
    } // NotificationDialog() -----------------------------------------------
	
    
    
    /**
     * Call back actions. Method called from DialogFrame when closing.
     */
    protected void callBack()
    {
        if(dqStatusWatcher != null)
        {
            dqStatusWatcher.resetDqCheckListAlreadyShown();
        }
        
    } // callBack() ---------------------------------------------------------
    
	
	
	/**
	 * Method called from outside, the only visible method here.
	 * @param gui
	 * @param msg
	 * @param title
	 */
	public void notify(final RunComGUI gui, final String msg,
	                          final String title)
	{
		final String focusWarningMsg =
		       "\n(press alt-tab to select another\n" +
		       "application, e.g. crss (login window))";
	    
		gui.setVisible(true);
		gui.toFront(); // brings the main window to front
						
		createTimer();
		
		dc = new ArrayList<DialogFrame>();
		
		// now should get information about virtual screens and loop over ...
		// however for ACR machine (DCS) this gives 2 screens and 40
		// configurations ...
		/*
		GraphicsEnvironment environment =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = environment.getScreenDevices();
		for(int i = 0; i < screens.length; i++)
		{
			GraphicsConfiguration[] configs = screens[i].getConfigurations();
			for(int j = 0; j < configs.length; j++)
			{
				JFrame d = createDialog(screens[i].getDefaultConfiguration(), msg, title);
				Rectangle gcBounds = configs[j].getBounds();
				int xoff = gcBounds.x;
				int yoff = gcBounds.y;
				if(screens.length == 1)
				{
					d.setLocationRelativeTo(gui); // not for multihead ...
				}
				else
				{
					d.setLocation((j*50)+xoff, (j*60)+yoff);
					// d.setLocation((j*xoff)/2, (j*+yoff)/2);					
				}
				d.pack();
				d.setVisible(true);
				dc.add(d);
				logger.debug("Notification dialog screen " + i + 
						    " configuration: " + j + " xoffset: " + xoff +
						    " yoffset: " + yoff);
			}
		}
		*/

		// good for now:
		// show up just one window, JDialog so that it is modal and blocks
		// the main thread ... using Runnable, this particular Runnable will
		// be blocked ...
		Runnable dialogThread = new Runnable()
		{
		    public void run()
		    {
		        // the dialog has reference to a container holding
		        // all the dialogs (for disposing), perhaps the listener
		        // should be a normal class ...
		        DialogFrame df = new DialogFrame(dc, timer, NotificationDialog.this);
		        df.createDialog(gui, msg + focusWarningMsg, title);
        		dc.add(df);
        		df.pack();
        		df.setModal(true);
        		// must be before the dialog is setVisible, then this thread is blocked
        		timer.start(); 
        		logger.debug("Dialog is blocking from here now once it " +"" +
                 			 "becomes visible.");
        		
                // (2009-07-10 - not really, still having problems with
                // log in dialog - just set to visible and not to front
                // when creating the dialog) - removed to front
        		// df.toFront();
        		// actually impossible to reproduce these reported issues
        		// as alt-tab works, but leave .toFront() removed for now
        		df.setVisible(true);
		    }
		};
		SwingUtilities.invokeLater(dialogThread);
		
		logger.debug("Dialog(s) being created in a dedicated thread ...");
				
	} // notify() ----------------------------------------------------------
	
	

	/**
	 * Timer periodically changes colour of the dialog - blinking.
	 */
	private void createTimer()
	{
		timer = new Timer(DELAY, new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				// loop over all dialogs that are stored in the container
				for(Iterator<DialogFrame> iter = dc.iterator(); iter.hasNext();)
				{
					DialogFrame d = iter.next();
					d.toggeColor();
					// this way, it is possible to change focus
					// (2009-07-10 - not really, still having problems with
					// log in dialog - just set to visible and not to front
					// when creating the dialog, see below)
					//d.setVisible(true);
					// d.toFront();
				}
				//logger.debug("Blinking timer fired, all notification dialogs " +
				//		     "should have changed background colour.");
			}
		});
		
	} // createTimer() ------------------------------------------------------
		

} // class NotificationDialog ===============================================



@SuppressWarnings("serial")
class DialogFrame extends JDialog
{

    private static MyLogger logger = MyLogger.getLogger(DialogFrame.class);
    
    // colours definition
    private static final Color BACKGROUND_1 = Color.red;
    private static final Color BACKGROUND_2 = Color.yellow;
    private static final Color FOREGROUND = Color.black;
        
    // should be instance variable
    private JPanel panel = null; // will be access for colour change
    
    // dialog keeps reference to the whole container of dialogs - so that the
    // button action listener can dispose all the dialogs
    private ArrayList<DialogFrame> dc = null;
    
    // dialog also holds reference to the timer so that it can be stopped and 
    // null-ed when closing one of the dialogs
    private Timer timer = null;
    
    // because of the callBack() method call, keep reference to the creating
    // NotificationDialog
    private NotificationDialog notificationDialog = null;

    
    
    public DialogFrame(ArrayList<DialogFrame> dc, Timer timer, NotificationDialog nd)
    {
        super();
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        this.dc = dc;
        this.timer = timer;
        this.notificationDialog = nd;

    } // DialogFrame() ------------------------------------------------------
    
    
    
    /**
     * Creates the dialog GUI.
     */
    public void createDialog(final RunComGUI gui, String msg, String title)
    {
        this.setTitle(title);
        
        panel = new JPanel();
        // border:  top, left, bottom, right border around panel
        panel.setBorder(BorderFactory.createEmptyBorder(30, 15, 30, 15));       
        panel.setLayout(new BorderLayout());
        
        JTextPane msgArea = new JTextPane();
        msgArea.setBackground(BACKGROUND_1);
        msgArea.setForeground(FOREGROUND);
        msgArea.setText(msg);
        msgArea.setEditable(false);
        // border:  top, left, bottom, right border around panel
        msgArea.setBorder(BorderFactory.createEmptyBorder(5, 15, 20, 15));
        panel.add(msgArea, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_1);
        buttonPanel.setLayout(new FlowLayout());
        JButton button = new JButton("ok");
        // button listener - disposing everything
        button.addActionListener(new ActionListener()
        {
            // closes all displayed dialogs and cleans up
            public void actionPerformed(ActionEvent ae)
            {
                logger.debug("Notification dialog OK button pressed.");
                if(timer != null)
                {   
                    timer.stop();
                }
                timer = null;
                logger.debug("Notification dialog timer stopped and disposed.");

                
                for(Iterator<DialogFrame> iter = dc.iterator(); iter.hasNext();)
                {
                    DialogFrame df = iter.next();
                    df.setVisible(false);
                    df.dispose();
                }
                
                logger.debug("All notification dialogs should now be disposed.");
                
                dc = null;
                
                // callback actions
                DialogFrame.this.notificationDialog.callBack();
            }
        });
        
        buttonPanel.add(button, BorderLayout.SOUTH);
        panel.add(buttonPanel);
        
        panel.setBackground(BACKGROUND_1);
        panel.setForeground(FOREGROUND);        
        
        this.getContentPane().add(panel);

        Point p = gui.getLocation();
        this.setLocation(p.x + gui.getWidth() / 2,
                         p.y + gui.getHeight() / 2);
        
        logger.debug("Dialog created.");
        
    } // createDialog() ----------------------------------------------------
    
    
    
    public void toggeColor()
    {
        Color c = panel.getBackground();
        Color n = (c == BACKGROUND_1) ? BACKGROUND_2 : BACKGROUND_1;                
        panel.setBackground(n);
        
    } // toggeColor() -------------------------------------------------------
    
    
} // class DialogFrame ======================================================