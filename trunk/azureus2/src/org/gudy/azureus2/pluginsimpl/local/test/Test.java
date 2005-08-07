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


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ddb.*;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeEvent;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeListener;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;


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
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						Thread	t  = 
							new AEThread("test")
							{
								public void
								runSupport()
								{
									
									testLinks();
									try{
										// PlatformManagerFactory.getPlatformManager().performRecoverableFileDelete( "C:\\temp\\recycle.txt" );
										// PlatformManagerFactory.getPlatformManager().setTCPTOSEnabled( false );
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
							};
							
						t.setDaemon(true);
						
						t.start();
					}
					
					public void
					closedownInitiated()
					{	
					}
					
					public void
					closedownComplete()
					{
					}
				});
	}
	
	protected void
	testLinks()
	{
		plugin_interface.getDownloadManager().addListener(
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{
					DiskManagerFileInfo[]	info = download.getDiskManagerFileInfo();
					
					for (int i=0;i<info.length;i++){
						
						info[i].setLink( new File( "C:\\temp" ));
					}
				}
				
				public void
				downloadRemoved(
					Download	download )
				{
					
				}
			});
	}
	
	protected void
	testDDB()
	{
		try{
			DistributedDatabase	db = plugin_interface.getDistributedDatabase();
			
			DistributedDatabaseKey	key = db.createKey( new byte[]{ 4,7,1,2,5,8 });

			boolean	do_write	= false;
			
			if ( do_write ){
				
				DistributedDatabaseValue[] values = new DistributedDatabaseValue[500];
				
				for (int i=0;i<values.length;i++){
					
					byte[]	val = new byte[20];
					
					Arrays.fill( val, (byte)i );
					
					values[i] = db.createValue( val );
				}
				
				
				db.write(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							System.out.println( "Event:" + event.getType());
							
							if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_WRITTEN ){
								
								try{
									System.out.println( 
											"    write - key = " + 
											ByteFormatter.encodeString((byte[])event.getKey().getKey()) + 
											", val = " + ByteFormatter.encodeString((byte[]) event.getValue().getValue(byte[].class)));
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
						}
					},
					key,
					values );
			}else{
				
				db.read(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
								System.out.println( "Event:" + event.getType());
								
								if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_READ ){
									
									try{
										System.out.println( 
												"    read - key = " + 
												ByteFormatter.encodeString((byte[])event.getKey().getKey()) + 
												", val = " + ByteFormatter.encodeString((byte[]) event.getValue().getValue(byte[].class)));
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
							}
						},
						key,
						60000 );			
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	taTest()
	{
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
					
			
			plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
						try{
							download.setAttribute( ta, "wibble" );
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						
					}
				});
				
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
