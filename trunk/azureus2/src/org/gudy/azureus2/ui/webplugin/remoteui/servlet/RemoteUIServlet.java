/*
 * File    : RemoteUIServlet.java
 * Created : 27-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.servlet;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.ui.webplugin.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.gudy.azureus2.plugins.tracker.web.*;

import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;

public class 
RemoteUIServlet
	extends WebPlugin
{
	protected Map	reply_cache	= new HashMap();
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL();
		
		if ( url.equals( "/process.cgi")){
	
			ObjectInputStream	dis = null;
			
			try{
				dis = new ObjectInputStream( new GZIPInputStream(request.getInputStream()));
								
				RPRequest	rp_request = (RPRequest)dis.readObject();
				
				System.out.println( "RemoteUIServler:got request: " + rp_request.getString());
				
				RPReply	reply = processRequest( rp_request );
				
				if ( reply == null ){
					
					reply = new RPReply( null );
				}
				
				response.setContentType( "application/octet-stream" );
				
				ObjectOutputStream	oos = new ObjectOutputStream(new GZIPOutputStream(response.getOutputStream()));
				
				try{
					oos.writeObject( reply );
				
				}finally{
					
					oos.close();
				}
				
				return( true );
				
			}catch( ClassNotFoundException e ){
				
				e.printStackTrace();
				
			}finally{
				
				if ( dis != null ){
					
					dis.close();
				}
			}
		}
		
		return( false );
	}
	
	protected RPReply
	processRequest(
		RPRequest		request )
	{
		Long	connection_id 	= new Long( request.getConnectionId());

		replyCache	cached_reply = (replyCache)reply_cache.get(connection_id);
		
		if ( cached_reply != null ){
			
			if ( cached_reply.getId() == request.getRequestId()){
				
				return( cached_reply.getReply());
			}
		}
		
		RPReply	reply = processRequestSupport( request );
		
		reply_cache.put( connection_id, new replyCache( request.getRequestId(), reply ));
		
		return( reply );
	}
	

	protected RPReply
	processRequestSupport(
		RPRequest		request )
	{
		try{
			RPObject		object 	= request.getObject();
			String			method	= request.getMethod();
			
			if ( method.equals( "getSingleton")){
				
				RPReply reply = new RPReply( RPPluginInterface.create(plugin_interface));
				
				return( reply );
				
			}else{
				System.out.println( "Request: con = " + request.getConnectionId() + ", req = " + request.getRequestId());
				object._setLocal();
				
				if ( method.equals( "_refresh" )){
				
					RPReply	reply = new RPReply( object );
				
					return( reply );
					
				}else{
								
					return( object._process( request ));
				}
			}
		}catch( RPException e ){
			
			return( new RPReply( e ));
			
		}catch( Throwable e ){
			
			return( new RPReply( new RPException( "server execution fails", e )));
		}
	}
	
	protected static class
	replyCache
	{
		protected long		id;
		protected RPReply	reply;
		
		protected
		replyCache(
			long		_id,
			RPReply		_reply )
		{
			id		= _id;
			reply	= _reply;
		}
		
		protected long
		getId()
		{
			return( id );
		}
		
		protected RPReply
		getReply()
		{
			return( reply );
		}
	}
}
