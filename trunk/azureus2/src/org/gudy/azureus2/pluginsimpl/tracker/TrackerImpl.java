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

package org.gudy.azureus2.pluginsimpl.tracker;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
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
	
	/**
	 * adds an identificator to the tracker
	 * @param indentificator the Identificator
	 */
	
	public void 
	addTrackerIdentificator(
			Identificator identificator)
	{
	}
	
	/**
	 * adds a stats listener to the tracker
	 * @param listener
	 */
	
	public void 
	addTrackerStatsListener(StatsListener listener){}
	
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
	
	public void
	addPageGenerator(
		TrackerWebPageGenerator	generator )
	{
		synchronized( this ){
			
			generators.add( generator );
		}
	}
	
	public synchronized void
	torrentAdded(
		TRHostTorrent		t )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TrackerListener)listeners.get(i)).torrentAdded(new TrackerTorrentImpl(t));
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
		String			_url,
		OutputStream	_os )
	
		throws IOException
	{	
		TrackerWebPageRequestImpl	request = new TrackerWebPageRequestImpl( this, _url );
		TrackerWebPageResponseImpl	reply 	= new TrackerWebPageResponseImpl( _os );
		
		synchronized( this ){
			
			for (int i=0;i<generators.size();i++){
				
				if (((TrackerWebPageGenerator)generators.get(i)).generate( request, reply )){
					
					reply.complete();
					
					return( true );
				}
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
