/**
 * Created on Feb 24, 2009
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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


import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnThumbnail;
import com.aelitis.azureus.ui.swt.devices.columns.*;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;


public class 
SBC_DevicesODView
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener
{
	public static final String TABLE_ID = "DevicesOD";

	private static boolean columnsAdded = false;
	
	private DeviceOfflineDownloader		device;
	
	private TableViewSWT<DeviceOfflineDownload> tv_downloads;

	private MdiEntrySWT 	mdi_entry;
	private Composite			control_parent;
	

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
					initColumns( core );
				}
			});

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		if ( mdi != null ){
			
			mdi_entry = mdi.getCurrentEntrySWT();
			
			mdi_entry.addToolbarEnabler( this );
			
			device = (DeviceOfflineDownloader)mdi_entry.getDatasource();
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
		
		tableManager.registerColumn( 
			DeviceOfflineDownload.class, ColumnThumbnail.COLUMN_ID,
			new TableColumnCreationListener() {
				public void tableColumnCreated(TableColumn column) {
					new ColumnThumbnail(column);
					column.setWidth(70);
				}
			});

		tableManager.registerColumn( 
			DeviceOfflineDownload.class, ColumnOD_Name.COLUMN_ID,
			new TableColumnCreationListener() {
				public void tableColumnCreated(TableColumn column) {
					new ColumnOD_Name(column);
				}
			});
		
		tableManager.registerColumn( 
			DeviceOfflineDownload.class, ColumnOD_Status.COLUMN_ID,
			new TableColumnCreationListener() {
				public void tableColumnCreated(TableColumn column) {
					new ColumnOD_Status(column);
				}
			});
		
		tableManager.registerColumn( 
				DeviceOfflineDownload.class, ColumnOD_Completion.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnOD_Completion(column);
					}
				});
		
		tableManager.registerColumn( 
				DeviceOfflineDownload.class, ColumnOD_Remaining.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnOD_Remaining(column);
					}
				});

		TableColumnManager tcm = TableColumnManager.getInstance();
		TableColumnCore[] allTCs = tcm.getAllTableColumnCoreAsArray(
				DeviceOfflineDownload.class, TABLE_ID);
		// for now, all columns are default
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < allTCs.length; i++) {
			TableColumn tc = allTCs[i];
			if (tc.isVisible()) {
				names.add(tc.getName());
			}
		}
		tcm.setDefaultColumnNames(TABLE_ID, names.toArray(new String[0]));
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
				control_parent,
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
				control_parent,
		});

		return( super.skinObjectDestroyed(skinObject, params));
	}
	
	private void 
	initTable(
		final Composite control ) 
	{	
		control_parent = new Composite(control, SWT.NONE);
		Utils.setLayoutData(control_parent, Utils.getFilledFormData());
		
		final StackLayout stack_layout = new StackLayout();
		
		control_parent.setLayout( stack_layout );
		
			// enabled
		
		final Composite enabled_device_parent = new Composite( control_parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		enabled_device_parent.setLayout(layout);
			
		tv_downloads = TableViewFactory.createTableViewSWT(
					DeviceOfflineDownload.class, 
					TABLE_ID,
					TABLE_ID, 
					new TableColumnCore[0], 
					ColumnOD_Name.COLUMN_ID, 
					SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );

		tv_downloads.setRowDefaultHeightEM(3);
		tv_downloads.setHeaderVisible(true);

		tv_downloads.addSelectionListener(
			new TableSelectionListener() 
			{
				public void 
				selected(
					TableRowCore[] row ) 
				{
					refreshIconBar();
				}
	
				public void 
				mouseExit(
					TableRowCore row ) 
				{
				}
	
				public void 
				mouseEnter(
					TableRowCore row )
				{
				}
	
				public void 
				focusChanged(
					TableRowCore focus ) 
				{
					refreshIconBar();
				}
	
				public void 
				deselected(
					TableRowCore[] rows) 
				{
					refreshIconBar();
				}
	
				public void 
				defaultSelected(TableRowCore[] rows, int stateMask)
				{
					refreshIconBar();
				}
				
				protected void
				refreshIconBar()
				{
					SelectedContentManager.clearCurrentlySelectedContent();
					
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						uiFunctions.refreshIconBar();
					}
				}
			}, false );
		
		tv_downloads.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				private final TableViewSWT<DeviceOfflineDownload>	f_table = tv_downloads;
				
				private Set<DeviceOfflineDownload>	download_set = new HashSet<DeviceOfflineDownload>();
				
				private boolean destroyed;
				
				private DeviceOfflineDownloaderListener od_listener = 
					new DeviceOfflineDownloaderListener()
					{
						public void
						downloadAdded(
							final DeviceOfflineDownload 	download )
						{	
							synchronized( download_set ){
								
								if ( destroyed ){
									
									return;
								}							
							
								if ( download_set.contains( download )){
									
									return;
								}
								
								download_set.add( download );
							}
							
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
												
												f_table.addDataSources( new DeviceOfflineDownload[]{ download });
											}
										}
									});
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
													
												synchronized( download_set ){
													
													if ( destroyed ){
														
														return;
													}
												}
												
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
							
								if ( !download_set.remove( download )){
									
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
													
												synchronized( download_set ){
													
													if ( destroyed ){
														
														return;
													}
												}
												
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
	
								download_set.add( download );
								
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

		tv_downloads.initialize( enabled_device_parent );	
			
			// disabled
		
		final Composite disabled_device_parent = new Composite( control_parent, SWT.NONE);
	
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		disabled_device_parent.setLayout(layout);

		Label l = new Label( disabled_device_parent, SWT.NULL );
		GridData grid_data = new GridData( GridData.FILL_HORIZONTAL );
		grid_data.horizontalIndent = 5;
		Utils.setLayoutData(l,  grid_data );
		
		l.setText( MessageText.getString( "device.is.disabled" ));
	
		device.addListener(
			new DeviceListener()
			{
				public void 
				deviceChanged(
					Device d )
				{
					Composite x = device.isEnabled()?enabled_device_parent:disabled_device_parent;

					if ( x.isDisposed()){
						
						device.removeListener( this );
						
					}else{
					
						if ( x != stack_layout.topControl ){
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										Composite x = device.isEnabled()?enabled_device_parent:disabled_device_parent;
										
										if ( !x.isDisposed() && x != stack_layout.topControl ){
											
											stack_layout.topControl = x;
											
											control.layout( true, true );
										}
									}
								});
						}
					}
				}
			});
		
		stack_layout.topControl = device.isEnabled()?enabled_device_parent:disabled_device_parent;
		
		control.layout(true);
	}	

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		long stateRemove = 0;
		if (tv_downloads != null && tv_downloads.getSelectedRowsSize() > 0) {
			stateRemove = UIToolBarItem.STATE_ENABLED;
		}
		list.put("remove", stateRemove);
	}
	
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if (item.getID().equals("remove")) {
			MessageBoxShell mb = 
				new MessageBoxShell(
					MessageText.getString("message.confirm.delete.title"),
					MessageText.getString("message.confirm.delete.text",
							new String[] {
								device.getName()
							}), 
					new String[] {
						MessageText.getString("Button.yes"),
						MessageText.getString("Button.no")
					},
					1 );
			
			mb.open(new UserPrompterResultListener() {
				public void prompterClosed(int result) {
					if (result == 0) {
						device.remove();
					}
				}
			});
			return true;
		}
		return false;
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
