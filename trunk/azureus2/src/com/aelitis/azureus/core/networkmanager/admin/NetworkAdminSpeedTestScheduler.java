/**
* Created on Apr 17, 2007
* Created by Alan Snyder
* Copyright (C) 2007 Aelitis, All Rights Reserved.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*
* AELITIS, SAS au capital de 63.529,40 euros
* 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
*
*/


package com.aelitis.azureus.core.networkmanager.admin;

import org.gudy.azureus2.plugins.torrent.TorrentAttribute;


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

    void removeSpeedTestListener(NetworkAdminSpeedTestListener listener);

    /**
     * Send a stage message to NetworkAdminSpeedTestListeners
     * @param message - text to send. Keep it short.
     */
    void sendStageUpdateToListeners(String message);

    /**
     * Send a Result to all of the NetworkAdminSpeedTestListeners.
     * @param r - Result of the test.
     */
    public void sendResultToListeners(NetworkAdminSpeedTester.Result r);

    /**
     * If system crashes on start-up, then speed tests torrents need to be
     * cleaned on start-up.
     */
    public void cleanTestTorrentsOnStartUp();

    /**
     * Get the TorrentAttribute used to determine if this download is a speed test.
     * @return TorrentAttribute used to identify Speed Tests.
     */
    public TorrentAttribute getTestTorrentAttribute();

}
