/**
 * Created on May 4, 2007
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

package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;

public class NetworkAdminSpeedTestMode
{
    public static final NetworkAdminSpeedTestMode BT_UP_AND_DOWN
            = new NetworkAdminSpeedTestMode("BT upload/download",
                NetworkAdminSpeedTestScheduler.TEST_TYPE_BT_UPLOAD_AND_DOWNLOAD);

    public static final NetworkAdminSpeedTestMode BT_UP_ONLY
            = new NetworkAdminSpeedTestMode("BT upload only",
                NetworkAdminSpeedTestScheduler.TEST_TYPE_BT_UPLOAD_ONLY);

    public static final NetworkAdminSpeedTestMode BT_DOWN_ONLY
            = new NetworkAdminSpeedTestMode("BT download only",
                NetworkAdminSpeedTestScheduler.TEST_TYPE_BT_DOWNLOAD_ONLY);

    private final String name;
    private final int index;

    private NetworkAdminSpeedTestMode(String n, int i) {
        name = n;
        index=i;
    }

    /**
     * Name in the combo-box drop down.
     * @return - String
     */
    public String getComboString(){
        return name;
    }

    /**
     * index in the dropdown.
     * @return - index
     */
    public int getSelectionIndex(){
        return index;
    }

    public String toString() {
        return name+","+index;
    }

}//class
