/**
 * Created on Feb 28, 2008
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

package com.aelitis.azureus.util;

import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.aelitis.azureus.core.messenger.config.PlatformDCAdManager;

public class AzpdFileAccess {

	public static final String PARAM_EXPIRE_TIME = "az-expire-time";
	public static final String PARAM_CREATE_TIME = "az-create-time";

	public static final String PARAM_IS_OFFLINE = "is-off-line";

	/**
	 *
	 * @param azpdFile -
	 * @return - true if expired.
	 */
	public static synchronized boolean isAzpdFileExpired(File azpdFile){

		try{
			if (!azpdFile.exists()) {
				return true;
			}
			
			//turn this string into a Map.
			Map params = readAzpdFileToMap(azpdFile);

			String expireString = (String) params.get(PARAM_EXPIRE_TIME);

			long expireTime = Long.parseLong(expireString);
			long currTime = System.currentTimeMillis();

			return ( currTime > expireTime );

		}catch(IOException ioe){
			//consider this file expired.
			ioe.printStackTrace();
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return true;
		}
	}

	public static synchronized String readAzpdFile(File azpdFile)
		throws IOException
	{
		String data = FileUtil.readFileAsString(azpdFile,10000000);
		return data;
	}

	public static synchronized Map readAzpdFileToMap(File azpdFile)
		throws IOException
	{
		String data = readAzpdFile(azpdFile);
		return JSONUtils.decodeJSON(data);
	}



	public static synchronized void writeAzpdFile(File azpdFile, String data){
		FileUtil.writeBytesAsFile(azpdFile.getAbsolutePath(),data.getBytes());
	}

    static final String EXT_AZUREUS_PLAYER_DATA = "azpd";
    public static synchronized Map getPlayerDataMap(DownloadManager dm)
    {
        try
        {
            File azureusPlayDataFile = determineAzpdFileLocation(dm);

			String data = AzpdFileAccess.readAzpdFile(azureusPlayDataFile);

			return JSONUtils.decodeJSON(data);

        }catch(TOTorrentException tte){
            PlatformDCAdManager.debug("TOTorrent Error - getPlayerDataMap(): "+tte);
            tte.printStackTrace();
            return null;
        }catch(Throwable t){

            PlatformDCAdManager.debug("Error - getPlayerDataMap(): "+t);
            t.printStackTrace();

            return null;
        }

    }//getPlayerDataMap


    /**
     * Get the location of the azpd file.
     * @param dm - DownloadManager
     * @return - File -
     * @throws TOTorrentException - t
     */
    public static File determineAzpdFileLocation(DownloadManager dm)
        throws TOTorrentException
    {
        File azpdDir = getAzpdDir();

        String fileNamePrefix = dm.getTorrent().getHashWrapper().toBase32String();
        return new File( azpdDir ,fileNamePrefix+"."+EXT_AZUREUS_PLAYER_DATA );
    }

    public static File getAzpdDir() {
        File mediaDir = FileUtil.getUserFile("media");
        if( !mediaDir.exists() ){
            FileUtil.mkdirs(mediaDir);
        }
        File azpdDir = new File(mediaDir,"azpd");
        if( !azpdDir.exists() ){
            FileUtil.mkdirs(azpdDir);
        }
        return azpdDir;
    }

}
