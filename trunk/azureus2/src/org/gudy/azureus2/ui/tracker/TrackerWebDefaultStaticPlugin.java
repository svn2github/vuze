/*
 * File    : TrackerWebDefaultStaticPlugin.java
 * Created : 09-Dec-2003
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


import java.net.*;
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.UIImageRepository;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

public class 
TrackerWebDefaultStaticPlugin
	implements Plugin, TrackerWebPageGenerator
{
	protected static final String	NL			= "\r\n";
	
	protected PluginInterface		plugin_interface;
	protected Tracker				tracker;
	
	protected String				file_root;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		tracker = plugin_interface.getTracker();
		
		file_root = FileUtil.getApplicationPath() + "web";
		
		
		tracker.addPageGenerator( this );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL();
					
		OutputStream	os = response.getOutputStream();
		
		String	target = file_root + url.replace('/',File.separatorChar);
		
		File canonical_file = new File(target).getCanonicalFile();
				
		System.out.println( "static request: " + canonical_file.toString());
		
			// make sure some fool isn't trying to use ../../ to escape from web dir
		
		if ( !canonical_file.toString().startsWith( file_root )){
			
			return( false );
		}
		
		if ( canonical_file.isDirectory()){
			
			return( false );
		}
		
		if ( canonical_file.canRead()){
			
			String str = canonical_file.toString().toLowerCase();
			
			if ( str.endsWith( ".html" ) || str.endsWith(".htm")){
				
				response.setContentType( "text/html");
				
				byte[]	buffer = new byte[4096];
				
				FileInputStream	fis = null;
				
				try{
					fis = new FileInputStream(canonical_file);
				
					while(true){
						
						int	len = fis.read(buffer);
						
						if ( len <= 0 ){
							
							break;
						}
						
						os.write( buffer, 0, len );
					}
				}finally{
					if ( fis != null ){
						
						fis.close();
					}
				}
			}
		
			return( true );
		}
		
		return( false );
	}
}	