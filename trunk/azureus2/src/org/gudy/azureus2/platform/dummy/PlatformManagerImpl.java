package org.gudy.azureus2.platform.dummy;

import java.io.File;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;



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

	public String
	getApplicationCommandLine()
	
		throws PlatformManagerException
	{
        throw new PlatformManagerException("Unsupported capability called on platform manager");
	}
	
	public File
	getLocation(
		long	location_id )
	
		throws PlatformManagerException
	{
	    return( null );
	}
	
	public boolean
	isAdditionalFileTypeRegistered(
		String		name,				// e.g. "BitTorrent"
		String		type )				// e.g. ".torrent"
	
		throws PlatformManagerException
	{
	    throw new PlatformManagerException("Unsupported capability called on platform manager");
	}	
	
	public void
	unregisterAdditionalFileType(
			String		name,				// e.g. "BitTorrent"
			String		type )				// e.g. ".torrent"
		
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

	public void
	registerAdditionalFileType(
		String		name,				// e.g. "BitTorrent"
		String		description,		// e.g. "BitTorrent File"
		String		type,				// e.g. ".torrent"
		String		content_type )		// e.g. "application/x-bittorrent"
	
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

    /**
     * Does nothing
     */
    public void dispose()
    {
    }
}
