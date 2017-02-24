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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.history.DownloadHistory;
import org.gudy.azureus2.core3.history.DownloadHistoryEvent;
import org.gudy.azureus2.core3.history.DownloadHistoryListener;
import org.gudy.azureus2.core3.history.DownloadHistoryManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.dlhistory.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;


public class SBC_DownloadHistoryView
	extends SkinView
	implements 	UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<DownloadHistory>,
	TableViewSWTMenuFillListener, TableSelectionListener, DownloadHistoryListener
{
	private static final String TABLE_NAME = "DownloadHistory";

	private static final DownloadHistoryManager dh_manager =
			(DownloadHistoryManager)AzureusCoreFactory.getSingleton().getGlobalManager().getDownloadHistoryManager();
	
	private TableViewSWT<DownloadHistory> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

	private boolean dh_listener_added;

	private Object datasource;
	  
	public Object 
	skinObjectInitialShow(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{		
		initColumns();
		
		return( null );
	}

	protected void 
	initColumns() 
	{
		synchronized (SBC_DownloadHistoryView.class) {

			if ( columnsAdded ){

				return;
			}

			columnsAdded = true;
		}

		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.registerColumn(DownloadHistory.class, ColumnDLHistoryName.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistoryName(column);
					}
				});
		
		tableManager.registerColumn(
				DownloadHistory.class,
				ColumnDLHistoryAddDate.COLUMN_ID,
				new TableColumnCoreCreationListener() {
					public TableColumnCore createTableColumnCore(
							Class<?> forDataSourceType, String tableID, String columnID) {
						return new ColumnDateSizer(DownloadHistory.class, columnID,
								TableColumnCreator.DATE_COLUMN_WIDTH, tableID) {
						};
					}

					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistoryAddDate(column);
					}
				});

		tableManager.registerColumn(
				DownloadHistory.class,
				ColumnDLHistoryCompleteDate.COLUMN_ID,
				new TableColumnCoreCreationListener() {
					public TableColumnCore createTableColumnCore(
							Class<?> forDataSourceType, String tableID, String columnID) {
						return new ColumnDateSizer(DownloadHistory.class, columnID,
								TableColumnCreator.DATE_COLUMN_WIDTH, tableID) {
						};
					}

					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistoryCompleteDate(column);
					}
				});
		
		tableManager.registerColumn(
				DownloadHistory.class,
				ColumnDLHistoryRemoveDate.COLUMN_ID,
				new TableColumnCoreCreationListener() {
					public TableColumnCore createTableColumnCore(
							Class<?> forDataSourceType, String tableID, String columnID) {
						return new ColumnDateSizer(DownloadHistory.class, columnID,
								TableColumnCreator.DATE_COLUMN_WIDTH, tableID) {
						};
					}

					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistoryRemoveDate(column);
					}
				});
		
		tableManager.registerColumn(DownloadHistory.class, ColumnDLHistoryHash.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistoryHash(column);
					}
				});
		
		tableManager.registerColumn(DownloadHistory.class, ColumnDLHistorySize.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistorySize(column);
					}
				});

		tableManager.registerColumn(DownloadHistory.class, ColumnDLHistorySaveLocation.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnDLHistorySaveLocation(column);
					}
				});
		
		tableManager.setDefaultColumnNames(TABLE_NAME,
				new String[] {
					ColumnDLHistoryName.COLUMN_ID,
					ColumnDLHistoryAddDate.COLUMN_ID,
					ColumnDLHistoryCompleteDate.COLUMN_ID,
					ColumnDLHistoryRemoveDate.COLUMN_ID,
				});
		
		tableManager.setDefaultSortColumnName(TABLE_NAME, ColumnDLHistoryName.COLUMN_ID);
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
		
		if ( dh_listener_added ){
		
			dh_manager.removeListener( this );
			
			dh_listener_added = false;
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
		
		SWTSkinObject so_list = getSkinObject( "dl-history-list" );

		if ( so_list != null ){
			
			initTable((Composite)so_list.getControl());
			
		}else{
			
			System.out.println("NO dl-history-list");
			
			return( null );
		}
				
		if ( tv == null ){
			
			return( null );
		}

		if ( dh_manager != null ){
			
			dh_manager.addListener( this, true );
			
			dh_listener_added = true;
		}
		
		return( null );
	}

	@Override
	public Object 
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{
		if ( dh_listener_added ){
			
			dh_manager.removeListener( this );
			
			dh_listener_added = false;
		}		
		
		return super.skinObjectDestroyed(skinObject, params);
	}
	

	private void 
	initTable(
		Composite control )
	{
		if ( tv == null ){
			
			tv = TableViewFactory.createTableViewSWT(
					DownloadHistory.class, TABLE_NAME, TABLE_NAME,
					new TableColumnCore[0], 
					ColumnDLHistoryName.COLUMN_ID, 
					SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
			
			if ( txtFilter != null){
				
				tv.enableFilterCheck( txtFilter, this );
			}
			
			tv.setRowDefaultHeightEM(1);
			
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
			
			if ( dh_manager == null ){
				
				control.setEnabled( false );
			}
		}

		control.layout( true );
	}

	public boolean 
	toolBarItemActivated(
		ToolBarItem item, 
		long activationType,
		Object datasource) 
	{
		if ( tv == null || !tv.isVisible() || dh_manager == null ){
			
			return( false );
		}
		
		List<Object> datasources = tv.getSelectedDataSources();
		
		if ( datasources.size() > 0 ){

			List<DownloadHistory>	dms = new ArrayList<DownloadHistory>( datasources.size());
			
			for ( Object o: datasources ){
				
				dms.add((DownloadHistory)o);
			}
			
			String id = item.getID();
			
			if ( id.equals("remove")) {
			
				dh_manager.removeHistory( dms );
				
			}else if ( id.equals("startstop")) {
				
				for ( DownloadHistory download: dms ){
					
					download.setRedownloading();
					
					String magnet = UrlUtils.getMagnetURI( download.getTorrentHash(), download.getName(), null );
					
					TorrentOpener.openTorrent( magnet );
				}	
			}

				
			return true;
		}
		
		return false;
	}

	public void 
	refreshToolBarItems(
		Map<String, Long> list) 
	
	{
		if ( tv == null || !tv.isVisible() || dh_manager == null ){
			
			return;
		}

		boolean canEnable = false;
		boolean canStart = false;
		
		Object[] datasources = tv.getSelectedDataSources().toArray();
		
		if ( datasources.length > 0 ){
			
			canEnable = true;
			canStart = true;
		}
		
		list.put( "remove", canEnable ? UIToolBarItem.STATE_ENABLED : 0);
		list.put( "start", canStart ? UIToolBarItem.STATE_ENABLED : 0);
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
		Menu 		menu ) 
	{
		if ( dh_manager != null ){
			
			new MenuItem( menu, SWT.SEPARATOR );

			if ( dh_manager.isEnabled()){
				
					// reset 
					
				MenuItem itemReset = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText(itemReset, "label.reset.history" );
				
				itemReset.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						resetHistory();
					}
				});
				
					// disable
					
				MenuItem itemDisable = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText( itemDisable, "label.disable.history" );
				
				itemDisable.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						dh_manager.setEnabled( false );
					}
				});
			
			}else{
							
					// enable 
					
				MenuItem itemEnable = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText( itemEnable, "label.enable.history" );
				
				itemEnable.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						dh_manager.setEnabled( true );
					}
				});
			}
		
			new MenuItem( menu, SWT.SEPARATOR );
		}
	}
	
	public void 
	fillMenu(
		String 	sColumnName, 
		Menu 	menu )
	{
		if ( dh_manager != null ){
			
			if ( dh_manager.isEnabled()){
			
				List<Object>	ds = tv.getSelectedDataSources();
				
				final List<DownloadHistory>	dms = new ArrayList<DownloadHistory>( ds.size());
				
				for ( Object o: ds ){
					
					dms.add((DownloadHistory)o);
				}
				
				boolean	hasSelection = dms.size() > 0;
				
					// Explore (or open containing folder)
				
				final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
				
				MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText(itemExplore, "MyTorrentsView.menu."
						+ (use_open_containing_folder ? "open_parent_folder" : "explore"));
				
				itemExplore.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						for ( DownloadHistory download: dms ){
						
							ManagerUtils.open( new File( download.getSaveLocation()), use_open_containing_folder);
						}
					}
				});
				
				itemExplore.setEnabled(hasSelection);
				
					// redownload
				
				MenuItem itemRedownload = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText(itemRedownload, "label.redownload" );
				
				itemRedownload.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						for ( DownloadHistory download: dms ){
						
							download.setRedownloading();
							
							String magnet = UrlUtils.getMagnetURI( download.getTorrentHash(), download.getName(), null );
							
							TorrentOpener.openTorrent( magnet );
						}
					}
				});
				
				itemExplore.setEnabled(hasSelection);
					// remove
					
				MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
				Utils.setMenuItemImage(itemRemove, "delete");
				
				Messages.setLanguageText( itemRemove, "MySharesView.menu.remove" );
				
				itemRemove.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						dh_manager.removeHistory( dms );
					}
				});
			
				itemRemove.setEnabled(hasSelection);
				
				new MenuItem( menu, SWT.SEPARATOR );
				
					// reset 
				
				MenuItem itemReset = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText(itemReset, "label.reset.history" );
				
				itemReset.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						resetHistory();
					}
				});
			
					// disable
				
				MenuItem itemDisable = new MenuItem(menu, SWT.PUSH);
				
				Messages.setLanguageText( itemDisable, "label.disable.history" );
				
				itemDisable.addListener(SWT.Selection, new Listener() {
					public void 
					handleEvent(
						Event event) 
					{
						dh_manager.setEnabled( false );
					}
				});
				
				new MenuItem( menu, SWT.SEPARATOR );
			}
		}
	}
	
	private void
	resetHistory()
	{
		MessageBoxShell mb = new MessageBoxShell(
				MessageText.getString("downloadhistoryview.reset.title"),
				MessageText.getString("downloadhistoryview.reset.text"));
		
		mb.setButtons(0, new String[] {
			MessageText.getString("Button.yes"),
			MessageText.getString("Button.no"),
		}, new Integer[] { 0, 1 });

		mb.open(new UserPrompterResultListener(){
			public void prompterClosed(int result) {
				if (result == 0) {
					dh_manager.resetHistory();
				}
			}
		});
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
	}

	public void
	downloadHistoryEventOccurred(
		DownloadHistoryEvent		event )
	{
		int type = event.getEventType();
		
		List<DownloadHistory> dls = event.getHistory();
				
		if ( type == DownloadHistoryEvent.DHE_HISTORY_ADDED ){
			
			tv.addDataSources( dls.toArray( new DownloadHistory[dls.size()] ));
						
		}else if ( type == DownloadHistoryEvent.DHE_HISTORY_REMOVED ){
			
			tv.removeDataSources( dls.toArray( new DownloadHistory[dls.size()] ));
			
		}else{
			
			for ( DownloadHistory d: dls ){
			
				TableRowCore row = tv.getRow( d );
				
				if ( row != null ){
					
					row.invalidate( true );
				}
			}
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
		DownloadHistory 	ds, 
		String 				filter, 
		boolean 			regex) 
	{
		Object o_name;
			
		if ( filter.startsWith( "t:" )){
			
			filter = filter.substring( 2 );
			
			byte[] hash = ds.getTorrentHash();
			
			List<String> names = new ArrayList<String>();
			
			names.add( ByteFormatter.encodeString( hash ));
			
			names.add( Base32.encode( hash ));
			
			o_name = names;
			
		}else{
			
			o_name = ds.getName();
		
		}
		
		String s = regex ? filter : "\\Q" + filter.replaceAll("\\s*[|;]\\s*", "\\\\E|\\\\Q") + "\\E";
		
		boolean	match_result = true;
		
		if ( regex && s.startsWith( "!" )){
			
			s = s.substring(1);
			
			match_result = false;
		}
		
		Pattern pattern = RegExUtil.getCachedPattern( "downloadhistoryview:search", s, Pattern.CASE_INSENSITIVE);

		boolean bOurs;
		
		if ( o_name instanceof String ){
			
			bOurs = pattern.matcher((String)o_name).find() == match_result;
			
		}else{
			
			List<String>	names = (List<String>)o_name;
			
				// match_result: true -> at least one match; false -> any fail
			
			bOurs = !match_result;
			
			for ( String name: names ){
				
				if ( pattern.matcher( name ).find()){
					
					bOurs = match_result;
					
					break;
				}
			}
		}
		
		return( bOurs );
	}

	public Object 
	dataSourceChanged(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{
		if ( params instanceof DownloadHistory ){
			
			if (tv != null) {
				
				TableRowCore row = tv.getRow((DownloadHistory) params);
				
				if ( row != null ){
					
					tv.setSelectedRows(new TableRowCore[] { row });
				}
			}
		}
		
		datasource = params;
		
		return( null );
	}
}
