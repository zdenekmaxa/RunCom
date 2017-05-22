package runcom.activemqcommunication.broker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.thoughtworks.xstream.XStream;

import mylogger.MyLogger;

import runcom.activemqcommunication.DeskDataMessage;
import runcom.activemqcommunication.TextMessage;
import runcom.activemqcommunication.WhiteBoardMessage;


/**
 * Class handles storing DeskDataMessage, TextMessage and WhiteBoardMessage
 * classes in to the local repository (memory container) and to files on disk.
 * Each desk (system) has its own pair of repository files - one with the very
 * last message and second into which new DeskDataMessages are stored
 * for history (no overwriting or rotating is made) (all history).
 * TextMessages and WhiteBoardMessages are stored into separate files.
 * When starting the broker, for DeskData, -last- file is loaded in for
 * each subsystem, several tens of last TextMessages are sent and the 
 * last WhiteBoardMessage is loaded.
 * 
 * Explanation of the XML serialisation mechanism:
 * There are no open files kept in this class. Files which are appended
 * are always closed after each addition. The reason is robustness and higher
 * reliability of data consistency in case the Broker fails and buffers are
 * not flushed or a file writen/closed properly.
 * 
 * This requirement of closing files after each append brings in a problem
 * that it is not possible to serialise the whole container of data or any
 * "wrapping" class since it is not known which object comes last (and don't
 * want to rely on adding closing tags/wrapper closing for aforementioned reason).
 * Thus each object is written as its own XML string (block) followed by a
 * blank line. When reading (parsing) back, pair of openning-closing tag is
 * first found in the getXmlBlock() method, then the block is is passed for
 * deserialization.
 * 
 * Output XML files are not well-formatted XML.
 * 
 * @author Zdenek Maxa
 */


public class PersistentRepository
{
    private static MyLogger logger = MyLogger.getLogger(PersistentRepository.class);
    
    private static final String XML_BLOCKS_DELIMITER =
        System.getProperty("line.separator");
 
    // location of the text file to save last white board message (file
    // is exported on the webserver)
    private static String lastWhiteBoardMessageFile = null;
    // where all persistent binary object data are stored
    private static String persistentDataDir = null;
    
    
    // must ensure exclusive access to these memory repository fields 
    
    // desk data message repository store, holds copy of the last
    // DeskDataMessage
    private static HashMap<String, DeskDataMessage> deskDataRepository = null;
    // store for exchanged text messages, holds MAX_TEXT_MESSAGES messages
    private static LinkedList<TextMessage> textRepository = null;
    // store the last WhiteBoardMessage
    private static WhiteBoardMessage whiteBoardRepository = null;
    
    
    // names of the persistent files (EXT - extension of files) - the whole
    // file name is formed by desk name
    private static final String LAST_DESK_DATA_EXT = ".deskdata.last.rcpd.xml";
    private static final String ALL_DESK_DATA_EXT = ".deskdata.all.rcpd.xml";
    private static final String TEXT_MSG_FILE_NAME = "textmsg.rcpd.xml";
    private static final String WB_MSG_FILE_NAME = "whiteboardmsg.rcpd.xml";
    
    // files for serialised data
    // these files contain only the last message, last update of desk data for
    // each system, the key is the DeskName (it is derived from the file name)
    // value is full path file name of desk data history file
    private static HashMap<String, String> deskDataLastMsg = null;
    // contains all history of desk data exchanged, each new message is appended
    private static HashMap<String, String> deskDataAllMsg = null;
    // all text messages exchanged full path file name
    private static String textMsg = null;
    // all white board messages exchanged full path file name
    private static String wbMsg = null;
    
    // maximum number of TextMessages in the local memory container
    private static final int MAX_TEXT_MESSAGES = 100;
    
    // HTML write - writes a simple HTML page with overall status updates
    private static HTMLOverviewWriter htmlWriter = null; 
    
    
    
    
    public PersistentRepository(String lastWhiteBoardFile, String persistentDir,
                                String statusOverviewHtmlFile)
    throws Exception
    {
        lastWhiteBoardMessageFile = lastWhiteBoardFile;
        persistentDataDir = persistentDir;
        
        logger.info("Creating instance of PersistentRepository, last white " +
                    "board file: \"" + lastWhiteBoardMessageFile + "\" persistent " +
                    " data directory: \"" + persistentDataDir + "\"");
        
        textMsg = persistentDataDir + File.separator + TEXT_MSG_FILE_NAME;
        wbMsg = persistentDataDir + File.separator + WB_MSG_FILE_NAME;
        
        initialize();
        
        htmlWriter = new HTMLOverviewWriter(statusOverviewHtmlFile);
        
    } // PersistentRepository() ---------------------------------------------
    
    
    
