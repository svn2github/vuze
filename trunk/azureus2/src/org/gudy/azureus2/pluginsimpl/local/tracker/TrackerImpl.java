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

import java.util.*;
import java.io.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
TrackerImpl
	implements Tracker, TRHostListener
{
	protected static TrackerImpl	tracker;
	
	protected List	generators 	= new ArrayList();
	protected List	listeners	= new ArrayList();
	
	protected TRHost		host;
	
	public static synchronized Tracker
	getSingleton(
		TRHost		host )
	{
		if ( tracker == null ){
			
			tracker	= new TrackerImpl( host );
		}		
		
		return( tracker );
	}
	
	protected
	TrackerImpl(
		TRHost		_host )
	{
		host		= _host;
		
		host.addListener( this );
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
		return( new TrackerWebContextImpl( this, port, protocol ));
	}
	
	public synchronized void
	addPageGenerator(
		TrackerWebPageGenerator	generator )
	{			
		generators.add( generator );
	}
	
	public TrackerWebPageGenerator[]
	getPageGenerators()
	{
		TrackerWebPageGenerator[]	res = new TrackerWebPageGenerator[generators.size()];
		
		generators.toArray( res );
		
		return( res );
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
	
	public boolean
	handleExternalRequest(
		String			_client_address,
		String			_url,
		String			_header,
		InputStream		_is,
		OutputStream	_os )
	
		throws IOException
	{	
		TrackerWebPageRequestImpl	request = new TrackerWebPageRequestImpl( this, _client_address, _url, _header, _is );
		TrackerWebPageResponseImpl	reply 	= new TrackerWebPageResponseImpl( _os );
		
		for (int i=0;i<generators.size();i++){

			TrackerWebPageGenerator	generator;
			
			synchronized( this ){
				
				if ( i >= generators.size()){
					
					break;
				}
				
				generator = (TrackerWebPageGenerator)generators.get(i);
			}
			
			if ( generator.generate( request, reply )){
					
				reply.complete();
					
				return( true );
			}
		}
		
		return( false );
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
}
