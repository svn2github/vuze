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
import java.util.jar.*;

import org.gudy.azureus2.plugins.tracker.web.*;

import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;
import org.gudy.azureus2.ui.webplugin.util.*;

public class 
RemoteUIServlet
	extends WebPlugin
{
	static String[] resource_names = {
		"ui/webplugin/remoteui/applet/RemoteUIApplet.class",
		"ui/webplugin/remoteui/plugins/RPRequestDispatcher.class",
		"ui/webplugin/remoteui/plugins/RPException.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanelListener.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel.class",
		"plugins/download/DownloadManager.class",
		"core3/config/COConfigurationManager.class",
		"core3/config/impl/ConfigurationManager.class",
		"core3/config/impl/ConfigurationParameterNotFoundException.class",
		"ui/webplugin/remoteui/plugins/RPFactory.class",
		"ui/webplugin/remoteui/plugins/RPRequest.class",
		"ui/webplugin/remoteui/plugins/RPObject.class",
		"ui/webplugin/remoteui/plugins/RPReply.class",
		"ui/webplugin/remoteui/plugins/RPPluginInterface.class",
		"plugins/PluginInterface.class",
		"plugins/PluginListener.class",
		"plugins/PluginView.class",
		"ui/swt/views/AbstractIView.class",
		"ui/swt/views/IView.class",
		"ui/swt/IconBarEnabler.class",
		"plugins/ui/config/Parameter.class",
		"plugins/ui/tables/peers/PluginPeerItemFactory.class",
		"plugins/ui/config/ConfigSection.class",
		"plugins/tracker/Tracker.class",
		"plugins/tracker/web/TrackerWebContext.class",
		"plugins/logging/Logger.class",
		"plugins/peers/protocol/PeerProtocolManager.class",
		"plugins/sharing/ShareManager.class",
		"plugins/sharing/ShareException.class",
		"plugins/PluginConfig.class",
		"plugins/ui/config/PluginConfigUIFactory.class",
		"plugins/PluginEventListener.class",
		"ui/webplugin/remoteui/plugins/download/RPDownloadManager.class",
		"plugins/download/DownloadException.class",
		"plugins/download/Download.class",
		"plugins/download/DownloadManagerListener.class",
		"plugins/torrent/Torrent.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadModel.class",
		"ui/webplugin/remoteui/plugins/download/RPDownload.class",
		"plugins/download/DownloadStats.class",
		"plugins/download/DownloadScrapeResult.class",
		"plugins/download/DownloadAnnounceResult.class",
		"plugins/download/DownloadRemovalVetoException.class",
		"ui/webplugin/remoteui/plugins/torrent/RPTorrent.class",
		"ui/webplugin/remoteui/plugins/download/RPDownloadStats.class",
		"ui/webplugin/remoteui/plugins/download/RPDownloadAnnounceResult.class",
		"ui/webplugin/remoteui/plugins/download/RPDownloadScrapeResult.class",
		"plugins/download/DownloadListener.class",
		"plugins/download/DownloadTrackerListener.class",
		"plugins/download/DownloadWillBeRemovedListener.class",
		"plugins/torrent/TorrentFile.class",
		"plugins/torrent/TorrentException.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView.class",
		"ui/webplugin/remoteui/applet/view/TableSorter.class",
		"ui/webplugin/remoteui/applet/view/TableMap.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$1.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$2.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$3.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$4.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$5.class",
		"ui/webplugin/remoteui/applet/view/TableSorter$1.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$1.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$2.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$3.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$4.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$5.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$1.class",
		"core3/util/DisplayFormatters.class",
		"core3/config/ParameterListener.class",
		"core3/util/DisplayFormatters$1.class",
	};
	
	protected Map	reply_cache	= new HashMap();
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL();
		
		if ( url.equals( "/remui.jar")){
			
			JarOutputStream	jos = null;
			
			try{
				jos = new JarOutputStream( response.getOutputStream());
			
				WUJarBuilder.buildFromResources( 
						jos, 
						plugin_interface.getPluginClassLoader(), 
						"org/gudy/azureus2", resource_names );
				
				response.setContentType("application/java-archive");
				
				return( true );
				
			}finally{
				
				if ( jos != null ){

					jos.close();
				}
			}
		}else if ( url.equals( "/process.cgi")){
	
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
