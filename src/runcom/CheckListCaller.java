package runcom;


import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JOptionPane;
import javax.swing.JFrame;

import mylogger.MyLogger;

/**
 * The only class of the RunCom application which interacts with CheckList.
 * The is no API dependency on CheckList. This method will try to load
 * CheckList.showCheckListWindow(requestedCheckList)
 * via Java Reflection which only relies that checklist.jar is present in
 * the classpath. This way RunCom will compile without CheckList.
 * 
 * @author Zdenek Maxa
 */
public final class CheckListCaller
{
    private static MyLogger logger = MyLogger.getLogger(CheckListCaller.class);
    
    private static final String CLASS_NAME = "checklist.CheckList";
    private static final String METHOD_NAME = "showCheckListWindow";
    
    
    
    
    /**
     * 
     * @param requiredCheckListFileName - exact file name of the checklist
     * @param deskName - name of the active desk here in RunCom
     */
    public static void call(String requiredCheckListFileName, String deskName)
    {
        logger.info("Trying to load CheckList class and display \"" +
                     requiredCheckListFileName + "\" checklist, calling: " +
                     CLASS_NAME + "." + METHOD_NAME + "()");
        
        String m = null;

        try 
        {
            // checklist.jar should be in classpath, will try loading the class
            Class<?> checkListClass = Class.forName(CLASS_NAME);
            // array is list of parameter types of the invoked method
            Method method = null;
            method = checkListClass.getDeclaredMethod(METHOD_NAME,
                                 new Class[] { String.class, String.class });
            // invokes method on the specified object (which is null since the
            // method which is called is static) with one String argument            
            method.invoke(null, requiredCheckListFileName, deskName);
          }
          catch(ClassNotFoundException cnf) 
          {
              m = "Could not show CheckList application because CheckList " +
                  "class is not available.";
              logger.debug(m, cnf);
              handleError(m);
          }
          catch(IllegalAccessException iae) 
          {
              m = "Could not show CheckList because CheckList class " +
                  "could not be accessed.";
              logger.debug(m, iae);
              handleError(m);
          }
          catch(NoSuchMethodException nsme) 
          {
              m = "Could not show CheckList because method " + 
                  "CheckList.showCheckListWindow() is not available.";
              logger.debug(m, nsme);
              handleError(m);
          }
          catch(InvocationTargetException ite)
          {
              m = "Could not show CheckList because method " +
                  "CheckList.showCheckListWindow() could not be called.";
              logger.debug(m, ite);
              handleError(m);
          }
          catch(Throwable t)
          {
        	  m = "Unknown exception ocurred, reason: " + t.getMessage();
        	  logger.debug(m, t);
        	  handleError(m);
          }
        
    } // call() -------------------------------------------------------------
    
    
    
    private static void handleError(String message)
    {
        logger.error(message);
        JOptionPane.showMessageDialog((JFrame) RunCom.getGUI(),
                                      message, "RunCom error",
                                      JOptionPane.ERROR_MESSAGE);
        
    } // handleError() ------------------------------------------------------

} // class CheckListCaller ==================================================