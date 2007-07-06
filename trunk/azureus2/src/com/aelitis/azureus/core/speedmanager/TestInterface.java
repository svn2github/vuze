package com.aelitis.azureus.core.speedmanager;

/**
 * Created on Jul 5, 2007
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

public interface TestInterface {

    /**
     * Negative values are bad, positive values are good.
     * @return - values from -1.0 to +1.0
     */
    public float getCurrentMetric();

    /**
     * The current min and max limits allowed.
     * @return int[2] , with maxUpload, minUpload, maxDownload and minDownload respectively.
     */
    public int[] getLimits();

    static final int UPLOAD_MAX_INDEX = 0;
    static final int DOWNLOAD_MAX_INDEX = 1;
    
}
