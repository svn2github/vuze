/*
 * Created on 10-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
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
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;

public abstract class 
TrackerWCHelper 
	implements TrackerWebContext, TRHostAuthenticationListener, TRTrackerServerAuthenticationListener
{
	protected Tracker	tracker;
	protected List		generators 	= new ArrayList();

	protected
	TrackerWCHelper()
	{
	}
	
	protected void
	setTracker(
		Tracker		_tracker )
	{
		tracker	= _tracker;
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
	
	
	public TrackerWebPageGenerator[]
	getPageGenerators()
	{
		TrackerWebPageGenerator[]	res = new TrackerWebPageGenerator[generators.size()];
		
		generators.toArray( res );
		
		return( res );
	}
	
	public synchronized void
	addPageGenerator(
		TrackerWebPageGenerator	generator )
	{			
		generators.add( generator );
	}
	
	public void
	removePageGenerator(
		TrackerWebPageGenerator	generator )
	{
		generators.remove( generator );
	}	
}
