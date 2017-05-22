package runcom;

import java.util.Date;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.BadLocationException;

import mylogger.MyLogger;

import runcom.activemqcommunication.WhiteBoardMessage;


/**
 * WhiteBoardWindow class - maintains white board window - displays
 * message (like day plan) and is a replacement to knotes (KDE) used
 * currently (2008-10-10).
 * Only the Shift Leader (supervisor mode RunCom instance) is allowed
 * to edit the white board message and has 'update' button after
 * which the message is distributed to other RunCom instances.
 * Supervisor window itself gets however updated (i.e. the timestamp
 * gets updated) when its own message is received from network (proof
 * that everybody else got the message).
 * 2009-07-06 autoresizing of the window removed. 
 * @author Zdenek Maxa
 */
@SuppressWarnings("serial")
final public class WhiteBoardWindow extends JFrame
{
    private static MyLogger logger =
        MyLogger.getLogger(WhiteBoardWindow.class);
    
    // for too large white board messages
    private static final int MAX_WHITE_BOARD_WINDOW_WIDTH = 700;
    private static final int MAX_WHITE_BOARD_WINDOW_HEIGHT = 950;
    
    // WhiteBoardWindow is a singleton
    private static WhiteBoardWindow instance = null;
    
    private static JTextPane msgTextPane = null;
    private static JTextPane dateTimeTextPane = null;
    
    // styles
    private static final String TIMESTAMP_STYLE = "TIMESTAMP_STYLE";
    private static final String WHITE_BOARD_STYLE = "WHITE_BOARD_STYLE";
    private static Style timeStampStyle = null;
    private static Style wbStyle = null; // white board, the content itself
    
    
    
    
    
    private WhiteBoardWindow()
    {
        logger.debug("Creating instance of WhiteBoardWindow ...");
        createGUI();
        
    } // WhiteBoardWindow() -------------------------------------------------
    
    
    
    /**
     * Handler method when 'update' button is pressed. 'update' button
     * is available only for the supervisor mode
     */
    private void updateButtonPressed()
    {
        logger.debug("Going distribute updated WhiteBoardMessage ...");
        
        Date now = new Date(System.currentTimeMillis());      
        String msg = msgTextPane.getText();
        
        WhiteBoardMessage wbm = new WhiteBoardMessage();
        wbm.setMessage(msg);
        wbm.setTimeStamp(now);
        logger.debug("Going to send WhiteBoardMessage: " + wbm.toString());
        RunCom.sendMessage(wbm); // send to the others
        
    } // updateButtonPressed()


    
    /**
     * Method should not be called directly, only via closeWindow()
     */
    private void myCloseWindow()
    {
        this.setVisible(false);
        this.dispose();
        
    } // myCloseWindow() ----------------------------------------------------
    
    
    
    protected void increaseFont()
    {
        int size = StyleConstants.getFontSize(timeStampStyle);
        size += 2;
        logger.debug("Setting for size for timestamp: " + size);
        StyleConstants.setFontSize(timeStampStyle, size);
       
        size = StyleConstants.getFontSize(wbStyle);
        size += 2;
        logger.debug("Setting for size for white board content: " + size);
        StyleConstants.setFontSize(wbStyle, size);

        // this is not actually update, just need to draw TextPane contents
        processWhiteBoardUpdate(msgTextPane.getText(), dateTimeTextPane.getText());
        
    } // increaseFont() -----------------------------------------------------
    
    
    
    protected void decreaseFont()
    {
        int size = StyleConstants.getFontSize(timeStampStyle);
        size -= 2; 
        logger.debug("Setting for size for timestamp: " + size);
        StyleConstants.setFontSize(timeStampStyle, size);
       
        size = StyleConstants.getFontSize(wbStyle);
        size -= 2;
        logger.debug("Setting for size for white board content: " + size);
        StyleConstants.setFontSize(wbStyle, size);

        // this is not actually update, just need to draw TextPane contents
        processWhiteBoardUpdate(msgTextPane.getText(), dateTimeTextPane.getText());
                
    } // decreaseFont() -----------------------------------------------------
    
    
    
