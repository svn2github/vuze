/*
 * File    : TrackerImpl.java
 * Created : 08-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.tracker;

/**
 * @author parg
 *
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
TrackerImpl
	extends		TrackerWCHelper
	implements 	Tracker, TRHostListener, TRHostAuthenticationListener
{
	protected static TrackerImpl	tracker;
	
	protected List	listeners	= new ArrayList();
	
	protected TRHost		host;
	
	protected List	auth_listeners	= new ArrayList();
	
	
	public static synchronized Tracker
	getSingleton()
	{
		if ( tracker == null ){
						
			tracker	= new TrackerImpl( TRHostFactory.getSingleton());
		}		
		
		return( tracker );
	}
	
	protected
	TrackerImpl(
		TRHost		_host )
	{
		setTracker( this );
		
		host		= _host;
				
		host.addListener( this );
	}
	
	public String
	getName()
	{
		return( host.getName());
	}
	
	public URL[]
	getURLs()
	{
		String	tracker_host = COConfigurationManager.getStringParameter( "Tracker IP", "" );

		List	urls = new ArrayList();
		
		if ( tracker_host.length() > 0 ){
			
			if ( COConfigurationManager.getBooleanParameter( "Tracker Port Enable", true )){
										
				int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
				
				try{
					urls.add( new URL( "http://" + tracker_host + ":" + port + "/announce" ));
					
				}catch( MalformedURLException e ){
					
					e.printStackTrace();
				}
			}
			
			if ( COConfigurationManager.getBooleanParameter( "Tracker Port SSL Enable", true )){
				
				int port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
				
				try{
					urls.add( new URL( "https://" + tracker_host + ":" + port + "/announce" ));
				
				}catch( MalformedURLException e ){
				
					e.printStackTrace();
				}
			}
			
			if ( COConfigurationManager.getBooleanParameter( "Tracker Port UDP Enable" )){
				
				int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
				
				boolean	auth = COConfigurationManager.getBooleanParameter( "Tracker Password Enable Torrent" );
					
				try{
					urls.add( new URL( "udp://" + tracker_host + ":" + port + "/announce" +
										(auth?"?auth":"" )));
				
				}catch( MalformedURLException e ){
				
					e.printStackTrace();
				}
			}
		}
		
		URL[]	res = new URL[urls.size()];
		
		urls.toArray( res );
		
		return( res );
	}

	public TrackerTorrent
	host(
		Torrent		_torrent,
		boolean		_persistent )
	
		throws TrackerException
	{
		TorrentImpl	torrent = (TorrentImpl)_torrent;
		
		try{
			return( new TrackerTorrentImpl( host.hostTorrent( torrent.getTorrent(), _persistent )));
			
		}catch( Throwable e ){
			
			throw( new TrackerException( "Tracker: host operation fails", e ));
		}
	}
	
	public TrackerTorrent[]
	getTorrents()
	{
		TRHostTorrent[]	hts = host.getTorrents();
		
		TrackerTorrent[]	res = new TrackerTorrent[hts.length];
		
		for (int i=0;i<hts.length;i++){
			
			res[i] = new TrackerTorrentImpl(hts[i]);
		}
		
		return( res );
	}
	
	public TrackerWebContext
	createWebContext(
		int		port,
		int		protocol )
	
		throws TrackerException
	{
		return( new TrackerWebContextImpl( this, null, port, protocol ));
	}
	
	public TrackerWebContext
	createWebContext(
		String	name,
		int		port,
		int		protocol )
	
		throws TrackerException
	{
		return( new TrackerWebContextImpl( this, name, port, protocol ));
	}
	
	public synchronized void
	torrentAdded(
		TRHostTorrent		t )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TrackerListener)listeners.get(i)).torrentAdded(new TrackerTorrentImpl(t));
		}
	}
	
	public void
	torrentChanged(
		TRHostTorrent		t )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TrackerListener)listeners.get(i)).torrentChanged(new TrackerTorrentImpl(t));
		}
	}
	

	public synchronized void
	torrentRemoved(
		TRHostTorrent		t )	
	{	
		for (int i=0;i<listeners.size();i++){
			
			((TrackerListener)listeners.get(i)).torrentRemoved(new TrackerTorrentImpl(t));
		}
	}
	

	public synchronized void
	addListener(
		TrackerListener		listener )
	{
		listeners.add( listener );
		
		TrackerTorrent[] torrents = getTorrents();
		
		for (int i=0;i<torrents.length;i++){
			
			listener.torrentAdded( torrents[i]);
		}
	}
	
	public synchronized void
	removeListener(
		TrackerListener		listener )
	{
		listeners.remove( listener );
	}
	
	public boolean
	authenticate(
		URL			resource,
		String		user,
		String		password )
	{
		for (int i=0;i<auth_listeners.size();i++){
			
			try{
				boolean res = ((TrackerAuthenticationListener)auth_listeners.get(i)).authenticate( resource, user, password );
				
				if ( res ){
					
					return(true );
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		return( false );
	}
	
	public byte[]
	authenticate(
		URL			resource,
		String		user )
	{
		for (int i=0;i<auth_listeners.size();i++){
			
			try{
				byte[] res = ((TrackerAuthenticationListener)auth_listeners.get(i)).authenticate( resource, user );
				
				if ( res != null ){
					
					return( res );
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		return( null );
	}
	
	public synchronized void
	addAuthenticationListener(
		TrackerAuthenticationListener	l )
	{	
		auth_listeners.add(l);
		
		if ( auth_listeners.size() == 1 ){
			
			host.addAuthenticationListener( this );
		}
	}
	
	public synchronized void
	removeAuthenticationListener(
		TrackerAuthenticationListener	l )
	{	
		auth_listeners.remove(l);
		
		if ( auth_listeners.size() == 0 ){
				
			host.removeAuthenticationListener( this );
		}
	}
}
