/*
 * File    : RPRequestHandler.java
 * Created : 15-Mar-2004
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

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsremote.download.*;

public class 
RPRequestHandler 
{
	protected PluginInterface	plugin_interface;
	
	protected boolean	view_mode;
	
	protected Map	reply_cache	= new HashMap();
	
	public
	RPRequestHandler(
		PluginInterface		_pi )
	{
		plugin_interface	= _pi;
		
		Properties properties				= plugin_interface.getPluginProperties();

		String	mode_str = (String)properties.get("mode");
		
		view_mode = mode_str != null && mode_str.trim().equalsIgnoreCase("view");
	}
	
	public RPReply
	processRequest(
		RPRequest		request )
	{
		Long	connection_id 	= new Long( request.getConnectionId());

		replyCache	cached_reply = connection_id.longValue()==0?null:(replyCache)reply_cache.get(connection_id);
		
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
			
			// System.out.println( "object = " + object + ", method = " + method );
				
			if ( object == null && method.equals("getSingleton")){
				
				RPReply reply = new RPReply( RPPluginInterface.create(plugin_interface));
				
				return( reply );
				
			}else if ( object == null && method.equals( "getDownloads")){
					
					// short cut method for quick access to downloads
					// used by GTS
				
				RPPluginInterface pi = RPPluginInterface.create(plugin_interface);
					
				RPDownloadManager dm = (RPDownloadManager)pi._process( new RPRequest(null, "getDownloadManager", null )).getResponse();
				
				RPReply	rep = dm._process(new RPRequest( null, "getDownloads", null ));
				
				rep.setProperty( "azureus_name", pi.azureus_name );
				
				rep.setProperty( "azureus_version", pi.azureus_version );
				
				return( rep );
			}else{
					// System.out.println( "Request: con = " + request.getConnectionId() + ", req = " + request.getRequestId());
				
				object = RPObject._lookupLocal( object._getOID());
				
					// _setLocal synchronizes the RP objects with their underlying 
					// plugin objects
				
				object._setLocal();
				
				if ( method.equals( "_refresh" )){
				
					RPReply	reply = new RPReply( object );
				
					return( reply );
					
				}else{
							
					if ( view_mode ){
						
						String	name = object._getName();
						
						System.out.println( "request: " + name + "/" + method );
						
						if ( name.equals( "Download" )){
							
							if ( 	method.equals( "start" ) ||
									method.equals( "stop" ) ||
									method.equals( "restart" ) ||
									method.equals( "remove")){
								
								throw( new RPException( "Access Denied" ));
							}
						}else if ( name.equals( "DownloadManager" )){
							
							if ( 	method.startsWith( "addDownload")){
								
								throw( new RPException( "Access Denied" ));
							}
						}else if ( name.equals( "TorrentManager" )){
							
							if ( 	method.startsWith( "getURLDownloader")){
								
								throw( new RPException( "Access Denied" ));
							}	
						}else if ( name.equals( "PluginConfig" )){
								
							if ( 	method.startsWith( "setParameter")){
									
								throw( new RPException( "Access Denied" ));
							}
						}					
					}
					
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