    /**
     * Clear up local repositories (fields), then it checks for persistent
     * repositories (files) and loads the latest necessary content.
     */
    private void initialize() throws Exception
    {
        logger.debug("Creating local memory repositories ...");
        
        deskDataRepository = new HashMap<String, DeskDataMessage>();
        deskDataRepository.clear();
        
        textRepository = new LinkedList<TextMessage>();
        textRepository.clear();
        
        deskDataLastMsg = new HashMap<String, String>();
        deskDataAllMsg = new HashMap<String, String>();
        
        initDeskDataAllMessagePersistency(); // this method has to be first
        initDeskDataLastMessagePersistency();
        
        initTextMessagePersistency();
        initWhiteBoardMessagePersistency();
    
    } // initialize() -------------------------------------------------------


    
    /**
     * Read all WhiteBoardMessages from file, keep only the last one.
     */
    private void initWhiteBoardMessagePersistency() throws Exception
    {
        FileReader fr = null;
        BufferedReader br = null;
        XStream xstream = new XStream();
        xstream.alias(WhiteBoardMessage.getClassName(), WhiteBoardMessage.class);
        int counter = 0;
                        
        try
        {
            // create input streams
            fr = new FileReader(wbMsg);
            br = new BufferedReader(fr);
            
            logger.info(wbMsg + " file exists, going to load last " +
                        "WhiteBoardMessage ...");

            while(true)
            {
                String xmlData = getXmlBlock(br);
                whiteBoardRepository = (WhiteBoardMessage) xstream.fromXML(xmlData);
                
                logger.debug("WhiteBoardMessage read from file, timestamp: " +
                                whiteBoardRepository.getTimeStampString());
                counter++;
             }
        }
        catch(FileNotFoundException fnfe)
        {
            logger.warn(wbMsg + " file does not exists, loading no " +
                        "history of WhiteBoardMessages.");    
        }
        // thrown by BufferedReader when end of file is reached, then the
        // last WhiteBoardMessage remains in the whiteBoardRepository variable
        catch(NoXmlDataAvailableException nxdae)
        {
            String s = whiteBoardRepository != null ? 
                       whiteBoardRepository.toString() : "<null>";
            logger.warn("End of file " + wbMsg + " reached.");
            logger.info("Read " + counter + " WhiteBoardMessages from file " +
                         wbMsg + " last one: \"" + s + "\"");
            br.close();
            fr.close();            
        }
        catch(IOException ioe)
        {
            String m = "I/O error while reading WhiteBoardMessages from " +
                       wbMsg + " reason: " + ioe.getMessage();
            logger.error(m);
            logger.debug(m, ioe);
        }
        
    } // initWhiteBoardMessagePersistency() ---------------------------------
    
    

    /**
     * Loads all text messages from file.
     */
    private void initTextMessagePersistency() throws Exception
    {
        FileReader fr = null;
        BufferedReader br = null;
        XStream xstream = new XStream();
        xstream.alias(TextMessage.getClassName(), TextMessage.class);
        int counter = 0;
        
        try
        {
            // create input streams
            fr = new FileReader(textMsg);
            br = new BufferedReader(fr);
            
            logger.info(textMsg + " file exists, going to load up to last " +
                        MAX_TEXT_MESSAGES + " TextMessages ...");
            
            while(true)
            {
                String xmlData = getXmlBlock(br);
                TextMessage tm = (TextMessage) xstream.fromXML(xmlData);
                
                logger.debug("TextMessage read from file, content: \"" +
                             tm.toString() + "\"");
                textRepository.add(tm);
                
                if(textRepository.size() > MAX_TEXT_MESSAGES)
                {
                    textRepository.removeFirst();
                }
                counter++;
            }
        }
        // thrown by BufferedReader when end of file is reached
        catch(NoXmlDataAvailableException nxdae)
        {
            logger.warn("End of file " + textMsg + " reached.");
            logger.info("Read " + counter + " TextMessages from file " +
                        textMsg);
            br.close();
            fr.close();
        }
        catch(FileNotFoundException fnfe)
        {
            logger.warn(textMsg + " file does not exists, loading no " +
                        "history TextMessages.");    
        }
        catch(IOException ioe)
        {
            String m = "I/O error while reading TextMessages from " +
                       textMsg + " reason: " + ioe.getMessage();
            logger.error(m);
            logger.debug(m, ioe);
        }
        
    } // initTextMessagePersistency() ---------------------------------------
    
    
    
