package runcom.deskconfig;

import com.thoughtworks.xstream.annotations.XStreamAlias;



/**
 * Class holds all configuration data associated with a single desk.
 * 
 * @author Zdenek Maxa
 *
 */
@XStreamAlias("DeskConfiguration")
public final class DeskConfiguration
{
    // desk configuration data
    private String deskName = null;
    // sign-in is a checklist which brings desk to ready state
    private String signInCheckListFileName = null;
    private String dqCheckListFileName = null;
    // request checklists, not all may be relevant to all desks
    private String injectionCheckListFileName = null;
    private String stableBeamCheckListFileName = null;
    private String startRunCheckListFileName = null;
    private boolean ignoreInjectionRequest = false;
    private boolean ignoreStableBeamRequest = false;
    private boolean ignoreStartRunRequest = false;
    
    // other, potentially possible predefined values:
    // start-up / initialisation status
    // data quality check periods

            
    public String getDeskName()
    {
        return deskName;
    }
    
    
    public void setDeskName(String deskName)
    {
        this.deskName = deskName;
    }
    
    
    public String getSignInCheckListFileName()
    {
        return signInCheckListFileName;
    }
    
    
    public void setSignInCheckListFileName(String signInCheckListFileName)
    {
        this.signInCheckListFileName = signInCheckListFileName;
    }
    
    
    public String getDqCheckListFileName()
    {
        return dqCheckListFileName;
    }
    
    
    public void setDqCheckListFileName(String dqCheckListFileName)
    {
        this.dqCheckListFileName = dqCheckListFileName;
    }
    
    
    public String getInjectionCheckListFileName()
    {
        return injectionCheckListFileName;
    }
    
    
    public void setInjectionCheckListFileName(String injectionCheckListFileName)
    {
        this.injectionCheckListFileName = injectionCheckListFileName;
    }
    
    
    public String getStableBeamCheckListFileName()
    {
        return stableBeamCheckListFileName;
    }
    
    
    public void setStableBeamCheckListFileName(String stableBeamCheckListFileName)
    {
        this.stableBeamCheckListFileName = stableBeamCheckListFileName;
    }
    
    
    public String getStartRunCheckListFileName()
    {
        return startRunCheckListFileName;
    }
    
    
    public void setStartRunCheckListFileName(String startRunCheckListFileName)
    {
        this.startRunCheckListFileName = startRunCheckListFileName;
    }
    
    
    public boolean isIgnoreInjectionRequest()
    {
        return ignoreInjectionRequest;
    }
    
    
    public void setIgnoreInjectionRequest(boolean ignoreInjectionRequest)
    {
        this.ignoreInjectionRequest = ignoreInjectionRequest;
    }
    
    
    public boolean isIgnoreStableBeamRequest()
    {
        return ignoreStableBeamRequest;
    }
    
    
    public void setIgnoreStableBeamRequest(boolean ignoreStableBeamRequest)
    {
        this.ignoreStableBeamRequest = ignoreStableBeamRequest;
    }
    
    
    public boolean isIgnoreStartRunRequest()
    {
        return ignoreStartRunRequest;
    }
    
    
    public void setIgnoreStartRunRequest(boolean ignoreStartRunRequest)
    {
        this.ignoreStartRunRequest = ignoreStartRunRequest;
    }
    
    
} // class DeskConfiguration ================================================