/*
 * File    : RPRequest.java
 * Created : 28-Jan-2004
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

package org.gudy.azureus2.pluginsremote;

import java.io.Serializable;

/**
 * @author parg
 *
 */
public class 
RPRequest
	implements Serializable
{
	public RPObject	object;
	public String	method;
	public Object	params;
	
	public long		connection_id;
	public long		request_id;
	
		// public constructor for XML deserialiser
	
	public
	RPRequest()
	{
	}
	
	public
	RPRequest(
		RPObject			_object,
		String				_method,
		Object				_params )
	{
		object		= _object;
		method		= _method;
		params		= _params;
		
		if ( object != null ){
			
			RPPluginInterface	pi = object.getDispatcher().getPlugin();
			
			connection_id	= pi._getConectionId();
			request_id		= pi._getNextRequestId();
		}
	}
	
	public long
	getConnectionId()
	{
		return( connection_id );
	}
	
	public long
	getRequestId()
	{
		return( request_id );
	}
	
	public String
	getString()
	{
		return( "obj=" + object+", method=" + method + ",params=" + params );
	}
	
	public RPObject
	getObject()
	{
		return( object );
	}
	
	public String
	getMethod()
	{
		return( method );
	}
	
	public Object
	getParams()
	{
		return( params );
	}
}
