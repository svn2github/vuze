/*
 * File    : WebPlugin.java
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

package org.gudy.azureus2.ui.webplugin;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.ui.model.*;


public class 
WebPlugin
	implements Plugin, TrackerWebPageGenerator
{
	public static final String DEFAULT_PORT		= "8089";
	public static final String DEFAULT_PROTOCOL	= "HTTP";
	public static final String DEFAULT_ACCESS	= "all";
	
	protected static final String	NL			= "\r\n";
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.php", "index.tmpl" };
	protected static File[]				welcome_files;
	
	protected PluginInterface		plugin_interface;
	protected LoggerChannel			log;
	protected Tracker				tracker;
	
	protected String				home_page;
	protected String				file_root;
	protected String				resource_root;
	
	protected boolean				ip_range_all	= false;
	protected IpRange				ip_range;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel("WebPlugin");

		final BasicPluginViewModel model = plugin_interface.getUIManager().createBasicPluginViewModel( plugin_interface.getPluginName());
		
		model.getStatus().setText( "Running" );
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	message )
				{
					model.getLogArea().appendText( message+"\n");
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					model.getLogArea().appendText( error.toString()+"\n");
				}
			});
		
		tracker = plugin_interface.getTracker();
	
		Properties	props = plugin_interface.getPluginProperties();
		
		home_page = (String)props.get("homepage");
		
		if ( home_page != null ){
			
			home_page = home_page.trim();
			
			if ( home_page.length() == 0 ){
				
				home_page = null;
				
			}else if (!home_page.startsWith("/" )){
			
				home_page = "/" + home_page;
			}
		}
		
		resource_root = (String)props.get("rootresource");
		
		if ( resource_root != null ){
			
			resource_root = resource_root.trim();
			
			if ( resource_root.length() == 0 ){
				
				resource_root = null;
				
			}else if ( resource_root.startsWith("/" )){
			
				resource_root = resource_root.substring(1);
			}
		}
		
		String	root_dir	= (String)props.get("rootdir");
		
		if ( root_dir != null ){
			
			root_dir = root_dir.trim();
		}
		
		if ( root_dir == null || root_dir.length() == 0 ){
			
			file_root = plugin_interface.getPluginDirectoryName();
			
			if ( file_root == null ){
				
				file_root = SystemProperties.getUserPath() + "web";
			}
		}else{
			
				// absolute or relative
			
			if ( root_dir.startsWith(File.separator) || root_dir.indexOf(":") != -1 ){
				
				file_root = root_dir;
				
			}else{
				
				file_root = SystemProperties.getUserPath() + "web" + File.separator + root_dir;
				
			}
		}

		File	f_root = new File( file_root );
		
		if ( !f_root.exists()){
	
			String	error = "WebPlugin: root dir '" + file_root + "' doesn't exist";
			
			log.log( LoggerChannel.LT_ERROR, error );
			
			throw( new PluginException( error ));
		}

		if ( !f_root.isDirectory()){
			
			String	error = "WebPlugin: root dir '" + file_root + "' isn't a directory";
			
			log.log( LoggerChannel.LT_ERROR, error );
			
			throw( new PluginException( error ));
		}
		
		welcome_files = new File[welcome_pages.length];
		
		for (int i=0;i<welcome_pages.length;i++){
			
			welcome_files[i] = new File( file_root + File.separator + welcome_pages[i] );
		}
		
					
		int port	= Integer.parseInt( props.getProperty( "port", DEFAULT_PORT ));

		String	protocol_str = props.getProperty( "protocol", DEFAULT_PROTOCOL );
		
		int	protocol = protocol_str.equalsIgnoreCase( "HTTP")?
							Tracker.PR_HTTP:Tracker.PR_HTTPS;
	
		log.log( LoggerChannel.LT_INFORMATION, "WebPlugin Initialisation: port = " + port + ", protocol = " + protocol_str + ", root = " + root_dir );
		
		String	access_str = props.getProperty( "access", DEFAULT_ACCESS ).trim();
		
		if ( access_str.length() > 7 && Character.isDigit(access_str.charAt(0))){
			
			ip_range	= IpFilter.getInstance().createRange(true);
			
			int	sep = access_str.indexOf("-");
				
			if ( sep == -1 ){
				
				ip_range.setStartIp( access_str );
				
				ip_range.setEndIp( access_str );
				
			}else{				
				
				ip_range.setStartIp( access_str.substring(0,sep).trim());
				
				ip_range.setEndIp( access_str.substring( sep+1 ).trim());
			}
			
			ip_range.checkValid();
			
			if (!ip_range.isValid()){
			
				log.log( LoggerChannel.LT_ERROR, "access parameter '" + access_str + "' is invalid" );
			
				ip_range	= null;
			}
		}else{
			
			if ( access_str.equalsIgnoreCase( "all" )){
								
				ip_range_all	= true;				
			}
		}
		
		log.log( 	LoggerChannel.LT_INFORMATION, 
					"acceptable IP range = " +
						( ip_range==null?
							(ip_range_all?"all":"local"):
							(ip_range.getStartIp() + " - " + ip_range.getEndIp())));
				
							
		try{
			TrackerWebContext	context = tracker.createWebContext( port, protocol );
		
			context.addPageGenerator( this );
	
			/*
			context.addAuthenticationListener(
				new TrackerAuthenticationAdapter()
				{
					public boolean
					authenticate(
						URL			resource,
						String		user,
						String		password )
					{
						System.out.println( "res:" + resource.toString() + ":" + user + "/" + password );
						
						return( false );
					}
				});
			*/
			
		}catch( TrackerException e ){
			
			log.log( "Plugin Initialisation Fails", e );
		}
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		return( false );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		if ( !ip_range_all ){
		
			String	client = request.getClientAddress();
			
			// System.out.println( "client = " + client );
			
			try{
				InetAddress ia = InetAddress.getByName( client );
				
				if ( ip_range == null ){
					
					if ( !ia.isLoopbackAddress()){
				
						log.log( LoggerChannel.LT_ERROR, "Client '" + client + "' is not local, rejecting" );
						
						return( false );
					}
				}else{
					
					if ( !ip_range.isInRange( ia.getHostAddress())){
						
						log.log( LoggerChannel.LT_ERROR, "Client '" + client + "' (" + ia.getHostAddress() + ") is not in range, rejecting" );
						
						return( false );
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				return( false );
			}
		}
		
		if ( request.getURL().toString().endsWith(".class")){
			
			System.out.println( "WebPlugin::generate:" + request.getURL());
		}
			
		if ( generateSupport( request, response )){
			
			return(true);
		}
		
		OutputStream os = response.getOutputStream();
		
		String	url = request.getURL();
		
		if (url.equals("/")){
			
			if (home_page != null ){
				
				url = home_page;
				
			}else{
			
				for (int i=0;i<welcome_files.length;i++){
					
					if ( welcome_files[i].exists()){
						
						url = "/" + welcome_pages[i];
						
						break;
					}
				}	
			}
		}
	
			// first try file system for data
		
		if ( response.useFile( file_root, url )){
			
			return( true );
		}
		
				// now try jars		
			
		String	resource_name = url;
		
		if (resource_name.startsWith("/")){
			
			resource_name = resource_name.substring(1);
		}
					
		int	pos = resource_name.lastIndexOf(".");
		
		if ( pos != -1 ){
			
			String	type = resource_name.substring( pos+1 );
		
			ClassLoader	cl = plugin_interface.getPluginClassLoader();
			
			InputStream is = cl.getResourceAsStream( resource_name );
		
			if ( is == null ){
				
				// failed absolute load, try relative
				
				if ( resource_root != null ){ 
					
					resource_name = resource_root + "/" + resource_name;
					
					is = cl.getResourceAsStream( resource_name );	
				}
			}
			
			// System.out.println( resource_name + "->" + is + ", url = " + url );
		
			if (is != null ){
			
				try{
					response.useStream( type, is );
				
				}finally{
				
					is.close();
				}
			
				return( true );
			}
		}
		
		return( false );
	}
}
