package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;

/**
 * Created on May 14, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */


/**
 * This algorithm will have the following concepts.
 *
 * A) an absolute range for good ping ranges and bad ping ranges.
 *    1) The forced min is 20 ms.
 *    2) The forced max is 200 ms.
 * B) concept for the number of concecutive increase and decreases.
 *    1) Three concecutive changes in a row will increase the amount of the change.
 * C) On a "reset" we need for force the application to stop uploading data. It needs
 *     to be quite to measure idle ping values.
 * D) The concept of a ping time tolerance.
 *
 */
public class SpeedManagerAlgorithmProviderSpeedSense
    implements SpeedManagerAlgorithmProvider
{
    /**
     * Reset any state to start of day values
     */

    private static final int GOOD_PING_TIME = 20;
    private static final int BAD_PING_TIME = 200;

    //ping time history.

    int numConsecutiveUp=0;
    int numConsecutiveDown=0;
    

    //Stats for data that goes on the UI.  (Don't worry what the real meaning is at the moment.)



    public void reset() {

    }

    /**
     * Called periodically (see period above) to allow stats to be updated.
     */

    public void updateStats() {

        

    }

    /**
     * Called when a new source of ping times has been found
     *
     * @param source
     * @param is_replacement One of the initial sources or a replacement for a failed one
     */

    public void pingSourceFound(SpeedManagerPingSource source, boolean is_replacement) {

    }

    /**
     * Ping source has failed
     *
     * @param source
     */

    public void pingSourceFailed(SpeedManagerPingSource source) {

    }

    /**
     * Called whenever a new set of ping values is available for processing
     *
     * @param sources
     */

    public void calculate(SpeedManagerPingSource[] sources) {


    }

    /**
     * Various getters for interesting info shown in stats view
     *
     * @return
     */

    public int getIdlePingMillis() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getCurrentPingMillis() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getMaxPingMillis() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns the current view of when choking occurs
     *
     * @return speed in bytes/sec
     */

    public int getCurrentChokeSpeed() {
        return 0;  //Is this displayed anywhere?
    }

    public int getMaxUploadSpeed() {
        return 0;
    }
    
    public boolean getAdjustsDownloadLimits() {
    	// TODO Auto-generated method stub
    	return false;
    }
}