    /**
     * File names for all desk data messages (all history) as well as for the
     * last desk data message are set - only for those which have their
     * corresponding persistent files found in persistentDataDir
     * Processes only files names of the files with serialised persistent
     * data, doesn't actually read in any persistent data at this point.
     */
    private void initDeskDataAllMessagePersistency() throws Exception
    {
        logger.info("Going to figure out files names for all history " +
                    "persistent files ...");
        
        File path = new File(persistentDataDir);

        String[] allDeskDataFiles =
            path.list(new PersistentFilesFilter(ALL_DESK_DATA_EXT));
        
        if(allDeskDataFiles.length == 0)
        {
            logger.info("No files found (mask *" + ALL_DESK_DATA_EXT + ").");
        }
        
        for(int i = 0; i < allDeskDataFiles.length; i++)
        {
            logger.debug("All history DeskDataMessage file: \"" +
                         allDeskDataFiles[i] + "\"");
            String deskName = allDeskDataFiles[i].replaceFirst(ALL_DESK_DATA_EXT, "");
            logger.debug("All history DeskDataMessage desk name: \"" +
                         deskName + "\"");
            
            // store the file name into the all history persistent files container
            // save full path file name item into deskDataAllMsg container
            if(deskDataAllMsg.containsKey(deskName))
            {
                // wrong, this should never happen
                String m = "Looks like duplicite file read from disk ... " +
                           "for desk name: \"" + deskName + "\" file name: " +
                           allDeskDataFiles[i];
                throw new Exception(m);
            }
            else
            {
                String fullPath = persistentDataDir + File.separator + allDeskDataFiles[i];
                deskDataAllMsg.put(deskName, fullPath);
                logger.debug("Persistent file name \"" + fullPath +
                             "\" stored under key \"" + deskName + "\"");   
            }
            
        } // for
        
    } // initDeskDataAllMessagePersistency() --------------------------------
    
    
    
    /**
     * Method reads line by line from input file and checks for opening tag
     * (while ignoring blank line before first open tag is encountered) - first
     * non empty, non white space line is considered to contain opening tag.
     * 
     * Then the block is read until closing tag is encountered. This block
     * of data is then considered one XML block and is returned.
     * 
     * The method is somewhat 'expensive', especially when the input XML
     * files tend to be long (each line is examined), however it is the price
     * for robustness (no open files) and this mode of XML files parsing happens
     * only when the broker is being started.
     */
    private static String getXmlBlock(BufferedReader br) 
    throws IOException, NoXmlDataAvailableException
    {
        String line = null;
        // corresponds to one XML element block, e.g. WhiteBoardMessage
        StringBuffer xmlBlock = new StringBuffer("");
        String firstOpenTag = null;  
        
        while((line = br.readLine()) != null)
        {
            // check for empty lines before starting tag, ignore ; line will be ""
            // if xmlBlock is still empty, we are right at the beginning
            if("".equals(line.trim()) && "".equals(xmlBlock.toString()))
            {
                continue;
            }
            
            // check if closing tag of the block was reached, stop if so
            // first '<' has been removed from firstOpenTag
            // keep this condition at this place ... otherwise could will stop
            // at the first iteration, i.e. before anything is added
            // into xmlBlock
            if(firstOpenTag != null && line.trim().endsWith(firstOpenTag))
            {
                // must append the last line before leaving the loop
                xmlBlock.append(line);
                xmlBlock.append("\n");
                break;
            }
            
            // check for the first opening tag and save it
            // this condition must be before the previous one
            if(! "".equals(line.trim()) && "".equals(xmlBlock.toString()))
            {
                // exclude first character '<'
                firstOpenTag = line.trim().substring(1);
            }
            
            // just normal lines, add to the resulting buffer
            xmlBlock.append(line);
            xmlBlock.append("\n");
            
        } // while
        
        if("".equals(xmlBlock.toString()))
        {
            throw new NoXmlDataAvailableException();
        }
        else
        {
            return xmlBlock.toString();
        }
        
    } // getXmlBlock() ------------------------------------------------------
    
    
    
