/*
 * Created on 17 juil. 2003
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
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BubbleTextBox;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListenerEx;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventImpl;
import org.gudy.azureus2.ui.swt.views.file.FileInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.files.*;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.AlertsItem;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck.TableViewFilterCheckEx;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;


/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/23: extends TableView instead of IAbstractView
 */
public class FilesView
	extends TableViewTab<DiskManagerFileInfo>
	implements TableDataSourceChangedListener, TableSelectionListener,
	TableViewSWTMenuFillListener, TableRefreshListener, 
	DownloadManagerStateAttributeListener, DownloadManagerListener,
	TableLifeCycleListener, TableViewFilterCheckEx<DiskManagerFileInfo>, KeyListener, ParameterListener,
	UISWTViewCoreEventListenerEx
{
	private static boolean registeredCoreSubViews = false;
	boolean refreshing = false;
  private DragSource dragSource = null;

  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new PathItem(),
    new PathNameItem(),
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
    new FileIndexItem(),
    new TorrentRelativePathItem(),
    new FileCRC32Item(),
    new FileMD5Item(),
    new FileSHA1Item(),
    new FileAvailabilityItem(),
    new AlertsItem(  TableManager.TABLE_TORRENT_FILES ),
    new FileReadSpeedItem(),
    new FileWriteSpeedItem(),
    new FileETAItem(),
    new RelocatedItem(),
  };
  
  static{
	TableColumnManager tcManager = TableColumnManager.getInstance();

	tcManager.setDefaultColumnNames( TableManager.TABLE_TORRENT_FILES, basicItems );
  }
	
  public static final String MSGID_PREFIX = "FilesView";
  
  private List<DownloadManager> managers = new ArrayList<DownloadManager>();
  
  private boolean	enable_tabs = true;
  
  public static boolean show_full_path;
  public boolean hide_dnd_files;

	private volatile long selection_size;
	private volatile long selection_done;

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
	private Composite cTop;
	private BubbleTextBox bubbleTextBox;
	private Button btnShowDND;
	private Label lblHeader;
  
	private boolean	disableTableWhenEmpty	= true;
	

  /**
   * Initialize 
   */
	public FilesView() {
		super(MSGID_PREFIX);
		allowTabViews = true;
	}

	public FilesView(boolean allowTabViews) {
		super(MSGID_PREFIX);
		this.allowTabViews = allowTabViews;
	}

	public boolean
	isCloneable()
	{
		return( true );
	}

	public UISWTViewCoreEventListener
	getClone()
	{
		return( new FilesView( allowTabViews ));
	}

	public TableViewSWT<DiskManagerFileInfo> initYourTableView() {
		tv = TableViewFactory.createTableViewSWT(
				org.gudy.azureus2.plugins.disk.DiskManagerFileInfo.class,
				TableManager.TABLE_TORRENT_FILES, getPropertiesPrefix(), basicItems,
				"firstpiece", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		if (allowTabViews) {
	  		tv.setEnableTabViews(enable_tabs,true,null);
		}
		
  		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
  		if (uiFunctions != null) {
  			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
  			
  			if (pluginUI != null && !registeredCoreSubViews) {

  				DownloadManager manager;
  				
  				if ( managers.size() == 1 ){
  					
  					manager = managers.get(0);
  					
  				}else{
  					
  					manager = null;
  				}
  				
  				pluginUI.addView(TableManager.TABLE_TORRENT_FILES, "FileInfoView",
							FileInfoView.class, manager);

  				registeredCoreSubViews = true;
  			}
  		}

		tv.addTableDataSourceChangedListener(this, true);
		tv.addRefreshListener(this, true);
		tv.addSelectionListener(this, false);
		tv.addMenuFillListener(this);
		tv.addLifeCycleListener(this);
		tv.addKeyListener(this);

		return tv;
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite initComposite(Composite composite) {
		Composite parent = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		parent.setLayout(layout);

		Layout compositeLayout = composite.getLayout();
		if (compositeLayout instanceof GridLayout) {
			parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		} else if (compositeLayout instanceof FormLayout) {
			parent.setLayoutData(Utils.getFilledFormData());
		}
		
		cTop = new Composite(parent, SWT.NONE);
		
		cTop.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		cTop.setLayout(new FormLayout());
		
		btnShowDND = new Button(cTop, SWT.CHECK);
		Messages.setLanguageText(btnShowDND, "FilesView.hide.dnd");
		btnShowDND.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				COConfigurationManager.setParameter("FilesView.hide.dnd", !hide_dnd_files);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
    
		hide_dnd_files = COConfigurationManager.getBooleanParameter("FilesView.hide.dnd");
    
		btnShowDND.setSelection(hide_dnd_files);
		
		lblHeader = new Label(cTop, SWT.CENTER);

		bubbleTextBox = new BubbleTextBox(cTop, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.SINGLE);
		bubbleTextBox.getTextWidget().setMessage(MessageText.getString("TorrentDetailsView.filter"));
		
		FormData fd = Utils.getFilledFormData();
		fd.left = null;
		bubbleTextBox.getParent().setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(bubbleTextBox.getParent(), 10, SWT.CENTER);
		fd.left = new FormAttachment(0, 0);
		btnShowDND.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(bubbleTextBox.getParent(), 10, SWT.CENTER);
		fd.left = new FormAttachment(btnShowDND, 10);
		fd.right = new FormAttachment(bubbleTextBox.getParent(), -10);
		lblHeader.setLayoutData(fd);

		tv.enableFilterCheck(bubbleTextBox.getTextWidget(), this);
		
		Composite tableParent = new Composite(parent, SWT.NONE);
		
		tableParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		tableParent.setLayout(gridLayout);
		
		parent.setTabList(new Control[] {tableParent, cTop});

		return tableParent;
	}

  
  // @see com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener#tableDataSourceChanged(java.lang.Object)
	public void tableDataSourceChanged(Object newDataSource) {
		List<DownloadManager> newManagers = ViewUtils.getDownloadManagersFromDataSource( newDataSource );

		if (newManagers.size() == managers.size()){
			boolean diff = false;
			for (DownloadManager manager: managers ){
				if ( !newManagers.contains( manager )){
					diff = true;
					break;
				}
			}
			if ( !diff ){
				if ( disableTableWhenEmpty ){
					tv.setEnabled(managers.size() > 0 );
				}
				return;
			}
		}

		for (DownloadManager manager: managers ){
			manager.getDownloadState().removeListener(this,
					DownloadManagerState.AT_FILE_LINKS2,
					DownloadManagerStateAttributeListener.WRITTEN);
			manager.removeListener(this);
		}

		managers = newManagers;

		for (DownloadManager manager: managers ){
			manager.getDownloadState().addListener(this,
					DownloadManagerState.AT_FILE_LINKS2,
					DownloadManagerStateAttributeListener.WRITTEN);

			manager.addListener(this);
		}

		if (!tv.isDisposed()) {
			tv.removeAllTableRows();
			if ( disableTableWhenEmpty ){
				tv.setEnabled(managers.size()>0);
			}
			updateHeader();
		}
	}
	
	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
		updateSelectedContent();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void selected(TableRowCore[] rows) {
		updateSelectedContent();
	}

	public void
	stateChanged(
		DownloadManager manager,
		int		state )
	{
	}

	public void
	downloadComplete(DownloadManager manager)
	{
	}

	public void
	completionChanged(DownloadManager manager, boolean bCompleted)
	{
	}

	public void
	positionChanged(DownloadManager download, int oldPosition, int newPosition)
	{
	}

	public void
	filePriorityChanged( DownloadManager download, DiskManagerFileInfo file )
	{
		if ( hide_dnd_files ){
			
			tv.refilter();
		}
	}
  
	public boolean 
	filterCheck(
		DiskManagerFileInfo ds, String filter, boolean regex )
	{
		if ( hide_dnd_files && ds.isSkipped()){
			
			return( false );
		}
		
		if ( filter == null || filter.length() == 0 ){
			
			return( true );
		}

		if ( tv.getFilterControl() == null ){
		
				// view has no visible filter control so ignore any current filter as the
				// user's going to get confused...
			
			return( true );
		}
		
		try {
			File file = ds.getFile(true);

			String name = filter.contains( File.separator )?file.getAbsolutePath():file.getName();
			
			String s = regex ? filter : "\\Q" + filter.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
			
			boolean	match_result = true;
			
			if ( regex && s.startsWith( "!" )){
				
				s = s.substring(1);
				
				match_result = false;
			}
			
			Pattern pattern = RegExUtil.getCachedPattern( "fv:search", s, Pattern.CASE_INSENSITIVE);
  
			return( pattern.matcher(name).find() == match_result );
			
		} catch (Exception e) {
			
			return true;
		}	
	}
	
	public void filterSet(String filter)
	{
		// System.out.println( filter );
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.table.TableViewFilterCheck.TableViewFilterCheckEx#viewChanged(com.aelitis.azureus.ui.common.table.TableView)
	 */
	public void viewChanged(TableView<DiskManagerFileInfo> view) {
		updateHeader();
	}
	
	public void updateSelectedContent() {
		long	total_size 	= 0;
		long	total_done	= 0;
		
		Object[] dataSources = tv.getSelectedDataSources(true);
		List<SelectedContent> listSelected = new ArrayList<SelectedContent>(
				dataSources.length);
		for (Object ds : dataSources) {
			if (ds instanceof DiskManagerFileInfo) {
				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
				listSelected.add(new SelectedContent(fileInfo.getDownloadManager(),
						fileInfo.getIndex()));
				if ( !fileInfo.isSkipped()){
					
					total_size 	+= fileInfo.getLength();
					total_done	+= fileInfo.getDownloaded();
				}
			}
		}
		
		
		
		selection_size	= total_size;
		selection_done	= total_done;

		updateHeader();
		
		SelectedContent[] sc = listSelected.toArray(new SelectedContent[0]);
		SelectedContentManager.changeCurrentlySelectedContent(tv.getTableID(),
				sc, tv);
	}

	
	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows, int stateMask) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tv.getFirstSelectedDataSource();
		
		if ( fileInfo == null ){
			
			return;
		}
					
		boolean webInBrowser = COConfigurationManager.getBooleanParameter( "Library.LaunchWebsiteInBrowser" );
		
		if ( webInBrowser ){
												
			if ( ManagerUtils.browseWebsite( fileInfo )){
				
				return;
			}
		}
				
		String mode = COConfigurationManager.getStringParameter("list.dm.dblclick");
		
		if ( mode.equals("2")){
			
			boolean openMode = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
			
			ManagerUtils.open( fileInfo, openMode) ;
			
		}else if ( mode.equals( "3" ) || mode.equals( "4" )){
	
			
			if ( fileInfo.getAccessMode() == DiskManagerFileInfo.READ ){
				
				if ( 	mode.equals( "4" ) &&
						fileInfo.getDownloaded() == fileInfo.getLength() &&
						Utils.isQuickViewSupported( fileInfo )){
					
					Utils.setQuickViewActive( fileInfo, true );
					
				}else{
				
					Utils.launch(fileInfo);
				}
			}	
		}else if ( mode.equals( "5" )){
			
			ManagerUtils.browse( fileInfo );
			
		}else{
			
			AZ3Functions.provider az3 = AZ3Functions.getProvider();
			
			if ( az3 != null ){
				
				DownloadManager dm = fileInfo.getDownloadManager();
				
				if ( az3.canPlay(dm, fileInfo.getIndex()) || (stateMask & SWT.CONTROL) != 0 ){
					
					az3.play( dm, fileInfo.getIndex() );
					
					return;
				}
			}
			
			if ( fileInfo.getAccessMode() == DiskManagerFileInfo.READ ){
				
				Utils.launch(fileInfo);
			}
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWTMenuFillListener#fillMenu(org.eclipse.swt.widgets.Menu)
	public void fillMenu(String sColumnName, final Menu menu) {

		if ( managers.size() == 0 ){
			
		}else if (managers.size() == 1 ){
			
			Object[] data_sources = tv.getSelectedDataSources().toArray();
			
			DiskManagerFileInfo[] files = new DiskManagerFileInfo[data_sources.length];
			
			for ( int i=0;i<data_sources.length;i++ ){
				files[i] = (DiskManagerFileInfo)data_sources[i];
			}
			
			FilesViewMenuUtil.fillMenu(
				tv, 
				menu, 
				new DownloadManager[]{ managers.get(0) }, 
				new DiskManagerFileInfo[][]{ files });
		}else{
		
			Object[] data_sources = tv.getSelectedDataSources().toArray();
			
			Map<DownloadManager,List<DiskManagerFileInfo>> map = new IdentityHashMap<DownloadManager, List<DiskManagerFileInfo>>();
			
			List<DownloadManager> dms = new ArrayList<DownloadManager>();
			
			for ( Object ds: data_sources ){
				
				DiskManagerFileInfo file = (DiskManagerFileInfo)ds;
				
				DownloadManager dm = file.getDownloadManager();
				
				List<DiskManagerFileInfo> list = map.get(dm);
				
				if ( list == null ){
					
					list = new ArrayList<DiskManagerFileInfo>(dm.getDiskManagerFileInfoSet().nbFiles());
					
					map.put( dm, list );
					
					dms.add( dm );
				}
				
				list.add( file );
			}
			
			DownloadManager[] manager_list = dms.toArray( new DownloadManager[ dms.size()]);
			
			DiskManagerFileInfo[][] files_list = new DiskManagerFileInfo[manager_list.length][];
			
			for ( int i=0;i<manager_list.length;i++){
				
				List<DiskManagerFileInfo> list =  map.get( manager_list[i] );
				
				files_list[i] = list.toArray( new DiskManagerFileInfo[list.size()]);
			}
			
			FilesViewMenuUtil.fillMenu(tv, menu, manager_list, files_list );
		}
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
		    	tv.tableInvalidate();
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

			if ( tv.isFiltered( fileinfo )){
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
      
    }else if (sColumnName.equals("file_eta")) {
        final MenuItem item = new MenuItem(menuThisColumn, SWT.CHECK );
        Messages.setLanguageText(item, "MyTorrentsView.menu.eta.abs");
        item.setSelection( MyTorrentsView.eta_absolute );
                
        item.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
        	  MyTorrentsView.eta_absolute = item.getSelection();
            tv.columnInvalidate("eta");
            tv.refreshTable(false);
            COConfigurationManager.setParameter( "mtv.eta.show_absolute", MyTorrentsView.eta_absolute );
          }
        });
    }else if (sColumnName.equals("priority")) {
        final MenuItem item = new MenuItem(menuThisColumn, SWT.CHECK );
        Messages.setLanguageText(item, "FilesView.hide.dnd");
        item.setSelection( hide_dnd_files );
                
        item.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
        	  hide_dnd_files = item.getSelection();
        	  COConfigurationManager.setParameter( "FilesView.hide.dnd", hide_dnd_files );
          }
        });
    }
  }
  
  
  private DiskManagerFileInfo[]
  getFileInfo()
  {
	  if (managers.size() == 0 ){

		  return null;
		  
	  }else if ( managers.size() == 1 ){
  	
		  return( managers.get(0).getDiskManagerFileInfoSet().getFiles());
		  
	  }else{
		  
		  List<DiskManagerFileInfo>	temp = new ArrayList<DiskManagerFileInfo>();
		  
		  for ( DownloadManager dm: managers ){
			  
			  temp.addAll( Arrays.asList( dm.getDiskManagerFileInfoSet().getFiles()));
		  }
		  
		  return( temp.toArray( new DiskManagerFileInfo[ temp.size()]));
	  }
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
    
    hide_dnd_files = COConfigurationManager.getBooleanParameter("FilesView.hide.dnd");
    COConfigurationManager.addParameterListener("FilesView.hide.dnd", this);
  }
  
  public void tableViewTabInitComplete() {
  	updateSelectedContent();
  	super.tableViewTabInitComplete();
  }
  
  public void tableViewDestroyed() {
  	COConfigurationManager.removeParameterListener("FilesView.hide.dnd", this);
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

  	for ( DownloadManager manager: managers ){
		  manager.getDownloadState().removeListener(this, DownloadManagerState.AT_FILE_LINKS2, DownloadManagerStateAttributeListener.WRITTEN);
		  
		  manager.removeListener( this );
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

			Transfer[] types = new Transfer[] { TextTransfer.getInstance(), FileTransfer.getInstance() };

			if (dragSource != null && !dragSource.isDisposed()) {
				dragSource.dispose();
			}

			dragSource = tv.createDragSource(DND.DROP_COPY | DND.DROP_MOVE);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					private String eventData1;
					private String[] eventData2;

					public void dragStart(DragSourceEvent event) {
						TableRowCore[] rows = tv.getSelectedRows();
						
						if (rows.length != 0 && managers.size() > 0 ) {
							event.doit = true;
						} else {
							event.doit = false;
							return;
						}

						// Build eventData here because on OSX, selection gets cleared
						// by the time dragSetData occurs
						Object[] selectedDownloads = tv.getSelectedDataSources().toArray();
						eventData2 = new String[selectedDownloads.length];
						eventData1 = "DiskManagerFileInfo\n";
						
						for (int i = 0; i < selectedDownloads.length; i++) {
							DiskManagerFileInfo fi = (DiskManagerFileInfo) selectedDownloads[i];
							
							try {
								TOTorrent torrent = fi.getDownloadManager().getTorrent();
								if ( torrent != null ){
									eventData1 += torrent.getHashWrapper().toBase32String() + ";"
											+ fi.getIndex() + "\n";
								}
							} catch (Exception e) {
							}
							try {
								eventData2[i] = fi.getFile(true).getAbsolutePath();
  						} catch (Exception e) {
  						}
						}
					}
					
					

					public void dragSetData(DragSourceEvent event) {
						if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
							event.data = eventData2;
						} else {
							event.data = eventData1;
						}
					}
				});
			}
		} catch (Throwable t) {
			Logger.log(new LogEvent(LogIDs.GUI, "failed to init drag-n-drop", t));
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		if (event.getType() == UISWTViewEvent.TYPE_CREATE){
	    	  if ( event instanceof UISWTViewEventImpl ){
	    		  
	    		  String parent = ((UISWTViewEventImpl)event).getParentID();
	    		  
	    		  enable_tabs = parent != null && parent.equals( UISWTInstance.VIEW_TORRENT_DETAILS );
	    	  }
	    }
		boolean b = super.eventOccurred(event);
		if (event.getType() == UISWTViewEvent.TYPE_FOCUSGAINED) {
	    updateSelectedContent();
		} else if (event.getType() == UISWTViewEvent.TYPE_FOCUSLOST) {
			SelectedContentManager.clearCurrentlySelectedContent();
		}
		return b;
	}

	// @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	public void keyPressed(KeyEvent e) {
		if (e.keyCode == SWT.F2 && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
			FilesViewMenuUtil.rename(tv, tv.getSelectedDataSources(true), true, false);
			e.doit = false;
			return;
		}
	}

	// @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	public void keyReleased(KeyEvent e) {
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	 */
	public void parameterChanged(String parameterName) {
		if ("FilesView.hide.dnd".equals(parameterName)) {
			hide_dnd_files = COConfigurationManager.getBooleanParameter(parameterName);
			if (btnShowDND != null && !btnShowDND.isDisposed()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (btnShowDND != null && !btnShowDND.isDisposed()) {
							btnShowDND.setSelection(hide_dnd_files);
						}
					}
				});
			}
			if (tv == null || tv.isDisposed()) {
				return;
			}
			tv.refilter();
		}
	}

	private void updateHeader() {
		if ( managers.size() == 0 ){
			if (lblHeader != null) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (lblHeader == null || lblHeader.isDisposed()) {
							return;
						}
						lblHeader.setText("");
					}
				});
			}
			return;
		}
		int total = 0;
		
		for ( DownloadManager manager: managers ){
			total += manager.getNumFileInfos();
		}
		
		int numInList = tv.getRowCount();

		String s;
		
		s = MessageText.getString("library.unopened.header"
				+ (total > 1 ? ".p" : ""), new String[] {
					String.valueOf(total)
				});
		if (total != numInList) {
			s = MessageText.getString("v3.MainWindow.xofx", new String[] {
				String.valueOf(numInList),
				s
			});
		}

		s += getSelectionText();
		
		final String sHeader = s;
		
		if (lblHeader != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (lblHeader == null || lblHeader.isDisposed()) {
						return;
					}
					lblHeader.setText(sHeader);
				}
			});
		}
	}

	private String
	getSelectionText()
	{
		int selection_count = tv.getSelectedRowsSize();
		if (selection_count == 0) {
			return "";
		}

		String str = ", " + 
				MessageText.getString(
				"label.num_selected", new String[]{ String.valueOf( selection_count )});
		
		if ( selection_size > 0 ){
			
			if ( selection_size == selection_done ){
				
				str += " (" + DisplayFormatters.formatByteCountToKiBEtc( selection_size ) + ")";
			}else{
				str += " (" + DisplayFormatters.formatByteCountToKiBEtc( selection_done ) + "/" + DisplayFormatters.formatByteCountToKiBEtc( selection_size ) + ")";

			}
		}
		
		return( str );
	}

	public void
	setDisableWhenEmpty(
		boolean	b )
	{
		disableTableWhenEmpty	= b;
	}
}
