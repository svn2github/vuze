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
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerTPSListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventImpl;
import org.gudy.azureus2.ui.swt.views.table.TableSelectedRowsListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWT_TabsCommon;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.tracker.*;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;


/**
 * aka "Sources" view
 */
public class TrackerView 
	extends TableViewTab<TrackerPeerSource>
	implements 	TableLifeCycleListener, TableDataSourceChangedListener, 
				DownloadManagerTPSListener, TableViewSWTMenuFillListener
{
	private static boolean registeredCoreSubViews = false;

	private final static TableColumnCore[] basicItems = {
		new TypeItem(TableManager.TABLE_TORRENT_TRACKERS),
		new NameItem(TableManager.TABLE_TORRENT_TRACKERS),
		new StatusItem(TableManager.TABLE_TORRENT_TRACKERS),
		new PeersItem(TableManager.TABLE_TORRENT_TRACKERS),
		new SeedsItem(TableManager.TABLE_TORRENT_TRACKERS),
		new LeechersItem(TableManager.TABLE_TORRENT_TRACKERS),
		new CompletedItem(TableManager.TABLE_TORRENT_TRACKERS),
		new UpdateInItem(TableManager.TABLE_TORRENT_TRACKERS),
		new IntervalItem(TableManager.TABLE_TORRENT_TRACKERS),
		new LastUpdateItem(TableManager.TABLE_TORRENT_TRACKERS),
	};

	public static final String MSGID_PREFIX = "TrackerView";

	private DownloadManager 	manager;
	private boolean				enable_tabs = true;
	
	private TableViewSWT<TrackerPeerSource> tv;

	/**
	 * Initialize
	 *
	 */
	public TrackerView() {
		super(MSGID_PREFIX);
	}

	public TableViewSWT<TrackerPeerSource>
	initYourTableView() 
	{
		tv = TableViewFactory.createTableViewSWT(
				TrackerPeerSource.class,
				TableManager.TABLE_TORRENT_TRACKERS, 
				getPropertiesPrefix(), 
				basicItems,
				basicItems[0].getName(), 
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );

		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		tv.addTableDataSourceChangedListener(this, true);
		
		tv.setEnableTabViews(enable_tabs,true,null);
		
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
			
			if (pluginUI != null && !registeredCoreSubViews) {

				pluginUI.addView(TableManager.TABLE_TORRENT_TRACKERS, "ScrapeInfoView",
						ScrapeInfoView.class, manager);

				registeredCoreSubViews = true;
			}
		}

		return tv;
	}

	
	public void 
	fillMenu(
		String sColumnName, Menu menu) 
	{
		final Object[] sources = tv.getSelectedDataSources().toArray();
		
		boolean	found_tracker		= false;
		boolean	found_dht_tracker	= false;
		boolean	update_ok 			= false;
		boolean delete_ok			= false;
		
		for ( Object o: sources ){
	
			TrackerPeerSource ps = (TrackerPeerSource)o;
		
			if ( ps.getType() == TrackerPeerSource.TP_TRACKER ){
				
				found_tracker = true;
				
			}	
			
			if ( ps.getType() == TrackerPeerSource.TP_DHT  ){
				
				found_dht_tracker = true;
			}
			
			int	state = ps.getStatus();
						
			if ( 	( 	state == TrackerPeerSource.ST_ONLINE || 
						state == TrackerPeerSource.ST_QUEUED || 
						state == TrackerPeerSource.ST_ERROR ) &&
					!ps.isUpdating() &&
					ps.canManuallyUpdate()){
				
				update_ok = true;
			}
			
			if ( ps.canDelete()){
				
				delete_ok = true;
			}
		}
		
		boolean	needs_sep = false;
		
		if ( found_tracker || found_dht_tracker ){
			
			final MenuItem update_item = new MenuItem( menu, SWT.PUSH);
	
			Messages.setLanguageText(update_item, "GeneralView.label.trackerurlupdate");
			
			update_item.setEnabled( update_ok );
			
			update_item.addListener(
				SWT.Selection, 
				new TableSelectedRowsListener(tv) 
				{
					public void 
					run(
						TableRowCore row )
					{
						for ( Object o: sources ){
							
							TrackerPeerSource ps = (TrackerPeerSource)o;
	
							if ( ps.canManuallyUpdate()){
								
								ps.manualUpdate();
							}
						}
					}
				});
			
			if ( found_tracker ){
				
				final MenuItem edit_item = new MenuItem( menu, SWT.PUSH);
				
				Messages.setLanguageText(edit_item, "MyTorrentsView.menu.editTracker" );
							
				edit_item.addListener(
					SWT.Selection, 
					new TableSelectedRowsListener(tv) 
					{
						public boolean 
						run(
							TableRowCore[] rows )
						{
							final TOTorrent torrent = manager.getTorrent();
	
							if (torrent != null) {
	
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											List<List<String>> group = TorrentUtils.announceGroupsToList(torrent);
					
											new MultiTrackerEditor(null,null, group, new TrackerEditorListener() {
												public void trackersChanged(String str, String str2, List<List<String>> _group) {
													TorrentUtils.listToAnnounceGroups(_group, torrent);
					
													try {
														TorrentUtils.writeToFile(torrent);
													} catch (Throwable e2) {
					
														Debug.printStackTrace(e2);
													}
					
													TRTrackerAnnouncer tc = manager.getTrackerClient();
					
													if (tc != null) {
					
														tc.resetTrackerUrl(true);
													}
												}
											}, true, true );
										}
									});
								
							}
							
							return( true );
						}
					});
				
				TOTorrent torrent = manager.getTorrent();
				
				edit_item.setEnabled( torrent != null && !TorrentUtils.isReallyPrivate( torrent ));
			}
		
			needs_sep = true;
		}
		
		if ( delete_ok ){
			
			final MenuItem delete_item = new MenuItem( menu, SWT.PUSH);
			
			Messages.setLanguageText(delete_item, "Button.remove" );
			Utils.setMenuItemImage(delete_item, "delete");	
			
			delete_item.addListener(
				SWT.Selection, 
				new TableSelectedRowsListener(tv) 
				{
					public void 
					run(
						TableRowCore row )
					{
						for ( Object o: sources ){
							
							TrackerPeerSource ps = (TrackerPeerSource)o;
							
							if ( ps.canDelete()){
								
								ps.delete();
							}
						}
					}
				});
			
			needs_sep = true;
		}
		
		if ( needs_sep ){
			
			new MenuItem( menu, SWT.SEPARATOR );
		}
	}
	
	public void 
	addThisColumnSubMenu(
		String columnName, 
		Menu menuThisColumn) 
	{
	}
	
	public void 
	trackerPeerSourcesChanged() 
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void
				runSupport()
				{
					if ( manager == null || tv.isDisposed()){
						
						return;
					}
					
					tv.removeAllTableRows();
					
					addExistingDatasources();
				}
			});
	}
	
	public void 
	tableDataSourceChanged(
		Object newDataSource ) 
 {
		DownloadManager newManager = ViewUtils.getDownloadManagerFromDataSource( newDataSource );
	
		if (newManager == manager) {
			tv.setEnabled(manager != null);
			return;
		}

		if (manager != null) {
			manager.removeTPSListener(this);
		}
		
		manager = newManager;

		if (tv.isDisposed()) {
			return;
		}

		tv.removeAllTableRows();
		tv.setEnabled(manager != null);

		if (manager != null) {
			manager.addTPSListener(this);
			addExistingDatasources();
		}
	}
	
	public void 
	tableViewInitialized() 
	{
		if ( manager != null ){

			manager.addTPSListener( this );
			
			addExistingDatasources();
			
				// For this view the tab datasource isn't driven by table row selection so we
				// need to update it with the primary data source
			
	 		TableViewSWT_TabsCommon tabs = tv.getTabsCommon();
	  		
	  		if ( tabs != null ){
	  			
	  			tabs.triggerTabViewsDataSourceChanged( tv );
	  		}
		}
    }

	public void 
	tableViewDestroyed() 
	{
		if ( manager != null ){
			
			manager.removeTPSListener( this );
		}
	}

	private void 
	addExistingDatasources() 
	{
		if ( manager == null || tv.isDisposed()){
			
			return;
		}

		List<TrackerPeerSource> tps = manager.getTrackerPeerSources();
		
		tv.addDataSources( tps.toArray( (new TrackerPeerSource[tps.size()])));
		
		tv.processDataSourceQueueSync();
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
	      	String id = "DMDetails_Sources";
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
	    		SelectedContentManager.clearCurrentlySelectedContent();
	    		break;
	    }
	    
	    return( super.eventOccurred(event));
	}
	
}