    /**
     * Load last desk data update from disk and put that last message into the
     * deskDataRepository. Files with -last- objects contain only one XML
     * block corresponding with the last DeskDataMessage exchanged for
     * a given desk.
     */
    private void initDeskDataLastMessagePersistency() throws Exception
    {
        logger.info("Going to load last DeskDataMessage persistent files ...");
        
        File path = new File(persistentDataDir);
        
        String[] lastDeskDataFiles =
            path.list(new PersistentFilesFilter(LAST_DESK_DATA_EXT));
        
        if(lastDeskDataFiles.length == 0)
        {
            logger.info("No files found (mask *" + LAST_DESK_DATA_EXT + ").");
        }

        
        for(int i = 0; i < lastDeskDataFiles.length; i++)
        {
            // get only desk name (from file name - first part)
            String deskName = lastDeskDataFiles[i].replaceFirst(LAST_DESK_DATA_EXT, "");
            logger.debug("Last DeskDataMessage desk name: \"" + deskName + "\"");

            // read the last DeskDataMessage
            String fullPath = persistentDataDir + File.separator + lastDeskDataFiles[i]; 
            
            logger.debug("Last DeskDataMessage file: \"" + fullPath + "\"");
            
            // load serialised persistent data
            // this file contains only one (last) object, don't have to
            // consider XML block delimiter separating XML blocks in a 
            // file with multiple blocks
            XStream xstream = new XStream();
            xstream.alias(DeskDataMessage.getClassName(), DeskDataMessage.class);
            FileReader fr = new FileReader(fullPath);
            BufferedReader br = new BufferedReader(fr);
            String xmlData = getXmlBlock(br);
            logger.debug("Going to parse data: \"" + xmlData + "\"");
            DeskDataMessage ddm = (DeskDataMessage) xstream.fromXML(xmlData);
            
            br.close();
            fr.close();
            
            logger.debug("Object successfully read, content: \"" +
                          ddm.toString() + "\"");

            // check if the ddm.deskName is equal to deskName (which was
            // derived from file name)
            if(! deskName.equals(ddm.getDeskName()))
            {
                // serious error, someone must have manipulated the file name
                String m = "Last DeskDataMessage desk name \"" +
                           ddm.getDeskName() + "\" is different from desk name " +
                           "derived from persistent file name: \"" + deskName + "\"";
                throw new Exception(m);
            }
            
            logger.debug("Store the last DeskDataMessage into the local " +
                          "repository ...");
            storeDeskDataMessageIntoContainer(ddm);
                        
            // save full path file name item into deskDataLastMsg container
            if(deskDataLastMsg.containsKey(deskName))
            {
                // wrong, this should never happen
                String m = "Looks like duplicite file read from disk ... " +
                           "for desk name: \"" + deskName + "\" file name: " +
                           lastDeskDataFiles[i];
                throw new Exception(m);
            }
            else
            {
                if(deskDataAllMsg.containsKey(deskName))
                {
                    // all ok, both persistent file names containers contain
                    // the same file name now
                    deskDataLastMsg.put(deskName, fullPath);
                    logger.debug("Persistent file name \"" + fullPath +
                                 "\" stored under key \"" + deskName + "\"");
                }
                else
                {
                    String m = "File " + fullPath + " " +
                               "doesn't have corresponding file with all " +
                               "messages (manipulated file names or file deleted).";
                    throw new Exception(m);
                }
            }
            
        } // for
                
    } // initDeskDataLastMessagePersistency() -------------------------------
    
    
    
    synchronized 
    private void storeDeskDataMessageIntoContainer(DeskDataMessage ddm)
    {
        
        String deskName = ddm.getDeskName();

        logger.debug("Broker stores \"" + deskName + "\" message, " +
                     "this message content: " + ddm.toString());
        // if the HashMap contains a previous value stored under the
        // deskName key, the old value is replaced by the new one
        deskDataRepository.put(deskName, ddm);
        
    } // storeDeskDataMessageIntoContainer() --------------------------------
    
    
    
    /**
     * Method called from broker.Receiver when message with all desk data
     * was received and its copy should be stored in the local repository.
     * @param ddm
     */
    synchronized protected void storeDeskDataMessage(DeskDataMessage ddm)
    {
        storeDeskDataMessageIntoContainer(ddm);        
        updateDeskDataLastPersistentFile(ddm);
        updateDeskDataAllPersistentFile(ddm);
        
        htmlWriter.writeHtmlStatusPage(deskDataRepository);
                
    } // storeDeskDataMessage() ---------------------------------------------

    
    
