package runcom;

import java.util.ArrayList;
import java.util.Date;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;


import mylogger.MyLogger;

import runcom.activemqcommunication.TextMessage;



/**
 * Chat panel - sending to / receiving messages from other subsystem
 * RunCom instances.
 * 
 * @author Zdenek Maxa
 */

@SuppressWarnings("serial")
public class ChatPanel extends JPanel
{
	private static MyLogger logger = MyLogger.getLogger(ChatPanel.class);
	
	protected static final int DEFAULT_FONT_SIZE = 11;

	private JScrollPane scrollReceived = null;
	private MsgTableModel msgReceivedModel = null;
	private JTable msgReceivedTable = null;
	private JTextPane msgToSent = null;
    private JCheckBox ackStatusCheckBox = null;

    
    
    
    public ChatPanel()
    {
    	createChatPanel();
    	    	    	
    } // ChatPanel ----------------------------------------------------------

    
    
    public void increaseMessagesFontSize()
    {
    	CellRenderer cr = (CellRenderer) 
    		msgReceivedTable.getColumnModel().getColumn(0).getCellRenderer();
    	cr.increaseFontSize();
    	msgReceivedTable.updateUI();
    	setScrollBarToBottom();
    	    	
    } // increaseMessagesFontSize() -----------------------------------------
    
    
    
    public void decreaseMessagesFontSize()
    {
    	CellRenderer cr = (CellRenderer)
    		msgReceivedTable.getColumnModel().getColumn(0).getCellRenderer();
    	cr.decreaseFontSize();
    	msgReceivedTable.updateUI();
    	setScrollBarToBottom();
    	
    } // decreaseMessagesFontSize() -----------------------------------------

    
    
    /**
     * Method is supposed to scroll the received message pane so that the
     * latest message is visible (scrollbar at the botton of the pane).
     * 
     * TODO:
     * The method generally works fine, but the scroll bar doesn't always
     * go right to the bottom and sometimes last line or half a line
     * of the very last message is not completely visible.
     */
    public synchronized void setScrollBarToBottom()
    {
    	SwingUtilities.invokeLater(new Runnable()
    	{
    		public void run()
    		{

    			/* brute force solution and doesn't work either ...
    			try
    			{ 
    				Point p = new Point(0, 9999999);
    				scrollReceived.getViewport().setViewPosition(p);
    			}
    			catch(Exception ex)
    			{
    				String m = "ChatPanel could not set scroll bar to the bottom.";
    				logger.error(m);
    	            JOptionPane.showMessageDialog(null, m, "RunCom error",
    	            		                      JOptionPane.ERROR_MESSAGE);            
    			}    			
    			*/
    			
    			
    			// proper solution, but doens't always work properly ...
    			int rows = msgReceivedModel.getRowCount() - 1;
    			// System.out.println("rows = " + rows);
    			Rectangle rect = msgReceivedTable.getCellRect(rows, 0, true);
    			Rectangle bottom = null;
    			// System.out.println("x = " + rect.getX() + 
    			// 	                  " y = " + (rect.getY() + rect.getHeight() - 1));
    			bottom = new Rectangle((int) rect.getX(),
    					               (int) (rect.getY() + rect.getHeight() - 1),
    					               1, 1); 
    			msgReceivedTable.scrollRectToVisible(bottom);
    			
    		}
    	});
    	
    } // setScrollBarToBottom() ---------------------------------------------
    
    
    