    protected void closeWindow()
    {
        logger.debug("Going to close WhiteBoardWindow ...");
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                myCloseWindow();                
            }
        });
        
    } // closeWindow() ------------------------------------------------------

    
    
    private void defineStyles()
    {
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style defaultStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
        
        timeStampStyle = dateTimeTextPane.addStyle(TIMESTAMP_STYLE, defaultStyle);
        wbStyle = msgTextPane.addStyle(WHITE_BOARD_STYLE, defaultStyle);

        StyleConstants.setFontFamily(timeStampStyle, "SansSerif");
        StyleConstants.setFontSize(timeStampStyle, 10);
        StyleConstants.setBold(timeStampStyle, true);
        StyleConstants.setForeground(timeStampStyle, Color.black);

        StyleConstants.setFontFamily(wbStyle, "Courier");
        StyleConstants.setFontSize(wbStyle, 28);
        StyleConstants.setBold(wbStyle, true);
        StyleConstants.setForeground(wbStyle, Color.black);
        
        // so that style (font sizes) are set before first editing
        processWhiteBoardUpdate(" ", " ");

    } // defineStyles() -----------------------------------------------------
    
    
    
    /**
     * Creates WhiteBoardWindow GUI, but don't show it for now.
     */
    private void createGUI()
    {
        this.setLayout(new BorderLayout());
         
        // this.setUndecorated(true); // is not possible to resize

        this.setJMenuBar(new WBWMenuBar(this));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        
        JScrollPane dateTimeScroll = new JScrollPane();
        JScrollPane msgScroll = new JScrollPane();
        dateTimeTextPane = new JTextPane();
        msgTextPane = new JTextPane();
        dateTimeScroll.getViewport().add(dateTimeTextPane);
        msgScroll.getViewport().add(msgTextPane);
        
        mainPanel.add(dateTimeScroll, BorderLayout.NORTH);
        mainPanel.add(msgScroll, BorderLayout.CENTER);
        
        dateTimeTextPane.setEditable(false);
        msgTextPane.setEditable(false);
        
        if(RunCom.isSupervisor())
        {
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            JButton updateButton = new JButton("update");
            String m = "send updated white board message to other RunCom instances";
            updateButton.setToolTipText(m);
            updateButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    updateButtonPressed();
                }
            });
            buttonPanel.add(updateButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
            // only supervisor can edit and has update button
            msgTextPane.setEditable(true);
        }
                
        this.add(mainPanel, BorderLayout.CENTER);
        
        // this.setAlwaysOnTop(true);
        
        // define styles, must be done after TextPanes are defined
        defineStyles();
        
        // set background color
        mainPanel.setBackground(Color.yellow.darker());
        dateTimeTextPane.setBackground(Color.yellow);
        msgTextPane.setBackground(Color.yellow);
        
        this.setResizable(true);
        
        // should always live in the left-hand upper corner
        this.setLocation(0, 0);
        this.setSize(MAX_WHITE_BOARD_WINDOW_WIDTH, MAX_WHITE_BOARD_WINDOW_HEIGHT);

    } // createGUI() --------------------------------------------------------

    
    
    public static WhiteBoardWindow getInstance()
    {
        if(instance == null)
        {
            instance = new WhiteBoardWindow();
        }
        
        return instance;
        
    } // getInstance() ------------------------------------------------------
    
    
    
    /**
     * Method should never be called directly, always via
     * processWhiteBoardUpdate(String msg, String timeStamp) which does
     * event dispatch thread wrapper 
     * @param msg
     * @param timeStamp
     */
    private void myWhiteBoardUpdate(String msg, String timeStamp)
    {
        // erase previous content
        dateTimeTextPane.setText("");
        msgTextPane.setText("");
        
        Document dateTimeDoc = dateTimeTextPane.getDocument();
        Document msgDoc = msgTextPane.getDocument();
        
        try
        {
            dateTimeDoc.insertString(0, timeStamp, timeStampStyle);
            msgDoc.insertString(0, msg, wbStyle);
        }
        catch(BadLocationException ble)
        {
            String m = "Can't update WhiteBoardWindow content - " +
                       "BadLocationException ...";
            logger.error(m);
            logger.debug(m, ble);
        }
        
        // autoresizing of the whiteboard window:
        // don't do any automatic resize - doesn't always work as user
        // desires, let it up to the user to adjust size of the windows
        
        // check maximal width of the whiteboard window and adjust
        /*
        int width = this.getPreferredSize().width;
        if(width > MAX_WHITE_BOARD_WINDOW_WIDTH)
        {
            Dimension d = new Dimension(MAX_WHITE_BOARD_WINDOW_WIDTH,
                                        MAX_WHITE_BOARD_WINDOW_HEIGHT);
            this.setPreferredSize(d);
        }
        // should auto-resize when update is received (to preferred size)
        this.pack(); 
        */
        
    } // myWhiteBoardUpdate() -----------------------------------------------
 
    
    
    /**
     * This method is called when an update (WhiteBoardMessage) is received
     * @param msg
     * @param timeStamp
     */
    public void processWhiteBoardUpdate(final String msg, final String timeStamp)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                myWhiteBoardUpdate(msg, timeStamp);
            }
        });
                
    } // processWhiteBoardUpdate() ------------------------------------------
    
    
    
    /**
     * This method should never be called directly, but only
     * though displayWhiteBoard() method, i.e. via Event-dispatching thread
     */
    private void myShowWindow()
    {
        this.setTitle("RunCom White Board");
        this.setVisible(true);
        
    } // myShowWindow() -----------------------------------------------------
    
    
    
    protected void displayWhiteBoard()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                // myCloseWindow(); was here b/c of autoresizing ...
                myShowWindow();
            }
        });
        
    } // show() -------------------------------------------------------------
    
   
    
} // class WhiteBoardWindow =================================================



