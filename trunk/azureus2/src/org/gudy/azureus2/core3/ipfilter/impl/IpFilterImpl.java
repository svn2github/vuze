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
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;
import org.gudy.azureus2.core3.util.*;

public class 
IpFilterImpl 
	implements IpFilter
{

	private final static int MAX_BLOCKS_TO_REMEMBER = 500;
  
	private static IpFilterImpl ipFilter;
	private static AEMonitor	class_mon	= new AEMonitor( "IpFilter:class" );
 
	private List 		all_ip_ranges;
	
	private IPAddressRangeManager	range_manager = new IPAddressRangeManager();
	
	private Map			bannedIps;
	 
    //Map ip blocked -> matching range
	
	private LinkedList		ipsBlocked;
	
	private int num_ips_blocked 			= 0;
	private int num_ips_blocked_loggable	= 0;

	private long	last_update_time;
    
  
	private List	listeners = new ArrayList();
	
	
	private IpFilterImpl() 
	{
	  ipFilter = this;
	  
	  all_ip_ranges	= new ArrayList(1024);
	  
	  bannedIps = new HashMap();
	  
	  ipsBlocked = new LinkedList();
	  
	  try{
		  loadBannedIPs();
		  
	  }catch( Throwable e ){
		  
		  Debug.printStackTrace(e);
	  }
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
  
	private void 
	loadFilters() 
		throws Exception
	{
		try{
			class_mon.enter();
		
		  List new_ipRanges = new ArrayList(1024);
	
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
		        
				  IpRangeImpl ipRange = new IpRangeImpl(description,startIp,endIp,false);

				  ipRange.setAddedToRangeList(true);
				  
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
  
	protected void
	loadBannedIPs()
	{
		if ( !COConfigurationManager.getBooleanParameter("Ip Filter Banning Persistent" )){
			
			return;
		}
		
		try{
			class_mon.enter();
			
			Map	map = FileUtil.readResilientConfigFile( "banips.config" );
		
			List	ips = (List)map.get( "ips" );
			
			if ( ips != null ){
				
				for (int i=0;i<ips.size();i++){
					
					Map	entry = (Map)ips.get(i);
					
					String	ip 		= new String((byte[])entry.get("ip"));
					String	desc 	= new String((byte[])entry.get("desc"));
					Long	time	= (Long)entry.get("time");
					
					int	int_ip = range_manager.addressToInt( ip );
					
					bannedIps.put( new Integer( int_ip ), new BannedIpImpl(ip, desc, time.longValue() ));
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected void
	saveBannedIPs()
	{
		if ( !COConfigurationManager.getBooleanParameter("Ip Filter Banning Persistent" )){
			
			return;
		}
		
		try{
			class_mon.enter();
			
			Map	map = new HashMap();
			
			List	ips = new ArrayList();
			
			Iterator	it = bannedIps.values().iterator();
			
			while( it.hasNext()){
				
				BannedIpImpl	bip = (BannedIpImpl)it.next();
				
				Map	entry = new HashMap();
				
				entry.put( "ip", bip.getIp());
				entry.put( "desc", bip.getTorrentName());
				entry.put( "time", new Long( bip.getBanningTime()));
				
				ips.add( entry );
			}
			
			map.put( "ips", ips );
			
			FileUtil.writeResilientConfigFile( "banips.config", map );
		
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
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
  
  
	public boolean 
	isInRange(
		String ipAddress, 
		String torrent_name) 
	{
		return( isInRange( ipAddress, torrent_name, true ));
	}
	
	public boolean 
	isInRange(
		String ipAddress, 
		String torrent_name,
		boolean	loggable ) 
	{
		//In all cases, block banned ip addresses
		
	  if(isBanned(ipAddress)){
	  
		  return true;
	  }
	  
	  	// never bounce the local machine (peer guardian has a range that includes it!)
	  
	  if ( ipAddress.equals("127.0.0.1")){
	  	
		  return( false );
	  }
	  
	  if(!COConfigurationManager.getBooleanParameter("Ip Filter Enabled",true)){
		  
	    return false;
	  }
	  
	  boolean allow = COConfigurationManager.getBooleanParameter("Ip Filter Allow");
	  
	  IpRange	match = (IpRange)range_manager.isInRange( ipAddress );

	  if(match != null) {
	    if(!allow) {
	    	
	      	// don't bounce non-public addresses (we can ban them but not filter them as they have no sensible
		  	// real filter address
		  
		  if ( AENetworkClassifier.categoriseAddress( ipAddress ) != AENetworkClassifier.AT_PUBLIC ){
			  
			  return( false );
		  }
		  
	      addBlockedIP( new BlockedIpImpl(ipAddress,match, torrent_name, loggable), loggable );
	      
	      LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", in range : " + match);
	      
	      return true;
	    }
      
	    return false;  
	  }

	
	  if( allow ){  
		  
		if ( AENetworkClassifier.categoriseAddress( ipAddress ) != AENetworkClassifier.AT_PUBLIC ){
			  
		  return( false );
		}
		  
	    addBlockedIP( new BlockedIpImpl(ipAddress,null, torrent_name, loggable), loggable );
	    
	    LGLogger.log(0,0,LGLogger.ERROR,"Ip Blocked : " + ipAddress + ", not in any range");
	    
	    return true;
	  }
	  
	  return false;
	}
	
  
  
  private void 
  addBlockedIP( 
	BlockedIp 	ip,
	boolean		loggable ) 
  {
    try{  class_mon.enter();
   
      ipsBlocked.addLast( ip );
      
      num_ips_blocked++;
      
      if ( loggable ){
    	  
    	  num_ips_blocked_loggable++;
      }
      
      if( ipsBlocked.size() > MAX_BLOCKS_TO_REMEMBER ) {  //only "remember" the last few blocks occurrences
    	  
        ipsBlocked.removeFirst();
      }
    }
    finally{  class_mon.exit();  }
  }
  
  
  
	private boolean 
	isBanned(
		String ipAddress) 
	{
	  try{
	  	class_mon.enter();
	  
		int	address = range_manager.addressToInt( ipAddress );
		
		Integer	i_address = new Integer( address );
		
	    return( bannedIps.get(i_address) != null );
	    
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
		try{
			class_mon.enter();

			return new ArrayList( all_ip_ranges );
			
		}finally{
			
			class_mon.exit();
		}
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
		
			((IpRangeImpl)range).setAddedToRangeList(true);
			
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
		
			((IpRangeImpl)range).setAddedToRangeList( false );
			
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
		IpRangeImpl		range,
		boolean			valid )
	{
		try{
			class_mon.enter();

				// this is an optimisation to deal with the way safepeer validates stuff
				// before adding it in
			
			if ( !range.getAddedToRangeList()){
				
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
	  return num_ips_blocked;
	}
	
	public int 
	getNbIpsBlockedAndLoggable() 
	{
	  return num_ips_blocked_loggable;
	}
	
	public boolean 
	ban(
		String 	ipAddress,
		String	torrent_name ) 
	{
		boolean	block_ban = false;
		
		List	new_bans = new ArrayList();
		
		try{
			class_mon.enter();
			
			int	address = range_manager.addressToInt( ipAddress );
			
			Integer	i_address = new Integer( address );
			
			if ( bannedIps.get(i_address) == null ){
				
				BannedIpImpl	new_ban = new BannedIpImpl( ipAddress, torrent_name );
				
				new_bans.add( new_ban );
				
				bannedIps.put( i_address, new_ban );
				
					// check for block-banning, but only for real addresses
				
				if ( !UnresolvableHostManager.isPseudoAddress( ipAddress )){
					
					long	l_address = address;
					
			    	if ( l_address < 0 ){
			     		
						l_address += 0x100000000L;
			     	}
					
					long	start 	= l_address & 0xffffff00;
					long	end		= start+256;
					
					int	hits = 0;
					
					for (long i=start;i<end;i++){
						
						Integer	a = new Integer((int)i);
						
						if ( bannedIps.get(a) != null ){
							
							hits++;
						}
					}
									
					int	hit_limit = COConfigurationManager.getIntParameter("Ip Filter Ban Block Limit");
					
					if ( hits >= hit_limit ){
						
						block_ban	= true;
						
						for (long i=start;i<end;i++){
							
							Integer	a = new Integer((int)i);
							
							if ( bannedIps.get(a) == null ){
								
								BannedIpImpl	new_block_ban = new BannedIpImpl( PRHelpers.intToAddress((int)i), torrent_name + " [block ban]" );
								
								new_bans.add( new_block_ban );

								bannedIps.put( a, new_block_ban );
							}
						}
					}
				}
				
				saveBannedIPs();
			}
		}finally{
			
			class_mon.exit();
		}
		
		List	listeners_ref = listeners;
		
		for (int i=0;i<new_bans.size();i++){
			
			BannedIp entry	= (BannedIp)new_bans.get(i);
			
			for (int j=0;j<listeners_ref.size();j++){
				
				try{
					((IPFilterListener)listeners_ref.get(j)).IPBanned( entry );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		return( block_ban );
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
			
			saveBannedIPs();
			
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
      
			num_ips_blocked 			= 0;
			num_ips_blocked_loggable	= 0;
			
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
	
	public long
	getTotalAddressesInRange()
	{
		return( range_manager.getTotalSpan());
	}
	
	public void
	addListener(
		IPFilterListener	l )
	{
		try{
			class_mon.enter();
		
			List	new_listeners = new ArrayList( listeners );
			
			new_listeners.add( l );
			
			listeners	= new_listeners;
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public void
	removeListener(
		IPFilterListener	l )
	{
		try{
			class_mon.enter();
		
			List	new_listeners = new ArrayList( listeners );
			
			new_listeners.remove( l );
			
			listeners	= new_listeners;
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		IpFilterImpl	filter = new IpFilterImpl();
		
		filter.ban( "255.1.1.1", "parp" );
		filter.ban( "255.1.1.2", "parp" );
		filter.ban( "255.1.2.2", "parp" );
		
		System.out.println( "is banned:" + filter.isBanned( "255.1.1.4" ));
	}
}