    /**
     * All history file containing all DeskDataMessage - append the
     * ddm message to the end 
     */
    synchronized private void updateDeskDataAllPersistentFile(DeskDataMessage ddm)
    {
        String deskName = ddm.getDeskName();
        logger.debug("Going to update persistent file (all messages) for " +
                     "\"" + deskName + "\"");
        
        String allMsgFile = null;
        if(deskDataAllMsg.containsKey(deskName))
        {
            allMsgFile = deskDataAllMsg.get(deskName);
            logger.debug("All DeskDataMessage file for \"" + deskName + "\" " +
                         "exists: \"" + allMsgFile + "\"");
        }
        else
        {
            allMsgFile = persistentDataDir + File.separator +
                         deskName + ALL_DESK_DATA_EXT;
            logger.debug("All DeskDataMessage file for \"" + deskName + "\" " +
                         "does not exist, will create one: \"" + allMsgFile + "\"");
            deskDataAllMsg.put(deskName, allMsgFile);
        }
        
        writeMessageIntoFile(allMsgFile, ddm, ddm.getClassName(), true);
                     
    } // updateDeskDataAllPersistentFile() ----------------------------------
    
    
    
    /**
     * last DeskDataMessage persistent file (write mode - the old 
     * content gets overwritten
     */
    synchronized
    private void updateDeskDataLastPersistentFile(DeskDataMessage ddm)
    {
        String deskName = ddm.getDeskName();
        logger.debug("Going to update persistent file (last message) for " +
                     "\"" + deskName + "\"");
        
        String lastMsgFile = null;
        if(deskDataLastMsg.containsKey(deskName))
        {
            lastMsgFile = deskDataLastMsg.get(deskName);
            logger.debug("Last DeskDataMessage file for \"" + deskName + "\" " +
                         "exists: \"" + lastMsgFile + "\"");
        }
        else
        {
            lastMsgFile = persistentDataDir + File.separator +
                          deskName + LAST_DESK_DATA_EXT;
            logger.debug("Last DeskDataMessage file for \"" + deskName + "\" " +
                         "does not exist, will create one: \"" + lastMsgFile + "\"");
            deskDataLastMsg.put(deskName, lastMsgFile);
        }
        
        writeMessageIntoFile(lastMsgFile, ddm, ddm.getClassName(), false);
                        
    } // updateDeskDataLastPersistentFile() ------------------------------------
    
    
    
    /**
     * Method is called when TextMessage arrives, it is stored in the local
     * repository.
     */
    synchronized protected void storeTextMessage(TextMessage tm)
    {
        logger.debug("Broker stores TextMessage, content: " + tm.toString());
        textRepository.add(tm);
        if(textRepository.size() > MAX_TEXT_MESSAGES)
        {
            textRepository.removeFirst();
        }
        
        logger.debug("Going to save TextMessage into persistent file ...");
    
        writeMessageIntoFile(textMsg, tm, tm.getClassName(), true);  
        
    } // storeTextMessage() -------------------------------------------------
    
    
    
    /**
     * Stores incoming WhiteBoardMessage into broker's local repository. 
     */
    synchronized protected void storeWhiteBoardMessage(WhiteBoardMessage wbm)
    {
        logger.debug("Broker stores WhiteBoardMessage, content: " +
                     wbm.toString());
        whiteBoardRepository = wbm;
        
        logger.debug("Going to save WhiteBoardMessage into persistent file ...");
         
        writeMessageIntoFile(wbMsg, wbm, wbm.getClassName(), true);
        
        exportLastWhiteBoardMessage(wbm); // export to the web
                
    } // storeWhiteBoardMessage() -------------------------------------------
    
    
    
    /**
     * Writing persistent data into a file on disk.
     * Method uses XML serialisation of Object msg, xstream library.
     * Each object written in a file is surrounded by object delimiter - which
     * is important when having multiple objects in a file.
     */
    private static void writeMessageIntoFile(String fileName, Object msg,
                                             String className, boolean append)
    {
        XStream xstream = new XStream();
        xstream.alias(className, msg.getClass());
        
        try
        {
            // true flag - open for appending (writing to the end)
            FileWriter fw = new FileWriter(fileName, append);
            
            // create XML data from object
            String xmlData = xstream.toXML(msg);
            xmlData += XML_BLOCKS_DELIMITER + XML_BLOCKS_DELIMITER;
            
            fw.write(xmlData);
            fw.flush();
            fw.close();
            
            logger.debug("File " + fileName + " successfully updated.");                    
         }
        catch(FileNotFoundException fnfe)
        {
            logger.error("Can't store persistent data, file not found: " +
                         fileName);
        }
        catch(IOException ioe)
        {
            logger.error("Can't store persistent data, I/O error while " +
                         "writing to file: " + fileName); 
        }
    
    } // writeMessageIntoFile() ---------------------------------------------
    
    
        
