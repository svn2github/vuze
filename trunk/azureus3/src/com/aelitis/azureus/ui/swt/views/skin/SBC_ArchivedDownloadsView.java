/**
 * Created on May 10, 2013
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

package com.aelitis.azureus.ui.swt.views.skin;


import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.plugins.download.DownloadStub.DownloadStubFile;
import org.gudy.azureus2.plugins.download.DownloadStubEvent;
import org.gudy.azureus2.plugins.download.DownloadStubListener;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.ArchivedFilesView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils.ArchiveCallback;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.columns.archivedls.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;


public class SBC_ArchivedDownloadsView
	extends SkinView
	implements 	UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<DownloadStub>,
	TableViewSWTMenuFillListener, TableSelectionListener, DownloadStubListener
{

	private static final String TABLE_NAME = "ArchivedDownloads";

	TableViewSWT<DownloadStub> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

	private boolean dm_listener_added;

	private boolean registeredCoreSubViews;

	private Object datasource;
	  
	public Object 
	skinObjectInitialShow(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{		
		initColumns();
		
		new InfoBarUtil(
			skinObject, 
			"archivedlsview.infobar", 
			false,
			"archivedls.infobar", 
			"archivedls.view.infobar" ) 
			{
				public boolean 
				allowShow() 
				{
					return( true );
				}
			};
		
		return( null );
	}

	protected void 
	initColumns() 
	{
		synchronized (SBC_ArchivedDownloadsView.class) {

			if ( columnsAdded ){

				return;
			}

			columnsAdded = true;
		}

		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.registerColumn(DownloadStub.class, ColumnArchiveDLName.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnArchiveDLName(column);
					}
				});

		tableManager.registerColumn(DownloadStub.class, ColumnArchiveDLSize.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnArchiveDLSize(column);
					}
				});
		
		tableManager.registerColumn(DownloadStub.class, ColumnArchiveDLFileCount.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnArchiveDLFileCount(column);
					}
				});
		
		tableManager.registerColumn(DownloadStub.class,
				ColumnArchiveDLDate.COLUMN_ID,
				new TableColumnCoreCreationListener() {
					public TableColumnCore createTableColumnCore(
							Class<?> forDataSourceType, String tableID, String columnID) {
						return new ColumnDateSizer(DownloadStub.class, columnID,
								TableColumnCreator.DATE_COLUMN_WIDTH, tableID) {
						};
					}

					public void tableColumnCreated(TableColumn column) {
						new ColumnArchiveDLDate(column);
					}
				});
		
		tableManager.registerColumn(DownloadStub.class, ColumnArchiveDLTags.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnArchiveDLTags(column);
					}
				});
		
		tableManager.setDefaultColumnNames(TABLE_NAME,
				new String[] {
					ColumnArchiveDLName.COLUMN_ID,
					ColumnArchiveDLSize.COLUMN_ID,
					ColumnArchiveDLFileCount.COLUMN_ID,
					ColumnArchiveDLDate.COLUMN_ID,
				});
		
		tableManager.setDefaultSortColumnName(TABLE_NAME, ColumnArchiveDLName.COLUMN_ID);
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject skinObject, 
		Object params) 
	{
		if ( tv != null ){

			tv.delete();

			tv = null;
		}

		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});
		
		if ( dm_listener_added ){
		
			PluginInitializer.getDefaultInterface().getDownloadManager().removeDownloadStubListener( this );
			
			dm_listener_added = false;
		}

		return super.skinObjectHidden(skinObject, params);
	}

	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{
		super.skinObjectShown( skinObject, params );
		
		SWTSkinObjectTextbox soFilter = (SWTSkinObjectTextbox)getSkinObject( "filterbox" );
		
		if ( soFilter != null ){
		
			txtFilter = soFilter.getTextControl();
		}
		
		SWTSkinObject so_list = getSkinObject( "archived-dls-list" );

		if ( so_list != null ){
			
			initTable((Composite)so_list.getControl());
			
		}else{
			
			System.out.println("NO archived-dls-list");
			
			return( null );
		}
				
		if ( tv == null ){
			
			return( null );
		}

		PluginInitializer.getDefaultInterface().getDownloadManager().addDownloadStubListener( this, true );
		
		dm_listener_added = true;
		
		return( null );
	}

	@Override
	public Object 
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{
		if ( dm_listener_added ){
			
			PluginInitializer.getDefaultInterface().getDownloadManager().removeDownloadStubListener( this );
			
			dm_listener_added = false;
		}		
		
		return super.skinObjectDestroyed(skinObject, params);
	}
	

	private void 
	initTable(
		Composite control )
	{
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		
		if ( uiFunctions != null ) {
			
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
			
			registerPluginViews( pluginUI );
		}

		if ( tv == null ){
			
			tv = TableViewFactory.createTableViewSWT(
					DownloadStub.class, TABLE_NAME, TABLE_NAME,
					new TableColumnCore[0], 
					ColumnArchiveDLName.COLUMN_ID, 
					SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
			
			if ( txtFilter != null){
				
				tv.enableFilterCheck( txtFilter, this );
			}
			
			tv.setRowDefaultHeight(16);
			
			tv.setEnableTabViews(true, true, null);
	
			table_parent = new Composite(control, SWT.BORDER);
			
			table_parent.setLayoutData(Utils.getFilledFormData());
			
			GridLayout layout = new GridLayout();
			
			layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
			
			table_parent.setLayout(layout);
	
			tv.addMenuFillListener(this);
			tv.addSelectionListener( this, false );
			
			tv.initialize( table_parent );

			tv.addCountChangeListener(
				new TableCountChangeListener() 
				{
					public void 
					rowRemoved(
						TableRowCore row) 
					{
					}
					
					public void 
					rowAdded(
						TableRowCore row) 
					{
						if ( datasource == row.getDataSource()){
							
							tv.setSelectedRows(new TableRowCore[] { row });
						}
					}
				});
		}

		control.layout( true );
	}

	private void 
	registerPluginViews(
		UISWTInstance pluginUI ) 
	{
		if ( registeredCoreSubViews ){
			
			return;
		}
			
		pluginUI.addView(TABLE_NAME, "ArchivedFilesView", ArchivedFilesView.class, null);
		
		registeredCoreSubViews = true;
	}

	public boolean 
	toolBarItemActivated(
		ToolBarItem item, 
		long activationType,
		Object datasource) 
	{
		if ( tv == null || !tv.isVisible()){
			
			return( false );
		}
		
		List<Object> datasources = tv.getSelectedDataSources();
		
		if ( datasources.size() > 0 ){

			List<DownloadStub>	dms = new ArrayList<DownloadStub>( datasources.size());
			
			for ( Object o: datasources ){
				
				dms.add((DownloadStub)o);
			}
			
			String id = item.getID();
			
			if ( id.equals("remove")) {
			
				TorrentUtil.removeDataSources(datasources.toArray());
				
			}else if ( id.equals( "startstop" ) || id.equals( "start" )){
				
				ManagerUtils.restoreFromArchive( dms, true, null );
			}
			
				
			return true;
		}
		
		return false;
	}

	public void 
	refreshToolBarItems(
		Map<String, Long> list) 
	
	{
		if ( tv == null || !tv.isVisible()){
			
			return;
		}

		boolean canEnable = false;
		
		Object[] datasources = tv.getSelectedDataSources().toArray();
		
		if ( datasources.length > 0 ){
			
			canEnable = true;
		}

		list.put( "start", canEnable ? UIToolBarItem.STATE_ENABLED : 0);
		list.put( "startstop", canEnable ? UIToolBarItem.STATE_ENABLED : 0);
		
		list.put( "remove", canEnable ? UIToolBarItem.STATE_ENABLED : 0);
	}

	public void 
	updateUI() 
	{
		if (tv != null) {
			
			tv.refreshTable(false);
		}
	}

	public String 
	getUpdateUIName() 
	{
		return( TABLE_NAME );
	}
	
	public void 
	addThisColumnSubMenu(
		String 		columnName, 
		Menu 		menuThisColumn ) 
	{
	}
	
	public void 
	fillMenu(
		String 	sColumnName, 
		Menu 	menu )
	{
		List<Object>	ds = tv.getSelectedDataSources();
		
		final List<DownloadStub>	dms = new ArrayList<DownloadStub>( ds.size());
		
		for ( Object o: ds ){
			
			dms.add((DownloadStub)o);
		}
		
		boolean	hasSelection = dms.size() > 0;
		
			// Explore (or open containing folder)
		
		final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
		
		final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
		
		Messages.setLanguageText(itemExplore, "MyTorrentsView.menu."
				+ (use_open_containing_folder ? "open_parent_folder" : "explore"));
		
		itemExplore.addListener(SWT.Selection, new Listener() {
			public void 
			handleEvent(
				Event event) 
			{
				for ( DownloadStub download: dms ){
				
					ManagerUtils.open( new File( download.getSavePath()), use_open_containing_folder);
				}
			}
		});
		
		itemExplore.setEnabled(hasSelection);
		
		new MenuItem( menu, SWT.SEPARATOR );
		
		final MenuItem itemRestore = new MenuItem(menu, SWT.PUSH);
		
		Messages.setLanguageText(itemRestore, "MyTorrentsView.menu.restore");
		
		itemRestore.addListener(
			SWT.Selection, 
			new Listener()
			{
				public void 
				handleEvent(
					Event event) 
				{
					ManagerUtils.restoreFromArchive( dms,false, null );				
				}
			});
		
		itemRestore.setEnabled( hasSelection );
		
		final MenuItem itemRestoreAnd = new MenuItem(menu, SWT.PUSH);
		
		Messages.setLanguageText(itemRestoreAnd, "MyTorrentsView.menu.restore.and");
		
		itemRestoreAnd.addListener(
			SWT.Selection, 
			new Listener()
			{
				public void 
				handleEvent(
					Event event) 
				{
					ManagerUtils.restoreFromArchive( 
						dms, 
						false, 
						new ArchiveCallback()
						{
							private List<DownloadManager>	targets = new ArrayList<DownloadManager>();
							
							@Override
							public void 
							success(
								final DownloadStub source,
								final DownloadStub target) 
							{
								DownloadManager dm = PluginCoreUtils.unwrap((Download)target);
								
								if ( dm != null ){
									
									targets.add( dm );
								}
							}
							
							@Override
							public void 
							completed() 
							{
								Utils.execSWTThread(
									new Runnable() 
									{
										public void 
										run()
										{
											if ( targets.size() == 0 ){
												
												return;
											}
											
											final Menu menu = new Menu( table_parent );
											
											DownloadManager[] dm_list = targets.toArray( new DownloadManager[ dms.size()]);
											
											TorrentUtil.fillTorrentMenu(
												menu, dm_list, AzureusCoreFactory.getSingleton(), table_parent, true, 0, tv);
											
											menu.addMenuListener(
												new MenuListener() {
													
													public void 
													menuShown(
														MenuEvent e) 
													{
													}
													
													public void 
													menuHidden(
														MenuEvent e) 
													{
														Utils.execSWTThreadLater(
															1,
															new Runnable() 
															{
																public void 
																run() 
																{
																	menu.dispose();
																}
															});
													}
												});
											
											menu.setVisible( true );
										}
									});
							}
						});
				}
			});
		
		itemRestoreAnd.setEnabled( hasSelection );
		
		new MenuItem( menu, SWT.SEPARATOR );
	}
	
	public void 
	selected(
		TableRowCore[] row )
	{
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		
		if ( uiFunctions != null ){
			
			uiFunctions.refreshIconBar();
		}
	}

	public void 
	deselected(
		TableRowCore[] rows )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	
	  	if ( uiFunctions != null ){
	  		
	  		uiFunctions.refreshIconBar();
	  	}
	}
	
	public void 
	focusChanged(
		TableRowCore focus )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	
	  	if ( uiFunctions != null ){
	  		
	  		uiFunctions.refreshIconBar();
	  	}
	}

	public void 
	defaultSelected(
		TableRowCore[] 	rows, 
		int 			stateMask )
	{
		if ( rows.length == 1 ){
			
			Object obj = rows[0].getDataSource();

		}
	}

	public void
	downloadStubEventOccurred(
		DownloadStubEvent		event )
	
		throws DownloadException
	{
		int type = event.getEventType();
		
		List<DownloadStub> dls = event.getDownloadStubs();
				
		if ( type == DownloadStubEvent.DSE_STUB_ADDED ){
			
			tv.addDataSources( dls.toArray( new DownloadStub[dls.size()] ));
						
		}else if ( type == DownloadStubEvent.DSE_STUB_REMOVED ){
			
			tv.removeDataSources( dls.toArray( new DownloadStub[dls.size()] ));
		}
	}
	
	public void 
	mouseEnter(
		TableRowCore row )
	{
	}

	public void 
	mouseExit(
		TableRowCore row)
	{	
	}
	
	public void 
	filterSet(
		String filter) 
	{
	}
	
	public boolean 
	filterCheck(
		DownloadStub 	ds, 
		String 			filter, 
		boolean 		regex) 
	{
		if ( filter.toLowerCase( Locale.US ).startsWith( "f:" )){
			
			filter = filter.substring(2).trim();
					
			DownloadStubFile[] files = ds.getStubFiles();
			
			String s = regex ? filter : "\\Q" + filter.replaceAll("\\s*[|;]\\s*", "\\\\E|\\\\Q") + "\\E";
			
			boolean	match_result = true;
			
			if ( regex && s.startsWith( "!" )){
				
				s = s.substring(1);
				
				match_result = false;
			}
			
			Pattern pattern = RegExUtil.getCachedPattern( "archiveview:search", s, Pattern.CASE_INSENSITIVE);

			
			boolean result = !match_result;
			
		
			for ( DownloadStubFile file: files ){
				
				String name = file.getFile().getName();
				
				if ( pattern.matcher( name ).find()){
				
					result = match_result;
						
					break;
				}
			}
			
			return( result );
			
		}else{
			
			String name = ds.getName();
			
			String s = regex ? filter : "\\Q" + filter.replaceAll("\\s*[|;]\\s*", "\\\\E|\\\\Q") + "\\E";
			
			boolean	match_result = true;
			
			if ( regex && s.startsWith( "!" )){
				
				s = s.substring(1);
				
				match_result = false;
			}
			
			Pattern pattern = RegExUtil.getCachedPattern( "archiveview:search", s, Pattern.CASE_INSENSITIVE);
	
			return( pattern.matcher(name).find() == match_result );
		}
	}

	public Object 
	dataSourceChanged(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{
		if ( params instanceof DownloadStub ){
			
			if (tv != null) {
				
				TableRowCore row = tv.getRow((DownloadStub) params);
				
				if ( row != null ){
					
					tv.setSelectedRows(new TableRowCore[] { row });
				}
			}
		}
		
		datasource = params;
		
		return( null );
	}
}
