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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.PropertiesWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.OpenCloseSearchDetailsListener;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarListener;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.azureus.util.UrlFilter;

import org.gudy.azureus2.plugins.PluginConfigListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImageListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

public class 
SubscriptionManagerUI 
{
	public static final Object	SUB_IVIEW_KEY 		= new Object();
	public static final Object	SUB_EDIT_MODE_KEY 	= new Object();
	
	private static final String EDIT_MODE_MARKER	= "&editMode=1";
	
	private Graphic	icon_rss_big;
	private Graphic	icon_rss_small;
	private Graphic	icon_rss_all_add_small;
	private Graphic	icon_rss_all_add_big;
	private Graphic	icon_rss_some_add_small;
	private Graphic	icon_rss_some_add_big;
	private List<Graphic>	icon_list	= new ArrayList<Graphic>();
	
	private SubscriptionManager	subs_man;
	
	private MenuItemListener markAllResultsListener;
	private MenuItemListener unmarkAllResultsListener;
	private MenuItemListener deleteAllResultsListener;
	private MenuItemListener resetAuthListener;
	private MenuItemListener resetResultsListener;
	private MenuItemListener exportListener;
	private MenuItemListener renameListener;
	private MenuItemListener removeListener;
	private MenuItemListener forceCheckListener;
	private MenuItemListener upgradeListener;
	private MenuItemListener propertiesListener;
	
	
	private boolean		side_bar_setup;

	private List<TableColumn> columns = new ArrayList<TableColumn>();
	
	public
	SubscriptionManagerUI(
		AzureusCore			core )
	{
		final PluginInterface	default_pi = core.getPluginManager().getDefaultPluginInterface();
		
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
			
			if ( false ){
			
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
							
							final List<byte[]>	hashes = new ArrayList<byte[]>();
							
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
													
													sub.addAssociation( hashes.get(i));
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
	
		createSubsColumns( table_manager );

		final UIManager	ui_manager = default_pi.getUIManager();
		
		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							final UISWTInstance	swt = (UISWTInstance)instance;
							
							icon_rss_small			= loadGraphic( swt, "btn_rss_subscribe_orange_30x14.png" );
							icon_rss_big			= icon_rss_small;

							//icon_rss_all_add_small	= loadGraphic( swt, "btn_rss_subscribed_green_30x14.png" );
							icon_rss_all_add_small	= loadGraphic( swt, "btn_rss_subscribed_gray_30x14.png" );
							icon_rss_all_add_big	= icon_rss_all_add_small;
							
							// icon_rss_some_add_small	= loadGraphic( swt, "btn_rss_subscribe_green_30x14.png" );
							icon_rss_some_add_small	= loadGraphic( swt, "btn_rss_subscribed_gray_30x14.png" );
							icon_rss_some_add_big	= icon_rss_some_add_small;
							
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
									subscriptionSelected(
										Subscription subscription )
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
										refreshColumns();
									}
								});	
							

							BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
									ConfigSection.SECTION_ROOT, "Subscriptions");

							final IntParameter max_results = 
								configModel.addIntParameter2( 
									"subscriptions.config.maxresults", 
									"subscriptions.config.maxresults", 
									subs_man.getMaxNonDeletedResults());
								
							default_pi.getPluginconfig().addListener(
								new PluginConfigListener()
								{
									public void 
									configSaved() 
									{
										subs_man.setMaxNonDeletedResults(max_results.getValue());
									}
								});
							
							/* grr, this generated intermediate events...
							max_results.addListener(
								new ParameterListener()
								{
									public void 
									parameterChanged(
										Parameter param )
									{
										subs_man.setMaxNonDeletedResults( max_results.getValue());
									}
								});
							*/
							
							SkinViewManager.addListener(
								new SkinViewManagerListener() 
								{
									public void 
									skinViewAdded(
										SkinView skinview) 
									{
										if ( skinview instanceof SideBar ){
											
											setupSideBar((SideBar) skinview, swt);
										}
									}
								});
							
							SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
							
							if ( sideBar != null ){
								
								setupSideBar( sideBar, swt );
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

	private void 
	createSubsColumns(
		TableManager table_manager )
	{
		final TableCellRefreshListener	subs_refresh_listener = 
			new TableCellRefreshListener()
			{
				public void 
				refresh(
					TableCell _cell )
				{
					TableCellSWT cell = (TableCellSWT)_cell;
					
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
														
						int	num_subscribed		= 0;
						int	num_unsubscribed	= 0;
						
						for (int i=0;i<subs.length;i++){
							
							if ( subs[i].isSubscribed()){
																
								num_subscribed++;
								
							}else{
								
								num_unsubscribed++;
							}
						}
						
						Graphic graphic;
						String	tooltip;
						
						int height = cell.getHeight();
						
						int	sort_order = 0;
						
						if ( subs.length == 0 ){
							
							graphic = null;
							tooltip	= null;
							
						}else{
						
							if ( num_subscribed == subs.length ){
								
								graphic = height >= 22?icon_rss_all_add_big:icon_rss_all_add_small;
								
								tooltip = MessageText.getString( "subscript.all.subscribed" );
								
							}else if ( num_subscribed > 0 ){
								
								graphic = height >= 22?icon_rss_some_add_big:icon_rss_some_add_small;

								tooltip = MessageText.getString( "subscript.some.subscribed" );

								sort_order	= 10000;
								
							}else{
								
								graphic = height >= 22?icon_rss_big:icon_rss_small;
								
								tooltip = MessageText.getString( "subscript.none.subscribed" );
								
								sort_order	= 1000000;
							}
						}
						
						sort_order += 1000*num_unsubscribed + num_subscribed;
						
						cell.setGraphic( graphic );
						cell.setToolTip( tooltip );
						
						cell.setSortValue( sort_order );
						
						cell.setCursorID( graphic==null?SWT.CURSOR_ARROW:SWT.CURSOR_HAND );

					}else{
						
						cell.setCursorID( SWT.CURSOR_ARROW );
						
						cell.setSortValue( 0 );
					}
				}
			};
			
		final TableCellMouseListener	subs_mouse_listener = 
			new TableCellMouseListener()
			{
				public void 
				cellMouseTrigger(
					TableCellMouseEvent event )
				{
					if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN ){
						
						
						TableCell cell = event.cell;
						
						Download	dl = (Download)cell.getDataSource();
						
						Torrent	torrent = dl.getTorrent();
						
						if ( torrent != null ){
							
							Subscription[] subs = subs_man.getKnownSubscriptions( torrent.getHash());
							
							if ( subs.length > 0 ){
								
								event.skipCoreFunctionality	= true;

								new SubscriptionWizard(PluginCoreUtils.unwrap(dl));
								
								//new SubscriptionListWindow(PluginCoreUtils.unwrap(dl),true);
							}
						}
					}
				}
			};
			
		table_manager.registerColumn(
			Download.class, 
			"azsubs.ui.column.subs", 
			new TableColumnCreationListener() 
			{
				public void tableColumnCreated(TableColumn result) {
					result.setAlignment(TableColumn.ALIGN_CENTER);
					result.setPosition(TableColumn.POSITION_LAST);
					result.setWidth(75);
					result.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
					result.setType(TableColumn.TYPE_GRAPHIC);
				
					result.addCellRefreshListener( subs_refresh_listener );
					result.addCellMouseListener( subs_mouse_listener );
					
					columns.add(result);
				}
			});
		
		final TableCellRefreshListener	link_refresh_listener = 
			new TableCellRefreshListener()
			{
				public void 
				refresh(
					TableCell _cell )
				{
					TableCellSWT cell = (TableCellSWT)_cell;
					
					if ( subs_man == null ){
						
						return;
					}
					
					Download	dl = (Download)cell.getDataSource();
					
					if ( dl == null ){
						
						return;
					}
					
					String	str 		= "";
					
					Torrent	torrent = dl.getTorrent();
					
					if ( torrent != null ){
						
						byte[]	hash = torrent.getHash();
						
						Subscription[] subs = subs_man.getKnownSubscriptions( hash );
														
						for (int i=0;i<subs.length;i++){
							
							Subscription sub = subs[i];
							
							if ( sub.hasAssociation( hash )){
								
								str += (str.length()==0?"":"; ") + sub.getName();
							}
						}
					}
					
					cell.setCursorID( str.length() > 0?SWT.CURSOR_HAND:SWT.CURSOR_ARROW );
					
					cell.setText( str );
				}
			};
		
			final TableCellMouseListener	link_mouse_listener = 
				new TableCellMouseListener()
				{
					public void 
					cellMouseTrigger(
						TableCellMouseEvent event )
					{
						if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN ){
										
							TableCell cell = event.cell;
							
							Download	dl = (Download)cell.getDataSource();
							
							Torrent	torrent = dl.getTorrent();
							
							if ( torrent != null ){
								
								byte[]	hash = torrent.getHash();
								
								Subscription[] subs = subs_man.getKnownSubscriptions( hash );
																
								for (int i=0;i<subs.length;i++){
									
									Subscription sub = subs[i];
									
									if ( sub.hasAssociation( hash )){
										
										sideBarItem item = (sideBarItem) sub.getUserData(SubscriptionManagerUI.SUB_IVIEW_KEY);
										
										if ( item != null ){
										
											event.skipCoreFunctionality	= true;

											item.activate();
											
											break;
										}
									}
								}
							}
						}
					}
				};
				
		table_manager.registerColumn(
				Download.class, 
				"azsubs.ui.column.subs_link", 
				new TableColumnCreationListener() 
				{
					public void tableColumnCreated(TableColumn result) {
						result.setAlignment(TableColumn.ALIGN_LEAD);
						result.setPosition(TableColumn.POSITION_INVISIBLE);
						result.setWidth(85);
						result.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
						result.setType(TableColumn.TYPE_TEXT_ONLY);
					
						result.addCellRefreshListener( link_refresh_listener );
						result.addCellMouseListener( link_mouse_listener );
						
						columns.add(result);
					}
				});
	}

	protected void
	setupSideBar(
		final SideBar			side_bar,
		final UISWTInstance		swt_ui )		
	{
		synchronized( this ){
			
			if ( side_bar_setup ){
				
				return;
			}
			
			side_bar_setup = true;
		}
		
		SideBarEntrySWT mainSBEntry = SideBar.getEntry(SideBar.SIDEBAR_SECTION_SUBSCRIPTIONS);
		if (mainSBEntry != null) {
			SideBarVitalityImage addSub = mainSBEntry.addVitalityImage("image.sidebar.subs.add");
			addSub.setToolTip("Add Subscription");
			addSub.addListener(new SideBarVitalityImageListener() {
				public void sbVitalityImage_clicked(int x, int y) {
					new SubscriptionWizard();
				}
			});
			
			mainSBEntry.setImageLeftID("image.sidebar.subscriptions");

			mainSBEntry.setTitleInfo(new ViewTitleInfo() {
				public Object getTitleInfoProperty(int propertyID) {
					if (propertyID == TITLE_TEXT) {
						return MessageText.getString("subscriptions.view.title");
					}
					return null;
				}
			});
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
		
		unmarkAllResultsListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					subs.getHistory().markAllResultsUnread();
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
		
		
		resetAuthListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					try{
						Engine engine = subs.getEngine();
						
						if ( engine instanceof WebEngine ){
							
							((WebEngine)engine).setCookies( null );
						}
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
					
					try{
						subs.getManager().getScheduler().downloadAsync(subs, true);
						
					}catch( Throwable e ){
						
						Debug.out(e);
					}
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
						subs.getEngine().reset();
					}catch( Throwable e ){
						Debug.printStackTrace(e);
					}
					try{
						subs.getManager().getScheduler().downloadAsync(subs, true);
						
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
		
		renameListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					
					UISWTInputReceiver entry = (UISWTInputReceiver)swt_ui.getInputReceiver();
					entry.setPreenteredText(subs.getName(), false );
					entry.maintainWhitespace(false);
					entry.allowEmptyInput( false );
					entry.setTitle("MyTorrentsView.menu.rename");
					entry.prompt();
					if (!entry.hasSubmittedInput()){
						
						return;
					}
					
					String input = entry.getSubmittedInput().trim();
					
					if ( input.length() > 0 ){
						
						try{
							subs.setName( input );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
			}
		};
		
		removeListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					MessageBoxShell mb = 
						new MessageBoxShell(
							Utils.findAnyShell(),
							MessageText.getString("message.confirm.delete.title"),
							MessageText.getString("message.confirm.delete.text",
									new String[] {
										subs.getName()
									}), 
							new String[] {
								MessageText.getString("Button.yes"),
								MessageText.getString("Button.no")
							},
							1 );
					
					int result = mb.open();
					if (result == 0) {
						subs.remove();
					}
				}
			}
		};
		
		forceCheckListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
					try{
						
						subs.getManager().getScheduler().downloadAsync( subs, true );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		};
		
		upgradeListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
				
					subs.resetHighestVersion();
				}
			}
		};
		
		propertiesListener = new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					Subscription subs = (Subscription) info.getDatasource();
				
					showProperties( subs );
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
				subscriptionSelected(
					Subscription subscription )
				{	
					sideBarItem item = (sideBarItem)subscription.getUserData(SubscriptionManagerUI.SUB_IVIEW_KEY);
					
					if (item != null ){
					
						item.activate();
					}
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
		
		side_bar.addListener(
			new SideBarListener() 
			{
				private long last_select = 0;
				
				public void 
				sidebarItemSelected(
					SideBarEntrySWT new_entry,
					SideBarEntrySWT old_entry ) 
				{
					if ( new_entry == old_entry ){
						
						IView view = new_entry.getIView();
						
						if ( view instanceof subscriptionView ){
							
							try{
								
								if ( SystemTime.getMonotonousTime() - last_select > 1000 ){
									
									((subscriptionView)view).updateBrowser( false );
								}
							}finally{
								
								last_select = SystemTime.getMonotonousTime();
							}
						}
					}
				}
			});
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
				
			final sideBarItem existing_si = (sideBarItem)subs.getUserData( SUB_IVIEW_KEY );
			
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
								
								SideBarEntrySWT	entry = SideBar.getEntry( key );
																
								new_si.setTreeItem( tree_item, entry );
								
								setStatus( subs, new_si );
								
								PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
								PluginInterface pi = pm.getDefaultPluginInterface();
								UIManager uim = pi.getUIManager();
								MenuManager menuManager = uim.getMenuManager();
								
								MenuItem menuItem;
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.forcecheck");
								menuItem.setText(MessageText.getString("Subscription.menu.forcecheck"));
								menuItem.addListener(forceCheckListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.clearall");
								menuItem.addListener(markAllResultsListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.dirtyall");
								menuItem.addListener(unmarkAllResultsListener);

								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.deleteall");
								menuItem.addListener(deleteAllResultsListener);
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.reset");
								menuItem.addListener(resetResultsListener);

								try{
									Engine e = subs.getEngine();
									
									if ( e instanceof WebEngine ){
										
										if (((WebEngine)e).isNeedsAuth()){
											
											menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.resetauth");
											menuItem.addListener(resetAuthListener);
										}
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
								
									// sep
								
								menuManager.addMenuItem("sidebar." + key,"s1").setStyle( MenuItem.STYLE_SEPARATOR );

								if ( subs.isUpdateable()){
									
									menuItem = menuManager.addMenuItem("sidebar." + key,"MyTorrentsView.menu.rename");
									menuItem.addListener(renameListener);
								}
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.upgrade");
								menuItem.addListener(upgradeListener);
									
								menuItem.addFillListener(
									new MenuItemFillListener()
									{
										public void 
										menuWillBeShown(
											MenuItem 	menu, 
											Object 		data ) 
										{									
											menu.setVisible( subs.getHighestVersion() > subs.getVersion());
										}
									});
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.export");
								menuItem.addListener(exportListener);
								
									// sep
								
								menuManager.addMenuItem("sidebar." + key,"s2").setStyle( MenuItem.STYLE_SEPARATOR );
								
								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.remove");
								menuItem.addListener(removeListener);
								
								menuManager.addMenuItem("sidebar." + key,"s3").setStyle( MenuItem.STYLE_SEPARATOR );

								menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.properties");
								menuItem.addListener(propertiesListener);
							}
						}
					});
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_si.getView());
								
								setStatus( subs, existing_si );
							}
						});
			}
		}
	}
	
	protected void
	setStatus(
		Subscription	subs,
		sideBarItem		sbi )
	{
		sbi.setWarning( subs );
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
				
				view.updateBrowser( false );
			}
		}
	}
	
	protected void
	refreshColumns()
	{
		for ( Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();){
			
			TableColumn column = iter.next();
			
			column.invalidateCells();
		}
	}
	
	protected void
	showProperties(
		Subscription		subs )
	{
		SubscriptionHistory history = subs.getHistory();
		
		SimpleDateFormat df = new SimpleDateFormat();
		
		String last_error = history.getLastError();
		
		if ( last_error == null ){
			last_error = "";
		}
		
		String	engine_str;
		String	auth_str	= String.valueOf(false);
		
		try{
			Engine engine = subs.getEngine();
			
			engine_str = engine.getNameEx();
			
			if ( engine instanceof WebEngine ){
			
				WebEngine web_engine = (WebEngine)engine;
				
				if ( web_engine.isNeedsAuth()){
					
					auth_str = String.valueOf(true) + ": cookies=" + toString( web_engine.getRequiredCookies());
				}
			}
		}catch( Throwable e ){
			
			engine_str 	= "Unknown";
			auth_str	= "";
		}
		
		String[] keys = {
				"subs.prop.enabled",
				"subs.prop.is_public",
				"subs.prop.is_auto",
				"subs.prop.is_auto_ok",
				"subs.prop.update_period",
				"subs.prop.last_scan",
				"subs.prop.last_result",
				"subs.prop.next_scan",
				"subs.prop.last_error",
				"subs.prop.num_read",
				"subs.prop.num_unread",
				"subs.prop.assoc",
				"subs.prop.version",
				"subs.prop.high_version",
				"subscriptions.listwindow.popularity",
				"subs.prop.template",
				"subs.prop.auth",
			};
		
		String[] values = { 
				String.valueOf( history.isEnabled()),
				String.valueOf( subs.isPublic()),
				String.valueOf( history.isAutoDownload()),
				String.valueOf( subs.isAutoDownloadSupported()),
				String.valueOf( history.getCheckFrequencyMins() + " " + MessageText.getString( "ConfigView.text.minutes")),
				df.format(new Date( history.getLastScanTime())),
				df.format(new Date( history.getLastNewResultTime())),
				df.format(new Date( history.getNextScanTime())),
				(last_error.length()==0?MessageText.getString("PeersView.uniquepiece.none"):last_error),
				String.valueOf( history.getNumRead()),
				String.valueOf( history.getNumUnread()),
				String.valueOf( subs.getAssociationCount()),
				String.valueOf( subs.getVersion()),
				subs.getHighestVersion() > subs.getVersion()?String.valueOf( subs.getHighestVersion()):null,
				subs.getCachedPopularity()<=1?null:String.valueOf( subs.getCachedPopularity()),
				engine_str,
				auth_str,
			};
		
		new PropertiesWindow( subs.getName(), keys, values );
	}
	
	private String
	toString(
		String[]	strs )
	{
		String	res = "";
		
		for(int i=0;i<strs.length;i++){
			res += (i==0?"":",") + strs[i];
		}
		
		return( res );
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
				
		//private Label			info_lab;
		//private Label			info_lab2;
		//private StyledText	json_area;
		//private Composite 		controls;
		
		private Browser			mainBrowser;
		private Browser			detailsBrowser;

		private SideBarVitalityImage spinnerImage;
		
		protected
		subscriptionView(
			Subscription		_subs )
		{
			subs = _subs;
		}
		
		public void delete() {
			// Fix/Hack for SWT Browser disposal bug + memory leak
			if(mainBrowser != null && ! mainBrowser.isDisposed()) {
				mainBrowser.setUrl("about:blank");
				mainBrowser.setVisible(false);
			}
			if(detailsBrowser != null && ! detailsBrowser.isDisposed()) {
				detailsBrowser.setUrl("about:blank");
				detailsBrowser.setVisible(false);
			}
			super.delete();
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{
			switch( propertyID ){
			
				case ViewTitleInfo.TITLE_TEXT:{
					
					return( subs.getName());
				}
				case ViewTitleInfo.TITLE_INDICATOR_TEXT_TOOLTIP:{
				
					long	pop = subs.getCachedPopularity();
					
					String res = subs.getName();
					
					if ( pop > 1 ){
						
						res += " (" + MessageText.getString("subscriptions.listwindow.popularity").toLowerCase() + "=" + pop + ")";
					}
					
					return( res );
				}
				case ViewTitleInfo.TITLE_INDICATOR_TEXT :{
					if(subs.getHistory().getNumUnread() > 0) {
						return ( "" + subs.getHistory().getNumUnread());
					}
					return null;
				}
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
							if (spinnerImage != null) {
								spinnerImage.setVisible(false);
							}
							destroyBrowsers();
						}				
					});
			
			composite = new Composite( parent_composite, SWT.NULL );
			
			composite.setLayout(new FormLayout());
			
			//GridData grid_data = new GridData(GridData.FILL_BOTH );
			//composite.setLayoutData(grid_data);
			//FormData data;

				// control area
			
			/*
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
			*/
			
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
							
							updateBrowser( true );
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
				
				context.addListener(new BrowserContext.loadingListener(){
					public void browserLoadingChanged(boolean loading, String url) {
						if (spinnerImage != null) {
							spinnerImage.setVisible(loading);
						}
					}
				});
				
				context.addMessageListener(new TorrentListener());
				context.addMessageListener(new VuzeListener());
				context.addMessageListener(new DisplayListener(mainBrowser));
				context.addMessageListener(new ConfigListener(mainBrowser));
				context.addMessageListener(
						new MetaSearchListener( this ));
				
				ContentNetwork contentNetwork = ContentNetworkManagerFactory.getSingleton().getContentNetwork(
						context.getContentNetworkID());
				// contentNetwork won't be null because a new browser context
				// has the default content network
				
				String url = contentNetwork.getSubscriptionURL(subs.getID());
					
				Boolean	edit_mode = (Boolean)subs.getUserData( SUB_EDIT_MODE_KEY );
				
				if ( edit_mode != null ){
				
					if ( edit_mode.booleanValue()){
						
						url += EDIT_MODE_MARKER;
					}
					
					subs.setUserData( SUB_EDIT_MODE_KEY, null );
				}
								
				mainBrowser.setUrl(url);
				mainBrowser.setData("StartURL", url);
				
				FormData data = new FormData();
				data.left = new FormAttachment(0,0);
				data.right = new FormAttachment(100,0);
				data.top = new FormAttachment(composite,0);
				data.bottom = new FormAttachment(100,0);
				mainBrowser.setLayoutData(data);
				
				detailsBrowser = new Browser(composite,Utils.getInitialBrowserStyle(SWT.NONE));
				BrowserContext detailsContext = 
					new BrowserContext("browser-window"	+ Math.random(), detailsBrowser, null, false);
				detailsContext.addListener(new BrowserContext.loadingListener(){
					public void browserLoadingChanged(boolean loading, String url) {
						if (spinnerImage != null) {
							spinnerImage.setVisible(loading);
						}
					}
				});
				
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
				detailsContext.addMessageListener(new VuzeListener());
				detailsContext.addMessageListener(new DisplayListener(detailsBrowser));
				detailsContext.addMessageListener(new ConfigListener(detailsBrowser));
				detailsContext.addMessageListener(new LightBoxBrowserRequestListener());
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
			
				//OSX bug : browsers don't really get disposed
				mainBrowser.setUrl("about:blank");
				
				mainBrowser.dispose();
				
				mainBrowser = null;
			}
			
			if ( detailsBrowser != null ){
				
				//OSX bug : browsers don't really get disposed
				detailsBrowser.setUrl("about:blank");
			
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
					if (UrlFilter.getInstance().urlCanRPC(url)) {
						url = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(url, false, true);
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
					detailsBrowser.setData("StartURL", url);
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
		updateBrowser(
			final boolean	is_auto )
		{
			if ( mainBrowser != null ){
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							if ( mainBrowser != null && mainBrowser.isVisible()){
							
								String url = (String)mainBrowser.getData( "StartURL" );

									// see if end of edit process indicated by the subscription being
									// re-downloaded on auto-mode
								
								if ( is_auto && url.endsWith( EDIT_MODE_MARKER )){
									
									url = url.substring(0,url.lastIndexOf( EDIT_MODE_MARKER ));
								
									mainBrowser.setData( "StartURL", url );
								}
								
								mainBrowser.setUrl( url );
							}
						}
					});
			}
		}
		
		protected void
		updateInfo()
		{
			/*
			String	engine_str = "";
			
			try{
				Engine engine = subs.getEngine();
				
				engine_str = engine.getString();
				
			}catch( Throwable e ){
				
				engine_str = Debug.getNestedExceptionMessage(e);
				
				Debug.out(e);
			}
			
			info_lab.setText( 
					"ID=" + subs.getID() +
					", version=" + subs.getVersion() +
					", subscribed=" + subs.isSubscribed() +
					", public=" + subs.isPublic() +
					", mine=" + subs.isMine() +
					", popularity=" + subs.getCachedPopularity() +
					", associations=" + subs.getAssociationCount() +
					", engine=" + engine_str );
			
			SubscriptionHistory history = subs.getHistory();
			
			info_lab2.setText( 
					"History: " + 
					"enabled=" + history.isEnabled() +
					", auto=" + history.isAutoDownload() +
					", last_scan=" + new SimpleDateFormat().format(new Date( history.getLastScanTime())) +
					", next_scan=" + new SimpleDateFormat().format(new Date( history.getNextScanTime())) +
					", last_new=" + new SimpleDateFormat().format(new Date( history.getLastNewResultTime())) +
					", read=" + history.getNumRead() +
					" ,unread=" + history.getNumUnread() +
					", error=" + history.getLastError() + " [af=" + history.isAuthFail() + "]" );
					
			try{
			
				json_area.setText( subs.getJSON());
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			*/
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
		
		public void resizeMainBrowser() {
			if ( mainBrowser != null ){
				
				Utils.execSWTThreadLater(0,
					new Runnable()
					{
						public void
						run()
						{
							if ( mainBrowser != null && ! mainBrowser.isDisposed() && mainBrowser.isVisible()){
							
								FormData data = (FormData) mainBrowser.getLayoutData();
								data.bottom = new FormAttachment(100,-1);
								mainBrowser.getParent().layout(true);
								Utils.execSWTThreadLater(0,
										new Runnable() {
									public void run() {
										if ( mainBrowser != null && ! mainBrowser.isDisposed() && mainBrowser.isVisible()){
											
											FormData data = (FormData) mainBrowser.getLayoutData();
											data.bottom = new FormAttachment(100,0);
											mainBrowser.getParent().layout(true);
										}
									}
								}
								);
							}
						}
					});
			}
			
		}
		
		public void resizeSecondaryBrowser() {
			// TODO Auto-generated method stub
			
		}

		/**
		 * @param spinnerImage
		 *
		 * @since 3.1.1.1
		 */
		public void setSpinnerImage(SideBarVitalityImage spinnerImage) {
			this.spinnerImage = spinnerImage;
		}
	}
	
	public static class
	sideBarItem
	{
		public static final String ALERT_IMAGE_ID	= "image.sidebar.vitality.alert";
		public static final String AUTH_IMAGE_ID	= "image.sidebar.vitality.auth";
		
		private subscriptionView	view;
		private SideBarEntrySWT		sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private SideBarVitalityImage	warning;
		private SideBarVitalityImage 	spinnerImage;
		
		protected
		sideBarItem()
		{
		}
		
		protected void
		setTreeItem(
			TreeItem		_tree_item,
			SideBarEntrySWT	_sb_entry )
		{
			tree_item	= _tree_item;
			sb_entry	= _sb_entry;
			
			warning = sb_entry.addVitalityImage( ALERT_IMAGE_ID );
			
			spinnerImage = sb_entry.addVitalityImage("image.sidebar.vitality.dots");
			
			spinnerImage.setVisible(false);
			
			if ( view != null ){
				
				view.setSpinnerImage(spinnerImage);
			}
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
			
			if (view != null) {
				
				view.setSpinnerImage( spinnerImage );
			}
		}
		
		protected subscriptionView
		getView()
		{
			return( view );
		}
		
		protected void
		setWarning(
			Subscription	subs )
		{
				// possible during initialisation, status will be shown again on complete
			
			if ( warning == null ){
				
				return;
			}
			
			SubscriptionHistory history = subs.getHistory();
			
			String	last_error = history.getLastError();

			boolean	auth_fail = history.isAuthFail();
			
				// don't report problem until its happened a few times, but not for auth fails as this is a perm error
			
			if ( history.getConsecFails() < 3 && !auth_fail ){
				
				last_error = null;
			}
			
			boolean	trouble = last_error != null;
			
			if ( trouble ){
			 
				warning.setToolTip( last_error );
				
				warning.setImageID( auth_fail?AUTH_IMAGE_ID:ALERT_IMAGE_ID );
				
				warning.setVisible( true );
				
			}else{
				
				warning.setVisible( false );
				
				warning.setToolTip( "" );
			}
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
		
		public void 
		activate() 
		{
			SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
			
			if ( sideBar != null && sb_entry != null ){
				
				sideBar.showEntryByID(sb_entry.getId());
			}
		}
	}
}