    public synchronized void displayIncomingTextMessage(TextMessage tm)
    {
    	String msg = tm.getHeader() + "\n" + tm.getMessage();
    	boolean ackFlag = tm.getToAcknowledge();
    	
    	msgReceivedModel.addMessage(msg, ackFlag);
    	setScrollBarToBottom();
    	    	
    } // displayIncomingTextMessage() ---------------------------------------
  
    
    
    
    /**
     * Creates GUI components within the chat panel
     */
    private void createChatPanel()
    {
    	logger.debug("Creating the ChatPanel ...");
    	
        // border:  top, left, bottom, right border around panel
    	this.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

    	this.setLayout(new BorderLayout());
        
    	
        // received message panel
        scrollReceived = new JScrollPane();
        scrollReceived.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollReceived.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
        msgReceivedTable = new JTable();
        msgReceivedTable.setSelectionBackground(Color.darkGray);
        msgReceivedModel = new MsgTableModel();
        msgReceivedTable.setModel(msgReceivedModel);
        
        // message acknowledgement listener
        // provide this listener only if running as a subsystem (this could
        // also be Shift Leader subsystem plus supervisor mode, but not
        // plain supervisor mode with no desk assigned)
        if(RunCom.getActiveSubSystem() != null)
        {
        	TableMouseListener tml = new TableMouseListener();
        	msgReceivedTable.addMouseListener(tml);
        
        	// ensure that only one message can be selected at a time
            ListSelectionModel lsm = msgReceivedTable.getSelectionModel();
            // not using the solution via list selection listener ....
            // lsm.addListSelectionListener(new TableRowSelectionListener());
            lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            msgReceivedTable.setSelectionModel(lsm);
        }
        
        // msgReceivedTable.setIntercellSpacing(new java.awt.Dimension(0, 3));
        TableColumn msgColumn = msgReceivedTable.getColumnModel().getColumn(0);
        msgColumn.setCellRenderer(new CellRenderer());
        scrollReceived.getViewport().add(msgReceivedTable);
        this.add(scrollReceived, BorderLayout.CENTER);
       

        // bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        
        // write message label panel
        JPanel toSentPanel = new JPanel();
        toSentPanel.setLayout(new BorderLayout());
        JLabel toSentLabel = new JLabel("write message");
        toSentLabel.setFont(new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE));
        toSentPanel.add(toSentLabel, BorderLayout.WEST);
        bottomPanel.add(toSentPanel, BorderLayout.NORTH);
        
        
        // write message text panel panel
        JScrollPane scrollToSent = new JScrollPane();
        scrollToSent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollToSent.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        msgToSent = new JTextPane();
        msgToSent.setEditable(true);
        scrollToSent.getViewport().add(msgToSent);
        // set size trick
        msgToSent.setText("\n\n\n");
        msgToSent.setPreferredSize(msgToSent.getPreferredSize());
        bottomPanel.add(scrollToSent, BorderLayout.CENTER);
        
        
        // bottom panel with acknowledge checkbox and send button
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JLabel ackStatusLabel = new JLabel("shall recipients acknowledge?");
        ackStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        buttonPanel.add(ackStatusLabel);
        ackStatusCheckBox = new JCheckBox();
        ackStatusCheckBox.setEnabled(true);
        ackStatusCheckBox.setSelected(false);
        buttonPanel.add(ackStatusCheckBox);
        JButton sendButton = new JButton("send");
        sendButton.addActionListener(new SendButtonListener(this));
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        this.add(bottomPanel, BorderLayout.SOUTH);
        
        // wipe out the text panes
        msgToSent.setText("");
        
        // allow send button and write messages for observer mode
        /*
        if(RunCom.getActiveSubSystem() == null)
        {
            sendButton.setEnabled(false);
            msgToSent.setEditable(false);
            msgToSent.setEnabled(false);
        }
        */

    } // createChatPanel() --------------------------------------------------

    
    
    protected String getMsgToSent()
    {
    	return msgToSent.getText();
    	
    } // getMsgToSent() -----------------------------------------------------
    
    
    
    protected void setMsgToSent(String text)
    {
    	msgToSent.setText(text);
    	
    } // clearMsgToSent() ---------------------------------------------------
    
    
    
    protected boolean getAcknowledgeStatus()
    {
    	return ackStatusCheckBox.isSelected();
    	
    } // getAcknowledgeStatus() ---------------------------------------------
    
    
    
    protected void setAcknowledgeStatus(boolean state)
    {
    	ackStatusCheckBox.setSelected(state);
    	
    } // setAcknowledgeStatus() ---------------------------------------------
    
    
        
} // class ChatPanel ========================================================




class SendButtonListener implements ActionListener
{
	private static MyLogger logger = MyLogger.getLogger(SendButtonListener.class);
	public static final int MAX_ALLOWED_MSG_SIZE = 2048; // 2kB
	private ChatPanel panel = null;
	
	
	
	public SendButtonListener(ChatPanel adaptee)
	{
		this.panel = adaptee;
		
	} // SendButtonListener() -----------------------------------------------
	
	
	
