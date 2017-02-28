/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
 
package org.gudy.azureus2.ui.swt.views.peer;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;

import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;




public class PeerFilesView
	extends TableViewTab<PeerFilesView.PeersFilesViewRow>
	implements TableDataSourceChangedListener, TableLifeCycleListener, TableRefreshListener
{
	public static final String TABLEID_PEER_FILES	= "PeerFiles";

	boolean refreshing = false;

	private static final TableColumnCore[] basicItems = {
	 
		new NameItem(),
		new PercentItem(),
	};
  
	static{
		TableColumnManager tcManager = TableColumnManager.getInstance();

		tcManager.setDefaultColumnNames( TABLEID_PEER_FILES, basicItems );
	}
	  


	private TableViewSWT<PeersFilesViewRow> tv;
  
	private PEPeer	current_peer;
	
	public PeerFilesView() {
		super( "PeerFilesView");
		
	}


	public TableViewSWT<PeersFilesViewRow> 
	initYourTableView() 
	{
		tv = TableViewFactory.createTableViewSWT(
				PeersFilesViewRow.class,
				TABLEID_PEER_FILES, getPropertiesPrefix(), basicItems,
				"firstpiece", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		
		tv.addTableDataSourceChangedListener(this, true);
		tv.addRefreshListener(this, true);
		tv.addLifeCycleListener(this);

		return tv;
	}

  
 	public void 
 	tableDataSourceChanged(
 		Object newDataSource) 
 	{
 		if ( newDataSource instanceof PEPeer ){
 			
 			current_peer = (PEPeer)newDataSource;
 			
 		}if ( newDataSource instanceof Object[] ){
 			
 			Object[] temp = (Object[])newDataSource;
 			
 			if ( temp.length > 0 && temp[0] instanceof PEPeer ){
 				
 				current_peer = (PEPeer)temp[0];
 				
 			}else{
 			
 				current_peer = null;
 			}
 		}else{
 		
 			current_peer = null;
 		}
	}
	
	public void 
	tableRefresh() 
	{
		synchronized( this ){
			
			if ( refreshing ){
				
				return;
			}
			
			refreshing = true;
		}
		
		try{
			PEPeer	peer = current_peer;
			
			if ( peer == null ){
				
				tv.removeAllTableRows();
				
			}else{
			
				if ( tv.getRowCount() == 0 ){
					
					DiskManagerFileInfo[] files = peer.getManager().getDiskManager().getFiles();
					
					PeersFilesViewRow[] rows = new PeersFilesViewRow[ files.length ];
					
					for ( int i=0;i<files.length;i++ ){
						
						rows[i] = new PeersFilesViewRow( files[i], peer );
					}
					
					tv.addDataSources( rows );
					
					tv.processDataSourceQueueSync();
					
				}else{
					
					TableRowCore[] rows = tv.getRows();
					
					for ( TableRowCore row: rows ){
						
						((PeersFilesViewRow)row.getDataSource()).setPeer( peer );
					}
				}
			}
		}finally{
			
			synchronized( this ){
				
				refreshing = false;
			}
		}
	} 
 
	public void tableViewInitialized() {
   
	}
  
	public void tableViewTabInitComplete() {
  
		super.tableViewTabInitComplete();
	}
  
	public void tableViewDestroyed() {

	}
	
	protected static class
	PeersFilesViewRow
	{
		private DiskManagerFileInfo		file;
		private PEPeer					peer;
		
		private
		PeersFilesViewRow(
			DiskManagerFileInfo		_file,
			PEPeer					_peer )
		{
			file	= _file;
			peer	= _peer;
		}
		
		private DiskManagerFileInfo
		getFile()
		{
			return( file );
		}
		
		private void
		setPeer(
			PEPeer	_peer )
		{
			peer	= _peer;
		}
		
		private PEPeer
		getPeer()
		{
			return( peer );
		}
	}
	
	private static class
	NameItem 
		extends CoreTableColumnSWT
		implements TableCellRefreshListener
	{
		private 
		NameItem() 
		{
			super( "name", ALIGN_LEAD, POSITION_LAST, 300, TABLEID_PEER_FILES );
			
			setType(TableColumn.TYPE_TEXT);
			
		}
		
		public void 
		refresh(
			TableCell cell )
		{
			PeersFilesViewRow row = (PeersFilesViewRow) cell.getDataSource();
			String name = (row == null) ? "" : row.getFile().getFile(true).getName();
			if (name == null)
				name = "";
			
			cell.setText( name );
		}
	}
	
	private static class
	PercentItem 
		extends CoreTableColumnSWT
		implements TableCellRefreshListener
	{
		private 
		PercentItem() 
		{
			super( "%", ALIGN_TRAIL, POSITION_LAST, 60, TABLEID_PEER_FILES );
			setRefreshInterval(INTERVAL_LIVE);
			setMinWidthAuto(true);
		}
		
		public void 
		refresh(
			TableCell cell )
		{
			PeersFilesViewRow row = (PeersFilesViewRow) cell.getDataSource();
			
			if ( row == null ){
				
				return;
			}
			
			DiskManagerFileInfo	file = row.getFile();
			
			PEPeer peer = row.getPeer();
			
			BitFlags pieces = peer.getAvailable();
			
			if( pieces == null ){
				
				cell.setText( "" );
				
				return;
			}
			
			boolean[] flags = pieces.flags;
			
			int	first_piece = file.getFirstPieceNumber();
			
			int	last_piece	= file.getLastPieceNumber();
			
			int	done = 0;
			
			for ( int i=first_piece;i<=last_piece;i++){
			
				if ( flags[i] ){
					
					done++;
				}
			}
			
			int percent = ( done * 1000 ) / (last_piece - first_piece + 1 );
			
			if ( !cell.setSortValue(percent) && cell.isValid()){

				return;
			}
			
			cell.setText(percent < 0 ? "" : DisplayFormatters.formatPercentFromThousands((int) percent));
				
		}
	}
}