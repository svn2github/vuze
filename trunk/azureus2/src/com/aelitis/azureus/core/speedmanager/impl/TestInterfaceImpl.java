package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.TestInterface;

import java.util.Random;

/**
 * Created on Jul 6, 2007
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

public class TestInterfaceImpl implements TestInterface {


    /**
     * Negative values are bad, positive values are good.
     *
     * @return - values from -1.0 to +1.0
     */
    public float getCurrentMetric() {

        Random r = new Random();

        boolean rBool = r.nextBoolean();

        //half the time give a random number.
        float retVal = 0.0f;
        if(rBool){
            int rInt = r.nextInt(200) - 100;
            retVal = (rInt/100.0f);
        }

        return retVal;
    }

    /**
     * The current min and max limits allowed.
     *
     * @return int[4] , with maxUpload, minUpload, maxDownload and minDownload respectively.
     */
    public int[] getLimits() {
        return ( new int[] {80000,8000,35000,7000} );  
    }
}
