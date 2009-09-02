/**
 * Created on Feb 24, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.devices;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;

import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.DeviceOfflineDownload;
import com.aelitis.azureus.core.devices.DeviceOfflineDownloader;
import com.aelitis.azureus.core.devices.DeviceOfflineDownloaderListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.content.columns.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;


public class 
SBC_DevicesODView
	extends SkinView
	implements UIUpdatable
{
	public static final String TABLE_RCM = "DevicesOD";

	private static boolean columnsAdded = false;
	
	private DeviceManager				device_manager;
	private DeviceOfflineDownloader		device;
	
	private TableViewSWTImpl<DeviceOfflineDownload> tv_downloads;

	private SideBarEntrySWT 	sidebar_entry;
	private Composite			table_parent;
	

	public Object 
	skinObjectInitialShow(
		SWTSkinObject skinObject, Object params ) 
	{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					device_manager = DeviceManagerFactory.getSingleton();
					
					initColumns( core );
				}
			});

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		
		if ( sidebar != null ){
			
			sidebar_entry = sidebar.getCurrentEntry();
			
			device = (DeviceOfflineDownloader)sidebar_entry.getDatasource();
		}
		
		return null;
	}


	private void 
	initColumns(
		AzureusCore core ) 
	{
		synchronized( SBC_DevicesODView.class ){
			
			if ( columnsAdded ){
			
				return;
			}
		
			columnsAdded = true;
		}
		
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		
		TableManager tableManager = uiManager.getTableManager();
		

	}

	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		SWTSkinObject so_list = getSkinObject("devicesod-list");
		
		if ( so_list != null ){
			
			initTable((Composite) so_list.getControl());
		}
		
		return null;
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_downloads != null ){
				
				tv_downloads.delete();
				
				tv_downloads = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		return( super.skinObjectHidden(skinObject, params));
	}

	public Object
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_downloads != null ){
				
				tv_downloads.delete();
				
				tv_downloads = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		return( super.skinObjectDestroyed(skinObject, params));
	}
	
	private void 
	initTable(
		Composite control ) 
	{

		tv_downloads = 
			new TableViewSWTImpl<DeviceOfflineDownload>(
					DeviceOfflineDownload.class, 
					TABLE_RCM,
					TABLE_RCM, 
					new TableColumnCore[0], 
					ColumnRC_New.COLUMN_ID, 
					SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		
		tv_downloads.setRowDefaultHeight(16);
		tv_downloads.setHeaderVisible(true);
		
		table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_downloads.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				private final TableViewSWTImpl<DeviceOfflineDownload>	f_table = tv_downloads;
				
				private Set<DeviceOfflineDownload>	download_set = new HashSet<DeviceOfflineDownload>();
				
				private boolean destroyed;
				
				private DeviceOfflineDownloaderListener od_listener = 
					new DeviceOfflineDownloaderListener()
					{
						public void
						downloadAdded(
							DeviceOfflineDownload 	download )
						{							
						}

						public void
						downloadChanged(
							final DeviceOfflineDownload	download )
						{
							synchronized( download_set ){
								
								if ( destroyed ){
									
									return;
								}							
							
								if ( !download_set.contains( download )){
									
									return;
								}
							}
							
							Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											if ( tv_downloads == f_table && !f_table.isDisposed()){
																									
												TableRowCore row = f_table.getRow( download );
												
												if ( row != null ){
												
													row.refresh(true );
												}
											}
										}
									});
						}
						
						public void
						downloadRemoved(
							final DeviceOfflineDownload	download )
						{
							synchronized( download_set ){
								
								if ( destroyed ){
									
									return;
								}							
							
								if ( !download_set.contains( download )){
									
									return;
								}
							}
							
							Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											if ( tv_downloads == f_table && !f_table.isDisposed()){
																									
												f_table.removeDataSources( new DeviceOfflineDownload[]{ download });
											}
										}
									});
						}
					};
				
				public void 
				tableViewInitialized() 
				{
					device.addListener( od_listener );
					
					DeviceOfflineDownload[] downloads = device.getDownloads();
					
					final ArrayList<DeviceOfflineDownload>	new_downloads = new ArrayList<DeviceOfflineDownload>( downloads.length );

					synchronized( download_set ){
										
						if ( destroyed ){
							
							return;
						}
												
						for ( DeviceOfflineDownload download: downloads ){
							
							if ( !download_set.contains( download )){
	
								new_downloads.add( download );
							}
						}
					}
					
					if ( new_downloads.size() > 0 ){
												
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									if ( tv_downloads == f_table && !f_table.isDisposed()){
									
										synchronized( download_set ){
											
											if ( destroyed ){
												
												return;
											}
										}
										
										f_table.addDataSources( new_downloads.toArray( new DeviceOfflineDownload[ new_downloads.size()]));
									}
								}
							});
					}
				}

				public void 
				tableViewDestroyed() 
				{
					device.removeListener( od_listener );

					synchronized( download_set ){
						
						destroyed = true;
					
						download_set.clear();
					}
				}
			});

		tv_downloads.initialize( table_parent );

		control.layout(true);
	}	
	
	public String 
	getUpdateUIName() 
	{
		return( "DevicesODView" );
	}

	public void 
	updateUI() 
	{
		if ( tv_downloads != null ){
			
			tv_downloads.refreshTable( false );
		}
	}
}
