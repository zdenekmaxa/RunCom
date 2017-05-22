package runcom;

import java.awt.Color;
import java.util.Date;

/**
 * Interface defining common exposed methods for DeskPanel DeskPanelSupervisor
 * @author Zdenek Maxa
 */
public interface IDeskPanel
{
    public void setStatus(String state, Color color, boolean setCompomentsState);
    
    public void setDeskPanelEnabled(boolean enabled);
        
    public void setPersonNameTextField(String text);

    public void setDQLastChecked(Date date);
    
    public void setDQCheckPeriodMinutes(int minutes);
    
    public void setDQStatusChecked(boolean status);
    
    public void setNotAcknowledgedCounterLabel(String counter);
   
} // interface IDeskPanel ===================================================