	public void actionPerformed(ActionEvent e) 
	{
		String msg = panel.getMsgToSent().trim();
		
		if("".equals(msg))
		{
			logger.warn("Empty text, nothing is sent.");
			return;
		}
		
		// check size of this message, don't sent too large messages
		if(msg.length() > MAX_ALLOWED_MSG_SIZE)
		{
			String m = "Trying to send too large message: " + 
			           msg.length() + " > max. allowed size " +
			           MAX_ALLOWED_MSG_SIZE + ", not sent."; 
            JOptionPane.showMessageDialog(RunCom.getGUI(), m, "RunCom warning",
            		                      JOptionPane.WARNING_MESSAGE);
            logger.warn(m);
            panel.setMsgToSent("");
            panel.setAcknowledgeStatus(false);
            return;
		}
		
		// prepare the message, init time of the message, fill in
		// sender identification and sent it over ...
		// (message is actually displayed in the received part of
		// the chat panel when the RunCom instance itself received
		// its own message from network)
		
		Date now = new Date(System.currentTimeMillis());
		String from = RunCom.getActiveSubSystem();
		boolean ackFlag = panel.getAcknowledgeStatus(); 
		if(from == null)
		{
			// must be observer mode running (or as well supervisor without
			// a desk assigned), say just observer here)
			from = "observer (" + System.getProperty("user.name") + ")";
		}

		// don't display the message automatically, should be received over
		// network and then it will be displayed in the received messages
		TextMessage tm = new TextMessage();
		tm.setMessage(msg);
		tm.setFrom(from);
		tm.setToAcknowledge(ackFlag);
		tm.setTimeStamp(now);
		logger.debug("Going to send TextMessage: " + tm.toString());
		RunCom.sendMessage(tm);
		
		panel.setMsgToSent("");
		panel.setAcknowledgeStatus(false);
		
	} // actionPerformed() --------------------------------------------------
	
} // class SendButtonListener ===============================================



@SuppressWarnings("serial")
class MsgTableModel extends DefaultTableModel
{
	private static MyLogger logger = MyLogger.getLogger(MsgTableModel.class);
	public static final int MAX_ALLOWED_NUMBER_OF_MSG = 600;
	// rowData has following structure: Object[] { msg, new Boolean(ackFlag) };
	private ArrayList<Object[]> rowData = null;

	
	
	public MsgTableModel()
	{
		addColumn("received messages");
		rowData = new ArrayList<Object[]>();
		
	} // MsgTableModel() ----------------------------------------------------
	
	
	
	protected synchronized void addMessage(String msg, boolean ackFlag)
	{
		// check if there are not too many messages in the container
		
		if(rowData.size() > MAX_ALLOWED_NUMBER_OF_MSG)
		{
			int halfOfMessages = MAX_ALLOWED_NUMBER_OF_MSG / 2;
			String m = "Number of received text messages is larger than " +
			           "allowed (" + MAX_ALLOWED_NUMBER_OF_MSG + "), " +
			           "removing old " + halfOfMessages + " messages.";
			JOptionPane.showMessageDialog(RunCom.getGUI(), m, "RunCom warning",
					                      JOptionPane.WARNING_MESSAGE);
			logger.warn(m);
			
			for(int i = 0; i < halfOfMessages; i++)
			{
				rowData.remove(i);
			}
			logger.debug("Old text messages indices 0-" + halfOfMessages + " " +
					     "removed from the container (received messages table)");			
		}
				
		rowData.add(new Object[] { msg, new Boolean(ackFlag) });
		fireTableDataChanged();
		
	} // addMessage() -------------------------------------------------------
	
    
    
    public int getRowCount()
    {
    	if(rowData != null)
    	{
    		return rowData.size();
    	}
    	else
    	{
    		return 0;
    	}
    	
    } // getRowCount() ------------------------------------------------------

    

    public Object getValueAt(int row, int col)
    {
    	Object[] data = rowData.get(row);
    	Object r = data[col];
    	return r;
    	
    } // getValueAt() -------------------------------------------------------

    
   
    /**
     * Need this access when setting false value to acknowledgement flag of
     * the message displayed in the table after which it should no longer
     * be displayed in red font colour.
     * @param data
     * @param row
     */
    public synchronized void setValueAt(Object[] data, int row)
    {
    	rowData.set(row, data);
    	fireTableRowsUpdated(row, row);
    	
    } // setValueAt() -------------------------------------------------------
    
    
    
