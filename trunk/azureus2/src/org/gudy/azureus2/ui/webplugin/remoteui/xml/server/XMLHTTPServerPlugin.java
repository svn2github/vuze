/*
 * File    : XMLServerPlugin.java
 * Created : 13-Mar-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.xml.server;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.ui.webplugin.*;

import java.util.Properties;
import java.io.*;

import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.pluginsimpl.remote.*;

import org.gudy.azureus2.plugins.*;

public class 
XMLHTTPServerPlugin
	extends WebPlugin
{
	public static final int	DEFAULT_PORT	= 6884;
	
	protected static Properties	defaults = new Properties();
	
	static{
		
		defaults.put( WebPlugin.CONFIG_PORT, new Integer( DEFAULT_PORT ));
	}
	
	protected RPRequestHandler		request_handler;
	
	public
	XMLHTTPServerPlugin()
	{
		super(defaults);
	}
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		super.initialize( _plugin_interface );
			
		BasicPluginConfigModel	config = getConfigModel();
		
		request_handler = new RPRequestHandler( _plugin_interface );		
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL().toLowerCase();
		
		if ( url.equals( "process.cgi") || url.equals( "/process.cgi")){
	
			InputStream	is = null;
						
			try{
				XMLRequestProcessor processor = 
						new XMLRequestProcessor( request_handler, request.getClientAddress(), request.getInputStream(), response.getOutputStream());

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
