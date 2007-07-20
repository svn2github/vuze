package com.aelitis.azureus.core.speedmanager.impl.v2;

/**
 * Created on Jul 18, 2007
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

public class SMConst
{

    //strictly a utility class.
    private SMConst(){}

    public static final int START_DOWNLOAD_RATE_MAX = 61440;
    public static final int START_UPLOAD_RATE_MAX = 30720;

    public static final int MIN_UPLOAD_BYTES_PER_SEC = 5120;
    public static final int MIN_DOWNLOAD_BYTES_PER_SEC = 20480;

    public static final int RATE_UNLIMITED = 0;


    /**
     * No limit should go below 5k bytes/sec.
     * @param rateBytesPerSec -
     * @return - "bytes/sec" rate.
     */
    public static int checkForMinUploadValue(int rateBytesPerSec){

        if( rateBytesPerSec < MIN_UPLOAD_BYTES_PER_SEC ){
            return MIN_UPLOAD_BYTES_PER_SEC;
        }
        return rateBytesPerSec;        
    }

    public static int checkForMinDownloadValue(int rateBytesPerSec){
        if( rateBytesPerSec < MIN_DOWNLOAD_BYTES_PER_SEC ){
            return MIN_DOWNLOAD_BYTES_PER_SEC;
        }
        return rateBytesPerSec;
    }

    /**
     * Rule: Min value is alway 10% of max, but not below 5k.
     * @param maxBytesPerSec -
     * @return - minRate.
     */
    public static int calculateMinUpload(int maxBytesPerSec){

        int min = maxBytesPerSec/10;
        return checkForMinUploadValue( min );
    }

    public static int calculateMinDownload(int maxBytesPerSec){
        int min = maxBytesPerSec/10;
        return checkForMinDownloadValue( min );
    }

}