    public boolean isCellEditable(int row, int col)
    {
        return false;
        
    } // isCellEditable() ---------------------------------------------------
    
    
} // class MsgTableModel ====================================================



@SuppressWarnings("serial")
class CellRenderer extends JTextArea implements TableCellRenderer
{
    
	public CellRenderer()
	{
		setLineWrap(true); // do wrap lines
		setWrapStyleWord(true); // wrap lines at word boundaries
		// this.preferredRowHeight = preferredRowHeight;
		setFont(new Font("SansSerif", Font.PLAIN, ChatPanel.DEFAULT_FONT_SIZE));
		
	} // CellRenderer() -----------------------------------------------------
	
	
	
	protected void increaseFontSize()
	{
		int size = getFont().getSize();
		size++;
		Font font = new Font("SansSerif", Font.PLAIN, size);
		setFont(font);
		
	} // increaseFontSize() -------------------------------------------------

	
	
	protected void decreaseFontSize()
	{
		int size = getFont().getSize();
		size--;
		Font font = new Font("SansSerif", Font.PLAIN, size);
		setFont(font);
		
	} // decreaseFontSize() -------------------------------------------------
	

	
	/**
	 * table row height sizing issue, bug in JTable+TextArea returning
	 * correct preferred size, more on:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4446522
	 * seems to be a bug still in Java 1.6
	 * these two lines set the size implicitly and remove the problem so that
	 * rows needing to accommodate longer text get properly resized
	 * @param table
	 * @param row
	 * @param column
	 */
	private void adjustCellSize(JTable table, int row, int column)
	{
	
		this.setSize(table.getColumnModel().getColumn(column).getWidth(), 0);
		this.getUI().getRootView(this).setSize(this.getWidth(), 0f);		
		
	    // now getPreferredSize() should return correct values
	    int prefRowHeight = (int) this.getPreferredSize().getHeight();
	    int currRowHeight = table.getRowHeight(row);

        // adjust height row height to display longer instruction
	    if(currRowHeight < prefRowHeight)
	    {
	        table.setRowHeight(row, prefRowHeight);
	    }		  
		
	} // adjustCellSize() ---------------------------------------------------

	
	
	private void setColors(JTable table, boolean isSelected, int row,
			               boolean ackFlag)
	{

		table.setForeground(Color.black);
		table.setSelectionBackground(Color.black);
		table.setSelectionForeground(Color.white);
		
		if(isSelected)
		{
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
		} 
		else
		{
			// normal, non-selected cell
			setForeground(table.getForeground());
			// setBackground(table.getBackground());
			if(row % 2 == 0)
			{
				setBackground(new Color(241, 246, 246));
			}
			else
			{
				setBackground(new Color(215, 200, 200));
			}
		}
		
		if(ackFlag)
		{
			setForeground(Color.red);
		}
		
	} // setColors() ---------------------------------------------------------
	
	
   
	public Component getTableCellRendererComponent(JTable table, Object value,
	 	               		                       boolean isSelected,
		               		                       boolean hasFocus,
		               		                       int row, int column)
	{
	
		this.setText(value.toString());
		
		// 1st column is a hidden column with acknowledge flag of a message
		// structure of rowData (behind the table model) is
		// rowData.add(new Object[] { msg, new Boolean(ackFlag) });
		// should this structure change, following line will fail
		Boolean ackFlag = (Boolean) table.getModel().getValueAt(row, 1);

		adjustCellSize(table, row, column);
		
		// set tool-tip (only if the message is meant to be acknowledged)
		if(ackFlag.booleanValue())
		{
			this.setToolTipText("right click to acknowledge");
		}
		else
		{
			this.setToolTipText(null);
		}
	 
	    setColors(table, isSelected, row, ackFlag.booleanValue());
		
	    return this;
	    
	} // getTableCellRendererComponent() ------------------------------------
	
	   
} // class CellRenderer ====================================================



/**
 * Mouse event listener at JTable. Checks table row selection and whether
 * right click was performed on a table cell. If a selected message is
 * supposed to be acknowledged by recipients, the context pop-up menu
 * is shown offering to acknowledge. The process of acknowledgement is
 * actually disconnected from a particular message, since it only
 * decreases the counter of non-acknowledge messages for a desk and
 * distributes this new desk data information to the others.
 * @author Zdenek Maxa
 *
 */
