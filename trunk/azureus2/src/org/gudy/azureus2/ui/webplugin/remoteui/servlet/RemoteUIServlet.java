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
import java.util.zip.*;
import java.util.jar.*;
import java.net.URL;

import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.pluginsimpl.remote.*;
import org.gudy.azureus2.ui.webplugin.util.*;

public class 
RemoteUIServlet
	extends WebPlugin
{
	static String[] resource_icon_names = {
			//"ui/icons/start2.png",
			"ui/icons/openFolder16x12.gif",
			"ui/icons/start.gif",
			"ui/icons/forcestart.gif",
			"ui/icons/stop.gif",
			"ui/icons/delete.gif",
			"ui/icons/recheck.gif",		
			"ui/icons/up.gif",		
			"ui/icons/down.gif",		
	};
	
	static String[] resource_names = {
			
		"ui/common/UIImageRepository.class",
		
		"ui/swing/UISwingImageRepository.class",	
		
		"ui/swt/views/AbstractIView.class",
		"ui/swt/views/IView.class",
		"ui/swt/IconBarEnabler.class",
		
		"ui/webplugin/remoteui/applet/RemoteUIApplet.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanelAdaptor.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadModel.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView.class",
		"ui/webplugin/remoteui/applet/view/TableSorter.class",
		"ui/webplugin/remoteui/applet/view/TableMap.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$1.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$2.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$3.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$4.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$5.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$6.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$7.class",
		"ui/webplugin/remoteui/applet/view/TableSorter$1.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$1.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$2.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$3.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$4.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$5.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$1.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModelListener.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModel.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$6.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$7.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$8.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$9.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$1.class",
		"ui/webplugin/remoteui/applet/view/VWGridBagConstraints.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$3.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$2.class",
		"ui/webplugin/remoteui/applet/view/VWStatusAreaView.class",
		"ui/webplugin/remoteui/applet/view/VWLabel.class",
		"ui/webplugin/remoteui/applet/model/MDStatusAreaModel.class",
		"ui/webplugin/remoteui/applet/view/VWStatusEntryBorder.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModelPropertyChangeEvent.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$10.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$11.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$12.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadViewListener.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadSplitModel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFilter.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFullModel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFilterModel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadSplitModel$1.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadFilterModel$1.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadSplitModel$2.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$13.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$14.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$15.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$2.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$3.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$4.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$5.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$6.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$intValueAdapter.class",

		"ui/webplugin/util/WUJarReader.class",
		
		"core3/config/COConfigurationManager.class",
		"core3/config/impl/ConfigurationManager.class",
		"core3/config/impl/ConfigurationParameterNotFoundException.class",
		"core3/util/DisplayFormatters.class",
		"core3/config/ParameterListener.class",
		"core3/util/DisplayFormatters$1.class",
		"core3/util/Semaphore.class",
		"core3/torrentdownloader/TorrentDownloaderException.class",
		"core3/torrent/TOTorrentException.class",
		"core3/util/SystemTime.class",
		"core3/util/SystemTime$1.class",
		"core3/util/DisplayFormatters$2.class",		
		
		"pluginsimpl/remote/RPRequestDispatcher.class",
		"pluginsimpl/remote/RPException.class",
		"pluginsimpl/remote/RPFactory.class",
		"pluginsimpl/remote/RPRequest.class",
		"pluginsimpl/remote/RPRequestHandler.class",		
		"pluginsimpl/remote/RPObject.class",
		"pluginsimpl/remote/RPReply.class",
		"pluginsimpl/remote/RPPluginInterface.class",
		"pluginsimpl/remote/download/RPDownloadManager.class",
		"pluginsimpl/remote/download/RPDownload.class",
		"pluginsimpl/remote/torrent/RPTorrent.class",
		"pluginsimpl/remote/download/RPDownloadStats.class",
		"pluginsimpl/remote/download/RPDownloadAnnounceResult.class",
		"pluginsimpl/remote/download/RPDownloadScrapeResult.class",
		"pluginsimpl/remote/RPPluginConfig.class",
		"pluginsimpl/remote/torrent/RPTorrentManager.class",
		"pluginsimpl/remote/torrent/RPTorrentDownloader.class",
		"pluginsimpl/remote/ipfilter/RPIPFilter.class",

		"plugins/download/DownloadManager.class",
		"plugins/PluginInterface.class",
		"plugins/PluginListener.class",
		"plugins/PluginView.class",
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
		"plugins/download/DownloadException.class",
		"plugins/download/Download.class",
		"plugins/download/DownloadManagerListener.class",
		"plugins/torrent/Torrent.class",
		"plugins/download/DownloadStats.class",
		"plugins/download/DownloadScrapeResult.class",
		"plugins/download/DownloadAnnounceResult.class",
		"plugins/download/DownloadRemovalVetoException.class",
		"plugins/download/DownloadListener.class",
		"plugins/download/DownloadTrackerListener.class",
		"plugins/download/DownloadWillBeRemovedListener.class",
		"plugins/torrent/TorrentFile.class",
		"plugins/torrent/TorrentException.class",
		"plugins/ui/tables/mytorrents/PluginMyTorrentsItemFactory.class",
		"plugins/PluginException.class",
		"plugins/torrent/TorrentManager.class",
		"plugins/torrent/TorrentDownloader.class",
		"plugins/ipfilter/IPFilter.class",
		"plugins/torrent/TorrentAnnounceURLList.class",
		"plugins/utils/Utilities.class",
		"plugins/download/DownloadPeerListener.class",
		"plugins/ipfilter/IPFilterException.class",
		"plugins/ipfilter/IPRange.class",
		"plugins/ipfilter/IPBlocked.class",
		"plugins/Plugin.class",
		"plugins/PluginManager.class",
		"plugins/ui/UIManager.class",
		"plugins/utils/resourcedownloader/ResourceDownloaderException.class",
		"plugins/utils/ShortCuts.class",
		"plugins/update/UpdateManager.class",


	};
	
	protected RPRequestHandler		request_handler;
	
	public
	RemoteUIServlet()
	{
		super();
	}
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		super.initialize( _plugin_interface );
		
		request_handler = new RPRequestHandler( _plugin_interface );
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL();
		
		if ( url.equals( "/remui.jar" ) || url.equals( "/remuiicons.jar" )){
			
			JarOutputStream	jos = null;
			
			try{
				jos = new JarOutputStream( response.getOutputStream());
			
				WUJarBuilder.buildFromResources( 
						jos, 
						plugin_interface.getPluginClassLoader(), 
						"org/gudy/azureus2", 
						url.equals( "/remui.jar")?resource_names:resource_icon_names );
				
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
				
				rp_request.setClientIP( request.getClientAddress());
				
				// System.out.println( "RemoteUIServlet:got request: " + rp_request.getString());
				
				RPReply	reply = request_handler.processRequest( rp_request );
				
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
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
			}finally{
				
				if ( dis != null ){
					
					dis.close();
				}
			}
		}else if ( url.equals( "/upload.cgi")){

			// -----------------------------7d4f2a310bca
			//Content-Disposition: form-data; name="upfile"; filename="C:\Temp\(HH)-Demon Beast Resurrection 1-2.torrent"
			//Content-Type: application/octet-stream
			//
			// <data>
			// -----------------------------7d4f2a310bca
						
			InputStream	is = request.getInputStream();
			
			try{
				
				String	content = "";
				
				while( true ){
					
					byte[]	buffer = new byte[1024];
					
					int	len = is.read(buffer);
					
					if ( len <= 0 ){
						
						break;
					}
					
					content += new String(buffer, 0,len, "ISO-8859-1" );
				}
				
				PrintWriter pw = new PrintWriter( new OutputStreamWriter( response.getOutputStream()));
				
				try{

					int	sep1 = content.indexOf( "\r\n" );
					
					String	tag = content.substring(0,sep1);
					
					int	sep2 = content.indexOf( "\r\n\r\n");
					
					int	data_start 	= sep2 + 4;
					int data_end	= content.indexOf(tag, data_start );
					
					String	str_data = content.substring(data_start, data_end);
					
					// System.out.println("ds = "+ data_start + ", de = " + data_end + ", content = " + content);
					
					Torrent	torrent	= null;
					
					if ( data_end - data_start <= 2 ){
						
						int	fn_start = content.indexOf( "filename=\"");
						
						if ( fn_start != -1 ){
							
							fn_start += 10;
							
							int	fn_end	= content.indexOf( "\"", fn_start );
							
							if ( fn_end != -1 ){
								
								String	file_name = content.substring( fn_start, fn_end );
																							
								if ( file_name.toLowerCase().startsWith("http")){
									
									file_name = file_name.replaceAll( " ", "%20");
									
									TorrentDownloader dl = plugin_interface.getTorrentManager().getURLDownloader( new URL( file_name ));
									
									torrent = dl.download();									
								}
							}
						}
					}
					
					if ( torrent == null ){
						
						byte[]	data = str_data.getBytes("ISO-8859-1");
			
						torrent = plugin_interface.getTorrentManager().createFromBEncodedData( data );
					}
					
					plugin_interface.getDownloadManager().addDownload( torrent );
				
					pw.println("<HTML><BODY><P><FONT COLOR=#00CC44>Upload OK</FONT></P></BODY></HTML>");
				
				}catch( Throwable e ){
					
					String	message_chain = "";
					
					Throwable	temp = e;
					
					while( temp != null ){
						
						String	this_message = temp.getMessage();
						
						if ( this_message != null ){
							
							message_chain += (message_chain.length()==0?"":"\n") + this_message;
						}
						
						temp = temp.getCause();
					}
								
					String	message = message_chain.length()==0?e.toString():message_chain;
						

					pw.println("<HTML><BODY><P><FONT COLOR=#FF0000>Upload Failed: " + message + "</FONT></P></BODY></HTML>");
					
				}finally{
					
					pw.close();
				}
				
				return( true );
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
			
		}
		
		return( false );
	}
}

