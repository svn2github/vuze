/*
 * Created on 19-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.ipfilter.impl;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.RandomAccess;

import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.FileUtil;

public class 
IpFilterManagerImpl
	implements IpFilterManager
{
	protected static IpFilterManagerImpl		singleton	= new IpFilterManagerImpl();

	private RandomAccessFile rafDescriptions = null;
	
	/**
	 * 
	 */
	public IpFilterManagerImpl() {
		File fDescriptions = FileUtil.getUserFile("ipfilter.cache");
		try {
			if (fDescriptions.exists()) {
				fDescriptions.delete();
			}
			rafDescriptions = new RandomAccessFile(fDescriptions, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Object addDecription(IpRange range, byte[] description) {
		//if (true) return;
		if (rafDescriptions == null)
			return null;
		
		try {
			if (description == null || description.length == 0)
				return null;
			
			int[] info = new int[2];
			info[0] = (int)rafDescriptions.getFilePointer();
			int len = (int)rafDescriptions.length();
			if (info[0] != len) {
				rafDescriptions.seek(len);
				info[0] = (int)rafDescriptions.getFilePointer();
			}
			
			rafDescriptions.write(description);
			info[1] = (int)rafDescriptions.getFilePointer();
			
			return info;
			//System.out.println("add " + new String(description) + "; " + info[0] + " - " + info[1]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public byte[] getDescription(Object info) {
		if (info instanceof Object[]) {
			return (byte[])(((Object[])info)[0]);
		}
		
		if (rafDescriptions == null || !(info instanceof int[])) {
			return "".getBytes();
		}
		
		try {
			int[] pos = (int[])info;
			int len = pos[1] - pos[0];

			if (rafDescriptions.getFilePointer() != pos[0]) {
				rafDescriptions.seek(pos[0]);
			}

			byte[] bytes = new byte[len];
			rafDescriptions.read(bytes);
			
			return bytes;
		} catch (IOException e) {
			return "".getBytes();
		}
	}
	
	public void cacheAllDescriptions() {
		IpRange[] ranges = getIPFilter().getRanges();
		for (int i = 0; i < ranges.length; i++) {
			Object info = ((IpRangeImpl)ranges[i]).getDescRef();
			if (info instanceof int[]) {
				byte[] desc = getDescription(info);
				Object[] data = { desc, info }; 
				((IpRangeImpl)ranges[i]).setDescRef(data);
			}
		}
	}
	
	public void clearDescriptionCache() {
		IpRange[] ranges = getIPFilter().getRanges();
		for (int i = 0; i < ranges.length; i++) {
			Object info = ((IpRangeImpl)ranges[i]).getDescRef();
			if (info instanceof Object[]) {
				int[] data = (int[])((Object[])info)[1]; 
				((IpRangeImpl)ranges[i]).setDescRef(data);
			}
		}
	}

	public static IpFilterManager
	getSingleton()
	{
		return( singleton );
	}
	
	public IpFilter
	getIPFilter()
	{
		return( IpFilterImpl.getInstance());
	}
	
	public BadIps
	getBadIps()
	{
		return (BadIpsImpl.getInstance());
	}
}
