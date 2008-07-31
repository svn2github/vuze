/*
 * Created on Jul 29, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.subscriptions;

import java.util.*;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;

public class 
SubscriptionManagerUI 
{
	private Graphic	icon_rss;
	private List	icon_list	= new ArrayList();
	
	private SubscriptionManager	subs_man;
	
	public
	SubscriptionManagerUI(
		AzureusCore			core )
	{
		PluginInterface	default_pi = core.getPluginManager().getDefaultPluginInterface();
		
		final TableManager	table_manager = default_pi.getUIManager().getTableManager();

		
		if ( Constants.isCVSVersion()){			
			
			// check assoc
		
		{
			final TableContextMenuItem menu_item_itorrents = 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azsubs.contextmenu.lookupassoc");
			final TableContextMenuItem menu_item_ctorrents 	= 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azsubs.contextmenu.lookupassoc");
			
			menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_PUSH);
			menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_PUSH);
	
			MenuItemListener listener = 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						TableRow[]	rows = (TableRow[])target;
						
						if ( rows.length > 0 ){
							
							Download download = (Download)rows[0].getDataSource();
							
							new SubscriptionListWindow(PluginCoreUtils.unwrap(download), false);
						}
						/*
						for (int i=0;i<rows.length;i++){
							
							Download download = (Download)rows[i].getDataSource();
							
							Torrent t = download.getTorrent();
							
							if ( t != null ){
								
								try{
									lookupAssociations( 
										t.getHash(),
										new SubscriptionLookupListener()
										{
											public void
											found(
												byte[]					hash,
												Subscription			subscription )
											{
												log( "    lookup: found " + ByteFormatter.encodeString( hash ) + " -> " + subscription.getName());
											}
											
											public void
											complete(
												byte[]					hash,
												Subscription[]			subscriptions )
											{
												log( "    lookup: complete " + ByteFormatter.encodeString( hash ) + " -> " +subscriptions.length );
	
											}
											
											public void
											failed(
												byte[]					hash,
												SubscriptionException	error )
											{
												log( "    lookup: failed", error );
											}
										});
									
								}catch( Throwable e ){
									
									log( "Lookup failed", e );
								}
							}	
						}*/
					}
				};
			
			menu_item_itorrents.addMultiListener( listener );
			menu_item_ctorrents.addMultiListener( listener );	
		}
		
			// make assoc
		
		{
			final TableContextMenuItem menu_item_itorrents = 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azsubs.contextmenu.addassoc");
			final TableContextMenuItem menu_item_ctorrents 	= 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azsubs.contextmenu.addassoc");
			
			menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_MENU);
			menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_MENU);
			
			MenuItemFillListener	menu_fill_listener = 
				new MenuItemFillListener()
				{
					public void
					menuWillBeShown(
						MenuItem	menu,
						Object		target )
					{	
						if ( subs_man == null ){
							
							return;
						}
						
						TableRow[]	rows;
						
						if ( target instanceof TableRow[] ){
							
							rows = (TableRow[])target;
							
						}else{
							
							rows = new TableRow[]{ (TableRow)target };
						}
						
						final List	hashes = new ArrayList();
						
						for (int i=0;i<rows.length;i++){
							
							Download	download = (Download)rows[i].getDataSource();
						
							if ( download != null ){
								
								Torrent torrent = download.getTorrent();
								
								if ( torrent != null ){
									
									hashes.add( torrent.getHash());
								}
							}
						}
													
						menu.removeAllChildItems();
						
						boolean enabled = hashes.size() > 0;
						
						if ( enabled ){
						
							Subscription[] subs = subs_man.getSubscriptions();
							
							boolean	incomplete = ((TableContextMenuItem)menu).getTableID() == TableManager.TABLE_MYTORRENTS_INCOMPLETE;
							
							TableContextMenuItem parent = incomplete?menu_item_itorrents:menu_item_ctorrents;
															
							for (int i=0;i<subs.length;i++){
								
								final Subscription	sub = subs[i];
								
								TableContextMenuItem item =
									table_manager.addContextMenuItem(
										parent,
										"!" + sub.getName() + "!");
								
								item.addListener(
									new MenuItemListener()
									{
										public void 
										selected(
											MenuItem 	menu,
											Object 		target ) 
										{
											for (int i=0;i<hashes.size();i++){
												
												sub.addAssociation( (byte[])hashes.get(i));
											}
										}
									});
							}
						}
						
						menu.setEnabled( enabled );
					}
				};
				
			menu_item_itorrents.addFillListener( menu_fill_listener );
			menu_item_ctorrents.addFillListener( menu_fill_listener );		
		}
	}
	
		TableCellRefreshListener	refresh_listener = 
			new TableCellRefreshListener()
			{
				public void 
				refresh(
					TableCell cell )
				{
					if ( subs_man == null ){
						
						return;
					}
					
					Download	dl = (Download)cell.getDataSource();
					
					if ( dl == null ){
						
						return;
					}
					
					Torrent	torrent = dl.getTorrent();
					
					if ( torrent != null ){
						
						Subscription[] subs = subs_man.getKnownSubscriptions( torrent.getHash());
											
						cell.setGraphic( subs.length > 0?icon_rss:null );
					}
				}
			};
			
		TableCellMouseListener	mouse_listener = 
			new TableCellMouseListener()
			{
				public void 
				cellMouseTrigger(
					TableCellMouseEvent event )
				{
					if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN ){
						
						event.skipCoreFunctionality	= true;
						
						TableCell cell = event.cell;
						
						Download	dl = (Download)cell.getDataSource();
						
						Torrent	torrent = dl.getTorrent();
						
						if ( torrent != null ){
							
							Subscription[] subs = subs_man.getKnownSubscriptions( torrent.getHash());
							
							if ( subs.length > 0 ){
								
								new SubscriptionListWindow(PluginCoreUtils.unwrap(dl),true);
							}
						}
					}
				}
			};
			
			// MyTorrents incomplete
			
		final TableColumn	subs_i_column = 
			table_manager.createColumn(
					TableManager.TABLE_MYTORRENTS_INCOMPLETE,
					"azsubs.ui.column.subs" );
		
		subs_i_column.setAlignment(TableColumn.ALIGN_CENTER);
		subs_i_column.setPosition(TableColumn.POSITION_LAST);
		subs_i_column.setMinWidth(100);
		subs_i_column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		subs_i_column.setType(TableColumn.TYPE_GRAPHIC);
		
		subs_i_column.addCellRefreshListener( refresh_listener );
		subs_i_column.addCellMouseListener( mouse_listener );
		
		table_manager.addColumn( subs_i_column );	
		
			// MyTorrents complete

		final TableColumn	subs_c_column = 
			table_manager.createColumn(
					TableManager.TABLE_MYTORRENTS_COMPLETE,
					"azsubs.ui.column.subs" );
		
		subs_c_column.setAlignment(TableColumn.ALIGN_CENTER);
		subs_c_column.setPosition(TableColumn.POSITION_LAST);
		subs_c_column.setMinWidth(100);
		subs_c_column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		subs_c_column.setType(TableColumn.TYPE_GRAPHIC);
		
		subs_c_column.addCellRefreshListener( refresh_listener );
		subs_c_column.addCellMouseListener( mouse_listener );
		
		table_manager.addColumn( subs_c_column );	

		default_pi.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UISWTInstance	swt = (UISWTInstance)instance;
							
							icon_rss			= loadGraphic( swt, "rss.png" );
						}

						subs_man = SubscriptionManagerFactory.getSingleton();
						
						subs_man.addListener(
							new SubscriptionManagerListener()
							{
								public void 
								subscriptionsChanged(
									byte[] hash )
								{
									subs_i_column.invalidateCells();
									subs_c_column.invalidateCells();
								}
							});						
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
					}
				});
		
	}
	
	protected Graphic
	loadGraphic(
		UISWTInstance	swt,
		String			name )
	{
		Image	image = swt.loadImage( "org/gudy/azureus2/ui/icons/" + name );

		Graphic graphic = swt.createGraphic(image );
		
		icon_list.add( graphic );
		
		return( graphic );
	}
}
