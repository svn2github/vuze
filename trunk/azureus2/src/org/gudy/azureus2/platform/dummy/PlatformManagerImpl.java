package org.gudy.azureus2.platform.dummy;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerException;
import org.gudy.azureus2.platform.PlatformManagerFactory;



/**
 * @version 1.0
 */

public class PlatformManagerImpl implements PlatformManager
{

    private static PlatformManager singleton;

    static
    {
        singleton = new PlatformManagerImpl();
    }
    
    public static PlatformManager getSingleton()
    {
        return singleton;
    }

    private PlatformManagerImpl() {}

    /**
     * {@inheritDoc}
     */
    public int getPlatformType()
    {
        return( PlatformManagerFactory.getPlatformType());
    }

    /**
     * {@inheritDoc}
     */
    public String getUserDataDirectory()

        throws PlatformManagerException
    {
        throw new PlatformManagerException("Unsupported capability called on platform manager");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isApplicationRegistered()

        throws PlatformManagerException
    {
        throw new PlatformManagerException("Unsupported capability called on platform manager");
    }

    /**
     * {@inheritDoc}
     */
    public void registerApplication()

        throws PlatformManagerException
    {
        throw new PlatformManagerException("Unsupported capability called on platform manager");
    }

    /**
     * {@inheritDoc}
     */
    public void createProcess(String command_line, boolean inherit_handles)

        throws PlatformManagerException
    {
        throw new PlatformManagerException("Unsupported capability called on platform manager");
    }

    /**
     * {@inheritDoc}
     */
    public void performRecoverableFileDelete(String file_name)

        throws PlatformManagerException
    {
        throw new PlatformManagerException("Unsupported capability called on platform manager");
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion()

    	throws PlatformManagerException
	{
    	throw new PlatformManagerException("Unsupported capability called on platform manager");
	}

    /**
     * {@inheritDoc}
     */
	public void
	setTCPTOSEnabled(
		boolean		enabled )
		
		throws PlatformManagerException
	{
        throw new PlatformManagerException("Unsupported capability called on platform manager");
	}

    /**
     * {@inheritDoc}
     */
    public void showFile(String file_name)

            throws PlatformManagerException
    {
        throw new PlatformManagerException("Unsupported capability called on platform manager");
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCapability(PlatformManagerCapabilities capability)
    {
        return false;
    }
}
