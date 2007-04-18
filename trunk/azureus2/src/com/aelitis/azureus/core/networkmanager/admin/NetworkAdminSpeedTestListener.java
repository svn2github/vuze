package com.aelitis.azureus.core.networkmanager.admin;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;

/**
 * User: asnyder
 * Date: Apr 17, 2007
 * Time: 5:29:45 PM
 * Azureus - 2007
 */
public interface NetworkAdminSpeedTestListener 
{

    /**
     * When a test completes.
     * @param result - String with the result
     */
    public void complete(NetworkAdminSpeedTester.Result result);

    /**
     * Informs listener when the test is at a new stage.
     * @param step - String with stage.
     */
    public void stage(String step);

}
