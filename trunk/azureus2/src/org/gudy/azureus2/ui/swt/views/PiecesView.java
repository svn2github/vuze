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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPieceListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListenerEx;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventImpl;
import org.gudy.azureus2.ui.swt.views.piece.MyPieceDistributionView;
import org.gudy.azureus2.ui.swt.views.piece.PieceInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWT_TabsCommon;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.*;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PriorityItem, SpeedItem
 */

public class PiecesView 
	extends TableViewTab<PEPiece>
	implements DownloadManagerPeerListener, 
	DownloadManagerPieceListener,
	TableDataSourceChangedListener,
	TableLifeCycleListener,
	TableViewSWTMenuFillListener,
	UISWTViewCoreEventListenerEx
{
	private static boolean registeredCoreSubViews = false;

	private final static TableColumnCore[] basicItems = {
		new PieceNumberItem(),
		new SizeItem(),
		new BlockCountItem(),
		new BlocksItem(),
		new CompletedItem(),
		new AvailabilityItem(),
		new TypeItem(),
		new ReservedByItem(),
		new WritersItem(),
		new PriorityItem(),
		new SpeedItem(),
		new RequestedItem()
	};

	static{
		TableColumnManager tcManager = TableColumnManager.getInstance();

		tcManager.setDefaultColumnNames( TableManager.TABLE_TORRENT_PIECES, basicItems );
	}
	
	public static final String MSGID_PREFIX = "PiecesView";

	private DownloadManager 		manager;
	private boolean					enable_tabs = true;
	private TableViewSWT<PEPiece> 	tv;

	private Composite legendComposite;

  
	/**
	 * Initialize
	 *
	 */
	public PiecesView() {
		super(MSGID_PREFIX);
	}

	public boolean
	isCloneable()
	{
		return( true );
	}
	
	public UISWTViewCoreEventListener
	getClone()
	{
		return( new PiecesView());
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
	public TableViewSWT<PEPiece> initYourTableView() {
		tv = TableViewFactory.createTableViewSWT(PEPiece.class,
				TableManager.TABLE_TORRENT_PIECES, getPropertiesPrefix(), basicItems,
				basicItems[0].getName(), SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		tv.setEnableTabViews(enable_tabs,true,null);

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
			
			if (pluginUI != null && !registeredCoreSubViews) {
				
				pluginUI.addView(TableManager.TABLE_TORRENT_PIECES,
						"PieceInfoView", PieceInfoView.class, manager);

				pluginUI.addView(TableManager.TABLE_TORRENT_PIECES,
						"MyPieceDistributionView", MyPieceDistributionView.class, manager);

				registeredCoreSubViews = true;
			}
		}

		tv.addTableDataSourceChangedListener(this, true);
		tv.addMenuFillListener(this);
		tv.addLifeCycleListener(this);

		return tv;
	}

	public void 
	fillMenu(
		String 	sColumnName, 
		Menu 	menu )
	{
		final List<Object>	selected = tv.getSelectedDataSources();
		
		if ( selected.size() == 0 ){
			
			return;
		}
		
		if ( manager == null ){
			
			return;
		}
		
		PEPeerManager pm = manager.getPeerManager();
		
		if ( pm == null ){
			
			return;
		}
		
		final PiecePicker picker = pm.getPiecePicker();
		
		boolean	has_undone	 	= false;
		boolean	has_unforced	= false;
		
		for ( Object obj: selected ){
			
			PEPiece piece = (PEPiece)obj;
			
			if ( !piece.getDMPiece().isDone()){
				
				has_undone = true;
				
				if ( picker.isForcePiece( piece.getPieceNumber())){
					
					has_unforced = true;
				}
			}
		}
		
		final MenuItem force_piece = new MenuItem( menu, SWT.CHECK );
		
		Messages.setLanguageText( force_piece, "label.force.piece" );
				
		force_piece.setEnabled( has_undone );
		
		if ( has_undone ){
		
			force_piece.setSelection( has_unforced );
			
			force_piece.addSelectionListener(
	    		new SelectionAdapter()
	    		{
	    			public void 
	    			widgetSelected(
	    				SelectionEvent e) 
	    			{
	    				boolean	forced = force_piece.getSelection();
	    				
	    				for ( Object obj: selected ){
	    					
	    					PEPiece piece = (PEPiece)obj;
	    					
	    					if ( !piece.getDMPiece().isDone()){
	    						
	    						picker.setForcePiece( piece.getPieceNumber(), forced );
	    					}
	    				}
	    			}
	    		});
		}
		
		final MenuItem cancel_reqs_piece = new MenuItem( menu, SWT.PUSH );
		
		Messages.setLanguageText( cancel_reqs_piece, "label.rerequest.blocks" );
			
		cancel_reqs_piece.addSelectionListener(
    		new SelectionAdapter()
    		{
    			public void 
    			widgetSelected(
    				SelectionEvent e) 
    			{
     				for ( Object obj: selected ){
    					
    					PEPiece piece = (PEPiece)obj;
    					
    					for ( int i=0;i<piece.getNbBlocks();i++){
    						
    						if ( piece.isRequested( i )){
    							
    							piece.clearRequested( i );
    						}
    					}
    				}
    			}
    		});
				
		final MenuItem reset_piece = new MenuItem( menu, SWT.PUSH );
		
		Messages.setLanguageText( reset_piece, "label.reset.piece" );
			
		reset_piece.addSelectionListener(
    		new SelectionAdapter()
    		{
    			public void 
    			widgetSelected(
    				SelectionEvent e) 
    			{
     				for ( Object obj: selected ){
    					
    					PEPiece piece = (PEPiece)obj;
    					
    					piece.reset();
    				}
    			}
    		});
		
		new MenuItem( menu, SWT.SEPARATOR );
	}
	
	public void 
	addThisColumnSubMenu(
		String 	sColumnName, 
		Menu 	menuThisColumn )
	{
		
	}
	
	private boolean comp_focused;
	private Object focus_pending_ds;

	private void
	setFocused( boolean foc )
	{
		if ( foc ){

			comp_focused = true;

			dataSourceChanged( focus_pending_ds );

		}else{

			focus_pending_ds = manager;

			dataSourceChanged( null );

			comp_focused = false;
		}
	}
	  
	// @see com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener#tableDataSourceChanged(java.lang.Object)
	public void tableDataSourceChanged(Object newDataSource) {
		if ( !comp_focused ){
			focus_pending_ds = newDataSource;
			return;
		}

		DownloadManager newManager = ViewUtils.getDownloadManagerFromDataSource( newDataSource );
	
		if (newManager == manager) {
			tv.setEnabled(manager != null);
			return;
		}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
		}
		
		manager = newManager;
		
		if (tv.isDisposed()){
			return;
		}

		tv.removeAllTableRows();
		tv.setEnabled(manager != null);

		if (manager != null) {
			manager.addPeerListener(this, false);
			manager.addPieceListener(this, false);
			addExistingDatasources();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
	public void tableViewInitialized() {
		if (legendComposite != null && tv != null) {
			Composite composite = tv.getTableComposite();

			legendComposite = Legend.createLegendComposite(composite,
					BlocksItem.colors, new String[] {
						"PiecesView.legend.requested",
						"PiecesView.legend.written",
						"PiecesView.legend.downloaded",
						"PiecesView.legend.incache"
					});
		}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
			manager.addPeerListener(this, false);
			manager.addPieceListener(this, false);
			addExistingDatasources();

		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewDestroyed()
	public void tableViewDestroyed() {
		if (legendComposite != null && legendComposite.isDisposed()) {
			legendComposite.dispose();
		}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
		}
	}

	/* DownloadManagerPeerListener implementation */
	public void pieceAdded(PEPiece created) {
    tv.addDataSource(created);
	}

	public void pieceRemoved(PEPiece removed) {    
    tv.removeDataSource(removed);
	}

	public void peerAdded(PEPeer peer) {  }
	public void peerRemoved(PEPeer peer) {  }
  public void peerManagerWillBeAdded( PEPeerManager	peer_manager ){}
	public void peerManagerAdded(PEPeerManager manager) {	}
	public void peerManagerRemoved(PEPeerManager	manager) {
		tv.removeAllTableRows();
	}

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		if (manager == null || tv.isDisposed()) {
			return;
		}

		PEPiece[] dataSources = manager.getCurrentPieces();
		if (dataSources != null && dataSources.length >= 0) {
  		tv.addDataSources(dataSources);
    	tv.processDataSourceQueue();
		}

		// For this view the tab datasource isn't driven by table row selection so we
		// need to update it with the primary data source
		
		// TODO: TrackerView and PiecesView now have this similar code -- this
		//       would be better handled in TableViewTab (or TableViewSWT?)
	
		TableViewSWT_TabsCommon tabs = tv.getTabsCommon();
		
		if ( tabs != null ){
			
			tabs.triggerTabViewsDataSourceChanged(tv);
		}

	}

	/**
	 * @return the manager
	 */
	public DownloadManager getManager() {
		return manager;
	}
	
	public boolean eventOccurred(UISWTViewEvent event) {
	    switch (event.getType()) {
	     
	      case UISWTViewEvent.TYPE_CREATE:{
	    	  if ( event instanceof UISWTViewEventImpl ){
	    		  
	    		  String parent = ((UISWTViewEventImpl)event).getParentID();
	    		  
	    		  enable_tabs = parent != null && parent.equals( UISWTInstance.VIEW_TORRENT_DETAILS );
	    	  }
	    	  break;
	      }
	      case UISWTViewEvent.TYPE_FOCUSGAINED:
	      	String id = "DMDetails_Pieces";
	      	
	      	setFocused( true );	// do this here to pick up corrent manager before rest of code
	      	
	      	if (manager != null) {
	      		if (manager.getTorrent() != null) {
	  					id += "." + manager.getInternalName();
	      		} else {
	      			id += ":" + manager.getSize();
	      		}
						SelectedContentManager.changeCurrentlySelectedContent(id,
								new SelectedContent[] {
									new SelectedContent(manager)
						});
					} else {
						SelectedContentManager.changeCurrentlySelectedContent(id, null);
					}
	  
		    break;
	      case UISWTViewEvent.TYPE_FOCUSLOST:
	    	  setFocused( false );
	    		SelectedContentManager.clearCurrentlySelectedContent();
	    	  break;	
	    }
	    
	    return( super.eventOccurred(event));
	}
	
}
