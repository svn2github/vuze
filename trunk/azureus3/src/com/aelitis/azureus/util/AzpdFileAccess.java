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

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

public class AzpdFileAccess {

	private static AzpdFileAccess ourInstance = new AzpdFileAccess();

	Map files = new HashMap(); //<File,Boolean>

	public static AzpdFileAccess getInstance() {
		return ourInstance;
	}

	private AzpdFileAccess() {
	}

	public synchronized boolean canAccess(File azpdFile){
		Boolean isAvail = (Boolean) files.get(azpdFile);

		if( isAvail==null){
			files.put(azpdFile,Boolean.TRUE);
			return true;
		}

		return isAvail.booleanValue();
	}

	/**
	 * Only one other thread access this file. Just need to delay access.
	 * @param azpdFile -
	 * @return -
	 * @throws IOException -
	 */
	public synchronized String readAzpdFile(File azpdFile)
		throws IOException
	{
		if( !canAccess(azpdFile) ){
			try{Thread.sleep(100);}
			catch(InterruptedException ie){}
		}
		files.put(azpdFile, Boolean.FALSE);
		String data = FileUtil.readFileAsString(azpdFile,10000000);
		files.put(azpdFile, Boolean.TRUE);

		return data;
	}

	/**
	 * Only one other thread accesses this file. Just need to delay access.
	 * @param azpdFile -
	 * @param data -
	 */
	public synchronized void writeAzpdFile(File azpdFile, String data){
		if( !canAccess(azpdFile) ){
			try{Thread.sleep(100);}
			catch(InterruptedException ie){}
		}
		files.put(azpdFile, Boolean.FALSE);
		FileUtil.writeBytesAsFile(azpdFile.getAbsolutePath(),data.getBytes());
		files.put(azpdFile, Boolean.TRUE);
	}

}
