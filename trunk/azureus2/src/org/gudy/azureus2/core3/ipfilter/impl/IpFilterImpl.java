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

package org.gudy.azureus2.core3.ipfilter.impl;

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
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

public class 
IpFilterImpl 
	extends IpFilter
{

	private static IpFilterImpl ipFilter;
  
	private List ipRanges;
	private List bannedIps;
	
	//Number of ip Blocked
    private int nbIpsBlocked;
  
    //Map ip blocked -> matching range
    private List ipsBlocked;
 
  
	private IpFilterImpl() 
	{
	  ipFilter = this;
	  
	  nbIpsBlocked = 0;
	  bannedIps = new ArrayList();
	  
	  ipsBlocked = new ArrayList();
	  
	  try{
	  	
	  	loadFilters();
	  	
	  }catch( Exception e ){
	  	
	  	e.printStackTrace();
	  }
	}
  
	public static synchronized IpFilter getInstance() {
	  if(ipFilter == null) {
		ipFilter = new IpFilterImpl();
	  }
	  return ipFilter;
	}
  
	public File
	getFile()
	{
		return( FileUtil.getUserFile("filters.config"));
	}
	
	public void
	reload()
		throws Exception
	{
		loadFilters();
	}
	
	public void save() 
	{
		Map map = new HashMap();
		List filters = new ArrayList();
		map.put("ranges",filters);
		Iterator iter = this.ipRanges.iterator();
		while(iter.hasNext()) {
		  IpRange range = (IpRange) iter.next();
		  if(range.isValid() && ! range.isSessionOnly()) {
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
    try {
		//  Open the file
    File filtersFile = FileUtil.getUserFile("filters.config");
    FileOutputStream fos = new FileOutputStream(filtersFile);
		fos.write(BEncoder.encode(map));
		fos.close();     
	  } catch (Exception e) {
		e.printStackTrace();
		// TODO: handle exception
	  }
	}
  
	private void loadFilters() 
		throws Exception
	{
		
	  List new_ipRanges = new ArrayList();

	  FileInputStream fin = null;
	  BufferedInputStream bin = null;
	  try {
		//open the file
		File filtersFile = FileUtil.getUserFile("filters.config");
		if (filtersFile.exists()) {
			fin = new FileInputStream(filtersFile);
			bin = new BufferedInputStream(fin, 8192);
			Map map = BDecoder.decode(bin);
			List list = (List) map.get("ranges");
			Iterator iter = list.listIterator();
			while(iter.hasNext()) {
			  Map range = (Map) iter.next();
			  String description =  new String((byte[])range.get("description"));
			  String startIp =  new String((byte[])range.get("start"));
			  String endIp = new String((byte[])range.get("end"));
	        
			  IpRange ipRange = new IpRangeImpl(description,startIp,endIp,false);
			  if(ipRange.isValid())
				new_ipRanges.add(ipRange);
			}
			bin.close();
			fin.close();
		}		
	  }finally{
	  
	  	ipRanges 	= new_ipRanges;
	  }
	}
  
	public boolean isInRange(String ipAddress) {
	  //In all cases, block banned ip addresses
	  if(isBanned(ipAddress))
	    return true;
	  
	  if(!COConfigurationManager.getBooleanParameter("Ip Filter Enabled",true))
	    return false;
	  boolean allow = COConfigurationManager.getBooleanParameter("Ip Filter Allow");
	  synchronized(ipRanges) { 
			Iterator iter = ipRanges.iterator();
			while(iter.hasNext()) {
			  IpRange ipRange = (IpRange) iter.next();
			  if(ipRange.isInRange(ipAddress)) {
			    if(!allow) {
			      synchronized(ipsBlocked) {
			        ipsBlocked.add(new BlockedIpImpl(ipAddress,ipRange));
			      }
						LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", in range : " + ipRange);
						nbIpsBlocked++;
						return true;
			    } else {		      
			      return false;
			    }
			  }
			}
		}
	  if(allow) {
	    synchronized(ipsBlocked) {
	      ipsBlocked.add(new BlockedIpImpl(ipAddress,null));
	    }
	    LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", not in any range");
	    nbIpsBlocked++;
	    return true;
	  }
	  return false;
	}
	
	private boolean isBanned(String ipAddress) {
	  synchronized(bannedIps) {
	    return bannedIps.contains(ipAddress);
	  }
	}
  
	/**
	 * @return
	 */
	public List getIpRanges() {
	  return ipRanges;
	}
	
	public IpRange
	createRange(boolean sessionOnly)
	{
		return( new IpRangeImpl("","","",sessionOnly));
	}
	
	public int getNbRanges() {
	  return ipRanges.size();
	}
	
	public int getNbIpsBlocked() {
	  return nbIpsBlocked;
	}
	
	public void ban(String ipAddress) {
	  if(!bannedIps.contains(ipAddress))
	    bannedIps.add(ipAddress);
	}
	
  public List getBlockedIps() {
  	synchronized(ipsBlocked) {
  	  return new ArrayList(ipsBlocked);
  	}
  }
}
