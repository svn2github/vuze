/*
 * Created on 2 juil. 2003
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.ui.swt.views;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.plugins.download.DownloadStub.DownloadStubFile;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.archivedfiles.*;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;




public class ArchivedFilesView 
	extends TableViewTab<DownloadStubFile>
	implements 	TableLifeCycleListener, TableDataSourceChangedListener, 
				TableViewSWTMenuFillListener
{
	private static final String TABLE_ID = "ArchivedFiles";
	
	private final static TableColumnCore[] basicItems = {
		new NameItem(TABLE_ID),
		new SizeItem(TABLE_ID),
	};

	static{
		TableColumnManager tcManager = TableColumnManager.getInstance();

		tcManager.setDefaultColumnNames( TABLE_ID, basicItems );
	}
	 
	public static final String MSGID_PREFIX = "ArchivedFilesView";
	
	private TableViewSWT<DownloadStubFile> tv;

	private DownloadStub	current_download;

	public static boolean show_full_path;

	static{
		COConfigurationManager.addAndFireParameterListener(
			"ArchivedFilesView.show.full.path",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameterName) 
				{
					show_full_path = COConfigurationManager.getBooleanParameter( "ArchivedFilesView.show.full.path" );
				}
			});
	}
	
	public 
	ArchivedFilesView() 
	{
		super(MSGID_PREFIX);
	}

	public TableViewSWT<DownloadStubFile>
	initYourTableView() 
	{
		tv = TableViewFactory.createTableViewSWT(
				DownloadStubFile.class,
				TABLE_ID, 
				getPropertiesPrefix(), 
				basicItems,
				basicItems[0].getName(), 
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );

		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		tv.addTableDataSourceChangedListener(this, true);
		
		tv.setEnableTabViews(false,true,null);
		
		return( tv );
	}
	
	public void 
	fillMenu(
		String sColumnName, Menu menu) 
	{
		List<Object>	ds = tv.getSelectedDataSources();
		
		final List<DownloadStubFile> files = new ArrayList<DownloadStub.DownloadStubFile>();
		
		for ( Object o: ds ){
			
			files.add((DownloadStubFile)o);
		}
		
		boolean	hasSelection = files.size() > 0;
		
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
				for ( DownloadStubFile file: files ){
				
					ManagerUtils.open( new File( file.getFile().getAbsolutePath()), use_open_containing_folder);
				}
			}
		});
		
		itemExplore.setEnabled(hasSelection);
		
		new MenuItem( menu, SWT.SEPARATOR );
	}
	
	public void 
	addThisColumnSubMenu(
		String columnName, 
		Menu menuThisColumn) 
	{
	    if ( columnName.equals("name")){
	    	
	    	new MenuItem( menuThisColumn, SWT.SEPARATOR );
	    	
	    	final MenuItem path_item = new MenuItem( menuThisColumn, SWT.CHECK );

	    	path_item.setSelection( show_full_path );

	    	Messages.setLanguageText(path_item, "FilesView.fullpath");

	    	path_item.addListener(SWT.Selection, new Listener() {
	    		public void handleEvent(Event e) {
	    			show_full_path = path_item.getSelection();
	    			tv.columnInvalidate("name");
	    			tv.refreshTable(false);
	    			COConfigurationManager.setParameter( "ArchivedFilesView.show.full.path", show_full_path );
	    		}
	    	});	
	    }
	}
	
	public void 
	tableDataSourceChanged(
		Object ds ) 
	{
		if ( ds == current_download ){
			
			tv.setEnabled( ds != null );
			
			return;
		}
		
		boolean	enabled = true;
		
		if ( ds instanceof DownloadStub ){
			
			current_download = (DownloadStub)ds;
			
		}else if ( ds instanceof Object[]) {
			
			Object[] objs = (Object[])ds;
			
			if ( objs.length != 1 ){
				
				enabled = false;
				
			}else{
				
				DownloadStub stub = (DownloadStub)objs[0];
				
				if ( stub == current_download ){
					
					return;
				}
				
				current_download = stub;
			}
		}else{
			
			current_download = null;
			
			enabled = false;
		}
		
	  	if ( !tv.isDisposed()){
	  		
			tv.removeAllTableRows();
			
			tv.setEnabled( enabled );
			
			if ( enabled ){
				
				if ( current_download != null ){
		  		
					addExistingDatasources();
				}
			}
	    }
	}
	
	public void 
	tableViewInitialized() 
	{
		if ( current_download != null ){
		
			addExistingDatasources();
			
		}else{
		
			tv.setEnabled( false );
		}
    }

	public void 
	tableViewDestroyed() 
	{
	}

	private void 
	addExistingDatasources() 
	{
		if ( current_download == null || tv.isDisposed()){
			
			return;
		}

		DownloadStubFile[] files = current_download.getStubFiles();
		
		tv.addDataSources( files );
		
		tv.processDataSourceQueueSync();
	}
}
