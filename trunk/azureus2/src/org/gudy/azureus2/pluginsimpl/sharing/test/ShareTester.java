/*
 * File    : ShareTester.java
 * Created : 30-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.sharing.test;



import java.util.*;
import java.io.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.core3.util.*;

public class 
ShareTester
	implements Plugin
{
	protected static Semaphore			init_sem = new Semaphore();
	
	protected static ShareTester		singleton;
	
	protected Map	seed_transport_map	= new HashMap();
	
	public static synchronized ShareTester
	getSingleton()
	{
		if ( singleton == null ){
			
			new Thread( "plugin initialiser ")
			{
				public void
				run()
				{
					PluginManager.registerPlugin( ShareTester.class );
	
					Properties props = new Properties();
					
					props.put( PluginManager.PR_MULTI_INSTANCE, "true" );
					
					PluginManager.startAzureus( PluginManager.UI_SWT, props );
				}
			}.start();
		
			init_sem.reserve();
		}
		
		return( singleton );
	}	
	
	protected PluginInterface		plugin_interface;
	
	public void 
	initialize(
		PluginInterface _pi )
	{
		System.out.println( "plugin initialize called");
	
		plugin_interface = _pi;
		
		singleton = this;
		
		init_sem.release();
		
		LoggerChannel log = plugin_interface.getLogger().getChannel("Plugin Test");
		
		log.log(LoggerChannel.LT_INFORMATION, "Plugin Initialised");
		
		ShareManager	sm = plugin_interface.getShareManager();
		
		sm.addFile( new File("C:\\temp\\wap.cer"));
	}
	
	
	public static void
	main(
		String[]	args )
	{
		getSingleton();
	}
}
