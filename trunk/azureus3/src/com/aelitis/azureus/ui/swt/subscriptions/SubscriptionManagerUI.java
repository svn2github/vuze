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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

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
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventListenerHolder;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;

public class 
SubscriptionManagerUI 
{
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
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");
		if (uiClassic) {
			registerAllSubscriptions();
		}
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
			});	
		
		createConfigModel();
	}

	private void createConfigModel() {
		final SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();

		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_ROOT, "Subscriptions");

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

							SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();

							if ( torrent != null && subs_man != null ){
								
								byte[]	hash = torrent.getHash();
								
								Subscription[] subs = subs_man.getKnownSubscriptions( hash );
																
								for (int i=0;i<subs.length;i++){
									
									Subscription sub = subs[i];
									
									if ( sub.hasAssociation( hash )){
										
										String key = "Subscription_" + ByteFormatter.encodeString(sub.getPublicKey());
										MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
										if ( mdi != null ){
											mdi.showEntryByID(key);
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
				      		 
				      		 uif.openView( UIFunctions.VIEW_CONFIG, "Subscriptions" );
				      	 }
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
					registerSubscriptionViewMdiEntry( subscription, false );
				}
	
				public void
				subscriptionChanged(
					Subscription		subscription )
				{
					changeSubscription( subscription );
				}
				
				public void 
				subscriptionSelected(
					Subscription sub )
				{	
					
					String key = "Subscription_" + ByteFormatter.encodeString(sub.getPublicKey());
					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					if ( mdi != null ){
						mdi.showEntryByID(key);
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
			});
		
		if (!uiClassic) {
			registerAllSubscriptions();
		}

		mdi.addListener(
			new MdiListener() 
			{
				private long last_select = 0;
				
				public void 
				mdiEntrySelected(
					MdiEntry new_entry,
					MdiEntry old_entry ) 
				{
					if ( new_entry == old_entry && (new_entry instanceof BaseMdiEntry) ){
						
						UISWTViewEventListener eventListener = ((BaseMdiEntry)new_entry).getEventListener();
						
						if ( eventListener instanceof SubscriptionView ){
							
							try{
								
								if ( SystemTime.getMonotonousTime() - last_select > 1000 ){
									
									((SubscriptionView)eventListener).updateBrowser( false );
								}
							}finally{
								
								last_select = SystemTime.getMonotonousTime();
							}
						}
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
						Subscription[] subs = subs_man == null ? new Subscription[0] : subs_man.getSubscriptions(true);
						
						if ( expanded ){
	
							warnSub.setVisible(false);
	
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
	
							warnSub.setVisible(warn);
							warnSub.setToolTip(error_str);
							
							if (total > 0) {
	
								result = String.valueOf( total );
							}
						}
						
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
	
					return( result );
				}
			});
	}

	private void 
	registerAllSubscriptions() 
	{
		SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
		if (subs_man == null) {
			return;
		}
		Subscription[]	subs = subs_man.getSubscriptions( true );
		
		Arrays.sort(
			subs,
			new Comparator<Subscription>()
			{
				public int 
				compare(
					Subscription o1, Subscription o2 )
				{
					return( o1.getName().compareToIgnoreCase( o2.getName()));
				}
			});
		
		for (int i=0;i<subs.length;i++){
			
			registerSubscriptionViewMdiEntry( subs[i], false );
		}
	}

	protected void
	changeSubscription(
		final Subscription	subs )
	{
		refreshTitles( mdiEntryOverview );

		if ( !subs.isSubscribed()){
			
			removeSubscription( subs);
		}
	}
	
	protected void
	registerSubscriptionViewMdiEntry(
		final Subscription		subs,
		final boolean			show )
	{
		if ( !subs.isSubscribed()){
			
			return;
		}
		
			// hack to hide useless entries
		
		String name = subs.getName();
		
		if ( name.startsWith( "Search Template: " )){
			
			return;
		}
	
		refreshColumns();
		
		final String key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());				

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

		if ( mdi == null ){
			
				// closing down
			
			return;
		}

		mdi.registerEntry(key, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				return createSubscriptionMdiEntry(subs);
			}
		});

		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");
		if (uiClassic && !show) {
			return;
		}
		
		mdi.loadEntryByID(key, show, true, subs);
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
				MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY,
				new UISWTViewEventListenerHolder(key, SubscriptionView.class, subs, null),
				key, true, subs, MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS);

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
}
