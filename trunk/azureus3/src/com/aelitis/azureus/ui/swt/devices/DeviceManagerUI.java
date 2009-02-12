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



import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImageListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.PropertiesWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;

import com.aelitis.azureus.core.devices.*;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
DeviceManagerUI 
{
	private static final Object	DEVICE_IVIEW_KEY = new Object();
	
	private static final String CONFIG_VIEW_TYPE	= "device.sidebar.ui.viewtype";
	
	private DeviceManager			device_manager;
	private DeviceManagerListener	device_manager_listener;
	
	private final PluginInterface	plugin_interface;
	private final UIManager			ui_manager;
	
	private boolean		ui_setup;
	
	private SideBar		side_bar;
	
	private static final int SBV_SIMPLE		= 0;
	private static final int SBV_FULL		= 0x7FFFFFFF;
	
	private int			side_bar_view_type		= COConfigurationManager.getIntParameter( CONFIG_VIEW_TYPE, SBV_SIMPLE );
	
	private int			next_sidebar_id;
		
	private List<categoryView>	categories = new ArrayList<categoryView>();
	
	
	private MenuItemListener properties_listener;
	private MenuItemListener hide_listener;
	private MenuItemListener remove_listener;
	
	private MenuItemFillListener	show_fill_listener;
	private MenuItemListener 		show_listener;

	
	private MenuItemFillListener will_browse_listener;
	
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
														
							SkinViewManager.addListener(
								new SkinViewManagerListener() 
								{
									public void 
									skinViewAdded(
										SkinView skinview) 
									{
										if ( skinview instanceof SideBar ){
											
											setupUI((SideBar)skinview);
										}
									}
								});
							
							SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
							
							if ( sideBar != null ){
								
								setupUI( sideBar );
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
	
	protected DeviceManager
	getDeviceManager()
	{
		return( device_manager );
	}
	
	protected PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected void
	setupUI(
		SideBar			_side_bar )	
	{
		synchronized( this ){
			
			if ( ui_setup ){
				
				return;
			}
			
			ui_setup = true;
		}
		
		device_manager 	= DeviceManagerFactory.getSingleton();

		device_manager_listener = 
			new DeviceManagerListener()
			{
				public void 
				deviceAdded(
					Device device ) 
				{
					addOrChangeDevice( device );
				}
				
				public void
				deviceChanged(
					Device		device )
				{
					addOrChangeDevice( device );
				}
				
				public void
				deviceAttentionRequest(
					Device		device )
				{
					showDevice( device );
				}
				
				public void
				deviceRemoved(
					Device		device )
				{
					removeDevice( device );
				}
			};
			
		side_bar		= _side_bar;
		
		setupListeners();
		
		buildSideBar( false );
		
		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_ROOT, "Devices");

		final BooleanParameter as = 
			configModel.addBooleanParameter2( 
				"device.search.auto", "device.search.auto",
				device_manager.getAutoSearch());
		
		as.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					device_manager.setAutoSearch( as.getValue());
					
					if ( device_manager.getAutoSearch()){
						
						search();
					}
				}
			});

		addAllDevices();
	
		setupMenus();
	}
	
	protected void
	setupListeners()
	{
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
			
		remove_listener = 
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
					
						device.remove();
					}
				}
			};
			
		will_browse_listener = 
				new MenuItemFillListener() 
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		targets) 
					{
						menu.removeAllChildItems();
				
						boolean	enabled = false;
						
						Object[]	rows;
						
						if ( targets instanceof Object[] ){
							
							rows = (Object[])targets;
							
						}else{
							
							rows = new Object[]{ targets };
						}
						
						if ( rows.length > 0 && rows[0] instanceof SideBarEntry ){
													
							SideBarEntry info = (SideBarEntry)rows[0];
						
							Device device = (Device)info.getDatasource();
					
							Device.browseLocation[] locs = device.getBrowseLocations();
							
							enabled = locs.length > 0;
							
							MenuManager menuManager = ui_manager.getMenuManager();

							for ( final Device.browseLocation loc: locs ){
							
								MenuItem loc_menu = menuManager.addMenuItem( menu, loc.getName());
								
								loc_menu.addListener(
									new MenuItemListener()
									{
										public void 
										selected(
											MenuItem 	menu,
											Object 		target ) 
										{
											Utils.launch( loc.getURL().toExternalForm());
										}
									});
							}
						}
						
						menu.setEnabled( enabled );
					}
				};
			
		show_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if ( target instanceof SideBarEntry ){
						
						SideBarEntry info = (SideBarEntry)target;
												
						Object ds = info.getDatasource();
						
						if ( ds instanceof Device ){
							
								// shouldn't get here really as its hidden :)
							
							Device device = (Device)ds;
					
							device.setHidden( true );
							
						}else{
							
							int	category_type = ds==null?Device.DT_UNKNOWN:(Integer)ds;
							
							Device[] devices = device_manager.getDevices();
							
							for ( Device device: devices ){
								
								if ( 	category_type == Device.DT_UNKNOWN ||
										device.getType() == category_type && device.isHidden()){
									
									device.setHidden( false );
								}
							}
						}
					}
				}
			};
			
		show_fill_listener = 
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
									
									int	category_type = ds==null?Device.DT_UNKNOWN:(Integer)ds;
										
									Device[] devices = device_manager.getDevices();
									
									for ( Device device: devices ){
										
										if ( 	category_type == Device.DT_UNKNOWN ||
												device.getType() == category_type && device.isHidden()){
											
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
	}
	
	protected void
	buildSideBar(
		boolean			rebuild )	
	{		
		SideBarEntrySWT mainSBEntry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_DEVICES );

		if ( mainSBEntry != null ){
				
			MenuManager menu_manager = ui_manager.getMenuManager();

			if ( !rebuild ){
				
				SideBarVitalityImage addDevice = mainSBEntry.addVitalityImage("image.sidebar.subs.add");
				
				addDevice.setToolTip("Add Device");
				
				addDevice.addListener(
					new SideBarVitalityImageListener() 
					{
						public void 
						sbVitalityImage_clicked(
							int x, int y) 
						{
							new DevicesWizard( DeviceManagerUI.this );
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
				
					// devices
							
				MenuItem de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "device.search" );
			
				de_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
								search();
							}
						});
				
					// show hidden
				
				de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "device.show" );

				de_menu_item.addListener( show_listener );
				de_menu_item.addFillListener( show_fill_listener );
				

					// simple
				
				de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "devices.sidebar.simple" );
				
				de_menu_item.setStyle( MenuItem.STYLE_CHECK );
				
				de_menu_item.setData( COConfigurationManager.getIntParameter( CONFIG_VIEW_TYPE, SBV_SIMPLE ) == SBV_SIMPLE );
				
				de_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
								removeAllDevices();
								
								if ( side_bar_view_type == SBV_SIMPLE ){
									
									side_bar_view_type = SBV_FULL;
									
								}else{
									
									side_bar_view_type = SBV_SIMPLE;
								}
								
								COConfigurationManager.setParameter( CONFIG_VIEW_TYPE, side_bar_view_type );
								
								buildSideBar( true );
								
								addAllDevices();
							}
						});
				
				de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "sep" );

				de_menu_item.setStyle( MenuItem.STYLE_SEPARATOR );
				
					// options 
				
				de_menu_item = menu_manager.addMenuItem( "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES, "ConfigView.title.short" );
				
				de_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
						      	 
						      	 if ( uif != null ){
						      		 
						      		 uif.openView( UIFunctions.VIEW_CONFIG, "Devices" );
						      	 }
							}
						});

			}
			
			if ( rebuild ){
				
				for ( categoryView category: categories ){
					
					category.destroy();
				}
			}
			
			categories.clear();
			
			if ( side_bar_view_type == SBV_FULL ){
				
					// renderers
				
				categoryView renderers_category 		= addDeviceCategory( Device.DT_MEDIA_RENDERER, "device.renderer.view.title", "image.sidebar.device.renderer" );
				
				categories.add( renderers_category );
				
				MenuItem re_menu_item = menu_manager.addMenuItem( "sidebar." + renderers_category.getKey(), "device.show" );
	
				re_menu_item.addListener( show_listener );
				re_menu_item.addFillListener( show_fill_listener );
				
					// media servers
				
				categoryView media_servers_category	= addDeviceCategory( Device.DT_CONTENT_DIRECTORY, "device.mediaserver.view.title", "image.sidebar.device.mediaserver" );
					
				categories.add( media_servers_category );
				
				MenuItem ms_menu_item = menu_manager.addMenuItem( "sidebar." + media_servers_category.getKey(), "device.show" );
	
				ms_menu_item.addListener( show_listener );
				ms_menu_item.addFillListener( show_fill_listener );
				
				ms_menu_item = menu_manager.addMenuItem( "sidebar." + media_servers_category.getKey(), "device.mediaserver.configure");
				
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
				
				categoryView routers_category			= addDeviceCategory( Device.DT_INTERNET_GATEWAY, "device.router.view.title", 			"image.sidebar.device.router" );
				
				categories.add( routers_category );
				
				MenuItem rt_menu_item = menu_manager.addMenuItem( "sidebar." + routers_category.getKey(), "device.show" );
	
				rt_menu_item.addListener( show_listener );
				rt_menu_item.addFillListener( show_fill_listener );
				
				rt_menu_item = menu_manager.addMenuItem( "sidebar." + routers_category.getKey(), "device.router.configure" );
				
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
				
					// internet
				
				categoryView internet_category	= addDeviceCategory( Device.DT_INTERNET, "MainWindow.about.section.internet", "image.sidebar.device.internet" );
				
				categories.add( internet_category );
			}
		}
	}
	
	
	private void 
	setupMenus()
	{					
			// top level menus
				
		final String[] tables = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
				TableManager.TABLE_TORRENT_FILES,
				TableManager.TABLE_MYTORRENTS_UNOPENED,
				TableManager.TABLE_MYTORRENTS_UNOPENED_BIG,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
			};
		
		TableManager table_manager = plugin_interface.getUIManager().getTableManager();
		
		MenuItemFillListener	menu_fill_listener = 
			new MenuItemFillListener()
			{
				public void
				menuWillBeShown(
					MenuItem	menu,
					Object		_target )
				{
					final TableRow[]	target;
					
					if ( _target instanceof TableRow ){
						
						target = new TableRow[]{ (TableRow)_target };
						
					}else{
						
						target = (TableRow[])_target;
					}
					
					boolean	enabled = target.length > 0;
					
					for ( TableRow row: target ){
						
						Object obj = row.getDataSource();
					
						if ( obj instanceof Download ){
						
							Download download = (Download)obj;

							if ( download.getState() == Download.ST_ERROR ){
								
								enabled = false;
							}
						}else{
							
							DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
							
							try{
								if ( file.getDownload().getState() == Download.ST_ERROR ){
								
									enabled = false;
								}
							}catch( Throwable e ){
								
								enabled = false;
							}
						}
					}
					
					menu.setEnabled( enabled );
					
					menu.removeAllChildItems();
					
					if ( enabled ){
						
						Device[] devices = device_manager.getDevices();
						
						int	devices_added = 0;
						
						for ( Device device: devices ){
							
							if ( device instanceof TranscodeTarget ){
								
								devices_added++;
								
								final TranscodeTarget renderer = (TranscodeTarget)device;
								
								TranscodeProfile[] profiles = renderer.getTranscodeProfiles();
								

								TableContextMenuItem device_item =
									plugin_interface.getUIManager().getTableManager().addContextMenuItem(
										(TableContextMenuItem)menu,
										"!" + device.getName() + (profiles.length==0?" (No Profiles)":"") + "!");
								
								device_item.setStyle( MenuItem.STYLE_MENU );
								
								if ( profiles.length == 0 ){
									
									device_item.setEnabled( false );
									
								}else{
									
									for ( final TranscodeProfile profile: profiles ){
										
										TableContextMenuItem profile_item =
											plugin_interface.getUIManager().getTableManager().addContextMenuItem(
												device_item,
												"!" + profile.getName() + "!");

										profile_item.addMultiListener(
											new MenuItemListener()
											{
												public void 
												selected(
													MenuItem 	menu,
													Object 		x ) 
												{
													TranscodeManager tm = device_manager.getTranscodeManager();
													
													for ( TableRow row: target ){
														
														Object obj = row.getDataSource();
													
														if ( obj instanceof Download ){
														
															Download download = (Download)obj;

															tm.getQueue().add( renderer, profile, download.getDiskManagerFileInfo()[0], false );
	
														}else{
															
															DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
															
															tm.getQueue().add( renderer, profile, file, false );
														}
													}
												}
											});
									}
								}
							}
						}
						
						if ( devices_added == 0 ){
						
							TableContextMenuItem device_item =
								plugin_interface.getUIManager().getTableManager().addContextMenuItem(
									(TableContextMenuItem)menu,
									"!(No Devices)!");
							
							device_item.setEnabled( false );

						}
					}
				}
			};
		
		for( String table: tables ){
				
			TableContextMenuItem menu = table_manager.addContextMenuItem(table, "devices.contextmenu.xcode" );
			
			menu.setStyle(TableContextMenuItem.STYLE_MENU);
		
			menu.addFillListener( menu_fill_listener );				
		}
	}
	
	protected void
	search()
	{
      	device_manager.search(
      			10*1000,
      			new DeviceSearchListener()
      			{
      				public void 
      				deviceFound(
      					Device device ) 
      				{
      				}
      				
      				public void 
      				complete() 
      				{
      				}
      			});
	}
	
	protected void
	addOrChangeDevice(
		final Device		device )
	{
		int	type = device.getType();
		
		String parent_key = null;
		
		if ( side_bar_view_type == SBV_FULL ){
			
			for ( categoryView view: categories ){
				
				if ( view.getDeviceType() == type ){
					
					parent_key = view.getKey();
					
					break;
				}
			}
		}else{
			
			if ( type != Device.DT_MEDIA_RENDERER ){
				
				return;
			}
			
			parent_key = SideBar.SIDEBAR_SECTION_DEVICES;
		}
		
		if ( parent_key == null ){
			
			Debug.out( "Unknown device type: " + device.getString());
			
			return;
		}
			
		if ( device.isHidden()){
			
			removeDevice( device );
			
			return;
		}
		
		final String parent = parent_key;
		
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
									
									deviceView view = new deviceView( device );
									
									new_di.setView( view );
										
									String key = parent + "/" + device.getID() + ":" + nextSidebarID();
										
									TreeItem  tree_item = 
										side_bar.createTreeItemFromIView(
											parent, 
											view,
											key, 
											device, 
											false, 
											false,
											false );
									
									SideBarEntrySWT	entry = SideBar.getEntry( key );
																	
									new_di.setTreeItem( tree_item, entry );
									
									setStatus( device, new_di );
									
									MenuManager menu_manager = ui_manager.getMenuManager();
										
									if ( device.isBrowsable()){
									
										MenuItem browse_menu_item = menu_manager.addMenuItem("sidebar." + key, "device.browse");
										
										browse_menu_item.setStyle( MenuItem.STYLE_MENU );
										
										browse_menu_item.addFillListener( will_browse_listener );
									}
									
									MenuItem hide_menu_item = menu_manager.addMenuItem("sidebar." + key, "device.hide");
									
									hide_menu_item.addListener( hide_listener );
	
									MenuItem remove_menu_item = menu_manager.addMenuItem("sidebar." + key, "MySharesView.menu.remove");
									
									remove_menu_item.addListener( remove_listener );

										// sep
									
									menu_manager.addMenuItem("sidebar." + key, "s2" ).setStyle( MenuItem.STYLE_SEPARATOR );
									
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
	showDevice(
		Device		device )
	{
		synchronized( this ){
			
			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );

			if ( existing_di != null ){
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( DeviceManagerUI.this ){

									TreeItem ti = existing_di.getTreeItem();
									
									if ( ti != null ){
										
										TreeItem x = ti;
										
										while( x != null ){
											
											x.setExpanded( true );
											
											x = x.getParentItem();
										}
																				
										ti.getParent().setSelection( ti );
									}
								}
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
	addAllDevices()
	{
		device_manager.addListener( device_manager_listener );
			
		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						Device[] devices = device_manager.getDevices();
						
						for ( Device device: devices ){
							
							addOrChangeDevice( device );
						}
					}
				});
	}
	
	protected void
	removeAllDevices()
	{
		device_manager.removeListener( device_manager_listener );

		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						Device[] devices = device_manager.getDevices();
						
						for ( Device device: devices ){
							
							removeDevice( device );
						}
					}
				});
	}
	
	protected void
	removeDevice(
		final Device		device )
	{
		synchronized( this ){
			
			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );
			
			if ( existing_di != null ){
				
				device.setTransientProperty( DEVICE_IVIEW_KEY, null );
				
				existing_di.destroy();
			}
		}
	}
	
	protected categoryView
	addDeviceCategory(
		int			device_type,
		String		category_title,
		String		category_image_id )
	{
		String key = "Device_" + category_title + ":" + nextSidebarID();
		
		categoryView view;
		
		if ( device_type == Device.DT_INTERNET ){
			
			view = new DeviceInternetView( this, category_title );
			
		}else{
			
			view = new categoryViewGeneric( device_type, category_title );
		}
		
		TreeItem item = 
			side_bar.createTreeItemFromIView(
				SideBar.SIDEBAR_SECTION_DEVICES, 
				view,
				key, 
				new Integer( device_type ), 
				false, 
				false,
				false );

		SideBarEntrySWT	entry = SideBar.getEntry( key );

		entry.setImageLeftID( category_image_id );
				
		view.setDetails( item, key );
		
		return( view );
	}
	
	protected void
	showProperties(
		Device		device )
	{
		String[][] props = device.getDisplayProperties();
		
		new PropertiesWindow( device.getName(), props[0], props[1] );
	}
	
	protected int
	nextSidebarID()
	{
		synchronized( this ){
			
			return( next_sidebar_id++ );
		}
	}
	
	protected abstract static class
	categoryView
		extends 	AbstractIView
		implements 	ViewTitleInfo
	{
		private int				device_type;
		private String			title;
			
		private TreeItem		tree_item;
		private String			key;
		
		protected
		categoryView(
			int			_device_type,
			String		_title )
		{
			device_type		= _device_type;
			title			= _title;
		}
		
		protected void
		setDetails(
			TreeItem		_ti,
			String			_key )
		{
			tree_item 	= _ti;
			key			= _key;
		}
		
		protected int
		getDeviceType()
		{
			return( device_type );
		}
		
		protected String
		getKey()
		{
			return( key );
		}
		
		protected String
		getTitle()
		{
			return( MessageText.getString( title ));
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
			}
			
			return null;
		}
		
		protected void
		destroy()
		{
			if ( Utils.isThisThreadSWT()){
				
				tree_item.dispose();
				
				delete();
				
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								tree_item.dispose();
								
								delete();
							}
						});
			}
		}
	}
	
	protected static class
	categoryViewGeneric
		extends 	categoryView
	{
		private Composite		composite;
		
		protected
		categoryViewGeneric(
			int			_device_type,
			String		_title )
		{
			super( _device_type, _title );
		}
		
		public void 
		initialize(
			Composite parent_composite )
		{  
			composite = new Composite( parent_composite, SWT.NULL );
			
			FormLayout layout = new FormLayout();
			
			layout.marginTop	= 4;
			layout.marginLeft	= 4;
			layout.marginRight	= 4;
			layout.marginBottom	= 4;
			
			composite.setLayout( layout );

			FormData data = new FormData();
			data.left = new FormAttachment(0,0);
			data.right = new FormAttachment(100,0);
			data.top = new FormAttachment(composite,0);
			data.bottom = new FormAttachment(100,0);


			Label label = new Label( composite, SWT.NULL );
			
			label.setText( "Nothing to show for " + getTitle());
			
			label.setLayoutData( data );
		}
		
		public Composite 
		getComposite()
		{
			return( composite );
		}
		
		public void
		delete()
		{
			super.delete();
		}
	}
	
	protected static class
	deviceView
		extends 	AbstractIView
		implements 	ViewTitleInfo
	{
		private Device			device;
		
		private Composite		parent_composite;
		private Composite		composite;
		
		protected
		deviceView(
			Device		_device )
		{
			device		= _device;
		}
		
		public void 
		initialize(
			Composite _parent_composite )
		{  
			parent_composite	= _parent_composite;

			composite = new Composite( parent_composite, SWT.NULL );
			
			FormLayout layout = new FormLayout();
			
			layout.marginTop	= 4;
			layout.marginLeft	= 4;
			layout.marginRight	= 4;
			layout.marginBottom	= 4;
			
			composite.setLayout( layout );
			
			FormData data = new FormData();
			
			data.left 	= new FormAttachment(0,0);
			data.right 	= new FormAttachment(100,0);
			data.top 	= new FormAttachment(composite,0);
			data.bottom = new FormAttachment(100,0);


			Label label = new Label( composite, SWT.NULL );
			
			label.setText( "Nothing to show for " + device.getName());
			
			label.setLayoutData( data );
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
				
				return( device.getName());
			}
			
			return null;
		}
		
		public void
		delete()
		{
			super.delete();
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
		
		protected SideBarEntrySWT
		getSideBarEntry()
		{
			return( sb_entry );
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
			
			Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( DeviceManagerUI.this ){
								
								if ( tree_item != null && !tree_item.isDisposed()){
									
									tree_item.dispose();
								}
							}
							
							view.delete();
						}
					});
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
