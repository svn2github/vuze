package org.gudy.azureus2.platform;



/**
 * @version 1.0
 */
final class DummyPlatformManager implements PlatformManager
{

    private static PlatformManager singleton;

    static
    {
        singleton = new DummyPlatformManager();
    }

    protected static PlatformManager getSingleton()
    {
        return singleton;
    }

    private DummyPlatformManager() {}

    /**
     * {@inheritDoc}
     */
    public int getPlatformType()
    {
        return PlatformManager.PT_OTHER;
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
    public boolean hasCapability(PlatformManagerCapabilities capability)
    {
        return false;
    }
}
