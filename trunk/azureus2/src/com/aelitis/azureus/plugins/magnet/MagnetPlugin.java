/*
 * Created on 03-Mar-2005
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

package com.aelitis.azureus.plugins.magnet;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;

import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;
import com.aelitis.net.magneturi.*;

/**
 * @author parg
 *
 */

public class 
MagnetPlugin
	implements Plugin, MagnetURIHandlerListener
{
	private PluginInterface		plugin_interface;
		
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Magnet URI Handler" );
		
		MenuItemListener	listener = 
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem		_menu,
					Object			_target )
				{
					Download download = (Download)((TableRow)_target).getDataSource();
				  
					if ( download == null || download.getTorrent() == null ){
						
						return;
					}
					
					Torrent t = download.getTorrent();
					
					String	url = "magnet:?xt=urn:sha1:" + Base32.encode( t.getHash());
					
					System.out.println( "MagnetPlugin: export = " + url );
					
					try{
						plugin_interface.getUIManager().copyToClipBoard( url );
						
					}catch( Throwable  e ){
						
						e.printStackTrace();
					}
				}
			};
		
		TableContextMenuItem menu1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "MagnetPlugin.contextmenu.exporturi" );
		TableContextMenuItem menu2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, 	"MagnetPlugin.contextmenu.exporturi" );
			
		menu1.addListener( listener );
		menu2.addListener( listener );

		MagnetURIHandler.getSingleton().addListener( this );
	}
	
	public byte[]
	badge()
	{
		return( null );
	}
	
	public byte[]
	download(
		final byte[]		hash,
		final long			timeout )
	{
		System.out.println( "MagnetPlugin: download( " + plugin_interface.getUtilities().getFormatters().encodeBytesToString( hash ) + ")");
		
		try{
			final DistributedDatabase db = plugin_interface.getDistributedDatabase();
			
			final List			live_ones 		= new ArrayList();
			final AESemaphore	live_ones_sem 	= new AESemaphore( "MagnetPlugin:liveones" );
			final AEMonitor		live_ones_mon	= new AEMonitor( "MagnetPlugin:liveones" );
			
			final int[]			outstanding		= {0};

			db.read(
				new DistributedDatabaseListener()
				{
					public void
					event(
						DistributedDatabaseEvent 		event )
					{
						int	type = event.getType();
	
						if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
							
							final DistributedDatabaseValue	value = event.getValue();
							
							System.out.println( "MagnetPlugin: found " + value.getContact().getName());
					
							outstanding[0]++;
							
							Thread t = 
								new AEThread( "MagnetPlugin:HitHandler")
								{
									public void
									runSupport()
									{
										try{
											boolean	alive = value.getContact().isAlive(10000);
												
											System.out.println( value.getContact().getName() + ": alive = " + alive );
											
											if ( alive ){
												
												try{
													live_ones_mon.enter();
													
													live_ones.add( value.getContact());
													
													live_ones_sem.release();
													
												}finally{
													
													live_ones_mon.exit();
												}
											}
										}finally{
											
											try{
												live_ones_mon.enter();													

												outstanding[0]--;
												
											}finally{
												
												live_ones_mon.exit();
											}
										}
									}
								};
								
							t.setDaemon(true);
							
							t.start();
						
						}else if (	type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ||
									type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
																		
							live_ones_sem.release();
						}
					}
				},
				db.createKey( hash ),
				timeout );
			
			long	remaining	= timeout;
			
			while( remaining > 0 ){
					
				long start = SystemTime.getCurrentTime();
				
				live_ones_sem.reserve( remaining );
				
				remaining -= ( SystemTime.getCurrentTime() - start );
				
				DistributedDatabaseContact	contact;
				
				try{
					live_ones_mon.enter();
					
					if ( live_ones.size() == 0 ){
						
						if ( outstanding[0] == 0 ){
						
							break;
							
						}else{
							
							continue;
						}
					}else{
					
						contact = (DistributedDatabaseContact)live_ones.remove(0);
					}
					
				}finally{
					
					live_ones_mon.exit();
				}
					
				try{
					System.out.println( "downloading torrent from " + contact.getName());
					
					DistributedDatabaseValue	value = 
						contact.read( 
								new DistributedDatabaseProgressListener()
								{
									public void
									reportSize(
										long	size )
									{
										System.out.println( "dl:size=" + size );
									}
									public void
									reportActivity(
										String	str )
									{
										System.out.println( "dl:act=" + str );
									}
									
									public void
									reportCompleteness(
										int		percent )
									{
										System.out.println( "dl:%=" + percent );
									}
								},
								db.getStandardTransferType( DistributedDatabaseTransferType.ST_TORRENT ),
								db.createKey( hash ),
								60000 );
					
					System.out.println( "download value = " + value );
					
					if ( value != null ){
						
						return( (byte[])value.getValue(byte[].class));
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}

		return( null );
	}
}
