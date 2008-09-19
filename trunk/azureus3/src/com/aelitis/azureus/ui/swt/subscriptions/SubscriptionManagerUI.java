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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AERunnableObject;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionHistory;
import com.aelitis.azureus.core.subs.SubscriptionListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.OpenCloseSearchDetailsListener;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.ExternalLoginCookieListener;
import com.aelitis.azureus.ui.swt.browser.listener.MetaSearchListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.MapUtils;

public class 
SubscriptionManagerUI 
{
	private static final Object	SUB_IVIEW_KEY = new Object();
	
	private Graphic	icon_rss;
	private List	icon_list	= new ArrayList();
	
	private TableColumn	subs_i_column;
	private TableColumn	subs_c_column;
	private TableColumn	subs_ib_column;
	private TableColumn	subs_cb_column;
	
	private SubscriptionManager	subs_man;
	
	private MenuItemListener markAllResultsListener;
	private MenuItemListener deleteAllResultsListener;
	private MenuItemListener resetResultsListener;
	private MenuItemListener exportListener;
	private MenuItemListener removeListener;
	private MenuItemListener forceCheckListener;
	
	private boolean		side_bar_setup;
	
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
		
			// make assoc - CVS only as for testing purposes
		
		if ( Constants.isCVSVersion()){
		
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
						
						cell.setSortValue( subs.length );
					}else{
						
						cell.setSortValue( 0 );
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
			
		// MyTorrents tables
			
		subs_i_column = createSubsColumn(table_manager, refresh_listener, mouse_listener, TableManager.TABLE_MYTORRENTS_INCOMPLETE);	
		subs_ib_column = createSubsColumn(table_manager, refresh_listener, mouse_listener, TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG);	
		subs_c_column = createSubsColumn(table_manager, refresh_listener, mouse_listener, TableManager.TABLE_MYTORRENTS_COMPLETE);	
		subs_cb_column = createSubsColumn(table_manager, refresh_listener, mouse_listener, TableManager.TABLE_MYTORRENTS_COMPLETE_BIG);	

		default_pi.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UISWTInstance	swt = (UISWTInstance)instance;
							
							icon_rss			= loadGraphic( swt, "btn_add_rss.png" );

							subs_man = SubscriptionManagerFactory.getSingleton();
							
							subs_man.addListener(
								new SubscriptionManagerListener()
								{
									public void 
									subscriptionAdded(
										Subscription subscription ) 
									{
									}
									
									public void
									subscriptionChanged(
										Subscription		subscription )
									{
									}
									
									public void 
									subscriptionRemoved(
										Subscription subscription ) 
									{
									}
									
									public void 
									associationsChanged(
										byte[] hash )
									{
										subs_i_column.invalidateCells();
										subs_ib_column.invalidateCells();
										subs_c_column.invalidateCells();
										subs_cb_column.invalidateCells();
									}
								});	
							

							SkinViewManager.addListener(
								new SkinViewManagerListener() 
								{
									public void 
									skinViewAdded(
										SkinView skinview) 
									{
										if ( skinview instanceof SideBar ){
											
											setupSideBar((SideBar) skinview);
										}
									}
								});
							
							SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
							
							if ( sideBar != null ){
								
								setupSideBar( sideBar );
							}
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
					}
				});
		
	}
	
	private TableColumn createSubsColumn(TableManager table_manager,TableCellRefreshListener refresh_listener,TableCellMouseListener mouse_listener,String tableID) {
		TableColumn result;
		
		result = table_manager.createColumn(
				tableID,
				"azsubs.ui.column.subs" );
		
		result.setAlignment(TableColumn.ALIGN_CENTER);
		result.setPosition(TableColumn.POSITION_LAST);
		result.setMinWidth(75);
		result.setWidthLimits(75, 75);
		result.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		result.setType(TableColumn.TYPE_GRAPHIC);
	
		result.addCellRefreshListener( refresh_listener );
		result.addCellMouseListener( mouse_listener );
		
		table_manager.addColumn( result );	
		return result;
	}
	
	protected void
	setupSideBar(
		final SideBar		side_bar )
	{
		synchronized( this ){
			
			if ( side_bar_setup ){
				
				return;
			}
			
			side_bar_setup = true;
		}
		
		markAllResultsListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					subs.getHistory().markAllResultsRead();
					refreshView( subs );
				}
			}
		};
		
		deleteAllResultsListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					subs.getHistory().deleteAllResults();
					refreshView( subs );
				}
			}
		};
		
		resetResultsListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					subs.getHistory().reset();
					
					try{
						subs.getManager().getScheduler().download(subs, true);
						
					}catch( Throwable e ){
						
						Debug.out(e);
					}
				}
			}
		};
		
		exportListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					final Subscription subs = (Subscription) info.getDatasource();
					
					final Shell shell = Utils.findAnyShell();
					
					shell.getDisplay().asyncExec(
						new AERunnable() 
						{
							public void 
							runSupport()
							{
								FileDialog dialog = 
									new FileDialog( shell, SWT.SYSTEM_MODAL | SWT.SAVE );
								
								dialog.setFilterPath( TorrentOpener.getFilterPathData() );
														
								dialog.setText(MessageText.getString("subscript.export.select.template.file"));
								
								dialog.setFilterExtensions(new String[] {
										"*.vuze",
										"*.vuz",
										Constants.FILE_WILDCARD
									});
								dialog.setFilterNames(new String[] {
										"*.vuze",
										"*.vuz",
										Constants.FILE_WILDCARD
									});
								
								String path = TorrentOpener.setFilterPathData( dialog.open());
			
								if ( path != null ){
									
									String lc = path.toLowerCase();
									
									if ( !lc.endsWith( ".vuze" ) && !lc.endsWith( ".vuz" )){
										
										path += ".vuze";
									}
									
									try{
										VuzeFile vf = subs.getVuzeFile();
										
										vf.write( new File( path ));
										

									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});
				}
			}
		};
		
		removeListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					subs.remove();
				}
			}
		};
		
		forceCheckListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					try {
						subs.getManager().getScheduler().download(subs,false);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
		};
		
		
		subs_man.addListener(
			new SubscriptionManagerListener()
			{
				public void 
				subscriptionAdded(
					Subscription 		subscription ) 
				{
					addSubscription( side_bar, subscription, true );
				}
	
				public void
				subscriptionChanged(
					Subscription		subscription )
				{
					changeSubscription( side_bar, subscription );
				}
				
				public void 
				subscriptionRemoved(
					Subscription 		subscription ) 
				{
					removeSubscription( side_bar, subscription );
				}
				
				public void
				associationsChanged(
					byte[]		association_hash )
				{
					 
				}
			});
		
		Subscription[]	subs = subs_man.getSubscriptions();
		
		for (int i=0;i<subs.length;i++){
			
			addSubscription( side_bar, subs[i], false );
		}
	}
	
	protected void
	changeSubscription(
		SideBar				side_bar,
		final Subscription	subs )
	{
		if ( subs.isSubscribed()){
			
			addSubscription( side_bar, subs, true );
			
		}else{
			
			removeSubscription( side_bar, subs);
		}
	}
	
	protected void
	addSubscription(
		final SideBar			side_bar,
		final Subscription		subs,
		final boolean			show )
	{
		if ( !subs.isSubscribed()){
			
			return;
		}
		
		refreshColumns();
		
		synchronized( this ){
				
			sideBarItem existing_si = (sideBarItem)subs.getUserData( SUB_IVIEW_KEY );
			
			if (  existing_si == null ){
	
				final sideBarItem new_si = new sideBarItem();
				
				subs.setUserData( SUB_IVIEW_KEY, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( SubscriptionManagerUI.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								subscriptionView view = new subscriptionView( subs );
								
								new_si.setView( view );
								
								String key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());
								
								TreeItem  tree_item = 
									side_bar.createTreeItemFromIView(
										SideBar.SIDEBAR_SECTION_SUBSCRIPTIONS, 
										view,
										key, 
										subs, 
										false, 
										show );
								
								new_si.setTreeItem( tree_item );
								
								PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
								PluginInterface pi = pm.getDefaultPluginInterface();
								UIManager uim = pi.getUIManager();
								MenuManager menuManager = uim.getMenuManager();
								
								MenuItem menuItem;
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.forcecheck");
								menuItem.setText(MessageText.getString("Subscription.menu.forcecheck",new String[] {subs.getName()}));
								menuItem.addListener(forceCheckListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.clearall");
								menuItem.addListener(markAllResultsListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.deleteall");
								menuItem.addListener(deleteAllResultsListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.reset");
								menuItem.addListener(resetResultsListener);

								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.export");
								menuItem.addListener(exportListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.remove");
								menuItem.addListener(removeListener);							
							}
						}
					});
			}else{
				
				ViewTitleInfoManager.refreshTitleInfo( existing_si.getView());
			}
		}
	}
	
	protected void
	removeSubscription(
		SideBar				side_bar,
		final Subscription	subs )
	{
		synchronized( this ){
			
			final sideBarItem existing = (sideBarItem)subs.getUserData( SUB_IVIEW_KEY );
			
			if ( existing != null ){
				
				subs.setUserData( SUB_IVIEW_KEY, null );
				
				existing.destroy();
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( SubscriptionManagerUI.this ){

									TreeItem ti = existing.getTreeItem();
									
									if ( ti != null ){
										
										ti.dispose();
									}
								}
							}
						});
			}
		}
		
		refreshColumns();
	}
	
	protected void
	refreshView(
		Subscription	subs )
	{		
		sideBarItem item = (sideBarItem)subs.getUserData( SUB_IVIEW_KEY );
		
		if ( item != null ){
			
			subscriptionView view = item.getView();
			
			if ( view != null ){
				
				view.updateBrowser();
			}
		}
	}
	
	protected void
	refreshColumns()
	{
		subs_i_column.invalidateCells();
		subs_ib_column.invalidateCells();
		subs_c_column.invalidateCells();
		subs_cb_column.invalidateCells();
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
	
	protected static class
	subscriptionView
		extends 	AbstractIView
		implements 	ViewTitleInfo,OpenCloseSearchDetailsListener
	{
		private Subscription	subs;
		
		private Composite		parent_composite;
		private Composite		composite;
				
		private Label			info_lab;
		private Label			info_lab2;
		private StyledText		json_area;
		
		private Composite 		controls;
		
		private Browser			mainBrowser;
		private Browser			detailsBrowser;
		
		protected
		subscriptionView(
			Subscription		_subs )
		{
			subs = _subs;
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{
			switch(propertyID) {
			case ViewTitleInfo.TITLE_TEXT :
				return subs.getName();
			case ViewTitleInfo.TITLE_INDICATOR_TEXT :
				if(subs.getHistory().getNumUnread() > 0) {
					return ( "" + subs.getHistory().getNumUnread());
				}
				return null;
			case ViewTitleInfo.TITLE_HAS_VITALITY :
				return new Boolean(false);
			}
			
			return( null );
		}

		public void 
		initialize(
			Composite _parent_composite )
		{  
			parent_composite	= _parent_composite;
			
			parent_composite.addListener(
				SWT.Show,
				new Listener()
				{
					public void 
					handleEvent(
						Event arg0 )
					{
						createBrowsers();
					}				
				});
			
			parent_composite.addListener(
					SWT.Hide,
					new Listener()
					{
						public void 
						handleEvent(
							Event arg0 )
						{
							destroyBrowsers();
						}				
					});
			
			composite = new Composite( parent_composite, SWT.NULL );
			
			composite.setLayout(new FormLayout());
			
			//GridData grid_data = new GridData(GridData.FILL_BOTH );
			//composite.setLayoutData(grid_data);
			FormData data;

				// control area
			
			controls = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			controls.setLayout(layout);
			
			data = new FormData();
			data.left = new FormAttachment(0,0);
			data.right = new FormAttachment(100,0);
			data.top = new FormAttachment(0,0);
			controls.setLayoutData(data);
			
			GridData grid_data;
			
			info_lab = new Label( controls, SWT.NULL );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			info_lab.setLayoutData(grid_data);
		
				
			info_lab2 = new Label( controls, SWT.NULL );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			info_lab2.setLayoutData(grid_data);
			
			json_area = new StyledText(controls,SWT.BORDER);
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.heightHint = 50;
			json_area.setLayoutData(grid_data);
			json_area.setWordWrap(true);
				
			subs.addListener(
				new SubscriptionListener()
				{
					public void 
					subscriptionChanged(
						Subscription subs ) 
					{
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									updateInfo();
								}
							});
					}
					
					public void
					subscriptionDownloaded(
						Subscription		subs,
						boolean				auto )
					{
						if ( auto ){
							
							updateBrowser();
						}
					}
				});
						
			updateInfo();
		}
		  
		
		protected void
		createBrowsers()
		{
			try{
				mainBrowser = new Browser(composite,Utils.getInitialBrowserStyle(SWT.NONE));
				BrowserContext context = 
					new BrowserContext("browser-window"	+ Math.random(), mainBrowser, null, true);
				
				context.addMessageListener(new TorrentListener());
				context.addMessageListener(new DisplayListener(mainBrowser));
				context.addMessageListener(new ConfigListener(mainBrowser));
				context.addMessageListener(
						new MetaSearchListener( this ));
				String url = com.aelitis.azureus.util.Constants.URL_PREFIX + "xsearch?subscription=" + subs.getID() + "&" + com.aelitis.azureus.util.Constants.URL_SUFFIX;
	
				mainBrowser.setUrl(url);
				mainBrowser.setData("StartURL", url);
				
				FormData data = new FormData();
				data.left = new FormAttachment(0,0);
				data.right = new FormAttachment(100,0);
				data.top = new FormAttachment(controls,0);
				data.bottom = new FormAttachment(100,0);
				mainBrowser.setLayoutData(data);
				
				detailsBrowser = new Browser(composite,Utils.getInitialBrowserStyle(SWT.NONE));
				BrowserContext detailsContext = 
					new BrowserContext("browser-window"	+ Math.random(), detailsBrowser, null, true);
				
				ClientMessageContext.torrentURLHandler url_handler =
					new ClientMessageContext.torrentURLHandler()
					{
						public void 
						handleTorrentURL(
							final String url ) 
						{
							Utils.execSWTThreadWithObject(
								"SMUI",
								new AERunnableObject()
								{
									public Object
									runSupport()
									{
										String subscriptionId 		= (String)detailsBrowser.getData("subscription_id");
										String subscriptionResultId = (String)detailsBrowser.getData("subscription_result_id");
				
										if ( subscriptionId != null && subscriptionResultId != null ){
											
											Subscription subs = SubscriptionManagerFactory.getSingleton().getSubscriptionByID( subscriptionId );
										
											if ( subs != null ){
												
												subs.addPotentialAssociation( subscriptionResultId, url );
											}
										}
										
										return( null );
									}
								},
								10*1000 );
						}
					};
					
				detailsContext.setTorrentURLHandler( url_handler );
				
				TorrentListener torrent_listener = new TorrentListener();
				
				torrent_listener.setTorrentURLHandler( url_handler );
				
				detailsContext.addMessageListener( torrent_listener );
				detailsContext.addMessageListener(new DisplayListener(detailsBrowser));
				detailsContext.addMessageListener(new ConfigListener(detailsBrowser));
				url = "about:blank";
				detailsBrowser.setUrl(url);
				detailsBrowser.setData("StartURL", url);
				
				final ExternalLoginCookieListener cookieListener = new ExternalLoginCookieListener(new CookiesListener() {
					public void cookiesFound(String cookies) {
						detailsBrowser.setData("current-cookies", cookies);
					}
				},detailsBrowser);
				
				cookieListener.hook();
				
				data = new FormData();
				data.left = new FormAttachment(0,0);
				data.right = new FormAttachment(100,0);
				data.top = new FormAttachment(mainBrowser,0);
				data.bottom = new FormAttachment(100,0);
				detailsBrowser.setLayoutData(data);
								
				mainBrowser.setVisible( true );
				detailsBrowser.setVisible( false );
				//detailsBrowser.set
				mainBrowser.getParent().layout(true,true);
				
			}catch( Throwable e ){
			
				Debug.printStackTrace(e);
			}
		}
		
		protected void
		destroyBrowsers()
		{
			if ( mainBrowser != null ){
			
				mainBrowser.dispose();
				
				mainBrowser = null;
			}
			
			if ( detailsBrowser != null ){
			
				detailsBrowser.dispose();

				detailsBrowser = null;
			}
		}
		
		public void closeSearchResults(final Map params) {
			Utils.execSWTThread(new AERunnable() {

				public void runSupport() {
					detailsBrowser.setVisible(false);
					
					FormData gd = (FormData) mainBrowser.getLayoutData();
					gd.bottom = new FormAttachment(100, 0);
					mainBrowser.setLayoutData(gd);
		
					mainBrowser.getParent().layout(true);
					detailsBrowser.setUrl("about:blank");
					//mainBrowser.setUrl( (String)mainBrowser.getData( "StartURL" ));
				}
			});
		}
		
		public void openSearchResults(final Map params) {
			Utils.execSWTThread(new AERunnable() {

				public void runSupport() {
					String url = MapUtils.getMapString(params, "url",
							"http://google.com/search?q=" + Math.random());
					if (PlatformConfigMessenger.urlCanRPC(url)) {
						url = com.aelitis.azureus.util.Constants.appendURLSuffix(url);
					}
					
					//Gudy, Not Tux, Listener Added
					String listenerAdded = (String) detailsBrowser.getData("g.nt.la");
					if(listenerAdded == null) {
						detailsBrowser.setData("g.nt.la","");
						detailsBrowser.addProgressListener(new ProgressListener() {
							public void changed(ProgressEvent event) {}
							
							public void completed(ProgressEvent event) {
								Browser search = (Browser) event.widget;
								String execAfterLoad = (String) search.getData("execAfterLoad");
								//Erase it, so that it's only used once after the page loads
								search.setData("execAfterLoad",null);
								if(execAfterLoad != null && ! execAfterLoad.equals("")) {
									//String execAfterLoadDisplay = execAfterLoad.replaceAll("'","\\\\'");
									//search.execute("alert('injecting script : " + execAfterLoadDisplay + "');");
									boolean result = search.execute(execAfterLoad);
									//System.out.println("Injection : " + execAfterLoad + " (" + result + ")");
								}
		
							}
						});
					}
					
					
					//Store the "css" match string in the search cdp browser object
					String execAfterLoad = MapUtils.getMapString(params, "execAfterLoad", null);
					
					detailsBrowser.setData("execAfterLoad",execAfterLoad);
					
					
					detailsBrowser.setData("subscription_id", MapUtils.getMapString(params, "subs_id", null));
					detailsBrowser.setData("subscription_result_id", MapUtils.getMapString(params, "subs_rid", null));
								
					detailsBrowser.setUrl(url);
					detailsBrowser.setVisible(true);
		
					FormData data = (FormData) mainBrowser.getLayoutData();
					data.bottom = null;
					data.height = MapUtils.getMapInt(params, "top-height", 120);
					//mainBrowser.setLayoutData(data);
		
					mainBrowser.getParent().layout(true,true);
				}
			});
				
		}
		
		protected void
		updateBrowser()
		{
			if ( mainBrowser != null ){
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							if ( mainBrowser != null && mainBrowser.isVisible()){
							
								mainBrowser.setUrl( (String)mainBrowser.getData( "StartURL" ));
							}
						}
					});
			}
		}
		
		protected void
		updateInfo()
		{
			info_lab.setText( 
					"ID=" + subs.getID() +
					", version=" + subs.getVersion() +
					", subscribed=" + subs.isSubscribed() +
					", public=" + subs.isPublic() +
					", mine=" + subs.isMine() +
					", popularity=" + subs.getCachedPopularity() +
					", associations=" + subs.getAssociationCount());
			
			SubscriptionHistory history = subs.getHistory();
			
			info_lab2.setText( 
					"History: " + 
					"enabled=" + history.isEnabled() +
					", auto=" + history.isAutoDownload() +
					", last_scan=" + new SimpleDateFormat().format(new Date( history.getLastScanTime())) +
					", next_scan=" + new SimpleDateFormat().format(new Date( history.getNextScanTime())) +
					", last_new=" + new SimpleDateFormat().format(new Date( history.getLastNewResultTime())) +
					", read=" + history.getNumRead() +
					" ,unread=" + history.getNumUnread());
					
			try{
			
				json_area.setText( subs.getJSON());
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		public Composite 
		getComposite()
		{ 
			return( composite );
		}
		
		public String 
		getFullTitle() 
		{
			return( subs.getName());
		}
	}
	
	protected static class
	sideBarItem
	{
		private subscriptionView	view;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		protected
		sideBarItem()
		{
		}
		
		protected void
		setTreeItem(
			TreeItem		_tree_item )
		{
			tree_item	= _tree_item;
		}
		
		protected TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected void
		setView(
			subscriptionView		_view )
		{
			view	= _view;
		}
		
		protected subscriptionView
		getView()
		{
			return( view );
		}
		
		protected boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		protected void
		destroy()
		{
			destroyed = true;
		}
	}
}
