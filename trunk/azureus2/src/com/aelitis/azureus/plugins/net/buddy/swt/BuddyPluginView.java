/*
 * Created on Mar 19, 2008
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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


package com.aelitis.azureus.plugins.net.buddy.swt;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.menus.MenuContext;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.utils.FormattersImpl;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyListener;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableLifecycleAdapter;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2Listener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAdapter;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginViewInterface;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatInstance;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTracker;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTrackerListener;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;


public class 
BuddyPluginView
	implements UISWTViewEventListener, BuddyPluginViewInterface
{
	private BuddyPlugin		plugin;
	private UISWTInstance	ui_instance;
	
	private BuddyPluginViewInstance		current_instance;
	
	private Image iconNLI;
	private Image iconIDLE;
	private Image iconIN;
	private Image iconOUT;
		
	public
	BuddyPluginView(
		BuddyPlugin		_plugin,
		UIInstance		_ui_instance,
		String			VIEW_ID )
	{
		plugin			= _plugin;
		ui_instance		= (UISWTInstance)_ui_instance;
		
		plugin.getAZ2Handler().addListener(
			new BuddyPluginAZ2Listener()
			{
				public void
				chatCreated(
					final BuddyPluginAZ2.chatInstance		chat )
				{
					final Display display = ui_instance.getDisplay();
					
					if ( !display.isDisposed()){
						
						display.asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									if ( !display.isDisposed()){
									
										new BuddyPluginViewChat( plugin, display, chat );
									}
								}
							});
					}
				}
				
				public void
				chatDestroyed(
					BuddyPluginAZ2.chatInstance		chat )
				{
				}
			});
		

		SimpleTimer.addEvent("BuddyStatusInit", SystemTime.getOffsetTime(1000),
				new TimerEventPerformer() {
					public void perform(
							TimerEvent event ) 
					{
						UISWTStatusEntry label = ui_instance.createStatusEntry();

						label.setText(MessageText.getString("azbuddy.tracker.bbb.status.title"));

						new statusUpdater(ui_instance);
					}
				});

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				ImageLoader imageLoader = ImageLoader.getInstance();

				iconNLI = imageLoader.getImage( "bbb_nli" );
				iconIDLE = imageLoader.getImage( "bbb_idle" );
				iconIN = imageLoader.getImage( "bbb_in" );
				iconOUT = imageLoader.getImage( "bbb_out" );
			}
		});
		
		ui_instance.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, this );
					
		initBeta();
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		switch( event.getType() ){

			case UISWTViewEvent.TYPE_CREATE:{
				
				if ( current_instance != null ){
					
					return( false );
				}
								
				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
				

				current_instance = new BuddyPluginViewInstance( this, plugin, ui_instance, (Composite)event.getData());
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					if ( current_instance != null ){
						
						current_instance.destroy();
					}
				}finally{
					
					current_instance = null;
				}
				
				break;
			}
		}
		
		return true;
	}
	
	public void 
	openChat(
		final ChatInstance chat )
	{
		final Display display = Display.getDefault();
	
		if ( display.isDisposed()){
			
			return;
		}
		
		display.asyncExec(
			new Runnable()
			{
				public void
				run()
				{
					if ( display.isDisposed()){
						
						return;
					}
				
					new BuddyPluginViewBetaChat( BuddyPluginView.this, plugin, chat );
				}
			});
	}
	
	protected class
	statusUpdater
		implements BuddyPluginTrackerListener
	{
		private UISWTStatusEntry	label;
		private UISWTStatusEntry	status;
		private BuddyPluginTracker	tracker;
		
		private TimerEventPeriodic	update_event;

		private CryptoManager	crypto;
		private boolean			crypto_ok;
		private boolean			has_buddies;
		
		protected
		statusUpdater(
			UISWTInstance		instance )
		{
			status	= ui_instance.createStatusEntry();
			label 	= ui_instance.createStatusEntry();
			
			label.setText( MessageText.getString( "azbuddy.tracker.bbb.status.title" ));
			label.setTooltipText( MessageText.getString( "azbuddy.tracker.bbb.status.title.tooltip" ));
			
			tracker = plugin.getTracker();
				
			status.setText( "" );
			
			status.setImageEnabled( true );
			
			tracker.addListener( this );
			
			has_buddies = plugin.getBuddies().size() > 0;
			
			status.setVisible( tracker.isEnabled() && has_buddies);
			label.setVisible( tracker.isEnabled() && has_buddies);
		
			/*
			MenuItem mi = plugin.getPluginInterface().getUIManager().getMenuManager().addMenuItem(
									status.getMenuContext(),
									"dweeble" );
			
			mi.addListener(
				new MenuItemListener()
				{
					public void
					selected(
						MenuItem			menu,
						Object 				target )
					{
						System.out.println( "whee" );
					}
				});
			*/
			
			UISWTStatusEntryListener click_listener = 
				new UISWTStatusEntryListener()
			{
					public void 
					entryClicked(
						UISWTStatusEntry entry )
					{
						try{
							plugin.getPluginInterface().getUIManager().openURL(
									new URL( "http://wiki.vuze.com" ));
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				};
				
			status.setListener( click_listener );
			label.setListener( click_listener );
	
			
			plugin.addListener( 
				new BuddyPluginAdapter()
				{
					public void
					initialised(
						boolean		available )
					{
					}
					
					public void
					buddyAdded(
						BuddyPluginBuddy	buddy )
					{
						if ( !has_buddies ){
							
							has_buddies = true;
						
							updateStatus();
						}
					}
					
					public void
					buddyRemoved(
						BuddyPluginBuddy	buddy )
					{
						has_buddies	= plugin.getBuddies().size() > 0;	
						
						if ( !has_buddies ){
							
							updateStatus();
						}
					}

					public void
					buddyChanged(
						BuddyPluginBuddy	buddy )
					{
					}
					
					public void
					messageLogged(
						String		str,
						boolean		error )
					{
					}
					
					public void
					enabledStateChanged(
						boolean enabled )
					{
					}
				});
			
			crypto = CryptoManagerFactory.getSingleton();
			
			crypto.addKeyListener(
				new CryptoManagerKeyListener()
				{
					public void
					keyChanged(
						CryptoHandler		handler )
					{
					}
					
					public void
					keyLockStatusChanged(
						CryptoHandler		handler )
					{
						boolean	ok = crypto.getECCHandler().isUnlocked();
						
						if ( ok != crypto_ok ){
							
							crypto_ok = ok;
							
							updateStatus();
						}
					}
				});
			
			crypto_ok = crypto.getECCHandler().isUnlocked();
				
			updateStatus();
		}
				
		public void
		networkStatusChanged(
			BuddyPluginTracker	tracker,
			int					new_status )
		{
			updateStatus();
		}
		
		protected synchronized void
		updateStatus()
		{
			if ( tracker.isEnabled() && has_buddies ){
				
				status.setVisible( true );
				label.setVisible( true );
				
				if ( has_buddies && !crypto_ok ){
					
					status.setImage( iconNLI );
					
					status.setTooltipText( MessageText.getString( "azbuddy.tracker.bbb.status.nli" ));

					disableUpdates();
					
				}else{
					
					int	network_status = tracker.getNetworkStatus();
					
					if ( network_status == BuddyPluginTracker.BUDDY_NETWORK_IDLE ){
						
						status.setImage( iconIDLE );
						
						status.setTooltipText( MessageText.getString( "azbuddy.tracker.bbb.status.idle" ));
						
						disableUpdates();
						
					}else if ( network_status == BuddyPluginTracker.BUDDY_NETWORK_INBOUND ){
						
						status.setImage( iconIN );
						
						enableUpdates();
						
					}else{
						
						status.setImage( iconOUT );
						
						enableUpdates();
					}
				}
			}else{
				
				disableUpdates();
				
				status.setVisible( false );
				label.setVisible( false );
			}
		}
		
		protected void
		enableUpdates()
		{
			if ( update_event == null ){
				
				update_event = SimpleTimer.addPeriodicEvent(
					"Buddy:GuiUpdater",
					2500,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event ) 
						{	
							synchronized( statusUpdater.this ){
								
								if ( tracker.isEnabled() && ( crypto_ok || !has_buddies )){
									
									String	tt;
															
									int ns = tracker.getNetworkStatus();
									
									if ( ns == BuddyPluginTracker.BUDDY_NETWORK_IDLE ){
										
										tt = MessageText.getString( "azbuddy.tracker.bbb.status.idle" );
									
									}else if ( ns == BuddyPluginTracker.BUDDY_NETWORK_INBOUND ){
										
										tt = MessageText.getString( "azbuddy.tracker.bbb.status.in" ) + ": " + DisplayFormatters.formatByteCountToKiBEtcPerSec( tracker.getNetworkReceiveBytesPerSecond());
										
									}else{
										
										tt = MessageText.getString( "azbuddy.tracker.bbb.status.out" ) + ": " + DisplayFormatters.formatByteCountToKiBEtcPerSec( tracker.getNetworkSendBytesPerSecond());
									}
																			
									status.setTooltipText( tt );
								}
							}
						}					
					});
			}
		}
		
		protected void
		disableUpdates()
		{
			if ( update_event != null ){

				update_event.cancel();
				
				update_event = null;
			}
		}
		
		public void 
		enabledStateChanged(
			BuddyPluginTracker 		tracker,
			boolean 				enabled ) 
		{
			updateStatus();
		}
	}
	
	private HashMap<UISWTView,BetaSubViewHolder> beta_subviews = new HashMap<UISWTView,BetaSubViewHolder>();
	
	private UISWTStatusEntry	beta_status;
	private Image				bs_chat_gray;
	private Image				bs_chat_green;
	
	private void
	initBeta()
	{
		if ( plugin.isBetaEnabled() && plugin.getBeta().isAvailable()){

			addBetaSubviews( true );
			
			beta_status	= ui_instance.createStatusEntry();

			beta_status.setImageEnabled( true );
			
			beta_status.setVisible( true );
			
			beta_status.setTooltipText(MessageText.getString( "label.no.messages" ));
			
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					ImageLoader imageLoader = ImageLoader.getInstance();

					bs_chat_gray	= imageLoader.getImage( "dchat_gray" );
					bs_chat_green 	= imageLoader.getImage( "dchat_green" );
					
					beta_status.setImage( bs_chat_gray );
				}
			});
		}
	}
	
	private void
	addBetaSubviews(
		boolean	enable )
	{
		String[] views = {
			TableManager.TABLE_MYTORRENTS_ALL_BIG,
			TableManager.TABLE_MYTORRENTS_INCOMPLETE,
			TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
			TableManager.TABLE_MYTORRENTS_COMPLETE,
		};
		
		if ( enable ){
			
			TagManagerFactory.getTagManager().addTaggableLifecycleListener(
				Taggable.TT_DOWNLOAD,
				new TaggableLifecycleAdapter()
				{
					public void
					taggableTagged(
						TagType			tag_type,	
						Tag				tag,
						Taggable		taggable )
					{
						if ( tag_type.getTagType() == TagType.TT_DOWNLOAD_MANUAL ){
							
							DownloadManager dm  = (DownloadManager)taggable;
							
							for ( BetaSubViewHolder h: beta_subviews.values()){
								
								h.tagsUpdated( dm );
							}
						}
					}
					
					public void
					taggableUntagged(
						TagType			tag_type,	
						Tag				tag,
						Taggable		taggable )
					{
						if ( tag_type.getTagType() == TagType.TT_DOWNLOAD_MANUAL ){
							
							DownloadManager dm  = (DownloadManager)taggable;
							
							for ( BetaSubViewHolder h: beta_subviews.values()){
								
								h.tagsUpdated( dm );
							}
						}
					}
				});
			
			UISWTViewEventListener listener = 
				new UISWTViewEventListener()
				{	
					public boolean 
					eventOccurred(
						UISWTViewEvent event ) 
					{
						UISWTView 	currentView = event.getView();
						
						switch (event.getType()) {
							case UISWTViewEvent.TYPE_CREATE:{
								
								beta_subviews.put(currentView, new BetaSubViewHolder());
								
								break;
							}
							case UISWTViewEvent.TYPE_INITIALIZE:{
							
								BetaSubViewHolder subview = beta_subviews.get(currentView);
								
								if ( subview != null ){
									
									subview.initialise(event.getView(), (Composite)event.getData());
								}
		
								break;
							}
							case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:{
								
								BetaSubViewHolder subview = beta_subviews.get(currentView);
								
								if ( subview != null ){
									
									subview.setDataSource( event.getData());
								}
								
								break;
							}
							case UISWTViewEvent.TYPE_FOCUSGAINED:{
								
								BetaSubViewHolder subview = beta_subviews.get(currentView);
								
								if ( subview != null ){
									
									subview.gotFocus();
								}
								
								break;
							}
							case UISWTViewEvent.TYPE_FOCUSLOST:{
								
								BetaSubViewHolder subview = beta_subviews.get(currentView);
								
								if ( subview != null ){
									
									subview.lostFocus();
								}
								
								break;
							}
							case UISWTViewEvent.TYPE_DESTROY:{
								
								BetaSubViewHolder subview = beta_subviews.remove(currentView);
							
								if ( subview != null ){
									
									subview.destroy();
								}
								
								break;
							}
						}
						return true;
					}
				};
				
			for ( String table_id: views ){
				
				ui_instance.addView(table_id, "azbuddy.ui.menu.chat",	listener );
			}
		}else{
			
			for ( String table_id: views ){
				
				ui_instance.removeViews( table_id, "azbuddy.ui.menu.chat" );
			}
			
			for ( UISWTView entry: new ArrayList<UISWTView>(beta_subviews.keySet())){
				
				entry.closeView();
			}
			
			beta_subviews.clear();
		}
	}
	
	private static Map<String,Object[]>	pending_msg_map = new HashMap<String,Object[]>();
	private static TimerEventPeriodic	pending_msg_event;
	
	protected void
	betaMessagePending(
		ChatInstance		chat,
		Control				comp,
		boolean				is_pending )
	{
		synchronized( pending_msg_map ){
			
			String key = chat.getNetAndKey();
			
			Object[] entry = pending_msg_map.get( key );
			
			if ( is_pending ){
				
				if ( entry == null ){
					
					entry = new Object[]{ 1, new HashSet<Control>(), chat };
					
					pending_msg_map.put( key, entry );
					
				}else{
					
					entry[0] = ((Integer)entry[0]) + 1;
				}
				
				((HashSet<Control>)entry[1]).add( comp );
				
				if ( pending_msg_event == null ){
					
					pending_msg_event = 
						SimpleTimer.addPeriodicEvent(
							"BPPM",
							2500,
							new TimerEventPerformer()
							{	
								private int	tick_count = 0;
								
								private Set<ChatInstance>	latest_instances = new HashSet<ChatInstance>();
								
								private List<MenuItem>		menu_items = new ArrayList<MenuItem>();
								
								public void 
								perform(
									TimerEvent event )
								{
									tick_count++;
									
									synchronized( pending_msg_map ){
										
										Set<ChatInstance>	current_instances = new HashSet<BuddyPluginBeta.ChatInstance>();
										
										Iterator<Map.Entry<String,Object[]>> it = pending_msg_map.entrySet().iterator();
										
										String tt_text = "";
										
										while( it.hasNext()){
											
											Map.Entry<String,Object[]> map_entry = it.next();
											
											Object[] entry = map_entry.getValue();
											
											ChatInstance chat = (ChatInstance)entry[2];
											
											if ( chat.isDestroyed()){
												
												it.remove();
												
											}else{
											
												HashSet<Control> comps = ((HashSet<Control>)entry[1]);
												
												Iterator<Control>	control_it = comps.iterator();
												
												while( control_it.hasNext()){
													
													Control c = control_it.next();
													
													if ( c.isDisposed()){
														
														it.remove();
													}
												}
												
												if ( comps.size() == 0 ){
													
													it.remove();
													
												}else{
												
													current_instances.add( chat );
													
													String	short_name = chat.getName();
													
													if ( short_name.length() > 60 ){
														
														short_name = short_name.substring( 0, 60 ) + "...";
													}

													tt_text += (tt_text.length()==0?"":"\n") + entry[0] + " - " + short_name;
												}
											}
										}
										
										
										if ( pending_msg_map.size() == 0 ){
											
											pending_msg_event.cancel();
											
											pending_msg_event = null;
										}
										
										if ( current_instances.size() == 0 ){
											
											beta_status.setTooltipText( MessageText.getString( "label.no.messages" ));
											
											beta_status.setImage( bs_chat_gray );
											
											for ( MenuItem mi: menu_items ){
												
												mi.remove();
											}
										}else{
											
											if ( !latest_instances.equals( current_instances )){
											
												for ( MenuItem mi: menu_items ){
													
													mi.remove();
												}
												
												menu_items.clear();
												
												MenuManager menu_manager = plugin.getPluginInterface().getUIManager().getMenuManager();

												MenuContext mc = beta_status.getMenuContext();
																	
												for ( final ChatInstance chat: current_instances ){
												
													String	short_name = chat.getName();
													
													if ( short_name.length() > 60 ){
														
														short_name = short_name.substring( 0, 60 ) + "...";
													}
													
													MenuItem mi = menu_manager.addMenuItem( mc, "!" + short_name + "!" );
													
													mi.addListener(
														new MenuItemListener() {
															
															public void selected(MenuItem menu, Object target) {
															
																openChat( chat );
															}
														});
													
													menu_items.add( mi );
												}
															
												latest_instances = current_instances;
											}
											
											beta_status.setTooltipText( tt_text );
										
											beta_status.setImage( tick_count%2==0?bs_chat_gray:bs_chat_green);
										}
									}
								}
							});
				}
			}else{
				
				if ( entry != null ){
					
					pending_msg_map.remove( key );
					
					if ( pending_msg_event == null ){
						
						Debug.out( "eh?" );
					}
				}
			}
		}
	}
	
	private static AsyncDispatcher	public_dispatcher 	= new AsyncDispatcher();
	private static AsyncDispatcher	anon_dispatcher 	= new AsyncDispatcher();
	
	private static AtomicInteger	public_done = new AtomicInteger();
	private static AtomicInteger	anon_done 	= new AtomicInteger();
	
	private class
	BetaSubViewHolder
	{
		private int CHAT_DOWNLOAD 		= 0;
		private int CHAT_TAG	 		= 1;
		private int CHAT_GENERAL		= 2;
		private int CHAT_FAVOURITES		= 3;
		
		private Composite[]		chat_composites;
		
		private List<Button>	mode_buttons = new ArrayList<Button>();
		
		private Group			middle;
		
		private	CTabFolder  	tab_folder;
		private CTabItem 		public_item;
		private CTabItem 		anon_item;
		
		private int				last_build_chat_mode	= -1;
		private int				chat_mode				= CHAT_DOWNLOAD;
		
		private Download		current_download;
		private Tag				current_tag;
		private String			current_general;
		
		private String			current_favourite_net;
		private String			current_favourite_key;
		
		private Tag				current_ds_tag;

		private boolean			have_focus;
		private boolean			rebuild_outstanding	= true;
		
		private
		BetaSubViewHolder()
		{
		}
		
		private void
		initialise(
			UISWTView		view,
			Composite		parent )
		{		
			UISWTView parent_view = view.getParentView();
			
			if ( parent_view != null ){
			
				Object initial_ds = parent_view.getInitialDataSource();
			
				if ( initial_ds instanceof Tag ){
					
					current_ds_tag = (Tag)initial_ds;
				}
			}
			
			final Composite composite	= parent;
			
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.marginTop = 4;
			layout.marginRight = 4;
			composite.setLayout(layout);
			
			GridData grid_data = new GridData(GridData.FILL_BOTH );
			composite.setLayoutData(grid_data);

				// left
			
			Group lhs = new Group( composite, SWT.NULL );
			lhs.setText( MessageText.getString( "label.chat.type" ));
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.horizontalSpacing = 1;
			layout.verticalSpacing = 1;
			lhs.setLayout(layout);
			grid_data = new GridData(GridData.FILL_VERTICAL );
			//grid_data.widthHint = 200;
			lhs.setLayoutData(grid_data);
			
			Button downloads = new Button( lhs, SWT.TOGGLE );		
			downloads.setText( MessageText.getString( "v3.MainWindow.button.download" ));
			downloads.setData( CHAT_DOWNLOAD );
			
			Button tags = new Button( lhs, SWT.TOGGLE );			
			tags.setText( MessageText.getString( "label.tags" ));
			tags.setData( CHAT_TAG );
			
			Button general = new Button( lhs, SWT.TOGGLE );			
			general.setText( MessageText.getString( "ConfigView.section.global" ));
			general.setData( CHAT_GENERAL );
			
			Button favourites = new Button( lhs, SWT.TOGGLE );
			favourites.setText( MessageText.getString( "label.favorites" ));
			favourites.setData( CHAT_FAVOURITES );
			
				// middle
			
			middle = new Group( composite, SWT.NULL );
			layout = new GridLayout();
			layout.numColumns = 1;
			middle.setLayout(layout);
			grid_data = new GridData(GridData.FILL_VERTICAL );
			grid_data.widthHint = 0;
			middle.setLayoutData(grid_data);

			middle.setText( "" );
			
			middle.setVisible( false );
			
			downloads.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					buildChatMode( CHAT_DOWNLOAD, true );
				}});
			
			tags.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					
					buildChatMode( CHAT_TAG, true );
				}});	
			
			general.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					
					buildChatMode( CHAT_GENERAL, true );
				}});
			
			favourites.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					
					buildChatMode( CHAT_FAVOURITES, true );
				}});
			
			downloads.setSelection( true );
			
			mode_buttons.add( downloads );
			mode_buttons.add( tags );
			mode_buttons.add( general );
			mode_buttons.add( favourites );
			
			setupButtonGroup( mode_buttons );
			
				// chat tab area
			
			tab_folder = new CTabFolder(composite, SWT.LEFT);
			
			tab_folder.setTabHeight(20);
			grid_data = new GridData(GridData.FILL_BOTH);
			tab_folder.setLayoutData(grid_data);
			
				// public
			
			public_item = new CTabItem(tab_folder, SWT.NULL);

			public_item.setText( MessageText.getString( "label.public.chat" ));
			public_item.setData( AENetworkClassifier.AT_PUBLIC );
			
			Composite public_composite = new Composite( tab_folder, SWT.NULL );
	
			public_item.setControl( public_composite );
			
			grid_data = new GridData(GridData.FILL_BOTH );
			public_composite.setLayoutData(grid_data);
			public_composite.setData( "tabitem", public_item );
			
				// anon

			Composite anon_composite = null;
			
			if ( plugin.getBeta().isI2PAvailable()){
								
				anon_item = new CTabItem(tab_folder, SWT.NULL);
	
				anon_item.setText( MessageText.getString( "label.anon.chat" ));
				anon_item.setData( AENetworkClassifier.AT_I2P );
				
				anon_composite = new Composite( tab_folder, SWT.NULL );
		
				anon_item.setControl( anon_composite );
				
				grid_data = new GridData(GridData.FILL_BOTH );
				anon_composite.setLayoutData(grid_data);
				anon_composite.setData( "tabitem", anon_item );
			}
			
			chat_composites = new Composite[]{ public_composite, anon_composite };
				
			tab_folder.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					CTabItem item = (CTabItem) e.item;
					
					String network = (String)item.getData();
											
					activateNetwork( network );
				}
			});
		}
		
		private void
		tagsUpdated(
			DownloadManager	dm )
		{
			Download download = current_download;
			
			if ( download == null ){
				
				return;
			}
			
			if ( chat_mode == CHAT_TAG ){
			
				if ( dm == PluginCoreUtils.unwrap( download )){
				
					Utils.execSWTThread(
						new Runnable() 
						{	
							public void 
							run() 
							{
								rebuild_outstanding = true;
								
								activate();
							}
						});
				}
			}
		}
		
		private void
		setupButtonGroup(
			final List<Button>		buttons )
		{
			for ( final Button b: buttons ){

				b.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if ( !b.getSelection()){
							
							b.setSelection( true );
						}
						for ( Button b2: buttons ){
							
							if ( b2 != b ){
								b2.setSelection( false );
							}
						}
					}});
			}
			
			Utils.makeButtonsEqualWidth( buttons );
		}
		
		private void
		selectButtonGroup(
			List<Button>		buttons,
			int					data )
		{
			for ( Button b: buttons ){
				
				b.setSelection( (Integer)b.getData() == data );
			}
		}
		
		private void
		selectButtonGroup(
			List<Button>		buttons,
			String				data )
		{
			for ( Button b: buttons ){
				
				String str = (String)b.getData();
				
				b.setSelection( str != null && str.endsWith( data ));
			}
		}
		
		private void
		setChatMode(
			int		mode )
		{
			if ( chat_mode == mode ){
				
				return;
			}
			
			chat_mode = mode;
			
			selectButtonGroup( mode_buttons, mode );
		}
		
		private void
		buildChatMode(
			int				mode,
			boolean			activate )		
		{
			Download	download = current_download;
			
			chat_mode = mode;
			
			if ( mode == CHAT_GENERAL && last_build_chat_mode == mode ){
				
					// doesn't change so no rebuild required
			}else{
				
				for ( Control c: middle.getChildren()){
					
					c.dispose();
				}
				
				if ( mode == CHAT_DOWNLOAD ||(  mode == CHAT_TAG &&  download == null && current_ds_tag == null )){
					
					middle.setVisible( false );
					middle.setText( "" );
					
					GridData grid_data = new GridData(GridData.FILL_VERTICAL );
					grid_data.widthHint = 0;
					middle.setLayoutData(grid_data);
											
				}else if ( mode == CHAT_TAG ){
						
					middle.setVisible( true );				
					middle.setText( MessageText.getString( "label.tag.selection" ));
					
					List<Tag> tags = current_download==null?new ArrayList<Tag>():TagManagerFactory.getTagManager().getTagsForTaggable( TagType.TT_DOWNLOAD_MANUAL, PluginCoreUtils.unwrap( current_download ));
					
					if ( current_ds_tag != null && !tags.contains( current_ds_tag )){
						
						tags.add( current_ds_tag );
					}
					
					GridLayout layout = new GridLayout();
					layout.horizontalSpacing = 1;
					layout.verticalSpacing = 1;
					
					layout.numColumns = 1;
					middle.setLayout(layout);
					GridData grid_data = new GridData(GridData.FILL_VERTICAL );
					middle.setLayoutData(grid_data);

					if ( tags.size() == 0 ){
						
						current_tag = null;
						Label label = new Label( middle, SWT.NULL );
						label.setText( MessageText.getString( "label.none.assigned" ));
						label.setEnabled( false );

					}else{
						
						tags = TagUIUtils.sortTags( tags );
						
						current_tag = tags.get(0);
							
						final List<Button>	buttons = new ArrayList<Button>();
	
						for ( final Tag tag: tags ){
							
							Button button = new Button( middle, SWT.TOGGLE );
							
							button.setText( tag.getTagName( true ));
	
							button.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									current_tag = tag;
									activate();
								}});
							buttons.add( button );
						}
						
						buttons.get(0).setSelection( true );
						
						setupButtonGroup( buttons );
					}
				}else if ( mode == CHAT_GENERAL ){
					
					middle.setVisible( true );				
					middle.setText( MessageText.getString( "azbuddy.dchat.general.chats" ));
	
					GridLayout layout = new GridLayout();
					layout.horizontalSpacing = 1;
					layout.verticalSpacing = 1;
					
					layout.numColumns = 1;
					middle.setLayout(layout);
					GridData grid_data = new GridData(GridData.FILL_VERTICAL );
					middle.setLayoutData(grid_data);
					
					final List<Button>	buttons = new ArrayList<Button>();
						
					String[][] general_data = {
							{ "label.help", 		"General: Help" },
							{ "label.announce", 	"General: Announce" },
							{ "label.beta",			BuddyPluginBeta.BETA_CHAT_KEY },
					};
					
					for ( String[] entry: general_data ){
											
						final	String key = entry[1];
						
						if ( key == BuddyPluginBeta.BETA_CHAT_KEY && !Constants.isCVSVersion()){
							
							continue;
						}
						
						Button button = new Button( middle, SWT.TOGGLE );
							
						button.setText( MessageText.getString( entry[0] ) );
						button.setData( key );
						
						button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {
								current_general = key;;
								activate();
							}});
						
						buttons.add( button );
					}
					
					setupButtonGroup( buttons );
					
					if ( current_general != null ){
						
						selectButtonGroup( buttons, current_general );
					}
				}else{
					
					middle.setVisible( true );				
					middle.setText( MessageText.getString( "azbuddy.dchat.fave.chats" ));
					
					GridLayout layout = new GridLayout();
					layout.horizontalSpacing = 1;
					layout.verticalSpacing = 1;
					
					layout.numColumns = 1;
					middle.setLayout(layout);
					GridData grid_data = new GridData(GridData.FILL_VERTICAL );
					middle.setLayoutData(grid_data);

					List<String[]>	list = plugin.getBeta().getFavourites();

					if ( list.size() == 0 ){
						
						Label label = new Label( middle, SWT.NULL );
						label.setText( MessageText.getString( "label.none.assigned" ));
						label.setEnabled( false );
											
					}else{
						
						final List<Button>	buttons = new ArrayList<Button>();
						
						Collections.sort(
							list,
							new Comparator<String[]>()
							{
								Comparator<String> c = new FormattersImpl().getAlphanumericComparator( true );
								
								public int compare(String[] o1, String[] o2) {
									
									int result = o1[0].compareTo( o2[0] );
						
									if ( result == 0 ){
										
										result = c.compare( o1[1], o2[1] );
									}
									
									return( result );
								}
							});
						
						for ( String[] entry: list ){
							
							final	String net = entry[0];
							final	String key = entry[1];
							
							Button button = new Button( middle, SWT.TOGGLE );
							
							String	short_name = "(" + MessageText.getString( net==AENetworkClassifier.AT_PUBLIC?"label.public.short":"label.anon.short" ) + ")";
							
							short_name += " " + key;
							
							if ( short_name.length() > 30 ){
								
								short_name = short_name.substring( 0, 30 ) + "...";
							}
							
							String	long_name = "(" + MessageText.getString( net==AENetworkClassifier.AT_PUBLIC?"label.public":"label.anon" ) + ")";
							
							long_name += " " + key;
	
							button.setText( short_name );
							button.setAlignment( SWT.LEFT );
							button.setToolTipText( long_name );
							
							button.setData( net + ":" + key );
							
							button.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									current_favourite_net = net;
									current_favourite_key = key;
									activate();
								}});
							
							buttons.add( button );
						}
						
						setupButtonGroup( buttons );
						
						if ( current_favourite_key != null ){
							
							selectButtonGroup( buttons, current_favourite_net + ":" + current_favourite_key );
						}
					}
				}
				
				middle.getParent().layout( true, true );
				
				last_build_chat_mode	= mode;
			}
		
			if ( activate ){
				
				activate();
			}
		}
		
		private void
		activate()
		{
			if ( rebuild_outstanding ){
				
				rebuild_outstanding = false;
				
				if ( current_download == null ){
					
					if ( chat_mode == CHAT_DOWNLOAD || chat_mode == CHAT_TAG ){
						
						setChatMode( current_ds_tag==null?CHAT_GENERAL:CHAT_TAG );
					}
				}
				
				buildChatMode( chat_mode, false );
			}
			
			activateNetwork( null );
		}
		
		private void
		activateNetwork(
			String		network  )
		{
			if ( network != null ){
				
					// explicit network is only set when we're switching public/anon tabs so
					// we use it directly and don't need to reselect the tab coz we're on it
				
				activateNetwork( network, false );
				
			}else{
					// network == null -> select most appropriate one
				
				if ( chat_mode == CHAT_FAVOURITES && current_favourite_net != null ){
					
					activateNetwork( current_favourite_net, true );
	
				}else{
					
					Download	download 	= current_download;
					
					if ( download == null ){
						
							// no current download to guide us, default to public
						
						activateNetwork( AENetworkClassifier.AT_PUBLIC, true );
						
					}else{
																			
						String[] nets = PluginCoreUtils.unwrap( download ).getDownloadState().getNetworks();
						
						boolean	pub 	= false;
						boolean	anon	= false;
						
						for ( String net: nets ){
						
							if ( net == AENetworkClassifier.AT_PUBLIC ){
								
								pub = true;
								
							}else if ( net == AENetworkClassifier.AT_I2P ){
								
								anon = true;
							}
						}
						
						if ( pub && anon ){
							activateNetwork( AENetworkClassifier.AT_PUBLIC, true );
							activateNetwork( AENetworkClassifier.AT_I2P, false );	// warm it up
						}else if ( pub ){
							activateNetwork( AENetworkClassifier.AT_PUBLIC, true );
						}else if ( anon ){
							activateNetwork( AENetworkClassifier.AT_I2P, true );
						}
					}
				}
			}
		}
		
		private void
		activateNetwork(
			String			network,
			boolean			select_tab )
		{		
			String key;
		
			if ( chat_mode == CHAT_DOWNLOAD ){
			
				Download	download 	= current_download;
				
				if ( download == null ){
					
					key = null;
					
				}else{

					key = "Download: " + download.getName() + " {" + ByteFormatter.encodeString( download.getTorrentHash()) + "}";
				}
			}else if ( chat_mode == CHAT_TAG ){
				
				Tag	tag = current_tag;
				
				if ( tag == null ){
					
					key = null;
					
				}else{
						
					key = TagUIUtils.getChatKey( tag );
				}
				
			}else if ( chat_mode == CHAT_GENERAL ){
				
				key = current_general;
				
			}else{
				
				key	= current_favourite_key;
			}
			
			activateChat( network, key, select_tab );
		}
		
		private void
		activateChat(
			final String		network,
			final String		key,
			boolean				select_tab )	
		{		
			final Composite chat_composite = chat_composites[network==AENetworkClassifier.AT_PUBLIC?0:1];
			
			if ( chat_composite == null ){
				
				return;
			}
			
			final String comp_key = network + ":" + key;
			
			String existing_comp_key = (String)chat_composite.getData();
			
			if ( existing_comp_key == null || !existing_comp_key.equals( comp_key )){
								
				for ( Control c: chat_composite.getChildren()){
					
					c.dispose();
				}
					
				if ( key == null ){
					
					chat_composite.setData( comp_key );
					
					return;
				}
				
				AsyncDispatcher disp 		= network==AENetworkClassifier.AT_PUBLIC?public_dispatcher:anon_dispatcher;
				
				final AtomicInteger	counter 	= network==AENetworkClassifier.AT_PUBLIC?public_done:anon_done;
				
				disp.dispatch(
					new AERunnable(){						
						@Override
						public void 
						runSupport() 
						{
							if ( chat_composite.isDisposed()){
								
								return;
							}
						
							try{
								final ChatInstance chat = plugin.getBeta().getChat( network, key );
						
								counter.incrementAndGet();
								
									// TODO: maintain list of chats
								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											if ( chat_composite.isDisposed()){
												
												return;
											}
										
											for ( Control c: chat_composite.getChildren()){
												
												c.dispose();
											}
											
											BuddyPluginViewBetaChat view = new BuddyPluginViewBetaChat( BuddyPluginView.this, plugin, chat, chat_composite );
											
											((CTabItem)chat_composite.getData("tabitem")).setToolTipText( key );
											
											chat_composite.layout( true, true );
											
											chat_composite.setData( comp_key );
											
											chat_composite.setData( "viewitem", view );
										}
									});
								
							}catch( Throwable e ){
								
								e.printStackTrace();
							}	
							
						}
					});
		
				if ( counter.get() == 0 ){
					
					GridLayout layout = new GridLayout();
					layout.numColumns = 1;
					chat_composite.setLayout(layout);
	
					Label label = new Label( chat_composite, SWT.NULL );
					
					label.setText( MessageText.getString( "v3.MainWindow.view.wait" ));
					GridData grid_data = new GridData(GridData.FILL_BOTH );
					label.setLayoutData(grid_data);
	
				}
				
				chat_composite.layout( true, true );
				
			}else{
				
				BuddyPluginViewBetaChat existing_chat =  (BuddyPluginViewBetaChat)chat_composite.getData( "viewitem" );
				
				if ( existing_chat != null ){
					
					existing_chat.activate();
				}
			}
			
			if ( select_tab ){
				
				tab_folder.setSelection( network==AENetworkClassifier.AT_PUBLIC?public_item:anon_item );
			}
		}
		
		private void
		setDataSource(
			Object		obj )
		{									
			Download 			dl 		= null;
			DiskManagerFileInfo	dl_file = null;
			Tag					tag		= null;
			
			if ( obj instanceof Object[]){
				
				Object[] ds = (Object[])obj;
				
				if ( ds.length > 0 ){
					
					if ( ds[0] instanceof Download ){
		
						dl = (Download)ds[0];
						
					}else if ( ds[0] instanceof DiskManagerFileInfo ){
						
						dl_file = (DiskManagerFileInfo)ds[0];
					}
				}
			}else{
				
				if ( obj instanceof Download ){
					
					dl = (Download)obj;
					
				}else if ( obj instanceof DiskManagerFileInfo ){
					
					dl_file = (DiskManagerFileInfo)obj;
					
				}else if ( obj instanceof Tag ){
					
					tag = (Tag)obj;
				}
			}
			
			if ( dl_file != null ){
				
				try{
					dl = dl_file.getDownload();
					
				}catch( Throwable e ){	
				}
			}
			
			synchronized( this ){
				
				if ( dl == current_download && tag == current_ds_tag ){
					
					return;
				}
				
				current_download 	= dl;
				current_ds_tag		= tag;
				
				rebuild_outstanding = true;
				
				if ( have_focus ){
					
					activate();
				}
			}
		}
		
		private void
		gotFocus()
		{
			synchronized( this ){
				
				have_focus = true;
								
				activate();
			}
		}
		
		private void
		lostFocus()
		{
			synchronized( this ){
				
				have_focus = false;
			}
		}
		
		private void
		destroy()
		{			
			//System.out.println( "Destroyed" );
		}
	}
}
