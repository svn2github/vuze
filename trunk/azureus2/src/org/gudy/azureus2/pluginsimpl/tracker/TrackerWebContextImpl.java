/*
 * File    : TrackerWebContextImpl.java
 * Created : 23-Jan-2004
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
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TrackerWebContextImpl 
	implements TrackerWebContext, TRTrackerServerListener
{
	protected TrackerImpl	tracker;
	
	protected List			generators 	= new ArrayList();
	
	public 
	TrackerWebContextImpl(
		TrackerImpl	_tracker,
		int			port,
		int			protocol )
	
		throws TrackerException
	{
		tracker	= _tracker;
		
		try{
			TRTrackerServer	server;
			
			if ( protocol == Tracker.PR_HTTP ){
				
				server = TRTrackerServerFactory.create( TRTrackerServerFactory.PR_TCP, port );
				
			}else{
				
				server = TRTrackerServerFactory.createSSL( TRTrackerServerFactory.PR_TCP, port );
			}
			
			server.addListener( this );
			
		}catch( TRTrackerServerException e ){
			
			throw( new TrackerException("TRTrackerServerFactory failed", e ));
		}
	}
	
	public void
	addPageGenerator(
			TrackerWebPageGenerator	generator )
	{
		synchronized( this ){
			
			generators.add( generator );
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
		TrackerWebPageRequestImpl	request = new TrackerWebPageRequestImpl( tracker, _client_address, _url, _header, _is );
		TrackerWebPageResponseImpl	reply 	= new TrackerWebPageResponseImpl( _os );
		
		for (int i=0;i<generators.size();i++){
	
			//try{
				if (((TrackerWebPageGenerator)generators.get(i)).generate( request, reply )){
					
					reply.complete();
					
					// System.out.println( "process ok for '" + _url + "'" );
					
					return( true );
				}
			//}catch( Throwable e ){
				
				// System.out.println( "processing failed for '" + _url + "'" );
			//}
		}
		
		return( false );
	}	
	
	public boolean
	permitted(
		byte[]	hash,
		boolean	explicit )
	{
		return( false );
	}
	
	public boolean
	denied(
		byte[]	hash,
		boolean	explicit )
	{
		return( false );
	}
}
