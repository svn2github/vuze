/*
 * Created on Jul 29, 2008
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */


package com.aelitis.azureus.ui.swt.subscriptions;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfigListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventListenerHolder;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.utils.TagUIUtilsV3;

public class 
SubscriptionManagerUI 
{
	private static final String CONFIG_SECTION_ID = "Subscriptions";
	public static final Object	SUB_ENTRYINFO_KEY 		= new Object();
	public static final Object	SUB_EDIT_MODE_KEY 	= new Object();
	
	private static final String ALERT_IMAGE_ID	= "image.sidebar.vitality.alert";
	private static final String INFO_IMAGE_ID	= "image.sidebar.vitality.info";
	

	static final String EDIT_MODE_MARKER	= "&editMode=1";
	
	private Graphic	icon_rss_big;
	private Graphic	icon_rss_small;
	private Graphic	icon_rss_all_add_small;
	private Graphic	icon_rss_all_add_big;
	private Graphic	icon_rss_some_add_small;
	private Graphic	icon_rss_some_add_big;
	private List<Graphic>	icon_list	= new ArrayList<Graphic>();
	
	private List<TableColumn> columns = new ArrayList<TableColumn>();
	protected UISWTInstance swt;
	private UIManager ui_manager;
	private PluginInterface default_pi;
	private MdiEntry mdiEntryOverview;
	
	private boolean	sidebar_setup_done;
	
	public
	SubscriptionManagerUI()
	{
		default_pi = PluginInitializer.getDefaultInterface();
		
		final TableManager	table_manager = default_pi.getUIManager().getTableManager();

		Utils.getOffOfSWTThread(new AERunnable() {
			public void runSupport() {
				SubscriptionManagerFactory.getSingleton();
			}
		});
		
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
							SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();

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
							
								Subscription[] subs = subs_man.getSubscriptions( true );
								
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

		ui_manager = default_pi.getUIManager();

		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if (!( instance instanceof UISWTInstance )){
							return;

						}
						
						swt = (UISWTInstance)instance;
						
						uiQuickInit();

        		Utilities utilities = default_pi.getUtilities();
        		
        		final DelayedTask dt = utilities.createDelayedTask(new Runnable()
        			{
        				public void 
        				run() 
        				{
        					Utils.execSWTThread(new AERunnable() {
									
										public void 
										runSupport() 
										{
       								delayedInit();
        						}
        					});
        				}
        			});
        			
        			dt.queue();		
					}

					public void UIDetached(UIInstance instance) {
					}
				});
	}
	
	void uiQuickInit() {
		
		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		
		if ( mdi == null ){
			
				// closing down
			
			return;
		}

		icon_rss_small			= loadGraphic( swt, "subscription_icon.png" );
		icon_rss_big			= icon_rss_small;

		icon_rss_all_add_small	= loadGraphic( swt, "subscription_icon_inactive.png" );
		icon_rss_all_add_big	= icon_rss_all_add_small;
		
		icon_rss_some_add_small	= icon_rss_all_add_small;
		icon_rss_some_add_big	= icon_rss_some_add_small;
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						setupSideBar( swt );
						return mdiEntryOverview;
					}
				});
		
		mdi.registerEntry("Subscription_.*", new MdiEntryCreationListener2() {
			// @see com.aelitis.azureus.ui.mdi.MdiEntryCreationListener2#createMDiEntry(com.aelitis.azureus.ui.mdi.MultipleDocumentInterface, java.lang.String, java.lang.Object, java.util.Map)
			public MdiEntry createMDiEntry(MultipleDocumentInterface mdi, String id,
					Object datasource, Map<?, ?> params) {
				Subscription sub = null;
				if (datasource instanceof Subscription) {
					sub = (Subscription) datasource;
				} else if (id.length() > 13) {
					String publicKey = id.substring(13);
					byte[] decodedPublicKey = ByteFormatter.decodeString(publicKey);
					SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();

					Subscription[] subscriptions = subs_man.getSubscriptions();
					for (Subscription subscription : subscriptions) {
						if (Arrays.equals(subscription.getPublicKey(), decodedPublicKey)) {
							sub = subscription;
							break;
						}
					}
				}
				// hack to hide useless entries
				if (sub != null && sub.isSearchTemplate()) {
					return null;
				}
				return sub == null ? null : createSubscriptionMdiEntry(sub);
			}
		});

		SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
		subs_man.addListener(
			new SubscriptionManagerListener()
			{
				public void 
				subscriptionAdded(
					Subscription 		subscription ) 
				{
				}
	
				public void
				subscriptionChanged(
					Subscription		sub )
				{
					
					changeSubscription( sub );
				}
				
				public void 
				subscriptionSelected(
					Subscription sub )
				{	
					
					String key = "Subscription_" + ByteFormatter.encodeString(sub.getPublicKey());
					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					if ( mdi != null ){
						mdi.showEntryByID(key, sub);
					}
				}
				
				public void 
				subscriptionRemoved(
					Subscription 		subscription ) 
				{
					removeSubscription( subscription );
				}
				
				public void
				associationsChanged(
					byte[]		association_hash )
				{ 
				}
				
				public void
				subscriptionRequested(
					URL					url )
				{	
				}
			});
	}

	void delayedInit() {
		if (swt == null) {
			return;
		}
		
		SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
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
				
				public void
				subscriptionRequested(
					final URL					url )
				{
					Utils.execSWTThread(
						new AERunnable() 
						{
							public void
							runSupport()
							{
								new SubscriptionWizard( url );
							}
						});
				}
			});	
		
		createConfigModel();
	}

	private void createConfigModel() {
		final SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();

		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_ROOT, CONFIG_SECTION_ID);

		final IntParameter max_results = 
			configModel.addIntParameter2( 
				"subscriptions.config.maxresults", 
				"subscriptions.config.maxresults", 
				subs_man.getMaxNonDeletedResults());
			
			// search
		
		final BooleanParameter search_enable = 
			configModel.addBooleanParameter2( 
				"subscriptions.search.enable", "subscriptions.search.enable",
				subs_man.isSearchEnabled());
		
		search_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					subs_man.setSearchEnabled( search_enable.getValue());
				}
			});
		
			// download subs enable
		
		final BooleanParameter download_subs_enable = 
			configModel.addBooleanParameter2( 
				"subscriptions.dl_subs.enable", "subscriptions.dl_subs.enable",
				subs_man.isSubsDownloadEnabled());
		
		download_subs_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					subs_man.setSubsDownloadEnabled( download_subs_enable.getValue());
				}
			});
		
		
			// rate limits
		
		final StringParameter rate_limits = configModel.addStringParameter2(
				"subscriptions.config.ratelimits",
				"subscriptions.config.ratelimits",
				subs_man.getRateLimits());
		
		rate_limits.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					subs_man.setRateLimits(rate_limits.getValue());
				}
			});

			// auto
		
		final BooleanParameter auto_start = configModel.addBooleanParameter2(
				"subscriptions.config.autostartdls",
				"subscriptions.config.autostartdls",
				subs_man.getAutoStartDownloads());
		
		auto_start.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					subs_man.setAutoStartDownloads( auto_start.getValue());
				}
			});
		
		final IntParameter min_auto_start_size = 
			configModel.addIntParameter2( 
				"subscriptions.config.autostart.min", 
				"subscriptions.config.autostart.min", 
				subs_man.getAutoStartMinMB());

		final IntParameter max_auto_start_size = 
			configModel.addIntParameter2( 
				"subscriptions.config.autostart.max", 
				"subscriptions.config.autostart.max", 
				subs_man.getAutoStartMaxMB());

		auto_start.addEnabledOnSelection( min_auto_start_size );
		auto_start.addEnabledOnSelection( max_auto_start_size );
		
		configModel.createGroup( 
			"subscriptions.config.auto", 
			new Parameter[]{
					auto_start, 
					min_auto_start_size,
					max_auto_start_size,
			});
		
			// int param fires intermediate events so we have to rely on the save :(
		
		default_pi.getPluginconfig().addListener(
			new PluginConfigListener()
			{
				public void 
				configSaved() 
				{
					subs_man.setMaxNonDeletedResults(max_results.getValue());
					subs_man.setAutoStartMinMB(min_auto_start_size.getValue());
					subs_man.setAutoStartMaxMB(max_auto_start_size.getValue());
				}
			});

		
			// rss
		
		final BooleanParameter rss_enable = 
			configModel.addBooleanParameter2( 
				"subscriptions.rss.enable", "subscriptions.rss.enable",
				subs_man.isRSSPublishEnabled());
		
		rss_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					subs_man.setRSSPublishEnabled( rss_enable.getValue());
				}
			});
				
		HyperlinkParameter rss_view = 
			configModel.addHyperlinkParameter2(
				"device.rss.view", subs_man.getRSSLink());
		
		rss_enable.addEnabledOnSelection( rss_view );
		
		configModel.createGroup(
			"device.rss.group",
			new Parameter[]
			{
					rss_enable, rss_view,
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
					
					SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
					
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
						
						cell.setMarginHeight(0);
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
							
							SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
							Subscription[] subs = subs_man.getKnownSubscriptions( torrent.getHash());
							
							if ( subs.length > 0 ){
								
								event.skipCoreFunctionality	= true;

								new SubscriptionWizard(PluginCoreUtils.unwrap(dl));
								
								COConfigurationManager.setParameter( "subscriptions.wizard.shown", true );

								refreshTitles( mdiEntryOverview );
								
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
					result.setWidth(32);
					result.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
					result.setType(TableColumn.TYPE_GRAPHIC);
				
					result.addCellRefreshListener( subs_refresh_listener );
					result.addCellMouseListener( subs_mouse_listener );
					result.setIconReference("image.subscription.column", true);
					
					synchronized (columns) {
						columns.add(result);
					}
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
					
					SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
					
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

							SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();

							if ( torrent != null ){
								
								byte[]	hash = torrent.getHash();
								
								Subscription[] subs = subs_man.getKnownSubscriptions( hash );
																
								for (int i=0;i<subs.length;i++){
									
									Subscription sub = subs[i];
									
									if ( sub.hasAssociation( hash )){
										
										String key = "Subscription_" + ByteFormatter.encodeString(sub.getPublicKey());
										MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
										if ( mdi != null ){
											mdi.showEntryByID(key, sub);
										}
										break;
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
						result.setMinimumRequiredUserMode( Parameter.MODE_INTERMEDIATE );
						
						synchronized (columns) {
							columns.add(result);
						}
					}
				});
	}
	
	protected void
	setupSideBar(
		final UISWTInstance		swt_ui )		
	{
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		if (mdi == null) {
			return;
		}

		mdiEntryOverview = mdi.createEntryFromEventListener(
				MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY, 
				new UISWTViewEventListenerHolder(
						MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS,
						SubscriptionsView.class, null, null),
				MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS, false, null, null );

		if (mdiEntryOverview == null) {
			return;
		}
		
		mdiEntryOverview.setDefaultExpanded(true);
			
		synchronized( this ){
				// seen double add buttons in the sidebar, not sure of cause but it would imply we are coming through here
				// twice which can't be good - protect against that
			
			if( sidebar_setup_done ){
				
				return;
			}
			
			sidebar_setup_done = true;
		}
		
		mdiEntryOverview.setImageLeftID("image.sidebar.subscriptions");

		setupHeader(mdi, mdiEntryOverview);

//		MdiEntry headerEntry = mdi.getEntry(MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY);
//		if (headerEntry != null) {
//			setupHeader(mdi, headerEntry);
//		}

		String parentID = "sidebar." + MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY;

		MenuManager menu_manager = ui_manager.getMenuManager();
		
		MenuItem mi = menu_manager.addMenuItem( parentID, "MainWindow.menu.view.configuration" );
		
		mi.addListener( 
				new MenuItemListener() 
				{
					public void 
					selected(
						MenuItem menu, Object target ) 
					{
				      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
				      	 
				      	 if ( uif != null ){
				      		 
				      		 uif.getMDI().showEntryByID(
				      				 MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG,
				      				 CONFIG_SECTION_ID);
				      	 }
					}
				});
	}

	private void setupHeader(MultipleDocumentInterface mdi,
			final MdiEntry headerEntry) {

		MdiEntryVitalityImage addSub = headerEntry.addVitalityImage("image.sidebar.subs.add");

		if (addSub != null) {
			addSub.setToolTip(MessageText.getString("subscriptions.add.tooltip"));

			addSub.addListener(new MdiEntryVitalityImageListener() {
				public void mdiEntryVitalityImage_clicked(int x, int y) {
					new SubscriptionWizard();
					
					COConfigurationManager.setParameter( "subscriptions.wizard.shown", true );

					refreshTitles( mdiEntryOverview );
				}
			});
		}

		final MdiEntryVitalityImage warnSub = headerEntry.addVitalityImage(ALERT_IMAGE_ID);
		if (warnSub != null) {
			warnSub.setVisible(false);
		}

		final MdiEntryVitalityImage infoSub = headerEntry.addVitalityImage(INFO_IMAGE_ID);
		if (infoSub != null) {
			infoSub.setVisible(false);
		}

		
		headerEntry.setViewTitleInfo(
			new ViewTitleInfo() 
			{
				private long	last_avail_calc = -1;
				private int		last_avail;
				
				public Object 
				getTitleInfoProperty(
					int propertyID) 
				{
					Object result = null;
	
					// COConfigurationManager.setParameter( "subscriptions.wizard.shown", false );
					
					if (propertyID == TITLE_INDICATOR_TEXT) {
	
						boolean expanded = headerEntry.isExpanded();
	
						SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
						
						Subscription[] subs = subs_man.getSubscriptions(true);
						
						if ( expanded ){
	
							if (warnSub != null) {
								warnSub.setVisible(false);
							}
	
						}else{
	
							int total = 0;
							
							boolean warn = false;
	
							String error_str = "";
	
							for (Subscription s : subs) {
	
								SubscriptionHistory history = s.getHistory();
	
								total += history.getNumUnread();
	
								String last_error = history.getLastError();
	
								if (last_error != null && last_error.length() > 0) {
	
									boolean auth_fail = history.isAuthFail();
	
									if (history.getConsecFails() >= 3 || auth_fail) {
	
										warn = true;
	
										if (error_str.length() > 128) {
	
											if (!error_str.endsWith(", ...")) {
	
												error_str += ", ...";
											}
										} else {
	
											error_str += (error_str.length() == 0 ? "" : ", ")
													+ last_error;
										}
									}
								}
							}
	
							if (warnSub != null) {
								warnSub.setVisible(warn);
								warnSub.setToolTip(error_str);
							}
							
							if (total > 0) {
	
								result = String.valueOf( total );
							}
						}
						
						if (infoSub != null) {
  						if ( subs.length == 0 && !COConfigurationManager.getBooleanParameter( "subscriptions.wizard.shown", false )){
  							
  							long now = SystemTime.getMonotonousTime();
  							
  							if ( 	last_avail_calc == -1 ||
  									now - last_avail_calc > 60*1000 ){
  								
  								last_avail = subs_man.getKnownSubscriptionCount();
  								
  								last_avail_calc = now;
  							}
  							
  							if ( last_avail > 0 ){
  								
  								infoSub.setVisible( true );
  								
  								infoSub.setToolTip( 
  									MessageText.getString(
  										"subscriptions.info.avail",
  										new String[]{
  											String.valueOf( last_avail )
  										}));
  							}
  						}else{
  							
  							infoSub.setVisible( false );
  						}
						}
					}
	
					return( result );
				}
			});
	}

	protected void
	changeSubscription(
		final Subscription	subs )
	{
		refreshTitles( mdiEntryOverview );

		
		if ( subs.isSubscribed()){
			String key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if ( mdi != null ){
				mdi.loadEntryByID(key, true, true, subs);
			}

		} else {
			
			removeSubscription( subs);
		}
	}
	
	
	private MdiEntry createSubscriptionMdiEntry(Subscription subs) {
		
		if (!subs.isSubscribed()) {
			// user may have deleted subscrtipion, but our register is staill there
			return null;
		}
		
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		if ( mdi == null ){
			
				// closing down
		
			return( null );
		}
	
		final String key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());
		
		MdiEntry entry = mdi.createEntryFromEventListener(
				MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS,
				new UISWTViewEventListenerHolder(key, SubscriptionView.class, subs, null),
				key, true, subs, null);

		// This sets up the entry (menu, etc)
		SubscriptionMDIEntry entryInfo = new SubscriptionMDIEntry(subs, entry);
		subs.setUserData(SUB_ENTRYINFO_KEY, entryInfo);

		return entry;
	}
	
	protected void
	refreshTitles(
		MdiEntry		entry )
	{
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		
		if ( mdi == null ){

			return;
		}
		
		while( entry != null ){
	
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());

			String key = entry.getParentID();
			
			if ( key == null ){
				
				return;
			}
			
			entry = mdi.getEntry( key );
		}
	}

	protected void
	removeSubscription(
		final Subscription	subs )
	{
		synchronized( this ){

			String key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			
			if  (mdi != null ){
				
				mdi.closeEntry(key);
			}

		}
		
		refreshColumns();
	}
	
	protected void
	refreshColumns()
	{
		synchronized (columns) {
  		for ( Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();){
  			
  			TableColumn column = iter.next();
  			
  			column.invalidateCells();
  		}
		}
	}
	
	protected Graphic
	loadGraphic(
		UISWTInstance	swt,
		String			name )
	{
		Image	image = swt.loadImage( "com/aelitis/azureus/ui/images/" + name );

		Graphic graphic = swt.createGraphic(image );
		
		icon_list.add( graphic );
		
		return( graphic );
	}
	
	protected interface
	MenuCreator
	{
		public MenuItem
		createMenu(
			String 	resource_id );
		
		public void
		refreshView();
	}
	
	protected static void
	createMenus(
		final MenuManager		menu_manager,
		final MenuCreator		menu_creator,
		final Subscription		subs )
	{
		boolean is_search_template = subs.isSearchTemplate();
		
		if ( !is_search_template ){
			
			MenuItem menuItem = menu_creator.createMenu( "Subscription.menu.forcecheck" );
			menuItem.setText(MessageText.getString("Subscription.menu.forcecheck"));
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
					try {
						subs.getManager().getScheduler().downloadAsync( subs, true );
					} catch (SubscriptionException e) {
						Debug.out(e);
					}
				}
			});
			
			menuItem = menu_creator.createMenu( "Subscription.menu.clearall");
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
					subs.getHistory().markAllResultsRead();
					menu_creator.refreshView();
				}
			});
			
			menuItem = menu_creator.createMenu( "Subscription.menu.dirtyall");
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
					subs.getHistory().markAllResultsUnread();
					menu_creator.refreshView();
				}
			});
	
			menuItem = menu_creator.createMenu( "Subscription.menu.deleteall");
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
					subs.getHistory().deleteAllResults();
					menu_creator.refreshView();
				}
			});
			
			menuItem = menu_creator.createMenu( "Subscription.menu.reset");
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
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
			});
	
			try{
				Engine engine = subs.getEngine();
					
				if ( engine instanceof WebEngine ){
					
					if (((WebEngine)engine).isNeedsAuth()){
						
						menuItem = menu_creator.createMenu( "Subscription.menu.resetauth");
						menuItem.addListener(new SubsMenuItemListener() {
							public void selected( Subscription subs) {
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
						});
						
						menuItem = menu_creator.createMenu( "Subscription.menu.setcookies");
						menuItem.addListener(new SubsMenuItemListener() {
							public void selected( final Subscription subs) {
								try{
									Engine engine = subs.getEngine();
									
									if ( engine instanceof WebEngine ){
										
										final WebEngine we = (WebEngine)engine;
										
										UISWTInputReceiver entry = new SimpleTextEntryWindow();
										
										String[] req = we.getRequiredCookies();
										
										String	req_str = "";
										
										for ( String r:req ){
											
											req_str += (req_str.length()==0?"":";") + r + "=?";
										}
										entry.setPreenteredText( req_str, true );
										entry.maintainWhitespace(false);
										entry.allowEmptyInput( false );
										entry.setTitle("general.enter.cookies");
										entry.prompt(new UIInputReceiverListener() {
											public void UIInputReceiverClosed(UIInputReceiver entry) {
												if (!entry.hasSubmittedInput()){
													
													return;
												}
	
												try {
			  									String input = entry.getSubmittedInput().trim();
			  									
			  									if ( input.length() > 0 ){
			  										
			  										we.setCookies( input );
			  										
			  										subs.getManager().getScheduler().downloadAsync(subs, true);
			  									}
												}catch( Throwable e ){
													
													Debug.printStackTrace(e);
												}
											}
										});
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						});
					}
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
			
				// sep
			
			menu_creator.createMenu( "s1").setStyle( MenuItem.STYLE_SEPARATOR );

				// category
			
			menuItem = menu_creator.createMenu( "MyTorrentsView.menu.setCategory");
			menuItem.setStyle( MenuItem.STYLE_MENU );
			
			menuItem.addFillListener(
				new MenuItemFillListener()
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		data ) 
					{		
						addCategorySubMenu( menu_manager, menu, subs );
					}
				});
			
				// tag
			
			menuItem = menu_creator.createMenu( "label.tag");
			
			menuItem.setStyle( MenuItem.STYLE_MENU );
			
			menuItem.addFillListener(
				new MenuItemFillListener()
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		data ) 
					{		
						addTagSubMenu( menu_manager, menu, subs );
					}
				});
			
				// chat
			
			try{
				Engine engine = subs.getEngine();
							
				if ( engine instanceof WebEngine ){
					
					WebEngine web_engine = (WebEngine)subs.getEngine();
					
					final String url = web_engine.getSearchUrl( true );
					
					menuItem = menu_creator.createMenu( "label.chat");
					
					MenuBuildUtils.addChatMenu(
						menu_manager, 
						menuItem,
						new MenuBuildUtils.ChatKeyResolver() 
						{
							public String getChatKey(Object object) {
								
								return( "Subscription: " + url );
							}
						});
				}
			}catch( Throwable e ){
				
			}
			
			if ( subs.isUpdateable()){
				
				menuItem = menu_creator.createMenu( "MyTorrentsView.menu.rename");
				menuItem.addListener(new SubsMenuItemListener() {
					public void selected( final Subscription subs) {
						UISWTInputReceiver entry = new SimpleTextEntryWindow();
						entry.setPreenteredText(subs.getName(), false );
						entry.maintainWhitespace(false);
						entry.allowEmptyInput( false );
						entry.setLocalisedTitle(MessageText.getString("label.rename",
								new String[] {
									subs.getName()
								}));
						entry.prompt(new UIInputReceiverListener() {
							public void UIInputReceiverClosed(UIInputReceiver entry) {
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
						});
					}
				});
			}
			
			menuItem = menu_creator.createMenu( "Subscription.menu.upgrade");
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
					subs.resetHighestVersion();
				}
			});
				
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
		}
		
		MenuItem menuItem = menu_creator.createMenu( "label.copy.uri.to.clip");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected( Subscription subs) {
				ClipboardCopy.copyToClipBoard( subs.getURI());
			}
		});
		
		menuItem = menu_creator.createMenu( "Subscription.menu.export");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected( Subscription subs) {
				export( subs );
			}
		});
		
			// sep
		
		menu_creator.createMenu( "s2").setStyle( MenuItem.STYLE_SEPARATOR );
		
		if ( !is_search_template ){
				// change url
			
			try{
				Engine engine = subs.getEngine();
							
				if ( engine instanceof WebEngine ){
						
					menuItem = menu_creator.createMenu( "menu.change.url");
					menuItem.addListener(new SubsMenuItemListener() {
						public void selected( final Subscription subs) {
							UISWTInputReceiver entry = new SimpleTextEntryWindow();
							
							try{
								WebEngine web_engine = (WebEngine)subs.getEngine();
		
								entry.setPreenteredText(web_engine.getSearchUrl( true ), false );
								entry.maintainWhitespace(false);
								entry.allowEmptyInput( false );
								entry.setLocalisedTitle(MessageText.getString("change.url.msg.title",
										new String[] {
											subs.getName()
										}));
								entry.setMessage( "change.url.msg.desc" );
								entry.prompt(new UIInputReceiverListener() {
									public void UIInputReceiverClosed(UIInputReceiver entry) {
										if (!entry.hasSubmittedInput()){
											
											return;
										}
										
										String input = entry.getSubmittedInput().trim();
										
										if ( input.length() > 0 ){
											
											try{
												WebEngine web_engine = (WebEngine)subs.getEngine();
												
												web_engine.setSearchUrl( input );
												
												subs.cloneWithNewEngine( web_engine );
												
											}catch( Throwable e ){
												
												Debug.out(e);
											}
										}
									}
								});
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					});
					
				}
			}catch( Throwable e ){
				Debug.out( e );
			}
			
				// public
			
			menuItem = menu_creator.createMenu( "subs.prop.is_public");
			menuItem.setStyle( MenuItem.STYLE_CHECK );
			
			menuItem.addFillListener( new MenuItemFillListener(){
				public void menuWillBeShown( MenuItem menu, Object data ){
					menu.setData( subs.isPublic());
				}});
		
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( Subscription subs) {
					try{
						subs.setPublic( !subs.isPublic());
					}catch( Throwable e ){
						Debug.out(e);
					}
				}
			});
			
			if ( subs.isAutoDownloadSupported()){
				
					// auto-dl
				
				menuItem = menu_creator.createMenu( "subs.prop.is_auto");
				menuItem.setStyle( MenuItem.STYLE_CHECK );
				
				menuItem.addFillListener( new MenuItemFillListener(){
					public void menuWillBeShown( MenuItem menu, Object data ){
						menu.setData( subs.getHistory().isAutoDownload());
					}});
				
				menuItem.addListener(new SubsMenuItemListener() {
					public void selected( Subscription subs) {
						try{
							subs.getHistory().setAutoDownload(!subs.getHistory().isAutoDownload());
						}catch( Throwable e ){
							Debug.out(e);
						}
					}
				});
			}
			
				// refresh period
				
			menuItem = menu_creator.createMenu(  "subs.prop.update_period" );
			
			menuItem.addFillListener( new MenuItemFillListener(){
				public void menuWillBeShown( MenuItem menu, Object data ){
					int check_freq = subs.getHistory().getCheckFrequencyMins();
					
					String text = MessageText.getString( "subs.prop.update_period" );
					
					if ( check_freq!=Integer.MAX_VALUE ){
						
						text += " (" +  check_freq + " " + MessageText.getString( "ConfigView.text.minutes") + ")";
					}
					
					menu.setText( text + "..." );
				}});
			
			
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( final Subscription subs) {
					UISWTInputReceiver entry = new SimpleTextEntryWindow();
					entry.maintainWhitespace(false);
					entry.allowEmptyInput( false );
					
					int check_freq = subs.getHistory().getCheckFrequencyMins();
					
					entry.setPreenteredText( check_freq==Integer.MAX_VALUE?"":String.valueOf( check_freq ), false );
					
					entry.maintainWhitespace(false);
					
					entry.allowEmptyInput( false );
					
					entry.setLocalisedTitle(MessageText.getString("subscriptions.enter.freq"));
			
					entry.prompt(new UIInputReceiverListener() {
						public void UIInputReceiverClosed(UIInputReceiver entry) {
							if (!entry.hasSubmittedInput()) {
								return;
							}
							String input = entry.getSubmittedInput().trim();
							
							if ( input.length() > 0 ){
								
								try{
									subs.getHistory().setCheckFrequencyMins( Integer.parseInt( input ));
									
								}catch( Throwable e ){
									
								}
							}
						}
					});
				}
			});
		
				// rename
			
			menuItem = menu_creator.createMenu( "MyTorrentsView.menu.rename" );
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected( final Subscription subs) {
					UISWTInputReceiver entry = new SimpleTextEntryWindow();
					entry.maintainWhitespace(false);
					entry.allowEmptyInput( false );
					
					entry.setPreenteredText(subs.getName(), false );
					
					entry.maintainWhitespace(false);
					
					entry.allowEmptyInput( false );
					
					entry.setLocalisedTitle(MessageText.getString("label.rename",
							new String[] {
							subs.getName()
					}));
			
					entry.prompt(new UIInputReceiverListener() {
						public void UIInputReceiverClosed(UIInputReceiver entry) {
							if (!entry.hasSubmittedInput()) {
								return;
							}
							String input = entry.getSubmittedInput().trim();
							
							if ( input.length() > 0 ){
								
								subs.setLocalName( input );
							}
						}
					});
				}
			});
		}
		
		
		menuItem = menu_creator.createMenu( "Subscription.menu.remove");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected( Subscription subs) {
				removeWithConfirm( subs );
			}
		});
		
		menu_creator.createMenu( "s3").setStyle( MenuItem.STYLE_SEPARATOR );

		menuItem = menu_creator.createMenu( "Subscription.menu.properties");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected( Subscription subs){
				showProperties( subs );
			}
		});
	}
	
	private static void 
	addCategorySubMenu(
		MenuManager				menu_manager,
		MenuItem				menu,
		final Subscription		subs )
	{
		menu.removeAllChildItems();

		Category[] categories = CategoryManager.getCategories();
		
		Arrays.sort( categories );

		MenuItem m;

		if ( categories.length > 0 ){
			
			String	assigned_category = subs.getCategory();
			
			final Category uncat = CategoryManager.getCategory( Category.TYPE_UNCATEGORIZED );
						
			if ( uncat != null ){
				
				m = menu_manager.addMenuItem( menu, uncat.getName());
				
				m.setStyle( MenuItem.STYLE_RADIO );
								
				m.setData( new Boolean( assigned_category == null ));
				
				m.addListener(
					new MenuItemListener() 
					{
						public void
						selected(
							MenuItem			menu,
							Object 				target )
						{
							assignSelectedToCategory( subs, uncat );
						}
					});
				

				m = menu_manager.addMenuItem( menu, "sep1" );
				
				m.setStyle( MenuItem.STYLE_SEPARATOR );
			}

			for ( int i=0; i<categories.length; i++ ){
				
				final Category cat = categories[i];
				
				if ( cat.getType() == Category.TYPE_USER) {
					
					m = menu_manager.addMenuItem( menu, "!" + cat.getName() + "!" );
					
					m.setStyle( MenuItem.STYLE_RADIO );
										
					m.setData( new Boolean( assigned_category != null && assigned_category.equals( cat.getName())));
					
					m.addListener(
						new MenuItemListener() 
						{
							public void
							selected(
								MenuItem			menu,
								Object 				target )
							{
								assignSelectedToCategory( subs, cat );
							}
						});
				}
			}

			m = menu_manager.addMenuItem( menu, "sep2" );
			
			m.setStyle( MenuItem.STYLE_SEPARATOR );
		}

		m = menu_manager.addMenuItem( menu, "MyTorrentsView.menu.setCategory.add" );
		
		m.addListener(
				new MenuItemListener() 
				{
					public void
					selected(
						MenuItem			menu,
						Object 				target )
					{
						addCategory( subs );
					}
				});

	}

	private static void 
	addCategory(
		Subscription			subs )
	{
		CategoryAdderWindow adderWindow = new CategoryAdderWindow(Display.getDefault());
		
		Category newCategory = adderWindow.getNewCategory();
		
		if ( newCategory != null ){
		
			assignSelectedToCategory( subs, newCategory );
		}
	}

	private static void 
	assignSelectedToCategory(
		Subscription		subs,
		Category 			category )
	{
		if ( category.getType() == Category.TYPE_UNCATEGORIZED ){
		
			subs.setCategory( null );
			
		}else{
			
			subs.setCategory( category.getName());
		}
	}

	private static void 
	addTagSubMenu(
		MenuManager				menu_manager,
		MenuItem				menu,
		final Subscription		subs )
	{
		menu.removeAllChildItems();

		TagManager tm = TagManagerFactory.getTagManager();
		
		List<Tag> tags = tm.getTagType( TagType.TT_DOWNLOAD_MANUAL ).getTags();
		
		tags = TagUIUtils.sortTags( tags );
					
		long	tag_id = subs.getTagID();
			
		Tag assigned_tag = tm.lookupTagByUID( tag_id );
		
		MenuItem m = menu_manager.addMenuItem( menu, "label.no.tag" );
				
		m.setStyle( MenuItem.STYLE_RADIO );
							
		m.setData( new Boolean( assigned_tag == null ));
				
		m.addListener(
			new MenuItemListener() 
			{
				public void
				selected(
					MenuItem			menu,
					Object 				target )
				{
					subs.setTagID( -1 );
				}
			});
				

		m = menu_manager.addMenuItem( menu, "sep1" );
				
		m.setStyle( MenuItem.STYLE_SEPARATOR );
	
		
		List<String>	menu_names 		= new ArrayList<String>();
		Map<String,Tag>	menu_name_map 	= new IdentityHashMap<String, Tag>();

		for ( Tag t: tags ){
			
			if ( !t.isTagAuto()){
				
				String name = t.getTagName( true );
				
				menu_names.add( name );
				menu_name_map.put( name, t );
			}
		}
			
		List<Object>	menu_structure = MenuBuildUtils.splitLongMenuListIntoHierarchy( menu_names, TagUIUtils.MAX_TOP_LEVEL_TAGS_IN_MENU );
		
		for ( Object obj: menu_structure ){

			List<Tag>	bucket_tags = new ArrayList<Tag>();
			
			MenuItem parent_menu;
			
			if ( obj instanceof String ){
				
				parent_menu = menu;
				
				bucket_tags.add( menu_name_map.get((String)obj));
				
			}else{
				
				Object[]	entry = (Object[])obj;
				
				List<String>	tag_names = (List<String>)entry[1];
				
				boolean	has_selected = false;
				
				for ( String name: tag_names ){
					
					Tag tag = menu_name_map.get( name );
					
					bucket_tags.add( tag );
					
					if ( assigned_tag == tag ){
						
						has_selected = true;
					}
				}
				
				parent_menu = menu_manager.addMenuItem (menu, "!" + (String)entry[0] + (has_selected?" (*)":"") + "!" );
				
				parent_menu.setStyle( MenuItem.STYLE_MENU );
			}
			
			for ( final Tag tag: bucket_tags ){
			
				m = menu_manager.addMenuItem( parent_menu, tag.getTagName( false ));
						
				m.setStyle( MenuItem.STYLE_RADIO );
											
				m.setData( new Boolean( assigned_tag == tag ));
						
				m.addListener(
					new MenuItemListener() 
					{
						public void
						selected(
							MenuItem			menu,
							Object 				target )
						{
							subs.setTagID( tag.getTagUID());
						}
					});
			}
		}
		
		m = menu_manager.addMenuItem( menu, "sep2" );
			
		m.setStyle( MenuItem.STYLE_SEPARATOR );

		m = menu_manager.addMenuItem( menu, "label.add.tag" );
		
		m.addListener(
			new MenuItemListener() 
			{
				public void
				selected(
					MenuItem			menu,
					Object 				target )
				{
					addTag( subs );
				}
			});
	}

	private static void 
	addTag(
		final Subscription			subs )
	{
		TagUIUtilsV3.showCreateTagDialog(new UIFunctions.TagReturner() {
			public void returnedTags(Tag[] tags) {
				if ( tags != null ){
					for (Tag new_tag : tags) {
						subs.setTagID( new_tag.getTagUID());
					}
				}
			}
		});
	}
	
	
	protected static void 
	export(
		final Subscription			subs )
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport()
				{
					FileDialog dialog = 
						new FileDialog( Utils.findAnyShell(), SWT.SYSTEM_MODAL | SWT.SAVE );
					
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

	protected static void
	removeWithConfirm( 
		final Subscription		subs )
	{
		MessageBoxShell mb = 
			new MessageBoxShell(
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
		
		mb.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
					subs.setSubscribed( false );
				}
			}
		});
	}
	
	protected static void
	showProperties(
		Subscription			subs )
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
			
			engine_str +=  ", eid=" + engine.getId();
			
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
				"TableColumn.header.category",
				"TableColumn.header.tag.name",
			};
		
		String	category_str;
		
		String category = subs.getCategory();
		
		if ( category == null ){
			
			category_str = MessageText.getString( "Categories.uncategorized" );
			
		}else{
			
			category_str = category;
		}
				
		Tag tag = TagManagerFactory.getTagManager().lookupTagByUID( subs.getTagID() );
		
		String tag_str = tag==null?"":tag.getTagName( true );
		
		int	 check_freq			= history.getCheckFrequencyMins();
		long last_new_result 	= history.getLastNewResultTime();
		long next_scan 			= history.getNextScanTime();
		
		String[] values = { 
				String.valueOf( history.isEnabled()),
				String.valueOf( subs.isPublic()),
				String.valueOf( history.isAutoDownload()),
				String.valueOf( subs.isAutoDownloadSupported()),
				(check_freq==Integer.MAX_VALUE?"":(String.valueOf( history.getCheckFrequencyMins() + " " + MessageText.getString( "ConfigView.text.minutes")))),
				df.format(new Date( history.getLastScanTime())),
				( last_new_result==0?"":df.format(new Date( last_new_result ))),
				( next_scan == Long.MAX_VALUE?"":df.format(new Date( next_scan ))),
				(last_error.length()==0?MessageText.getString("PeersView.uniquepiece.none"):last_error),
				String.valueOf( history.getNumRead()),
				String.valueOf( history.getNumUnread()),
				String.valueOf( subs.getAssociationCount()),
				String.valueOf( subs.getVersion()),
				subs.getHighestVersion() > subs.getVersion()?String.valueOf( subs.getHighestVersion()):null,
				subs.getCachedPopularity()<=1?null:String.valueOf( subs.getCachedPopularity()),
				engine_str + ", sid=" + subs.getID(),
				auth_str,
				category_str,
				tag_str,
			};
		
		new PropertiesWindow( subs.getName(), keys, values );
	}

	private static String
	toString(
		String[]	strs )
	{
		String	res = "";
		
		for(int i=0;i<strs.length;i++){
			res += (i==0?"":",") + strs[i];
		}
		
		return( res );
	}
	
	private abstract static class SubsMenuItemListener implements MenuItemListener {
		public final void selected(MenuItem menu, Object target) {
			if (target instanceof MdiEntry) {
				MdiEntry info = (MdiEntry) target;
				Subscription subs = (Subscription) info.getDatasource();
				
				try {
					selected( subs);
				} catch (Throwable t) {
					Debug.out(t);
				}
			}else if ( target instanceof TableRow ){
				
				Object ds = ((TableRow)target).getDataSource();
				
				if ( ds instanceof Subscription ){
					
					try {
						selected((Subscription)ds);
						
					} catch (Throwable t) {
						Debug.out(t);
					}
				}
			}
		}

		public abstract void selected(Subscription subs);
	}
}
