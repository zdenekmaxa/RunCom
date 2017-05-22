package runcom.activemqcommunication.broker;


import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import runcom.activemqcommunication.DeskDataMessage;

import runcom.deskconfig.DeskConfiguration;
import runcom.deskconfig.DeskConfigurationProcessor;

import mylogger.MyLogger;



/**
 * Class writes overview with statuses of all subsystems.
 * HTML file (which is included on some P1 overall status pages)
 * gets update everytime the Broker Receiver receives DeskDataMessage with
 * status update for a desk.
 * 
 * @author Zdenek Maxa
 *
 */
public final class HTMLOverviewWriter
{
    private static MyLogger logger = MyLogger.getLogger(HTMLOverviewWriter.class);
    
    // text file with definition of available subsystem names (desks)
    // XML file contains definition of all relevant initialisation
    // data associated with a desk, e.g. deskName, files names of various
    // XML checklists (displayed by the CheckList application)
    // see comment in the file
    // here only deskName is necessary
    private static final String DESKS_CONFIGURATIONS =
        "desks_configurations.xml";
    
    // ArrayList with desks configurations (start up config data) as defined
    // in DESKS_DEFINITION_FILE
    private static ArrayList<DeskConfiguration> deskConfigurations = null;

    
    private static String htmlOutputFile = null;
    
    
    // icon file names as defined in OverallPanel-DeskStatusIndicator
    private static final String UNASSIGNED_FILE = "grey_circle_icon.gif";
    private static final String ASSIGNED_FILE = "blue_square_icon.gif";
    private static final String ERROR_FILE = "red_cross_icon.gif";
    private static final String READY_FILE = "green_tick_icon.gif";
    
    // possible states of desk as defined in DeskData
    private static final String UNASSIGNED = "signed off";
    private static final String ASSIGNED = "signed in";
    private static final String ERROR = "error";
    private static final String READY = "ready";
    
    // key - status ; value - path to the status icon file
    private static HashMap<String, String> icons = new HashMap<String, String>(4);
    
    
    
    public HTMLOverviewWriter(String outputHtmlFile) throws Exception
    {
        logger.debug("Creating instance of HTMLOverviewWriter ...");
        
        htmlOutputFile = outputHtmlFile;
        
        logger.info("HTML desks status overview file: " + htmlOutputFile);
        
        // read desks configuration file
        DeskConfigurationProcessor proc =
            new DeskConfigurationProcessor(DESKS_CONFIGURATIONS);
        // use ArrayList - preserve order of desks as defined in the
        // DESKS_CONFIGURATIONS for the HTML overview
        deskConfigurations = proc.readAndGetDeskConfigurations();
        
        // fill in contents of the icons HashMap - full paths to the status
        // icon files
        String currentDir = new File (".").getCanonicalPath() + File.separator;
        logger.info("Path to the status icon files: " + currentDir);
        
        icons.put(UNASSIGNED, currentDir + UNASSIGNED_FILE);
        icons.put(ASSIGNED, currentDir + ASSIGNED_FILE);
        icons.put(ERROR, currentDir + ERROR_FILE);
        icons.put(READY, currentDir + READY_FILE);
        
    } // HTMLOverviewWriter() -----------------------------------------------
   
    
    
    protected void writeHtmlStatusPage(HashMap<String, DeskDataMessage>
                                       lastMessages)
    {
        logger.info("Writing HTML status overview page ...");
        
        // iterate over all desk configurations, if a desk is present in
        // lastMessages, use the update status of such desk, otherwise
        // there was not update for that desk, use default state 'signed off'
        String deskName = null;
        String status = null;
        
        // ArrayList - need to preserve order of desks from desk config file
        ArrayList<String[]> table = new ArrayList<String[]>();
        for(DeskConfiguration dc : deskConfigurations)
        {
            deskName = dc.getDeskName();
            if(lastMessages.containsKey(deskName))
            {
                status = lastMessages.get(deskName).getCurrentStatus();
            }
            else
            {
                status = UNASSIGNED;
            }
            
            // just two items
            String[] d = new String[] { deskName, icons.get(status) };
            table.add(d);
            
            // here will come HTML table, debugging, to remove
            // System.out.println("HTML status page: desk: " + deskName +
            //                   " status: " + status +
            //                   " icon: " + icons.get(status));
        }
        
        String page = HTMLPageTemplate.getHtmlPage(table);
    
        writePage(page);
                        
    } // writeHtmlStatusPage() ----------------------------------------------
    
    
    
    private void writePage(String data)
    {
        try
        {
            FileWriter fw = new FileWriter(htmlOutputFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(data);
            bw.flush();
            bw.close();
            fw.close();
            logger.info("HTML status overview page writen.");
        }
        catch(IOException ioe)
        {
            logger.error("Could not write HTML file " + htmlOutputFile +
                         " with overall status, reason: " + ioe.getMessage());
        }
        
    } // writePage() --------------------------------------------------------
    
    
    
} // class HTMLOverviewWriter ===============================================





/**
 * 
 * Class returns HTML page according to HashMap with data - tow rows in a
 * table. It is very specific and single-purpose. No optimisation or
 * generalisation is not done as this will very likely evolve further.
 * 
 * @author Zdenek Maxa
 *
 */
final class HTMLPageTemplate
{
    private static final String pageHeader = "<html>\n<body>\n";
    private static final String pageFooter = "</body>\n</html>\n";
    
    private static final String tableHeader =
        "<table align=\"center\" cellspacing=\"3\">\n";
    private static final String tableFooter = "</table>\n";
    
    private static final String rowHeader = "<tr align=\"center\">\n";
    private static final String rowFooter = "</tr>\n";
    
    
    
    
    public static String getHtmlPage(ArrayList<String[]> tableData)
    {
        StringBuffer sb = new StringBuffer("");
        sb.append(pageHeader);
        sb.append(getTable(tableData));
        sb.append(pageFooter);
        
        return sb.toString();
        
    } // getHtmlPage() ------------------------------------------------------
    
    
    
    private static String getTable(ArrayList<String[]> table)
    {
        StringBuffer r = new StringBuffer(tableHeader);
        
        StringBuffer tmp1 = new StringBuffer(rowHeader);
        StringBuffer tmp2 = new StringBuffer(rowHeader);
        
        for(String[] values : table)
        {
            tmp1.append("<td>" + values[0] + "</td>\n");
            tmp2.append("<td><img src=\"" + values[1] + "\"/></td>\n");
        }
        tmp1.append(rowFooter);
        tmp2.append(rowFooter);
        
        r.append(tmp1);
        r.append(tmp2);
        r.append(tableFooter);
        
        return r.toString();
        
    } // getTable() ---------------------------------------------------------
    
    
} // class HTMLPageTemplate =================================================