    /**
     * Export the very last WhiteBoard message as text file to a
     * directory visible by the P1 webserver (text file then appears
     * on the status page). Writes only text information.
     */
    private static void exportLastWhiteBoardMessage(WhiteBoardMessage wbm)
    {
        String content = "last update: " + wbm.getTimeStampString() + "\n" +
                          wbm.getMessage();
        
        logger.info("Going to update " + lastWhiteBoardMessageFile + " " +
                    "file with the last WhiteBoardMessage: \"" +
                    wbm.getMessage() + "\"" + " ...");
        
        try
        {
            FileWriter fw = new FileWriter(lastWhiteBoardMessageFile);
            fw.write(content);
            fw.flush();
            fw.close();
            logger.info("Last WhiteBoardMessage successfully saved.");
        }
        catch(IOException ioe)
        {
            String m = "Can't update " + lastWhiteBoardMessageFile + " " +
            "file with last WhiteBoardMessage.";
            logger.error(m);
            logger.debug(m, ioe);
        }
        
    } // exportLastWhiteBoardMessage() --------------------------------------
    

    
    /**
     * Method creates an ArrayList return with last messages (desk status)
     * for each subsystem. Accesses the local memory repository.
     */
    synchronized protected ArrayList<DeskDataMessage> getLastDeskMessages()
    {
        ArrayList<DeskDataMessage> r = new ArrayList<DeskDataMessage>();
        r.clear();
        
        if(deskDataRepository.isEmpty())
        {
            logger.warn("Message repository empty, sending back empty list.");
        }
        else
        {
            logger.debug("Message repository not empty, creating response ...");
            
            for(String deskName : deskDataRepository.keySet())
            {
                // get the last message
                DeskDataMessage ddm = deskDataRepository.get(deskName);
                r.add(ddm);
                logger.debug("Synchronization update (last message) on " +
                             "subsystem, content: " + ddm);
            }
            logger.debug("Created updates query response with " + r.size() +
                         " subsystem(s) updates.");
        }
        
        return r;
        
    } // getLastDeskMessages() ----------------------------------------------
    
     
    
    synchronized protected ArrayList<TextMessage> getLastTextMessages()
    {
        logger.debug("Number of TextMessages in the repository: " +
                     textRepository.size() + " (sending everything)");
        ArrayList<TextMessage> r = new ArrayList<TextMessage>(textRepository);
        return r;
        
    } // getLastTextMessages() ----------------------------------------------
    
    
    
    /**
     * This method returns last saved WhiteBoardMessage from the local
     * repository. If the whiteBoardRepository is null, then null is 
     * returned in the ArrayList.
     * The result is returned in the ArrayList - for compatibility with
     * StartupSynchorizer which deals with DeskDataMessage and TextMessage
     * which are by nature always ArrayLists.
     */
    synchronized protected ArrayList<WhiteBoardMessage> getLastWhiteBoardMessage()
    {
        ArrayList<WhiteBoardMessage> lastWhiteBoardMsg = null;
        lastWhiteBoardMsg = new ArrayList<WhiteBoardMessage>(1);
        if(whiteBoardRepository != null)
        {
            lastWhiteBoardMsg.add(whiteBoardRepository);
        }
        else
        {
            logger.info("No WhiteBoardMessage in the repository, sending " +
                        "empty data.");
        }
        return lastWhiteBoardMsg;
        
    } // getLastWhiteBoardMessage() -----------------------------------------
    


} // class PersistentRepository =============================================




class PersistentFilesFilter implements FilenameFilter
{
    private String extension = null;

    
    // instantiate with extension
    public PersistentFilesFilter(String ext)
    {
        this.extension = ext;
        
    } // PersistentFilesFilter() --------------------------------------------
    
    
    
    public boolean accept(File dir, String fileName)
    {
        File f = new File(dir + File.separator + fileName);
    
        if(f.isFile() && fileName.endsWith(this.extension))
        {
            // accept if it is file and if the extension matches
            return true;
        }
        else
        {
            return false;
        }
        
    } // accept() -----------------------------------------------------------
        
    
} // class PersistentFilesFilter ============================================



@SuppressWarnings("serial")
class NoXmlDataAvailableException extends Exception
{
    
} // NoXmlDataAvailableException ============================================
