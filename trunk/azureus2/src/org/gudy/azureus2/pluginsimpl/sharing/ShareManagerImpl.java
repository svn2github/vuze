/*
 * File    : ShareManagerImpl.java
 * Created : 30-Dec-2003
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.sharing;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
ShareManagerImpl
	implements ShareManager
{
	protected static ShareManagerImpl	singleton;
	
	public synchronized static ShareManagerImpl
	getSingleton()
	
		throws ShareException
	{
		if ( singleton == null ){
			
			singleton = new ShareManagerImpl();
		}
		
		return( singleton );
	}
	
	protected URL				announce_url;
	protected ShareConfigImpl	config;
	
	protected Set				shares = new TreeSet();	// ensures uniqueness of resources
	
	protected
	ShareManagerImpl()
	
		throws ShareException
	{
		String 	tracker_ip 		= COConfigurationManager.getStringParameter("Tracker IP", "");

		if ( tracker_ip.length() == 0 ){
			
			throw( new ShareException( "ShareManager: tracker must be configured"));
		}
		
		// port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
								
		int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
				
		try{
			announce_url = new URL( "http://" + tracker_ip + ":" + port + "/announce" );
			
		}catch( MalformedURLException e ){
			
			throw( new ShareException( "Announce URL invalid", e ));
		}
		
		config = new ShareConfigImpl();
		
		config.loadConfig(this);
	}
	
	protected void
	deserialiseResource(
		Map					map )
	{
		try{
			ShareResourceImpl	res = null;
			
			int	type = ((Long)map.get("type")).intValue();
			
			if ( type == ShareResource.ST_FILE ){
				
				res = ShareResourceFileImpl.deserialiseResource( this, map );
			}
			
			if ( res != null ){
				
				shares.add( res );
			}
		}catch( ShareException e ){
			
			e.printStackTrace();
		}
	}
	
	protected URL
	getAnnounceURL()
	{
		return( announce_url );
	}
	
	public ShareResource[]
	getShares()
	{
		ShareResource[]	res = new ShareResource[shares.size()];
		
		shares.toArray( res );
		
		return( res );
	}
	
	public ShareResourceFile
	addFile(
		File	file )
	
		throws ShareException
	{
		ShareResourceFile res = new ShareResourceFileImpl( this, file );
		
		shares.add( res );
		
		config.saveConfig();
		
		return( res );
	}
	
	public ShareResourceDir
	addDir(
		File	dir )
	{
		return( null );
		
	}
	
	public ShareResourceDirContents
	addDirContents(
		File	dir,
		boolean	recursive )
	{
		return( null );
		
	}	
}
