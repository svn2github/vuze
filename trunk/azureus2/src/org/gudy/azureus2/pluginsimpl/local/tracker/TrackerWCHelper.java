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
import java.net.URL;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.util.AEMonitor;

public abstract class 
TrackerWCHelper 
	implements TrackerWebContext, TRHostAuthenticationListener, TRTrackerServerAuthenticationListener
{
	private PluginInterface		plugin_interface;
	
	private Tracker		tracker;
	private List		generators 	= new ArrayList();

	protected AEMonitor this_mon 	= new AEMonitor( "TrackerWCHelper" );

	protected
	TrackerWCHelper()
	{
		plugin_interface = UtilitiesImpl.getPluginThreadContext();
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
		URL				_absolute_url,
		String			_header,
		InputStream		_is,
		OutputStream	_os )
	
		throws IOException
	{	
		UtilitiesImpl.setPluginThreadContext( plugin_interface );
		
		TrackerWebPageRequestImpl	request = new TrackerWebPageRequestImpl( tracker, this, _client_address, _url, _absolute_url, _header, _is );
		TrackerWebPageResponseImpl	reply 	= new TrackerWebPageResponseImpl( _os );
		
		for (int i=0;i<generators.size();i++){

			TrackerWebPageGenerator	generator;
			
			try{
				this_mon.enter();
				
				if ( i >= generators.size()){
					
					break;
				}
				
				generator = (TrackerWebPageGenerator)generators.get(i);
				
			}finally{
				
				this_mon.exit();
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
	
	public void
	addPageGenerator(
		TrackerWebPageGenerator	generator )
	{		
		try{
			this_mon.enter();
		
			generators.add( generator );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removePageGenerator(
		TrackerWebPageGenerator	generator )
	{
		try{
			this_mon.enter();
		
			generators.remove( generator );
			
		}finally{
			
			this_mon.exit();
		}
	}	
}