class TableMouseListener extends MouseAdapter
{
	private static MyLogger logger =
		MyLogger.getLogger(TableMouseListener.class);
	

	
	private synchronized void acknowledgeMessage(MsgTableModel model, int row,
			                                     String msg)
	{
		String deskName = RunCom.getActiveSubSystem();
		if(deskName != null)
		{
			DeskData deskData = RunCom.getGUI().getDeskData(deskName);
			int ackCounter = deskData.getNotAckMessagesCounter();
			ackCounter--;
			
			deskData.setNotAckMessagesCounter(ackCounter);
			RunCom.sendMessage(deskData);
			logger.info("\"" + deskName + "\" not acknowledged message counter " +
					    "set to " + ackCounter + ", update was sent (table row: " +
					    row + "  message: \"" + msg + "\")");
						
			// reset ackFlag column of the message, the message should no longer
			// be red in the message table
			model.setValueAt(new Object[] { msg, new Boolean(false) }, row);
		}
		else
		{
			logger.error("Tried to acknowledge a message while " +
					     "'currentSubSystem' is null? This should never " +
					     "happen! Nothing is changed.");
			return;
		}
		
	} // acknowledgeMessage() -----------------------------------------------
	
	
	
	private JPopupMenu createContextMenu(final MsgTableModel model, final int row,
			                             final String msg)
	{
		JPopupMenu contextMenu = new JPopupMenu();
		
		JMenuItem ackMenuItem = new JMenuItem("acknowledge this message");
		ackMenuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				acknowledgeMessage(model, row, msg);
			}
		});
		
		contextMenu.add(ackMenuItem);
		
		return contextMenu;
		
	} // createContextMenu() ------------------------------------------------

	

	/**
	 * Getting row index where a pop-up menu event occurred. It relies on the
	 * table mode as per MsgTableModel where there are two columns - one visible
	 * with the actual message and the other invisible - Boolean ackFlag (whether
	 * or not is the message supposed to be acknowledged) thus columnIndex is
	 * provided by constants: 0, 1
	 * This would fail should the MsgTableModel class change.
	 */
	private void maybeShowAcknowledgementPopup(MouseEvent me) 
	{
		if(me.isPopupTrigger())
		{
			JTable adapteeTable = (JTable) me.getSource(); 
			
			Point p = new Point(me.getX(), me.getY());

			// don't need column information, just for reference
			// int col = adapteeTable.columnAtPoint(p);
			// translate table index to model index
			// String colName = adapteeTable.getColumnName(col);
			// int mcol = adapteeTable.getColumn(colName).getModelIndex();
			
			int row = adapteeTable.rowAtPoint(p);

			MsgTableModel model = (MsgTableModel) adapteeTable.getModel();
			
			// column indices provided by constants
			// 1st column is a hidden column with acknowledge flag of a message
			// structure of rowData (behind the table model) is
			// rowData.add(new Object[] { msg, new Boolean(ackFlag) });
			// should this structure change, following lines will fail
			String msg = (String) model.getValueAt(row, 0);
			Boolean ackFlag = (Boolean) model.getValueAt(row, 1);
			
			// is this message supposed to be acknowledged?
			if(ackFlag.booleanValue())
			{
				JPopupMenu contextMenu = createContextMenu(model, row, msg);

				// don't need this check ...
				// if(contextMenu != null && contextMenu.getComponentCount() > 0)

				logger.debug("Message ack pop-up menu displayed for table row: " +
						     row + "  message: \"" + msg + "\"");

				contextMenu.show(adapteeTable, p.x, p.y);
			}
		}
		
	} // maybeShowAcknowledgementPopup() ------------------------------------

	

	public void mousePressed(MouseEvent me) 
	{
		maybeShowAcknowledgementPopup(me);
		
	} // mousePressed() -----------------------------------------------------

	
	
	/** mousePressed (above) seems to be enough to handle
	public void mouseClicked(MouseEvent me)
	{
		System.out.println("clicked");
		maybeShowAcknowledgementPopup(me);
		
	} // mouseClicked() -----------------------------------------------------
	*/
	
	
} // class TableMouseListener ===============================================