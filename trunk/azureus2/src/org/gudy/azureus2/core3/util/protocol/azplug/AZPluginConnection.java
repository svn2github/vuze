/*
 * Created on 06-Mar-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.util.protocol.azplug;

import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;


import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author parg
 *
 */

public class 
AZPluginConnection
	extends HttpURLConnection
{
	private InputStream		input_stream;
		
	protected
	AZPluginConnection(
		URL		_url )
	{
		super( _url );
	}
	
	public void
	connect()
		throws IOException
		
	{
		String url = getURL().toString();
		
		int	pos = url.indexOf( "?" );
		
		if ( pos == -1 ){
			
			throw( new IOException( "Malformed URL - ? missing" ));
		}
		
		url = url.substring( pos+1 );
		
		String[]	bits = url.split( "&" );
		
		Map args = new HashMap();
		
		for (int i=0;i<bits.length;i++ ){
			
			String	bit = bits[i];
			
			String[] x = bit.split( "=" );
			
			if ( x.length == 2 ){
				
				String	lhs = x[0];
				String	rhs = URLDecoder.decode(x[1], "UTF-8" );
				
				args.put( lhs.toLowerCase(), rhs );
			}
		}
		
		String	plugin_id = (String)args.get( "id" );
		
		if ( plugin_id == null ){
			
			throw( new IOException( "Plugin id missing" ));
		}
		
		String	plugin_name = (String)args.get( "name" );
		String	arg			= (String)args.get( "arg" );
		
		String plugin_str = plugin_id + (plugin_name==null?"":( " (" + plugin_name + ")" ));
		
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( plugin_id );
		
		if ( pi == null ){
			
			throw( new IOException( "Plugin id " + plugin_str + " not installed" ));
		}
		
		IPCInterface ipc = pi.getIPC();
		
		try{
			input_stream = (InputStream)ipc.invoke( "handleURLProtocol", new String[]{ arg });
			
		}catch( IPCException e ){
			
			throw( new IOException( "Communication error with plugin '" + plugin_str + "': " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	public InputStream
	getInputStream()
	
		throws IOException
	{
		return( input_stream );
	}
	
	public int
	getResponseCode()
	{
		return( HTTP_OK );
	}
	
	public String
	getResponseMessage()
	{
		return( "OK" );
	}
	
	public boolean
	usingProxy()
	{
		return( false );
	}
	
	public void
	disconnect()
	{
	}
}