@SuppressWarnings("serial")
class WBWMenuBar extends JMenuBar implements ActionListener
{
    private static MyLogger logger =
        MyLogger.getLogger(WBWMenuBar.class);
    
    private WhiteBoardWindow wbw = null; // adaptee
    
    
    public WBWMenuBar(WhiteBoardWindow wbw)
    {
        this.wbw = wbw;
        
        JMenu menuClose = new JMenu("close");
        JMenuItem menuItem = new JMenuItem("close");
        menuItem.setActionCommand("close_wbw");
        menuItem.addActionListener(this);
        menuClose.add(menuItem);
        
        JMenu menuPreferences = new JMenu("preferences");
        menuItem = new JMenuItem("font +");
        menuItem.setActionCommand("increase_font");
        menuItem.addActionListener(this);
        menuPreferences.add(menuItem);
        
        menuItem = new JMenuItem("font -");
        menuItem.setActionCommand("decrease_font");
        menuItem.addActionListener(this);
        menuPreferences.add(menuItem);
        
        this.add(menuClose);
        this.add(menuPreferences);

    } // WBWMenuBar() -------------------------------------------------------
    
    
    
    public void actionPerformed(ActionEvent ae)
    {
        String choice = ae.getActionCommand();
        
        logger.debug("WhiteBoardWindow menu item \"" + choice + "\" chosen.");
        
        if("close_wbw".equals(choice))
        {
            wbw.closeWindow();
        }
        else if("increase_font".equals(choice))
        {
            wbw.increaseFont();
        }
        else if("decrease_font".equals(choice))
        {
            wbw.decreaseFont();
        }
        
    } // actionPerformed() --------------------------------------------------
    
} // class WBWMenuBar =======================================================