/*
 * Created on 02-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.test;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeEvent;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeListener;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSChannel;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSItem;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */


public class 
Test 
	implements Plugin
{
	private static AESemaphore		init_sem 	= new AESemaphore("PluginTester");
	private static AEMonitor		class_mon	= new AEMonitor( "PluginTester" );

	private static Test		singleton;
		
	
	public static Test
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				new AEThread( "plugin initialiser" )
				{
					public void
					runSupport()
					{
						PluginManager.registerPlugin( Test.class );
		
						Properties props = new Properties();
						
						props.put( PluginManager.PR_MULTI_INSTANCE, "true" );
						
						PluginManager.startAzureus( PluginManager.UI_SWT, props );
					}
				}.start();
			
				init_sem.reserve();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}	
	
	protected PluginInterface		plugin_interface;
	
	public void 
	initialize(
		PluginInterface _pi )
	{	
		plugin_interface	= _pi;
		
		singleton = this;
		
		init_sem.release();
		
		try{
			
			final TorrentAttribute ta = plugin_interface.getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
			
			ta.addTorrentAttributeListener(
				new TorrentAttributeListener()
				{
					public void
					event(
						TorrentAttributeEvent	ev )
					{
						System.out.println( "ev: " + ev.getType() + ", " + ev.getData());
						
						if ( ev.getType() == TorrentAttributeEvent.ET_ATTRIBUTE_VALUE_ADDED ){
							
							if ( "plop".equals( ev.getData())){
								
								ta.removeDefinedValue( "plop" );
							}
						}
					}
				});
			
			ta.addDefinedValue( "wibble" );
							
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		getSingleton();
	}
}
