/*
 * File    : TrackerDefaultWeb.java
 * Created : 15-Apr-2004
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

package org.gudy.azureus2.ui.tracker;

/**
 * @author parg
 *
 */
import java.io.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;


public class 
TrackerDefaultWeb
	implements Plugin, TrackerWebPageGenerator
{
	protected static final String	NL			= "\r\n";
		
	protected PluginInterface		plugin_interface;
	protected Tracker				tracker;
	
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		// this is a built in one and hence initialised after the "normal" ones. Check to see if
		// there's a tracker web one already installed. If so we just do nothing
		
		PluginInterface[] plugins = plugin_interface.getPluginManager().getPlugins();
		
		for (int i=0;i<plugins.length;i++){
			
			PluginInterface	pi = plugins[i];
			
			String	plugin_name = (String)pi.getPluginProperties().getProperty( "name" );
			
			if ( plugin_name != null && plugin_name.equalsIgnoreCase("TrackerWeb")){
				
				return;
			}
			
			System.out.println("got plugin_name = " + plugin_name );
		}
		
		tracker = plugin_interface.getTracker();
		
		tracker.addPageGenerator( this );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		OutputStream	os = response.getOutputStream();
		
		PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os ));
		
		pw.println( "<HTML><TITLE>Plugin Required</TITLE>");
		pw.println( "<BODY><P>Due to re-factoring the tracker web pages are now available as a separate plugin. ");
		pw.println( "Please see <A href=\"http://azureus.sourceforge.net/plugin_list.php\">here</A> for details." );
		pw.println( "</BODY></HTML>");
		
		pw.flush();
		
		return( true );
	}
}
	