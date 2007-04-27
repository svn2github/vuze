package com.aelitis.azureus.core.networkmanager.admin;

import com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminSpeedTestSchedulerImpl;

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
     * Request a test using the testing service.
     * @param type - ID for the type of test.
     * @return boolean - true if a success, otherwise false.
     */
    public boolean requestTestFromService(int type);

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

    /**
     * Add NetworkSpeedTestListener to this test.
     * @param listener -
     */
    void addSpeedTestListener(NetworkAdminSpeedTestListener listener);

    /**
     * Send a stage message to NetworkAdminSpeedTestListeners
     * @param message - text to send. Keep it short.
     */
    void sendStateMessageToListeners(String message);
}
