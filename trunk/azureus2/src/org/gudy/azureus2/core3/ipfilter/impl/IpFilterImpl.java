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
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

public class 
IpFilterImpl 
	implements IpFilter
{

	private static IpFilterImpl ipFilter;
	private static AEMonitor	class_mon	= new AEMonitor( "IpFilter:class" );
 
	private Set 		all_ip_ranges;
	
	private IPAddressRangeManager	range_manager = new IPAddressRangeManager();
	
	private Map			bannedIps;
	 
    //Map ip blocked -> matching range
	
    private List		ipsBlocked;

    private long	last_update_time;
    
  
	private IpFilterImpl() 
	{
	  ipFilter = this;
	  
	  all_ip_ranges	= new HashSet();
	  
	  bannedIps = new HashMap();
	  
	  ipsBlocked = new ArrayList();
	  
	  try{
	  	
	  	loadFilters();
	  	
	  }catch( Exception e ){
	  	
	  	Debug.printStackTrace( e );
	  }
	}
  
	public static IpFilter getInstance() {
		try{
			class_mon.enter();
		
			  if(ipFilter == null) {
				ipFilter = new IpFilterImpl();
			  }
			  return ipFilter;
		}finally{
			
			class_mon.exit();
		}
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
	
	public void 
	save() 
	
		throws Exception
	{
		try{
			class_mon.enter();
		
			Map map = new HashMap();
		 
	
			List filters = new ArrayList();
			map.put("ranges",filters);
			Iterator iter = all_ip_ranges.iterator();
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
		  
		  	FileOutputStream fos  = null;
	    
	    	try {
	      	
	    		//  Open the file
	    		
	    		File filtersFile = FileUtil.getUserFile("filters.config");
	        
	    		fos = new FileOutputStream(filtersFile);
	    		
	    		fos.write(BEncoder.encode(map));
	    		
	    	}finally{
		  	
		  		if ( fos != null ){
		  			
		  			fos.close();
		  		}
	    	}
		}finally{
			
			class_mon.exit();
		}
	}
  
	private void loadFilters() 
		throws Exception
	{
		try{
			class_mon.enter();
		
		  Set new_ipRanges = new HashSet();
	
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

				  new_ipRanges.add( ipRange );
				}
				bin.close();
				fin.close();
			}		
		  }finally{
		  
		  	all_ip_ranges 	= new_ipRanges;
		  	
		  	Iterator	it = all_ip_ranges.iterator();
		  	
		  	while( it.hasNext()){
		  		
		  		((IpRange)it.next()).checkValid();
		  	}
		  	
		  	markAsUpToDate();
		  }
		}finally{
			
			class_mon.exit();
		}
	}
  
	protected boolean
	isInRange(
		IpRangeImpl	range,
		String		address )
	{
		return( range_manager.isInRange( range, address ));
	}
  
  public boolean isInRange(String ipAddress) {
    return isInRange( ipAddress, "" );
  }
  
  
	public boolean isInRange(String ipAddress, String torrent_name) {
	  //In all cases, block banned ip addresses
	  if(isBanned(ipAddress))
	    return true;
	  
	  	// never bounce the local machine (peer guardian has a range that includes it!)
	  
	  if ( ipAddress.equals("127.0.0.1")){
	  	return( false );
	  }
	  if(!COConfigurationManager.getBooleanParameter("Ip Filter Enabled",true))
	    return false;
	  boolean allow = COConfigurationManager.getBooleanParameter("Ip Filter Allow");
	  
	  IpRange	match = (IpRange)range_manager.isInRange( ipAddress );

	  if(match != null) {
	    if(!allow) {
	      try{
	      	class_mon.enter();
	     
	        ipsBlocked.add(new BlockedIpImpl(ipAddress,match, torrent_name));
	        
		  }finally{
		  	class_mon.exit();
		  }
		  
	      LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", in range : " + match);
		  return true;
	    }else {		      
	      return false;
	    }
	  }

	
	  if(allow) {
	    try{
	    	class_mon.enter();
	      
	    	ipsBlocked.add(new BlockedIpImpl(ipAddress,null, torrent_name));
	    	
	    }finally{
	    	
	    	class_mon.exit();
	    }
	    
	    LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", not in any range");
	    return true;
	  }
	  return false;
	}
	
	private boolean isBanned(String ipAddress) {
	  try{
	  	class_mon.enter();
	  
	    return( bannedIps.get(ipAddress) != null );
	    
	  }finally{
	  	
	  	class_mon.exit();
	  }
	}
  
	public boolean
	getInRangeAddressesAreAllowed()
	{
	  boolean allow = COConfigurationManager.getBooleanParameter("Ip Filter Allow");
	  
	  return( allow );
	}
	
	public void
	setInRangeAddressesAreAllowed(
		boolean	b )
	{
		COConfigurationManager.setParameter("Ip Filter Allow", b );
	}
	
	/**
	 * @return
	 * @deprecated
	 */
	
	public List 
	getIpRanges() 
	{
	  return new ArrayList( all_ip_ranges );
	}
	
	public IpRange[]
	getRanges()
	{
		try{
			class_mon.enter();
			
			IpRange[]	res = new IpRange[all_ip_ranges.size()];
			
			all_ip_ranges.toArray( res );
			
			return( res );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public IpRange
	createRange(boolean sessionOnly)
	{
		return( new IpRangeImpl("","","",sessionOnly));
	}
	
	public void
	addRange(
		IpRange	range )
	{
		try{
			class_mon.enter();
		
			all_ip_ranges.add( range );
			
				// we only allow the validity check to take effect once its added to
				// the list of all ip ranges (coz safepeer creates lots of dummy entries
				// during refresh and then never adds them...
			
			range.checkValid();
			
		}finally{
			
			class_mon.exit();
		}
		
		markAsUpToDate();
	}
	
	public void
	removeRange(
		IpRange	range )
	{
		try{
			class_mon.enter();
		
			all_ip_ranges.remove( range );
			
			range_manager.removeRange( range );
			
		}finally{
			
			class_mon.exit();
		}
		
		markAsUpToDate();
	}
	
	public int getNbRanges() {
	  return all_ip_ranges.size();
	}
	
	protected void
	setValidOrNot(
		IpRange		range,
		boolean		valid )
	{
		try{
			class_mon.enter();

			if ( !all_ip_ranges.contains( range )){
				
				return;
			}
			
		}finally{
			
			class_mon.exit();
		}
		
		if ( valid ){
					
			range_manager.addRange(range.getStartIp(), range.getEndIp(), range );
				
		}else{
			
			range_manager.removeRange( range );
		}
	}
	
	public int 
	getNbIpsBlocked() 
	{
	  return ipsBlocked.size();
	}
	
	public void 
	ban(
		String 	ipAddress,
		String	torrent_name ) 
	{
		try{
			class_mon.enter();
			
			if( bannedIps.get(ipAddress) == null ){
				
				bannedIps.put( ipAddress, new BannedIpImpl( ipAddress, torrent_name ));
			}
		}finally{
			
			class_mon.exit();
		}
	}
	
	public BannedIp[] 
	getBannedIps() 
	{
		try{
			class_mon.enter();
			
			BannedIp[]	res = new BannedIp[bannedIps.size()];
		
			bannedIps.values().toArray(res);
			
			return( res );
			
		}finally{
			
			class_mon.exit();
		}
  	}
	
	public int
	getNbBannedIps()
	{
		return( bannedIps.size());
	}
	
	public void
	clearBannedIps()
	{
		try{
			class_mon.enter();
		
			bannedIps.clear();
		}finally{
			
			class_mon.exit();
		}
	}
	
	public BlockedIp[] 
	getBlockedIps() 
	{
		try{
			class_mon.enter();
			
			BlockedIp[]	res = new BlockedIp[ipsBlocked.size()];
		
			ipsBlocked.toArray(res);
			
			return( res );
		}finally{
			
			class_mon.exit();
		}
  	}
	
	public void
	clearBlockedIPs()
	{
		try{
			class_mon.enter();
			
			ipsBlocked.clear();
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public boolean
	isEnabled()
	{
		return( COConfigurationManager.getBooleanParameter("Ip Filter Enabled",true));	
	}

	public void
	setEnabled(
		boolean	enabled )
	{
		COConfigurationManager.setParameter( "Ip Filter Enabled", enabled );
	}
	
	public void
	markAsUpToDate()
	{
	  	last_update_time	= SystemTime.getCurrentTime();		
	}

	public long
	getLastUpdateTime()
	{
		return( last_update_time );
	}
}
