/*
 * Created on 17 juil. 2003
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateAttributeListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.file.FileInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.files.*;

import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;


/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/23: extends TableView instead of IAbstractView
 */
public class FilesView
	extends TableViewTab<DiskManagerFileInfo>
	implements TableDataSourceChangedListener, TableSelectionListener,
	TableViewSWTMenuFillListener, TableRefreshListener, DownloadManagerStateAttributeListener,
	TableLifeCycleListener
{
	boolean refreshing = false;
  private DragSource dragSource = null;

  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new PathItem(),
    new SizeItem(),
    new DoneItem(),
    new PercentItem(),
    new FirstPieceItem(),
    new PieceCountItem(),
    new RemainingPiecesItem(),
    new ProgressGraphItem(),
    new ModeItem(),
    new PriorityItem(),
    new StorageTypeItem(),
    new FileExtensionItem(), 
  };
  
  private DownloadManager manager = null;
  
  public static boolean show_full_path;

  static{
	  COConfigurationManager.addAndFireParameterListener(
			  "FilesView.show.full.path",
			  new ParameterListener()
			  {
				  public void 
				  parameterChanged(
					String parameterName) 
				  {
					  show_full_path = COConfigurationManager.getBooleanParameter( "FilesView.show.full.path" );
				  }
			  });
  }
  
  private MenuItem path_item;

  private TableViewSWT<DiskManagerFileInfo> tv;
	private final boolean allowTabViews;
  

  /**
   * Initialize 
   */
	public FilesView() {
		super("FilesView");
		allowTabViews = true;
	}

	public FilesView(boolean allowTabViews) {
		super("FilesView");
		this.allowTabViews = allowTabViews;
	}

	public TableViewSWT<DiskManagerFileInfo> initYourTableView() {
		tv = new TableViewSWTImpl<DiskManagerFileInfo>(
				org.gudy.azureus2.plugins.disk.DiskManagerFileInfo.class,
				TableManager.TABLE_TORRENT_FILES, getPropertiesPrefix(), basicItems,
				"firstpiece", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		tv.setRowDefaultIconSize(new Point(16, 16));
		if (allowTabViews) {
  		tv.setEnableTabViews(true);
  		tv.setCoreTabViews(new IView[] { new FileInfoView()
  		});
		}

		tv.addTableDataSourceChangedListener(this, true);
		tv.addRefreshListener(this, true);
		tv.addSelectionListener(this, false);
		tv.addMenuFillListener(this);
		tv.addLifeCycleListener(this);

		return tv;
	}

  
  // @see com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener#tableDataSourceChanged(java.lang.Object)
  public void tableDataSourceChanged(Object newDataSource) {
	  DownloadManager old_manager = manager;
		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;
		
		if (old_manager != null) {
			old_manager.getDownloadState().removeListener(this, DownloadManagerState.AT_FILE_LINKS, DownloadManagerStateAttributeListener.WRITTEN);
		}
		if (manager != null) {
			manager.getDownloadState().addListener(this, DownloadManagerState.AT_FILE_LINKS, DownloadManagerStateAttributeListener.WRITTEN);
		}

		tv.removeAllTableRows();
	}
	
	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void selected(TableRowCore[] rows) {
		
		List<DiskManagerFileInfo> ds = tv.getSelectedDataSources();
			
		if ( ds.size() != 1 ){
			
			SelectedContent selected = new SelectedContent( manager );
			
			SelectedContentManager.changeCurrentlySelectedContent( "IconBarEnabler", new ISelectedContent[]{ selected }, null );
			
		}else{
				
			DiskManagerFileInfo info = ds.get(0);
			
			SelectedContent selected = new SelectedContent( info.getDownloadManager(), info.getIndex()); 
				
			SelectedContentManager.changeCurrentlySelectedContent( "IconBarEnabler", new ISelectedContent[]{ selected }, null );
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows, int stateMask) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tv.getFirstSelectedDataSource();
		
		if ( fileInfo == null ){
			return;
		}
		
		AZ3Functions.provider az3 = AZ3Functions.getProvider();
		
		if ( az3 != null ){
			
			DownloadManager dm = fileInfo.getDownloadManager();
			
			if ( az3.canPlay(dm, fileInfo.getIndex()) || (stateMask & SWT.CONTROL) > 0 ){
				
				az3.play( dm, fileInfo.getIndex() );
				
				return;
			}
		}
		
		if ( fileInfo.getAccessMode() == DiskManagerFileInfo.READ ){
			
			Utils.launch(fileInfo);
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWTMenuFillListener#fillMenu(org.eclipse.swt.widgets.Menu)
	public void fillMenu(String sColumnName, final Menu menu) {
		Object[] data_sources = tv.getSelectedDataSources().toArray();
		FilesViewMenuUtil.fillMenu(tv, menu, manager, data_sources);
	}

	
  // @see com.aelitis.azureus.ui.common.table.TableRefreshListener#tableRefresh()
  private boolean force_refresh = false;
  public void tableRefresh() {
  	if (refreshing)
  		return;

  	try {
	  	refreshing = true;
	    if (tv.isDisposed())
	      return;
	
	    DiskManagerFileInfo files[] = getFileInfo();

	    if (files != null && (this.force_refresh || !doAllExist(files))) {
	    	this.force_refresh = false;

	    	List<DiskManagerFileInfo> datasources = tv.getDataSources();
	    	if(datasources.size() == files.length)
	    	{
	    		// check if we actually have to replace anything
	    		ArrayList<DiskManagerFileInfo> toAdd = new ArrayList<DiskManagerFileInfo>(Arrays.asList(files));
		    	ArrayList<DiskManagerFileInfo> toRemove = new ArrayList<DiskManagerFileInfo>();
		    	for(int i = 0;i < datasources.size();i++)
		    	{
		    		DiskManagerFileInfo info = datasources.get(i);
		    		
		    		if(files[info.getIndex()] == info)
		    			toAdd.set(info.getIndex(), null);
		    		else
		    			toRemove.add(info);
		    	}
		    	tv.removeDataSources(toRemove.toArray(new DiskManagerFileInfo[toRemove.size()]));
		    	tv.addDataSources(toAdd.toArray(new DiskManagerFileInfo[toAdd.size()]));
		    	((TableViewSWTImpl)tv).tableInvalidate();
	    	} else
	    	{
		    	tv.removeAllTableRows();
	    		
		    	DiskManagerFileInfo filesCopy[] = new DiskManagerFileInfo[files.length]; 
			    System.arraycopy(files, 0, filesCopy, 0, files.length);

			    tv.addDataSources(filesCopy);
	    	}

		    tv.processDataSourceQueue();
	    }
  	} finally {
  		refreshing = false;
  	}
  }
  
  /**
	 * @param files
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	private boolean doAllExist(DiskManagerFileInfo[] files) {
		for (int i = 0; i < files.length; i++) {
			DiskManagerFileInfo fileinfo = files[i];

			// We can't just use tv.dataSourceExists(), since it does a .equals()
			// comparison, and we want a reference comparison
			TableRowCore row = tv.getRow(fileinfo);
			if (row == null) {
				return false;
			}
			// reference comparison
			if (row.getDataSource(true) != fileinfo) {
				return false;
			}
		}
		return true;
	}

  /* SubMenu for column specific tasks.
   */
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {

    if (sColumnName.equals("path")) {
      path_item = new MenuItem( menuThisColumn, SWT.CHECK );
      
      path_item.setSelection( show_full_path );
      
      Messages.setLanguageText(path_item, "FilesView.fullpath");
      
      path_item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          show_full_path = path_item.getSelection();
          tv.columnInvalidate("path");
          tv.refreshTable(false);
          COConfigurationManager.setParameter( "FilesView.show.full.path", show_full_path );
        }
      });
      
    }
  }
  
  
  private DiskManagerFileInfo[]
  getFileInfo()
  {
  	if (manager == null)
  		return null;
	  return( manager.getDiskManagerFileInfo());
  }
  
  // Used to notify us of when we need to refresh - normally for external changes to the
  // file links.
  public void attributeEventOccurred(DownloadManager dm, String attribute_name, int event_type) {
  	Object oIsChangingLinks = dm.getUserData("is_changing_links");
  	if ((oIsChangingLinks instanceof Boolean) && ((Boolean)oIsChangingLinks).booleanValue()) {
  		return;
  	}
	  this.force_refresh = true;
  }
  
  public void tableViewInitialized() {
    createDragDrop();
  }
  
  public void tableViewDestroyed() {
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					Utils.disposeSWTObjects(new Object[] {
						dragSource,
					});
					dragSource = null;
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		});

  	if (manager != null) {
		  manager.getDownloadState().removeListener(this, DownloadManagerState.AT_FILE_LINKS, DownloadManagerStateAttributeListener.WRITTEN);
	  }
  }


	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseEnter(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseEnter(TableRowCore row) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseExit(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseExit(TableRowCore row) {
	}

	private void createDragDrop() {
		try {

			Transfer[] types = new Transfer[] { TextTransfer.getInstance() };

			if (dragSource != null && !dragSource.isDisposed()) {
				dragSource.dispose();
			}

			dragSource = tv.createDragSource(DND.DROP_MOVE | DND.DROP_COPY);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					private String eventData;

					public void dragStart(DragSourceEvent event) {
						TableRowCore[] rows = tv.getSelectedRows();
						if (rows.length != 0 && manager != null
								&& manager.getTorrent() != null) {
							event.doit = true;
						} else {
							event.doit = false;
							return;
						}

						// Build eventData here because on OSX, selection gets cleared
						// by the time dragSetData occurs
						Object[] selectedDownloads = tv.getSelectedDataSources().toArray();
						eventData = "DiskManagerFileInfo\n";
						TOTorrent torrent = manager.getTorrent();
						for (int i = 0; i < selectedDownloads.length; i++) {
							DiskManagerFileInfo fi = (DiskManagerFileInfo) selectedDownloads[i];
							
							try {
								eventData += torrent.getHashWrapper().toBase32String() + ";"
										+ fi.getIndex() + "\n";
							} catch (Exception e) {
							}
						}
					}

					public void dragSetData(DragSourceEvent event) {
						// System.out.println("DragSetData");
						event.data = eventData;
					}
				});
			}
		} catch (Throwable t) {
			Logger.log(new LogEvent(LogIDs.GUI, "failed to init drag-n-drop", t));
		}
	}
}
