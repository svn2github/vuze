/*
 * File    : IpFilterImpl.java
 * Created : 16-Oct-2003
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author Olivier
 *
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.*;

public class 
IpFilterImpl 
	extends IpFilter
{

	private static IpFilterImpl ipFilter;
  
	private List ipRanges;
  
 
  
	private IpFilterImpl() {
	  ipFilter = this;
	  ipRanges = new ArrayList();
	  loadFilters();
	}
  
	public static synchronized IpFilter getInstance() {
	  if(ipFilter == null) {
		ipFilter = new IpFilterImpl();
	  }
	  return ipFilter;
	}
  
	public void save() {
      
	  try {
		//Open the file
		File filtersFile = FileUtil.getApplicationFile("filters.config");
		FileOutputStream fos = new FileOutputStream(filtersFile);
		Map map = new HashMap();
		List filters = new ArrayList();
		map.put("ranges",filters);
		Iterator iter = this.ipRanges.iterator();
		while(iter.hasNext()) {
		  IpRange range = (IpRange) iter.next();
		  if(range.isValid()) {
			String description =  range.getDescription();
			String startIp = range.getStartIp();
			String endIp = range.getEndIp();
			Map mapRange = new HashMap();
			mapRange.put("description",description);
			mapRange.put("start",startIp);
			mapRange.put("end",endIp);
			filters.add(mapRange);
		  }
		}
		fos.write(BEncoder.encode(map));
		fos.close();     
	  } catch (Exception e) {
		e.printStackTrace();
		// TODO: handle exception
	  }
	}
  
	private void loadFilters() {
	  FileInputStream fin = null;
	  BufferedInputStream bin = null;
	  try {
		//open the file
		File filtersFile = FileUtil.getApplicationFile("filters.config");
		if (filtersFile.exists()) {
			fin = new FileInputStream(filtersFile);
			bin = new BufferedInputStream(fin);
			Map map = BDecoder.decode(bin);
			List list = (List) map.get("ranges");
			Iterator iter = list.listIterator();
			while(iter.hasNext()) {
			  Map range = (Map) iter.next();
			  String description =  new String((byte[])range.get("description"));
			  String startIp =  new String((byte[])range.get("start"));
			  String endIp = new String((byte[])range.get("end"));
	        
			  IpRange ipRange = new IpRangeImpl(description,startIp,endIp);
			  if(ipRange.isValid())
				this.ipRanges.add(ipRange);
			}
		}
	  } catch(Exception e) {
		e.printStackTrace();
	  }
	}
  
	public boolean isInRange(String ipAddress) {
	  if(!COConfigurationManager.getBooleanParameter("Ip Filter Enabled",false))
		return false;
	  synchronized(ipRanges) { 
		Iterator iter = ipRanges.iterator();
		while(iter.hasNext()) {
		  IpRange ipRange = (IpRange) iter.next();
		  if(ipRange.isInRange(ipAddress)) {
			LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", in range : " + ipRange);
			return true;
		  }
		}
	  }
	  return false;
	}
  
	/**
	 * @return
	 */
	public List getIpRanges() {
	  return ipRanges;
	}
	
	public IpRange
	createRange()
	{
		return( new IpRangeImpl("","",""));
	}
}
