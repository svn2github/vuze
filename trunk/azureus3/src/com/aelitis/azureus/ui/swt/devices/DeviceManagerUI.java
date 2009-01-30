/*
 * Created on Jan 27, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImageListener;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.PropertiesWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.subs.Subscription;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI.sideBarItem;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
DeviceManagerUI 
{
	private static final Object	DEVICE_IVIEW_KEY = new Object();
	
	private DeviceManager		device_manager;
	
	private final PluginInterface	plugin_interface;
	private final UIManager			ui_manager;
	
	private boolean		side_bar_setup;
	
	private String		renderers_key;
	private String		media_servers_key;
	private String		routers_key;
	
	
	
	private MenuItemListener properties_listener;
	private MenuItemListener hide_listener;
	private MenuItemListener remove_listener;
	
	public
	DeviceManagerUI(
		AzureusCore			core )
	{
		plugin_interface = core.getPluginManager().getDefaultPluginInterface();
		
		ui_manager = plugin_interface.getUIManager();
		
		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							final UISWTInstance	swt = (UISWTInstance)instance;						

							BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
									ConfigSection.SECTION_ROOT, "Devices");

							
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
		
		properties_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if (target instanceof SideBarEntry) {
						SideBarEntry info = (SideBarEntry) target;
						Device device = (Device)info.getDatasource();
					
						showProperties( device );
					}
				}
			};
		
		hide_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if (target instanceof SideBarEntry){
						
						SideBarEntry info = (SideBarEntry) target;
						
						Device device = (Device)info.getDatasource();
					
						device.setHidden( true );
					}
				}
			};
				
		MenuItemListener show_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if ( target instanceof SideBarEntry ){
						
						SideBarEntry info = (SideBarEntry) target;
												
						Object ds = info.getDatasource();
						
						if ( ds instanceof Device ){
							
								// shouldn't get here really as its hidden :)
							
							Device device = (Device)ds;
					
							device.setHidden( true );
							
						}else{
							
							String id = info.getId();

							int	target_type = Device.DT_UNKNOWN;
							
							if ( id.equals( renderers_key )){
								
								target_type = Device.DT_MEDIA_RENDERER;
								
							}else if ( id.equals( media_servers_key )){
								
								target_type = Device.DT_CONTENT_DIRECTORY;
								
							}else if ( id.equals( routers_key )){
								
								target_type = Device.DT_INTERNET_GATEWAY;
								
							}else if ( id.equals( "Devices" )){
								
							}else{
								
								Debug.out( "Unrecognised key '" + id + "'" );
							}
							
							Device[] devices = device_manager.getDevices();
							
							for ( Device device: devices ){
								
								if ( 	target_type == Device.DT_UNKNOWN ||
										device.getType() == target_type && device.isHidden()){
									
									device.setHidden( false );
								}
							}
						}
					}
				}
			};
		
		MenuItemFillListener show_fill_listener = 
				new MenuItemFillListener() 
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		targets) 
					{
						boolean	enabled = false;
						
						Object[]	rows;
						
						if ( targets instanceof Object[] ){
							
							rows = (Object[])targets;
							
						}else{
							
							rows = new Object[]{ targets };
						}
						
						for ( Object row: rows ){
							
							if ( row instanceof SideBarEntry ){
								
								SideBarEntry info = (SideBarEntry)row;
														
								Object ds = info.getDatasource();
								
								if ( ds instanceof Device ){
																	
								}else{
									
									String id = info.getId();
	
									int	target_type = Device.DT_UNKNOWN;
									
									if ( id.equals( renderers_key )){
										
										target_type = Device.DT_MEDIA_RENDERER;
										
									}else if ( id.equals( media_servers_key )){
										
										target_type = Device.DT_CONTENT_DIRECTORY;
										
									}else if ( id.equals( routers_key )){
										
										target_type = Device.DT_INTERNET_GATEWAY;
										
									}else if ( id.equals( "Devices" )){
										
									}else{
										
										Debug.out( "Unrecognised key '" + id + "'" );
									}
									
									Device[] devices = device_manager.getDevices();
									
									for ( Device device: devices ){
										
										if ( 	target_type == Device.DT_UNKNOWN ||
												device.getType() == target_type && device.isHidden()){
											
											if ( device.isHidden()){
												
												enabled = true;
											}
										}
									}
								}
							}
						}
						
						menu.setEnabled( enabled );
					}
				};
			
			
		SideBarEntrySWT mainSBEntry = SideBar.getEntry(SideBar.SIDEBAR_SECTION_DEVICES );

		if ( mainSBEntry != null ){
			
			SideBarVitalityImage addDevice = mainSBEntry.addVitalityImage("image.sidebar.subs.add");
			
			addDevice.setToolTip("Add Device");
			
			addDevice.addListener(
				new SideBarVitalityImageListener() 
				{
					public void 
					sbVitalityImage_clicked(
						int x, int y) 
					{

					}
				});

			mainSBEntry.setImageLeftID( "image.sidebar.devices" );

			mainSBEntry.setTitleInfo(
				new ViewTitleInfo() 
				{
					public Object 
					getTitleInfoProperty(
						int propertyID ) 
					{
						if ( propertyID == TITLE_TEXT ){
							
							return MessageText.getString( "devices.view.title" );
						}
						
						return null;
					}
				});
			
			MenuManager menu_manager = ui_manager.getMenuManager();

			
			
				// devices
			
			
			MenuItem de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "device.search" );
			
			de_menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	device_manager.search();
						}
					});
			
			de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "device.show" );

			de_menu_item.addListener( show_listener );
			de_menu_item.addFillListener( show_fill_listener );
			
				// renderers
			
			renderers_key 		= addDeviceCategory( side_bar, "device.renderer.view.title", "image.sidebar.device.renderer" );
			
			MenuItem re_menu_item = menu_manager.addMenuItem( "sidebar." + renderers_key, "device.show" );

			re_menu_item.addListener( show_listener );
			re_menu_item.addFillListener( show_fill_listener );
			
				// media servers
			
			media_servers_key	= addDeviceCategory( side_bar, "device.mediaserver.view.title", "image.sidebar.device.mediaserver" );
				
			MenuItem ms_menu_item = menu_manager.addMenuItem( "sidebar." + media_servers_key, "device.show" );

			ms_menu_item.addListener( show_listener );
			ms_menu_item.addFillListener( show_fill_listener );
			
			ms_menu_item = menu_manager.addMenuItem( "sidebar." + media_servers_key, "device.mediaserver.configure");
			
			ms_menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
					      	 
					      	 if ( uif != null ){
					      		 
					      		 uif.openView( UIFunctions.VIEW_CONFIG, "upnpmediaserver.name" );
					      	 }
						}
					});

				// routers
			
			routers_key			= addDeviceCategory( side_bar, "device.router.view.title", 			"image.sidebar.device.router" );
			
			MenuItem rt_menu_item = menu_manager.addMenuItem( "sidebar." + routers_key, "device.show" );

			rt_menu_item.addListener( show_listener );
			rt_menu_item.addFillListener( show_fill_listener );
			
			rt_menu_item = menu_manager.addMenuItem( "sidebar." + routers_key, "device.router.configure" );
			
			rt_menu_item.addListener( 
					new MenuItemListener() 
					{
						public void 
						selected(
							MenuItem menu, Object target ) 
						{
					      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
					      	 
					      	 if ( uif != null ){
					      		 
					      		 uif.openView( UIFunctions.VIEW_CONFIG, "UPnP" );
					      	 }
						}
					});
			
			device_manager = DeviceManagerFactory.getSingleton();
			
			device_manager.addListener(
				new DeviceManagerListener()
				{
					public void 
					deviceAdded(
						Device device ) 
					{
						addOrChangeDevice( side_bar, device );
					}
					
					public void
					deviceChanged(
						Device		device )
					{
						addOrChangeDevice( side_bar, device );
					}
					
					public void
					deviceRemoved(
						Device		device )
					{
						removeDevice( side_bar, device );
					}
				});
			
			Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							Device[] devices = device_manager.getDevices();
							
							for ( Device device: devices ){
								
								addOrChangeDevice( side_bar, device );
							}
						}
					});
		}
	}
	
	protected void
	addOrChangeDevice(
		final SideBar		side_bar,
		final Device		device )
	{
		int	type = device.getType();
		
		final String parent;
		
		if ( type == Device.DT_CONTENT_DIRECTORY ){
			
			parent = media_servers_key;
			
		}else if ( type == Device.DT_INTERNET_GATEWAY ){
			
			parent = routers_key;
			
		}else if ( type == Device.DT_MEDIA_RENDERER ){
			
			parent = renderers_key;
			
		}else{
			
			Debug.out( "Unknown device type: " + device.getString());
			
			return;
		}
			
		if ( device.isHidden()){
			
			removeDevice( side_bar, device );
			
			return;
		}
		
		synchronized( this ){
			
			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );
			
			if (  existing_di == null ){
	
				if ( !device.isHidden()){
					
					final deviceItem new_di = new deviceItem();
					
					device.setTransientProperty( DEVICE_IVIEW_KEY, new_di );
					
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( DeviceManagerUI.this ){
	
									if ( new_di.isDestroyed()){
										
										return;
									}
									
									deviceView view = new deviceView( device.getName());
									
									new_di.setView( view );
										
									String key = parent + "/" + device.getID();
	
									boolean	show = false;
									
									TreeItem  tree_item = 
										side_bar.createTreeItemFromIView(
											parent, 
											view,
											key, 
											device, 
											false, 
											show );
									
									SideBarEntrySWT	entry = SideBar.getEntry( key );
																	
									new_di.setTreeItem( tree_item, entry );
									
									setStatus( device, new_di );
									
									MenuManager menu_manager = ui_manager.getMenuManager();
	
									
									MenuItem hide_menu_item = menu_manager.addMenuItem("sidebar." + key, "device.hide");
									
									hide_menu_item.addListener( hide_listener );
	
										// sep
									
									menu_manager.addMenuItem("sidebar." + key,"s2").setStyle( MenuItem.STYLE_SEPARATOR );
									
										// props
									
									MenuItem menu_item = menu_manager.addMenuItem("sidebar." + key,"Subscription.menu.properties");
									
									menu_item.addListener( properties_listener );
								}
							}
						});
				}
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_di.getView());
								
								setStatus( device, existing_di );
							}
						});
			}
		}
	}
	
	protected void
	setStatus(
		Device			device,
		deviceItem		sbi )
	{
		sbi.setWarning( device );
	}
	
	protected void
	removeDevice(
		SideBar				side_bar,
		final Device		device )
	{
		synchronized( this ){
			
			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );
			
			if ( existing_di != null ){
				
				device.setTransientProperty( DEVICE_IVIEW_KEY, null );
				
				existing_di.destroy();
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( DeviceManagerUI.this ){

									TreeItem ti = existing_di.getTreeItem();
									
									if ( ti != null ){
										
										ti.dispose();
									}
								}
							}
						});
			}
		}
	}
	
	protected String
	addDeviceCategory(
		SideBar		side_bar,
		String		category_title,
		String		category_image_id )
	{
		String key = "Device_" + category_title;
		
		categoryView view = new categoryView( category_title );
		
		side_bar.createTreeItemFromIView(
				SideBar.SIDEBAR_SECTION_DEVICES, 
				view,
				key, 
				null, 
				false, 
				true );

		SideBarEntrySWT	entry = SideBar.getEntry( key );

		entry.setImageLeftID( category_image_id );
		
		return( key );
	}
	
	protected void
	showProperties(
		Device		device )
	{
		String[][] props = device.getDisplayProperties();
		
		new PropertiesWindow( device.getName(), props[0], props[1] );
	}
	
	protected class
	categoryView
		extends 	AbstractIView
		implements 	ViewTitleInfo
	{
		private String			title;
		
		private Composite		parent_composite;
		private Composite		composite;
		
		protected
		categoryView(
			String		_title )
		{
			title	= _title;
		}
		
		public void 
		initialize(
			Composite _parent_composite )
		{  
			parent_composite	= _parent_composite;

			composite = new Composite( parent_composite, SWT.NULL );
		}
		
		public Composite 
		getComposite()
		{
			return( composite );
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{
			if ( propertyID == TITLE_TEXT ){
				
				return MessageText.getString( title );
			}
			
			return null;
		}
	}
	
	protected static class
	deviceView
		extends 	AbstractIView
		implements 	ViewTitleInfo
	{
		private String			title;
		
		private Composite		parent_composite;
		private Composite		composite;
		
		protected
		deviceView(
			String		_title )
		{
			title	= _title;
		}
		
		public void 
		initialize(
			Composite _parent_composite )
		{  
			parent_composite	= _parent_composite;

			composite = new Composite( parent_composite, SWT.NULL );
		}
		
		public Composite 
		getComposite()
		{
			return( composite );
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{
			if ( propertyID == TITLE_TEXT ){
				
				return( title );
			}
			
			return null;
		}
	}
	
	public class
	deviceItem
	{
		public static final String ALERT_IMAGE_ID	= "image.sidebar.vitality.alert";
		
		private deviceView			view;
		private SideBarEntrySWT		sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private SideBarVitalityImage	warning;
		
		protected
		deviceItem()
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
			
			warning.setVisible( false );
			warning.setToolTip( "" );
		}
		
		protected TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected void
		setView(
			deviceView		_view )
		{
			view	= _view;
		}
		
		protected deviceView
		getView()
		{
			return( view );
		}
		
		protected void
		setWarning(
			Device	subs )
		{
				// possible during initialisation, status will be shown again on complete
			
			if ( warning == null ){
				
				return;
			}
			
			boolean	trouble 	= false;
			String	last_error	= "";
			
			if ( trouble ){
			 
				warning.setToolTip( last_error );
				
				warning.setImageID( ALERT_IMAGE_ID );
				
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
