package com.aelitis.azureus.core.networkmanager.admin;

/**
 * Created by IntelliJ IDEA.
 * User: asnyder
 * Date: Apr 17, 2007
 * Time: 5:04:28 PM
 * Azureus - 2007
 */
public interface NetworkAdminSpeedTestScheduler
{

    /**
     * Create a test of type.
     * @param type -
     */
    public void start(int type);

    /**
     * If a test is currently running abort it.
     */
    public void abort();

    /**
     *
     * @return true is a test is already running.
     */
    public boolean isRunning();

	/**
	 *
	 * @return true if
	 */
	public boolean isComplete();

    /**
     * Get the most recent result for the test.
     * @return - Result
     */
    public NetworkAdminSpeedTester.Result getLastResult();
    
}
