package runcom.deskconfig;

import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import com.thoughtworks.xstream.XStream;

import mylogger.MyLogger;

/**
 * The class reads in configuration data (desk names, exact XML CheckLists
 * names, various flags, etc from desk XML configuration file.
 * Class DeskConfigurationProcessor holds an ArrayList
 * of particular DeskConfiguration instances.
 * 
 * The default (currently hard-coded) values as defined in DeskData class
 * could be defined in the XML configuration file and used as
 * initialisation values when instances of DeskData are created.
 * 
 * The class is also used by the HTMLOverviewWriter. It needs to load names
 * of all desks.
 * 
 * @author Zdenek Maxa
 *
 */
public final class DeskConfigurationProcessor
{
    private static MyLogger logger =
        MyLogger.getLogger(DeskConfigurationProcessor.class);
    
    // the full path to the DeskConfiguration XML file
    private static String configFileName = null;
    
    
    
    
    
    
    public DeskConfigurationProcessor(String configFile)
    {
        configFileName = configFile;
               
    } // DeskConfigurationProcessor() ---------------------------------------
    
    

    /**
     * Parses XML desk configuration.
     */
    public ArrayList<DeskConfiguration> readAndGetDeskConfigurations()
    throws Exception
    {
        FileReader fr = null;
        ObjectInputStream in = null;
        XStream xstream = new XStream();
        
        // not more than 20 desks
        ArrayList<DeskConfiguration> deskConfigurations = null;
        deskConfigurations = new ArrayList<DeskConfiguration>(20);
        deskConfigurations.clear();
 
        try
        {
            logger.debug("Going to load desk configurations from \"" +
                         configFileName + "\" ...");
            
            fr = new FileReader(configFileName);
            in = xstream.createObjectInputStream(fr);
            
            while(true)
            {
                DeskConfiguration dc = (DeskConfiguration) in.readObject();
                deskConfigurations.add(dc);
            }
        }
        catch(EOFException eofe)
        {
            logger.warn("End of file reached while parsing file \"" +
                        configFileName + "\", desk configurations loaded: " +
                        deskConfigurations.size());
        }
        catch(IOException ioe)
        {
            throw new Exception("I/O error while reading file \"" +
                                configFileName + "\"");
        }
        catch(ClassNotFoundException cnfe)
        {
            throw new Exception("ClassNotFound exception while parsing " +
                                "file \"" + configFileName + "\"");
        }
        finally
        {
            try
            {
                in.close();
                fr.close();
            }
            catch(IOException ioe)
            {
                logger.error("I/O error while closing file \"" +
                             configFileName + "\"");
            }
        }
        
        return deskConfigurations;
        
    } // readAndGetDeskConfigurations() -------------------------------------
    
    
} // class DeskConfigurationProcessor =======================================