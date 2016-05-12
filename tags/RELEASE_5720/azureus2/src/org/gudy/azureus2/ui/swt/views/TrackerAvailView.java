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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.core3.download.DownloadManagerAvailability;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.tracker.*;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.ui.common.table.*;




public class TrackerAvailView 
	extends TableViewTab<TrackerPeerSource>
	implements 	TableLifeCycleListener, TableDataSourceChangedListener, 
				TableViewSWTMenuFillListener
{
	private static final String TABLE_ID = "TrackerAvail";
	
	private final static TableColumnCore[] basicItems = {
		new TypeItem(TABLE_ID),
		new NameItem(TABLE_ID),
		new StatusItem(TABLE_ID),
		new SeedsItem(TABLE_ID),
		new LeechersItem(TABLE_ID),
		new PeersItem(TABLE_ID),
		new CompletedItem(TABLE_ID),
	};

	public static final String MSGID_PREFIX = "TrackerView";

	private DownloadManagerAvailability 	availability;
	
	private TableViewSWT<TrackerPeerSource> tv;

	/**
	 * Initialize
	 *
	 */
	public TrackerAvailView() {
		super(MSGID_PREFIX);
	}

	public TableViewSWT<TrackerPeerSource>
	initYourTableView() 
	{
		tv = TableViewFactory.createTableViewSWT(
				TrackerPeerSource.class,
				TABLE_ID, 
				getPropertiesPrefix(), 
				basicItems,
				basicItems[0].getName(), 
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );

		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		tv.addTableDataSourceChangedListener(this, true);
		
		tv.setEnableTabViews(false,true,null);

		return tv;
	}

	public boolean
	isUpdating()
	{
		List<TrackerPeerSource> peer_sources = tv.getDataSources();
		
		for ( TrackerPeerSource p: peer_sources ){
			
			int status = p.getStatus();
			
			if ( 	status == TrackerPeerSource.ST_INITIALISING ||
					status == TrackerPeerSource.ST_QUEUED ||
					status == TrackerPeerSource.ST_UPDATING ){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	public void 
	fillMenu(
		String sColumnName, Menu menu) 
	{
	}
	
	public void 
	addThisColumnSubMenu(
		String columnName, 
		Menu menuThisColumn) 
	{
	}
	

	
	public void 
	tableDataSourceChanged(
		Object newDataSource ) 
	{
	  	DownloadManagerAvailability old_avail = availability;
		if (newDataSource == null){
			availability = null;
		}else if (newDataSource instanceof Object[]){
			Object temp = ((Object[])newDataSource)[0];
			if ( temp instanceof DownloadManagerAvailability ){
				availability = (DownloadManagerAvailability)temp;
			}else{
				return;
			}
		}else{
			if ( newDataSource instanceof DownloadManagerAvailability ){
				availability = (DownloadManagerAvailability)newDataSource;
			}else{
				return;
			}
		}
		if ( old_avail == availability ){
			return;
		}
	  	
	  	if ( !tv.isDisposed()){
	  		
			tv.removeAllTableRows();
	  	
			if ( availability != null ){
	  		
				addExistingDatasources();
			}
	    }
	}
	
	public void 
	tableViewInitialized() 
	{
		if ( availability != null ){
			
			addExistingDatasources();
		}
    }

	public void 
	tableViewDestroyed() 
	{
	}

	private void 
	addExistingDatasources() 
	{
		if ( availability == null || tv.isDisposed()){
			
			return;
		}

		List<TrackerPeerSource> tps = availability.getTrackerPeerSources();
		
		tv.addDataSources( tps.toArray( (new TrackerPeerSource[tps.size()])));
		
		tv.processDataSourceQueueSync();
	}
}
