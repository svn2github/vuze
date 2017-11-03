/*
 * Created on 30 juin 2003
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
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerEvent;
import org.gudy.azureus2.core3.global.GlobalManagerEventListener;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.components.CompositeMinSize;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.views.piece.PieceInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWT_TabsCommon;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.table.painted.TableRowPainted;
import org.gudy.azureus2.ui.swt.views.utils.CategoryUIUtils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableViewImpl;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;

/** Displays a list of torrents in a table view.
 *
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/18: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 *         2005/Oct/01: Column moving in SWT >= 3.1
 */
public class MyTorrentsView
       extends TableViewTab<DownloadManager>
       implements GlobalManagerListener,
                  ParameterListener,
                  DownloadManagerListener,
                  TagTypeListener,
                  TagListener,
                  KeyListener,
                  TableLifeCycleListener, 
                  TableViewSWTPanelCreator,
                  TableSelectionListener,
                  TableViewSWTMenuFillListener,
                  TableRefreshListener,
                  TableViewFilterCheck.TableViewFilterCheckEx<DownloadManager>,
                  TableRowRefreshListener,
                  TableCountChangeListener,
                  TableExpansionChangeListener,
                  UIPluginViewToolBarListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
	
	public static volatile Set<String>	preferred_tracker_names;
	public static volatile boolean		eta_absolute;
	public static volatile boolean		progress_eta_absolute;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"mtv.trackername.pref.hosts",
				"mtv.eta.show_absolute",
				"mtv.progress_eta.show_absolute",
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					String prefs = COConfigurationManager.getStringParameter( "mtv.trackername.pref.hosts", null );
					
					Set<String>	new_vals = new HashSet<String>();
					
					if ( prefs != null ){
						
						String[] bits = prefs.split( ";" );
						
						for ( String s: bits ){
							
							s = s.trim();
							
							if ( s.length() > 0 ){
								
								new_vals.add( s );
							}
						}
					}
					
					preferred_tracker_names = new_vals;
					
					eta_absolute			= COConfigurationManager.getBooleanParameter( "mtv.eta.show_absolute", false );
					progress_eta_absolute	= COConfigurationManager.getBooleanParameter( "mtv.progress_eta.show_absolute", false );
				}
			});
	}
	
	private AzureusCore		azureus_core;

  private GlobalManager globalManager;
  
  	// keep this listener separate class as there is confusion within the globalmanager
  	// if the same instance is registered as both a GlobalManagerListener and a GlobalManagerEventListener
  	// yes, I know 
  
  private GlobalManagerEventListener gm_event_listener = 
	  new GlobalManagerEventListener()
  	{
		public void 
		eventOccurred(
			GlobalManagerEvent event ) 
		{
			if ( event.getEventType() == GlobalManagerEvent.ET_REQUEST_ATTENTION ){
		
				DownloadManager dm = event.getDownload();
				
				if ( isOurDownloadManager( dm )){
				
					TableRowCore row = tv.getRow( dm );
					
					if ( row != null ){
						
						TableRowCore[] existing = tv.getSelectedRows();
						
						if ( existing != null ){
							
							for ( TableRowCore e: existing ){
							
								if ( e != row ){
								
									e.setSelected( false );
								}
							}
						}
						
						if ( !row.isSelected()){
						
							row.setSelected( true );
						}
						
					}
				}
			}
		}
  	};
  	
  private boolean	supportsTabs;
  private Composite cTablePanel;
  private Font fontButton = null;
  protected Composite cCategoriesAndTags;
  private DragSource dragSource = null;
  private DropTarget dropTarget = null;
  protected Text txtFilter = null;
  private Menu	tableHeaderMenu = null;
  private TimerEventPeriodic	txtFilterUpdateEvent;

  
  private Tag[]		currentTags;
  private List<Tag>	allTags;
  
  // table item index, where the drag has started
  private int drag_drop_line_start = -1;
  private TableRowCore[] drag_drop_rows = null;

	private boolean bDNDalwaysIncomplete;
	private TableViewSWT<DownloadManager> tv;
	private Composite cTableParentPanel;
	protected boolean viewActive;
	private TableSelectionListener defaultSelectedListener;

	private Composite filterParent;

	protected boolean neverShowCatOrTagButtons;
	
	private boolean rebuildListOnFocusGain = false;

	private Menu oldMenu;

	private boolean isCompletedOnly;

	private Class<?> forDataSourceType;

	private SelectionListener buttonSelectionListener;

	private Listener buttonHoverListener;

	private DropTargetListener buttonDropTargetListener;

	protected boolean isEmptyListOnNullDS;

	public MyTorrentsView( boolean supportsTabs ) {
		super("MyTorrentsView");
		this.supportsTabs = supportsTabs;
	}

	public MyTorrentsView(String propertiesPrefix, boolean supportsTabs) {
		super(propertiesPrefix);
		this.supportsTabs = supportsTabs;
	}

  /**
   * Initialize
   * 
   * @param _azureus_core
   * @param isSeedingView
   * @param basicItems
   * @param cCats 
   */
  public 
  MyTorrentsView(
  		AzureusCore			_azureus_core,
  		String				tableID,
  		boolean 			isSeedingView,
  		TableColumnCore[]	basicItems,
  		Text 				txtFilter, 
  		Composite 			cCatsAndTags,
  		boolean				supportsTabs ) 
  {
		super("MyTorrentsView");
		this.txtFilter = txtFilter;
		this.cCategoriesAndTags = cCatsAndTags;
		this.supportsTabs = supportsTabs;
		init(_azureus_core, tableID, isSeedingView
				? DownloadTypeComplete.class : DownloadTypeIncomplete.class, basicItems);
  }
  
  // @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
  public TableViewSWT<DownloadManager> initYourTableView() {
  	return tv;
  }
  
  // @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#tableViewTabInitComplete()
  public void tableViewTabInitComplete() {
  	if (COConfigurationManager.getBooleanParameter("Library.showFancyMenu", true)) {
    	Composite tableComposite = tv.getComposite();
    	oldMenu = tableComposite.getMenu();
    	Menu menu = new Menu(tableComposite);
    	tableComposite.setMenu(menu);
    	menu.addMenuListener(new MenuListener() {
  			
  			public void menuShown(MenuEvent e) {
  				if (!showMyOwnMenu(e)) {
  					oldMenu.setVisible(true);
  				}
  			}
  			
  			public void menuHidden(MenuEvent e) {
  			}
  		});
  	}
  	super.tableViewTabInitComplete();
  }
  
	protected boolean showMyOwnMenu(MenuEvent e) {
		Display d = e.widget.getDisplay();
		if (d == null)
			return false;
		
		Object[] dataSources = tv.getSelectedDataSources(true);
		final DownloadManager[] dms = getSelectedDownloads();

		boolean hasSelection = (dms.length > 0);

		if (!hasSelection) {
			return false;
		}
		Point pt = e.display.getCursorLocation();
		pt = tv.getTableComposite().toControl(pt.x, pt.y);
		TableColumnCore column = tv.getTableColumnByOffset(pt.x);

		boolean isSeedingView = Download.class.equals(forDataSourceType) || DownloadTypeComplete.class.equals(forDataSourceType);
		new TorrentMenuFancy(tv, isSeedingView, getComposite().getShell(), dms,
				tv.getTableID()).showMenu(column, oldMenu);
		return true;
	}

	public void init(AzureusCore _azureus_core, String tableID,
			Class<?> forDataSourceType, TableColumnCore[] basicItems) {

		this.forDataSourceType = forDataSourceType;
		this.isCompletedOnly = forDataSourceType.equals(DownloadTypeComplete.class);
  	
    tv = createTableView(forDataSourceType, tableID, basicItems);
    
    /*
     * 'Big' table has taller row height
     */
    if (getRowDefaultHeight() > 0) {
			tv.setRowDefaultHeightPX(getRowDefaultHeight());
		} else {
	    tv.setRowDefaultHeightEM(1);
		}
    
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    

    if (currentTags == null) {
			currentTags = new Tag[] {
				CategoryManager.getCategory(Category.TYPE_ALL)
			};
    }
    tv.addLifeCycleListener(this);
    tv.setMainPanelCreator(this);
    tv.addSelectionListener(this, false);
    tv.addMenuFillListener(this);
    tv.addRefreshListener(this, false);
    if (tv.canHaveSubItems()) {
    	tv.addRefreshListener(this);
    	tv.addCountChangeListener(this);
    	tv.addExpansionChangeListener(this);
    }
    
    tv.addTableDataSourceChangedListener(new TableDataSourceChangedListener() {
			public void tableDataSourceChanged(Object newDataSource) {
				if (newDataSource instanceof Tag[]) {
					neverShowCatOrTagButtons = true;
					setCurrentTags((Tag[]) newDataSource);
					return;
				}

				if (newDataSource instanceof Object[]) {
					Object[] datasources = ((Object[]) newDataSource);
					Object firstDS = datasources.length > 0 ? datasources[0] : null;
					if (firstDS instanceof Tag) {
						Tag[] tags = new Tag[datasources.length];
						System.arraycopy(datasources, 0, tags, 0, datasources.length);
						setCurrentTags(tags);
						return;
					}
				}
				
				if ( newDataSource instanceof Tag ){
					neverShowCatOrTagButtons = true;
					setCurrentTags(new Tag[] {
						(Tag) newDataSource
					});
				}
				
				if (newDataSource == null && isEmptyListOnNullDS) {
					setCurrentTags(new Tag[] { });
				}
			}
		}, true);

		if (txtFilter != null) {
			filterParent = txtFilter.getParent();
			if (Constants.isWindows) {
				// dirty hack because window's filter box is within a bubble of it's own
				filterParent = filterParent.getParent();
			}
			
			Menu menuFilterHeader = getHeaderMenu(txtFilter);
			filterParent.setMenu( menuFilterHeader );
			Control[] children = filterParent.getChildren();
			for (Control control : children) {
				if (control != txtFilter) {
					control.setMenu(menuFilterHeader);
				}
			}
		}
	}

  // @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
  public void tableViewInitialized() {
  	tv.addKeyListener(this);

    createTabs();

    if (txtFilter == null) {
    	tv.enableFilterCheck(null, this);
    }

    createDragDrop();

    Utils.getOffOfSWTThread(new AERunnable() {
			
			public void runSupport() {
		    COConfigurationManager.addAndFireParameterListeners(new String[] {
					"DND Always In Incomplete",
					"User Mode",
					"Library.ShowCatButtons", "Library.ShowTagButtons", "Library.ShowTagButtons.CompOnly",
				}, MyTorrentsView.this);

		    
		    if ( currentTags != null ){
		    	for (Tag tag : currentTags) {
		    		tag.addTagListener(MyTorrentsView.this, false);
					}
		    }
		    TagManager tagManager = TagManagerFactory.getTagManager();
		    TagType ttManual = tagManager.getTagType(TagType.TT_DOWNLOAD_MANUAL);
		    TagType ttCat = tagManager.getTagType(TagType.TT_DOWNLOAD_CATEGORY);
		    ttManual.addTagTypeListener(MyTorrentsView.this, false);
		    ttCat.addTagTypeListener(MyTorrentsView.this, false);

		    globalManager.addListener(MyTorrentsView.this, false);
		    globalManager.addEventListener( gm_event_listener );
		    DownloadManager[] dms = globalManager.getDownloadManagers().toArray(new DownloadManager[0]);
		    for (int i = 0; i < dms.length; i++) {
					DownloadManager dm = dms[i];
					dm.addListener(MyTorrentsView.this);
					if (!isOurDownloadManager(dm)) {
						dms[i] = null;
					}
				}
		    tv.addDataSources(dms);
		    tv.processDataSourceQueue();
			}
		});
    
    cTablePanel.layout();
  }

  private Menu
  getHeaderMenu(
		Control		control )
  {
	  if ( tableHeaderMenu != null ){
		  
		  return( tableHeaderMenu );
	  }
	  
	  tableHeaderMenu = new Menu(control.getShell(), SWT.POP_UP );
		
	  // show uptime
	  
	  final MenuItem menuItemShowUptime = new MenuItem(tableHeaderMenu, SWT.CHECK);
	  Messages.setLanguageText( menuItemShowUptime, "ConfigView.label.showuptime" );

	  menuItemShowUptime.addSelectionListener(new SelectionAdapter() {
		  public void widgetSelected(SelectionEvent e) {
			  COConfigurationManager.setParameter(
					  "MyTorrentsView.showuptime", menuItemShowUptime.getSelection());
		  }
	  });

	  // selected download rates
	  
	  final MenuItem menuItemShowRates = new MenuItem(tableHeaderMenu, SWT.CHECK);
	  Messages.setLanguageText( menuItemShowRates, "label.show.selected.rates" );

	  menuItemShowRates.addSelectionListener(new SelectionAdapter() {
		  public void widgetSelected(SelectionEvent e) {
			  COConfigurationManager.setParameter(
					  "MyTorrentsView.showrates", menuItemShowRates.getSelection());
		  }
	  });
	  // show category buttons

	  final MenuItem menuItemShowCatBut = new MenuItem(tableHeaderMenu, SWT.CHECK);
	  Messages.setLanguageText( menuItemShowCatBut, "ConfigView.label.show.cat.but" );

	  menuItemShowCatBut.addSelectionListener(new SelectionAdapter() {
		  public void widgetSelected(SelectionEvent e) {
			  COConfigurationManager.setParameter(
					  "Library.ShowCatButtons", menuItemShowCatBut.getSelection());
		  }
	  });


	  // show tag buttons

	  final MenuItem menuItemShowTagBut = new MenuItem(tableHeaderMenu, SWT.CHECK);
	  Messages.setLanguageText( menuItemShowTagBut, "ConfigView.label.show.tag.but" );

	  menuItemShowTagBut.addSelectionListener(new SelectionAdapter() {
		  public void widgetSelected(SelectionEvent e) {
			  COConfigurationManager.setParameter(
					  "Library.ShowTagButtons", menuItemShowTagBut.getSelection());
		  }
	  });

	  new MenuItem( tableHeaderMenu, SWT.SEPARATOR );
	  
	  	// enable simple views

	  String rr = MessageText.getString( "ConfigView.section.security.restart.title" );
	  
	  final MenuItem menuEnableSimple = new MenuItem(tableHeaderMenu, SWT.CHECK);
	  
	  menuEnableSimple.setText( MessageText.getString( "ConfigView.section.style.EnableSimpleView" ) + " (" + rr + ")" );

	  menuEnableSimple.addSelectionListener(new SelectionAdapter() {
		  public void widgetSelected(SelectionEvent e) {
			  COConfigurationManager.setParameter(
					  "Library.EnableSimpleView", menuEnableSimple.getSelection());
		  }
	  });
	  

	  // hooks

	  tableHeaderMenu.addMenuListener(new MenuListener() {
		  public void menuShown(MenuEvent e) {
			  menuItemShowUptime.setSelection(COConfigurationManager.getBooleanParameter( "MyTorrentsView.showuptime" ));
			  menuItemShowRates.setSelection(COConfigurationManager.getBooleanParameter( "MyTorrentsView.showrates" ));
			  menuItemShowCatBut.setSelection(COConfigurationManager.getBooleanParameter( "Library.ShowCatButtons" ));
			  menuItemShowTagBut.setSelection(COConfigurationManager.getBooleanParameter( "Library.ShowTagButtons" ));

			  menuItemShowCatBut.setEnabled( !neverShowCatOrTagButtons );
			  menuItemShowTagBut.setEnabled( !neverShowCatOrTagButtons );
			  
			  menuEnableSimple.setSelection(COConfigurationManager.getBooleanParameter( "Library.EnableSimpleView" ));

		  }

		  public void menuHidden(MenuEvent e) {
		  }
	  });
	  
	  return( tableHeaderMenu );
  }
  
  // @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewDestroyed()
  public void tableViewDestroyed() {
    tv.removeKeyListener(this);
  	
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					Utils.disposeSWTObjects(new Object[] {
						dragSource,
						dropTarget,
						fontButton,
						tableHeaderMenu
					});
					dragSource 		= null;
					dropTarget 		= null;
					fontButton		= null;
					tableHeaderMenu = null;
					
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		});
    Object[] dms = globalManager.getDownloadManagers().toArray();
    for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = (DownloadManager) dms[i];
			dm.removeListener(this);
		}

		if (currentTags != null) {
			for (Tag tag : currentTags) {
				tag.removeTagListener(this);
			}
		}
    TagManager tagManager = TagManagerFactory.getTagManager();
    TagType ttManual = tagManager.getTagType(TagType.TT_DOWNLOAD_MANUAL);
    TagType ttCat = tagManager.getTagType(TagType.TT_DOWNLOAD_CATEGORY);
    ttManual.removeTagTypeListener(MyTorrentsView.this);
    ttCat.removeTagTypeListener(MyTorrentsView.this);

    globalManager.removeListener(this);
    globalManager.removeEventListener( gm_event_listener );
    COConfigurationManager.removeParameterListener("DND Always In Incomplete", this);
    COConfigurationManager.removeParameterListener("Library.ShowCatButtons", this);
    COConfigurationManager.removeParameterListener("Library.ShowTagButtons", this);
    COConfigurationManager.removeParameterListener("Library.ShowTagButtons.CompOnly", this);
  }
  
  
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator#createTableViewPanel(org.eclipse.swt.widgets.Composite)
  public Composite createTableViewPanel(Composite composite) {

    GridData gridData;
    cTableParentPanel = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cTableParentPanel.setLayout(layout);
    if (composite.getLayout() instanceof GridLayout) {
    	cTableParentPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    
    cTablePanel = new Composite(cTableParentPanel, SWT.NULL);

    cTablePanel.addListener(SWT.Activate, new Listener() {
		public void handleEvent(Event event) {
			viewActive = true;
	    updateSelectedContent();
	    //refreshIconBar();
		}
	});
    cTablePanel.addListener(SWT.Deactivate, new Listener() {
		public void handleEvent(Event event) {
			viewActive = false;
			// don't updateSelectedContent() because we may have switched
			// to a button or a text field, and we still want out content to be
			// selected
		}
	});
	
    gridData = new GridData(GridData.FILL_BOTH);
    cTablePanel.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    cTablePanel.setLayout(layout);

    cTablePanel.layout();
    return cTablePanel;
  }
  
  private void createTabs() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_createTabs();
			}
		});
  }
  
  private void swt_createTabs() {
	
    boolean catButtonsDisabled = neverShowCatOrTagButtons;
    if ( !catButtonsDisabled){
    	catButtonsDisabled = !COConfigurationManager.getBooleanParameter( "Library.ShowCatButtons" );
    }
    
    List<Tag> tags_to_show = new ArrayList<Tag>();

    boolean tagButtonsDisabled = neverShowCatOrTagButtons;
    if ( !tagButtonsDisabled){
    	tagButtonsDisabled = !COConfigurationManager.getBooleanParameter( "Library.ShowTagButtons" );
    	
    	if ( !tagButtonsDisabled ){
    		if ( !isCompletedOnly ){
    			tagButtonsDisabled = COConfigurationManager.getBooleanParameter( "Library.ShowTagButtons.CompOnly" );
    		}
    	}
    }
    
    if ( !tagButtonsDisabled ){
			ArrayList<Tag> tagsManual = new ArrayList<Tag>(
					TagManagerFactory.getTagManager().getTagType(
							TagType.TT_DOWNLOAD_MANUAL).getTags());
			for (Tag tag : tagsManual) {
				if (tag.isVisible()) {
					tags_to_show.add(tag);
				}
			}
    }
    
    if (!catButtonsDisabled) {
			ArrayList<Tag> tagsCat = new ArrayList<Tag>(
					TagManagerFactory.getTagManager().getTagType(
							TagType.TT_DOWNLOAD_CATEGORY).getTags());
			for (Tag tag : tagsCat) {
				if (tag.isVisible()) {
					tags_to_show.add(tag);
				}
			}

    }
    
    tags_to_show = TagUIUtils.sortTags( tags_to_show );
    
   	buildHeaderArea();
  	if (cCategoriesAndTags != null && !cCategoriesAndTags.isDisposed()) {
  		Utils.disposeComposite(cCategoriesAndTags, false);
  	}
      	
    if (tags_to_show.size() > 0 ) {
    	buildCatAndTag(tags_to_show);
    } else if (cTableParentPanel != null && !cTableParentPanel.isDisposed()) {
  		cTableParentPanel.layout();
  	}
  }
  
	private void buildHeaderArea() {
		if (cCategoriesAndTags == null) {
			cCategoriesAndTags = new CompositeMinSize(cTableParentPanel, SWT.NONE);
			((CompositeMinSize) cCategoriesAndTags).setMinSize(new Point(SWT.DEFAULT, 24));
			GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
			cCategoriesAndTags.setLayoutData(gridData);
			cCategoriesAndTags.moveAbove(null);
			
			if ( filterParent != null ){
					// inherit the background of the search filter - best that can be done to make things look ok
				Color background = filterParent.getBackground();
				if ( background != null ){
					cCategoriesAndTags.setBackground( background );
					cTableParentPanel.setBackground( background );
				}
			}
			
			cCategoriesAndTags.setBackgroundMode(SWT.INHERIT_FORCE);
		}else if ( cCategoriesAndTags.isDisposed()){
			return;
		}
		
		RowLayout rowLayout;
		
		if (cCategoriesAndTags.getLayout() instanceof RowLayout){
		  rowLayout = (RowLayout)cCategoriesAndTags.getLayout();
		}else{
	      rowLayout = new RowLayout();
	      cCategoriesAndTags.setLayout(rowLayout);
		}
	    rowLayout.marginTop = 0;
	    rowLayout.marginBottom = 0;
	    rowLayout.marginLeft = Utils.adjustPXForDPI(3);
	    rowLayout.marginRight = Utils.adjustPXForDPI(3);
	    rowLayout.spacing = 0;
	    rowLayout.wrap = true;
	     
		
	    Menu menu = getHeaderMenu(cTableParentPanel);
	    cTableParentPanel.setMenu( menu );
	    
	    if ( Constants.isOSX ){
	    	
	    		/* bug on OSX whereby the table is allowing menu-detect events to fire both on the table itself and the composite it
	    		 * sits on - this results in the header-area menu appearing after a menu appears for the table itself
	    		 * Doesn't happen on 10.6.8 but observed to happen on 10.9.4
	    		 */
	    	
		    cTableParentPanel.addListener( 
					SWT.MenuDetect,
					new Listener() {
						
						public void 
						handleEvent(
							Event event ) 
						{
							Display display = cTableParentPanel.getDisplay();
							
							Point pp_rel = display.map( null, cTableParentPanel, event.x, event.y );
														
							Control hit = Utils.findChild(cTableParentPanel, pp_rel.x, pp_rel.y );
							
							event.doit = hit == cTableParentPanel;
						}
					});
	    }
	    
	    tv.enableFilterCheck(txtFilter, this);
	}

  /**
	 * 
	 *
	 * @param categories 
   * @since 3.1.1.1
	 */
	private void buildCatAndTag(List<Tag> tags) {
		
		if (tags.size() == 0 || cCategoriesAndTags.isDisposed()){
			return;
		}
		
		int iFontPixelsHeight = Utils.adjustPXForDPI(10);
		int iFontPointHeight = (iFontPixelsHeight * 72)	/ Utils.getDPIRaw( cCategoriesAndTags.getDisplay()).y;

		Label spacer = null;
		
		int	max_rd_height = 0;
		
		allTags = tags;
		
		if (buttonSelectionListener == null) {
			buttonSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean add = (e.stateMask == SWT.MOD1);

					Button curButton = (Button) e.widget;
					boolean isEnabled = curButton.getSelection();

					Tag tag = (Tag) curButton.getData("Tag");

					if (!isEnabled) {
						removeTagFromCurrent(tag);
					} else {
						if (add) {
							Category catAll = CategoryManager.getCategory(Category.TYPE_ALL);
							
							if (tag.equals(catAll)) {
								setCurrentTags(new Tag[] { catAll });
							} else {
  							Tag[] newTags = new Tag[currentTags.length + 1];
  							System.arraycopy(currentTags, 0, newTags, 0, currentTags.length);
  							newTags[currentTags.length] = tag;
  							
  							newTags = (Tag[]) removeFromArray(newTags, catAll);
  							setCurrentTags(newTags);
							}
						} else {
							setCurrentTags(new Tag[] {
								(Tag) curButton.getData("Tag")
							});
						}
					}

					Control[] controls = curButton.getParent().getChildren();
					for (int i = 0; i < controls.length; i++) {
						if (!(controls[i] instanceof Button)) {
							continue;
						}
						Button b = (Button) controls[i];
						Tag btag = (Tag) b.getData("Tag");
						b.setSelection(isCurrent(btag));
					}
				}
			};

			buttonHoverListener = new Listener() {
				public void handleEvent(Event event) {
					Button curButton = (Button) event.widget;
					Tag tag = (Tag) curButton.getData("Tag");

					if (!(tag instanceof Category)) {
						curButton.setToolTipText(TagUIUtils.getTagTooltip(tag, true));
						return;
					}

					Category category = (Category) tag;

					List<DownloadManager> dms = category.getDownloadManagers(
							globalManager.getDownloadManagers());

					long ttlActive = 0;
					long ttlSize = 0;
					long ttlRSpeed = 0;
					long ttlSSpeed = 0;
					int count = 0;
					for (DownloadManager dm : dms) {

						if (!category.hasTaggable(dm)) {
							continue;
						}

						count++;
						if (dm.getState() == DownloadManager.STATE_DOWNLOADING
								|| dm.getState() == DownloadManager.STATE_SEEDING) {
							ttlActive++;
						}
						DownloadManagerStats stats = dm.getStats();
						ttlSize += stats.getSizeExcludingDND();
						ttlRSpeed += stats.getDataReceiveRate();
						ttlSSpeed += stats.getDataSendRate();
					}

					String up_details = "";
					String down_details = "";

					if (category.getType() != Category.TYPE_ALL) {

						String up_str = MessageText.getString(
								"GeneralView.label.maxuploadspeed");
						String down_str = MessageText.getString(
								"GeneralView.label.maxdownloadspeed");
						String unlimited_str = MessageText.getString(
								"MyTorrentsView.menu.setSpeed.unlimited");

						int up_speed = category.getUploadSpeed();
						int down_speed = category.getDownloadSpeed();

						up_details = up_str + ": " + (up_speed == 0 ? unlimited_str
								: DisplayFormatters.formatByteCountToKiBEtc(up_speed));
						down_details = down_str + ": " + (down_speed == 0 ? unlimited_str
								: DisplayFormatters.formatByteCountToKiBEtc(down_speed));
					}

					if (count == 0) {
						curButton.setToolTipText(
								down_details + "\n" + up_details + "\nTotal: 0");
						return;
					}

					curButton.setToolTipText((up_details.length() == 0 ? ""
							: (down_details + "\n" + up_details + "\n")) + "Total: " + count
							+ "\n" + "Downloading/Seeding: " + ttlActive + "\n" + "\n"
							+ "Total Speed: "
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlRSpeed)
							+ " / "
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlSSpeed)
							+ "\n" + "Average Speed: "
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(
									ttlRSpeed / (ttlActive == 0 ? 1 : ttlActive))
							+ " / "
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(
									ttlSSpeed / (ttlActive == 0 ? 1 : ttlActive))
							+ "\n" + "Size: "
							+ DisplayFormatters.formatByteCountToKiBEtc(ttlSize));

				}
			};

			buttonDropTargetListener = new DropTargetAdapter() {
				public void dragOver(DropTargetEvent e) {

					if (drag_drop_line_start >= 0) {
						boolean doAdd = false;

						Control curButton = ((DropTarget) e.widget).getControl();
						Tag tag = (Tag) curButton.getData("Tag");
						Object[] ds = tv.getSelectedDataSources().toArray();
						if (tag != null) {
  						for (Object obj : ds) {
  
  							if (obj instanceof DownloadManager) {
  
  								DownloadManager dm = (DownloadManager) obj;
  
  								if (!tag.hasTaggable(dm)) {
  									doAdd = true;
  									break;
  								}
  							}
  						}
						}

						e.detail = doAdd ? DND.DROP_COPY : DND.DROP_MOVE;

					} else {
						e.detail = DND.DROP_NONE;
					}
				}

				public void drop(DropTargetEvent e) {
					e.detail = DND.DROP_NONE;

					if (drag_drop_line_start >= 0) {
						drag_drop_line_start = -1;
						drag_drop_rows = null;

						Object[] ds = tv.getSelectedDataSources().toArray();

						Control curButton = ((DropTarget) e.widget).getControl();

						Tag tag = (Tag) curButton.getData("Tag");

						if (tag instanceof Category) {
							TorrentUtil.assignToCategory(ds, (Category) tag);
							return;
						}

						boolean doAdd = false;
						for (Object obj : ds) {

							if (obj instanceof DownloadManager) {

								DownloadManager dm = (DownloadManager) obj;

								if (!tag.hasTaggable(dm)) {
									doAdd = true;
									break;
								}
							}
						}

						for (Object obj : ds) {

							if (obj instanceof DownloadManager) {

								DownloadManager dm = (DownloadManager) obj;
								
								if (doAdd) {
									tag.addTaggable(dm);
								} else {
									tag.removeTaggable(dm);
								}
							}
						}
					}
				}
			};
		}

		for ( final Tag tag: tags ){
			boolean isCat = (tag instanceof Category);
			
			final Button button = new Button(cCategoriesAndTags, SWT.TOGGLE);
			
			if (isCat) {
  			if (spacer == null) {
  				spacer = new Label(cCategoriesAndTags, SWT.NONE);
  				RowData rd = new RowData();
  				rd.width = 8;
  				spacer.setLayoutData(rd);
  				spacer.moveAbove(null);
  			}
  			button.moveAbove(spacer);
			}

			button.addKeyListener(this);
			if ( fontButton == null) {
				Font f = button.getFont();
				FontData fd = f.getFontData()[0];
				fd.setHeight(iFontPointHeight);
				fontButton = new Font(cCategoriesAndTags.getDisplay(), fd);
			}
			button.setText("|");
			button.setFont(fontButton);
			button.pack(true);
			if (button.computeSize(100, SWT.DEFAULT).y > 0) {
				RowData rd = new RowData();
				int rd_height = button.computeSize(100, SWT.DEFAULT).y - 2 + button.getBorderWidth() * 2;
				rd.height = rd_height;
				max_rd_height = Math.max( max_rd_height, rd_height );
				button.setLayoutData(rd);
			}

			String tag_name = tag.getTagName( true );
			
			button.setText(tag_name);
			
			button.setData("Tag", tag);
			if (isCurrent(tag)) {
				button.setSelection(true);
			}

			button.addSelectionListener(buttonSelectionListener);


			button.addListener(SWT.MouseHover, buttonHoverListener);

			final DropTarget tabDropTarget = new DropTarget(button,
					DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			Transfer[] types = new Transfer[] {
				TextTransfer.getInstance()
			};
			tabDropTarget.setTransfer(types);
			tabDropTarget.addDropListener(buttonDropTargetListener);

			button.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (!tabDropTarget.isDisposed()) {
						tabDropTarget.dispose();
					}
				}
			});

			Menu menu = new Menu( button );
			
			button.setMenu( menu );

			if (isCat) {
				CategoryUIUtils.setupCategoryMenu(menu, (Category) tag);
			} else {
				TagUIUtils.createSideBarMenuItems(menu, tag);
			}
		}

		if ( max_rd_height > 0 ){
			RowLayout layout = (RowLayout)cCategoriesAndTags.getLayout();
			int top_margin = ( 24 - max_rd_height + 1 )/2;
			if (top_margin > 0 ){
				layout.marginTop = top_margin;
			}
		}
		
		cCategoriesAndTags.getParent().layout(true, true);
	}
	
	public boolean isOurDownloadManager(DownloadManager dm) {
		if (!isInTags(dm, currentTags)) {
			return false;
		}

		if (Download.class.equals(forDataSourceType)) {
			return true;
		}

		boolean bCompleted = dm.isDownloadComplete(bDNDalwaysIncomplete);
		boolean bOurs = (bCompleted && isCompletedOnly)
				|| (!bCompleted && !isCompletedOnly);

		//System.out.println("ourDM? " + tv.getTableID() + "; " + dm.getDisplayName()
		//		+ "; Complete=" + bCompleted + ";Ours=" + bOurs + ";bc"
		//		+ dm.getStats().getDownloadCompleted(false) + ";"
		//		+ dm.getStats().getDownloadCompleted(true));

		return bOurs;
	}

	public boolean filterCheck(DownloadManager dm, String sLastSearch, boolean bRegexSearch) {
		if ( dm == null ){
			return( false );
		}
		boolean bOurs;
		if (sLastSearch.length() > 0) {
			try {
				String	comment = dm.getDownloadState().getUserComment();
				if ( comment == null ){
					comment = "";
				}
				
				String[][] name_mapping = {
					{
						"",
						dm.getDisplayName()
					},
					{
						"t:",
						"", 	// defer (index = 1)this as costly dm.getTorrent().getAnnounceURL().getHost()
					},
					{
						"st:",
						"" + dm.getState()
					},
					{
						"c:",
						comment
					},
					{
						"f:",
						"",		//defer (index = 4)
					},
				};

				Object o_name = name_mapping[0][1];
				
				String tmpSearch = sLastSearch;

				for ( int i = 1; i < name_mapping.length; i++ ){
					
					if ( tmpSearch.startsWith(name_mapping[i][0])) {
						
						tmpSearch = tmpSearch.substring(name_mapping[i][0].length());
						
						if ( i == 1 ){
							
							List<String> names = new ArrayList<String>();
							
							o_name = names;
							
							TOTorrent t = dm.getTorrent();
								
							if ( t != null ){
								
								names.add( t.getAnnounceURL().getHost());
								
								TOTorrentAnnounceURLSet[] sets = t.getAnnounceURLGroup().getAnnounceURLSets();
								
								for ( TOTorrentAnnounceURLSet set: sets ){
									
									URL[] urls = set.getAnnounceURLs();
									
									for ( URL u: urls ){
										
										names.add( u.getHost());
									}
								}
							
								try{
									byte[] hash = t.getHash();
									
									names.add( ByteFormatter.encodeString( hash ));
									names.add( Base32.encode( hash ));
									
								}catch( Throwable e ){
									
								}
							}
						}else if ( i == 4 ){
							
							List<String> names = new ArrayList<String>();
							
							o_name = names;
							
							DiskManagerFileInfoSet file_set = dm.getDiskManagerFileInfoSet();
							
							DiskManagerFileInfo[] files = file_set.getFiles();
							
							for ( DiskManagerFileInfo f: files ){
								
								File file = f.getFile(true);

								String name = tmpSearch.contains( File.separator )?file.getAbsolutePath():file.getName();
								
								names.add( name );
							}
							
						}else{
							o_name = name_mapping[i][1];
						}
					}
				}

				String s = bRegexSearch ? tmpSearch : "\\Q"
						+ tmpSearch.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
				
				boolean	match_result = true;
				
				if ( bRegexSearch && s.startsWith( "!" )){
					s = s.substring(1);
					
					match_result = false;
				}
				
				Pattern pattern = RegExUtil.getCachedPattern( "tv:search", s, Pattern.CASE_INSENSITIVE);

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
			} catch (Exception e) {
				// Future: report PatternSyntaxException message to user.
				
				bOurs = true;
			}
		}else{
			
			bOurs = true;
		}
		
		return bOurs;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(final String filter) {
		Utils.execSWTThread(new AERunnable() {
						
			public void runSupport() {
				if (txtFilter != null) {
					Object x = filterParent.getData( "ViewUtils:ViewTitleExtraInfo" );
					
					if ( x instanceof ViewUtils.ViewTitleExtraInfo ){
						
						boolean	enabled = filter.length() > 0;
						
						if ( enabled ){
							
							if ( txtFilterUpdateEvent == null ){
								
								txtFilterUpdateEvent = 
									SimpleTimer.addPeriodicEvent(
										"MTV:updater",
										1000,
										new TimerEventPerformer()
										{
											public void 
											perform(
												TimerEvent event )
											{
												Utils.execSWTThread(
													new AERunnable() 
													{
														public void
														runSupport()
														{
															if ( txtFilterUpdateEvent != null ){
																
																if ( tv.isDisposed()){
																	
																	txtFilterUpdateEvent.cancel();
																	
																	txtFilterUpdateEvent = null;
																	
																}else{
																	
																	viewChanged( tv );
																}
															}
														}
													});
											}
										});
							}
						}else{
							
							if ( txtFilterUpdateEvent != null ){
								
								txtFilterUpdateEvent.cancel();
								
								txtFilterUpdateEvent = null;
							}
						}
						
						((ViewUtils.ViewTitleExtraInfo)x).setEnabled( tv.getComposite(), enabled );
					}
				}
			}
		});
	}
	
	public void 
	viewChanged(
		final TableView<DownloadManager> view ) 
	{
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_viewChanged(view);
			}
		});
	}
	
	private void swt_viewChanged(final TableView<DownloadManager> view) {

		if ( filterParent != null && !filterParent.isDisposed()){
			Object x = filterParent.getData( "ViewUtils:ViewTitleExtraInfo" );
		
			if ( x instanceof ViewUtils.ViewTitleExtraInfo ){
				
				TableRowCore[] rows = view.getRows();
				
				int	active = 0;
				
				for ( TableRowCore row: rows ){
					
					DownloadManager dm = (DownloadManager)row.getDataSource( true );
					
					int	state = dm.getState();
					
					if ( state == DownloadManager.STATE_DOWNLOADING || state == DownloadManager.STATE_SEEDING ){
						
						active++;
					}
				}
				
				((ViewUtils.ViewTitleExtraInfo)x).update( tv.getComposite(), rows.length, active );
			}
		}
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void selected(TableRowCore[] rows) {
  	updateSelectedContent();
  	refreshTorrentMenu();
  }

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
  	updateSelectedContent();
  	refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
		updateSelectedContent();
  	refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseEnter(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseEnter(TableRowCore row) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseExit(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseExit(TableRowCore row) {
	}

	private FrequencyLimitedDispatcher refresh_limiter = new FrequencyLimitedDispatcher(
			new AERunnable() {
				public void runSupport() {
					Utils.getOffOfSWTThread(new AERunnable() {
						public void runSupport() {
							updateSelectedContent();
						}
					});
				}
			}, 250 );

	{
		refresh_limiter.setSingleThreaded();
	}
	
	private void
	updateSelectedContentRateLimited()
	{
			// we can get a lot of these in succession when lots of rows are selected and we, for example, right-click or stop the torrents etc
		
		refresh_limiter.dispatch();
	}
	
	public void updateSelectedContent() {
		updateSelectedContent( false );
	}
	
	public void updateSelectedContent( boolean force ) {
		if (cTablePanel == null || cTablePanel.isDisposed()) {
			return;
		}
			// if we're not active then ignore this update as we don't want invisible components
			// updating the toolbar with their invisible selection. Note that unfortunately the 
			// call we get here when activating a view does't yet have focus
		
		if ( !isTableFocus()){
			if ( !force ){
				return;
			}
		}
		Object[] dataSources = tv.getSelectedDataSources(true);
		List<SelectedContent> listSelected = new ArrayList<SelectedContent>(dataSources.length);
		for (Object ds : dataSources) {
			if (ds instanceof DownloadManager) {
				listSelected.add(new SelectedContent((DownloadManager) ds));
			} else if (ds instanceof DiskManagerFileInfo) {
				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
				listSelected.add(new SelectedContent(fileInfo.getDownloadManager(), fileInfo.getIndex()));
			}
		}
		SelectedContent[] content = listSelected.toArray(new SelectedContent[0]);
		SelectedContentManager.changeCurrentlySelectedContent(tv.getTableID(), content, tv);
	}
	
	private void refreshTorrentMenu() {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null && uiFunctions instanceof UIFunctionsSWT) {
			((UIFunctionsSWT)uiFunctions).refreshTorrentMenu();
		}
	}
	
	public DownloadManager[] getSelectedDownloads() {
		Object[] data_sources = tv.getSelectedDataSources().toArray();
		List<DownloadManager> list = new ArrayList<DownloadManager>();
		for (Object ds : data_sources) {
			if (ds instanceof DownloadManager) {
				list.add((DownloadManager) ds);
			}
		}
		return list.toArray(new DownloadManager[0]);
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void defaultSelected(TableRowCore[] rows, int keyMask) {
  	if (defaultSelectedListener != null) {
  		defaultSelectedListener.defaultSelected(rows, keyMask);
  		return;
  	}
  	showSelectedDetails();
	}
  
  private void showSelectedDetails() {
		Object[] dm_sources = tv.getSelectedDataSources().toArray();
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		for (int i = 0; i < dm_sources.length; i++) {
			if (!(dm_sources[i] instanceof DownloadManager)) {
				continue;
			}
			if (uiFunctions != null) {
				uiFunctions.getMDI().showEntryByID(
						MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_DETAILS,
						dm_sources[i]);
			}
		}  	
  }
  
  public void overrideDefaultSelected(TableSelectionListener defaultSelectedListener) {
		this.defaultSelectedListener = defaultSelectedListener;
  }



  /* SubMenu for column specific tasks.
   */
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener#addThisColumnSubMenu(java.lang.String, org.eclipse.swt.widgets.Menu)
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
    if (sColumnName.equals("health")) {
      MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
      Messages.setLanguageText(item, "MyTorrentsView.menu.health");
      Utils.setMenuItemImage(item, "st_explain");
      item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          HealthHelpWindow.show(Display.getDefault());
        }
      });

    }else if (sColumnName.equals("trackername")) {
        MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
        Messages.setLanguageText(item, "MyTorrentsView.menu.trackername.editprefs");
        item.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
      		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
    				"trackername.prefs.title", "trackername.prefs.message");
      		entryWindow.setPreenteredText( COConfigurationManager.getStringParameter( "mtv.trackername.pref.hosts", "" ), true );
      		entryWindow.selectPreenteredText( false );
    		entryWindow.prompt();
    		if (entryWindow.hasSubmittedInput()) {
    			String text = entryWindow.getSubmittedInput();
    			
    			COConfigurationManager.setParameter( "mtv.trackername.pref.hosts", text.trim());
    		}
          }
        });

    }else if (sColumnName.equals("eta") || sColumnName.equals("smootheta")) {
        final MenuItem item = new MenuItem(menuThisColumn, SWT.CHECK );
        Messages.setLanguageText(item, "MyTorrentsView.menu.eta.abs");
        item.setSelection( eta_absolute );
                
        item.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
        	eta_absolute = item.getSelection();
            tv.columnInvalidate("eta");
            tv.refreshTable(false);
            COConfigurationManager.setParameter( "mtv.eta.show_absolute", eta_absolute );
          }
        });
    }else if ( sColumnName.equals( "ProgressETA" )) {
        final MenuItem item = new MenuItem(menuThisColumn, SWT.CHECK );
        Messages.setLanguageText(item, "MyTorrentsView.menu.eta.abs");
        item.setSelection( progress_eta_absolute );
                
        item.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
        	progress_eta_absolute = item.getSelection();
            tv.columnInvalidate("ProgressETA");
            tv.refreshTable(false);
            COConfigurationManager.setParameter( "mtv.progress_eta.show_absolute", progress_eta_absolute );
          }
        });
    }
  }

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener#fillMenu(java.lang.String, org.eclipse.swt.widgets.Menu)
	public void fillMenu(String sColumnName, final Menu menu) {
		Object[] dataSources = tv.getSelectedDataSources(true);
		DownloadManager[] dms = getSelectedDownloads();
		
		if (dms.length == 0 && dataSources.length > 0) {
  		List<DiskManagerFileInfo> listFileInfos = new ArrayList<DiskManagerFileInfo>();
  		DownloadManager firstFileDM = null;
  		for (Object ds : dataSources) {
  			if (ds instanceof DiskManagerFileInfo) {
  				DiskManagerFileInfo info = (DiskManagerFileInfo) ds;
  				// for now, FilesViewMenuUtil.fillmenu can only handle one DM
  				if (firstFileDM != null && !firstFileDM.equals(info.getDownloadManager())) {
  					break;
  				}
  				firstFileDM = info.getDownloadManager();
  				listFileInfos.add(info);
  			}
  		}
  		if (listFileInfos.size() > 0) {
  			FilesViewMenuUtil.fillMenu(
  					tv, 
  					menu, 
  					new DownloadManager[]{ firstFileDM },
  					new DiskManagerFileInfo[][]{ listFileInfos.toArray(new DiskManagerFileInfo[0])});
  			return;
  		}
		}
		
		boolean hasSelection = (dms.length > 0);

		if (hasSelection) {
			boolean isSeedingView = Download.class.equals(forDataSourceType) || DownloadTypeComplete.class.equals(forDataSourceType);
			TorrentUtil.fillTorrentMenu(menu, dms, azureus_core, cTablePanel, true,
					(isSeedingView) ? 2 : 1, tv);

			// ---
			new MenuItem(menu, SWT.SEPARATOR);
		}
	}

	private void createDragDrop() {
		try {

			Transfer[] types = new Transfer[] { TextTransfer.getInstance() };

			if (dragSource != null && !dragSource.isDisposed()) {
				dragSource.dispose();
			}

			if (dropTarget != null && !dropTarget.isDisposed()) {
				dropTarget.dispose();
			}

			dragSource = tv.createDragSource(DND.DROP_MOVE | DND.DROP_COPY);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					private String eventData;

					public void dragStart(DragSourceEvent event) {
						TableRowCore[] rows = tv.getSelectedRows();
						if (rows.length != 0) {
							event.doit = true;
							//System.out.println("DragStart");
							drag_drop_line_start = rows[0].getIndex();
							drag_drop_rows = rows;
						} else {
							event.doit = false;
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}

						// Build eventData here because on OSX, selection gets cleared
						// by the time dragSetData occurs
						boolean onlyDMs = true;
						StringBuilder sb = new StringBuilder();
						Object[] selectedDataSources = tv.getSelectedDataSources(true);
						for (Object ds : selectedDataSources) {
							if (ds instanceof DownloadManager) {
								DownloadManager dm = (DownloadManager) ds;
								TOTorrent torrent = dm.getTorrent();
								if (torrent != null) {
									try {
										sb.append(torrent.getHashWrapper().toBase32String());
										sb.append('\n');
									} catch (TOTorrentException e) {
									}
								}
							} else if (ds instanceof DiskManagerFileInfo) {
								DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
								DownloadManager dm = fileInfo.getDownloadManager();
								TOTorrent torrent = dm.getTorrent();
								if (torrent != null) {
									try {
										sb.append(torrent.getHashWrapper().toBase32String());
										sb.append(';');
										sb.append(fileInfo.getIndex());
										sb.append('\n');
										onlyDMs = false;
									} catch (TOTorrentException e) {
									}
								}
							}
						}
						
						eventData = (onlyDMs ? "DownloadManager\n" : "DiskManagerFileInfo\n") + sb.toString();
					}

					public void dragSetData(DragSourceEvent event) {
						// System.out.println("DragSetData");
						event.data = eventData;
					}
				});
			}

			dropTarget = tv.createDropTarget(DND.DROP_DEFAULT | DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
			if (dropTarget != null) {
				dropTarget.setTransfer(new Transfer[] { HTMLTransfer.getInstance(),
						URLTransfer.getInstance(), FileTransfer.getInstance(),
						TextTransfer.getInstance() });

				dropTarget.addDropListener(new DropTargetAdapter() {
					Point enterPoint = null;
					public void dropAccept(DropTargetEvent event) {
						event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
								event.currentDataType);
					}

					public void dragEnter(DropTargetEvent event) {
						// no event.data on dragOver, use drag_drop_line_start to determine
						// if ours
						if (drag_drop_line_start < 0) {
							if (event.detail != DND.DROP_COPY) {
								if ((event.operations & DND.DROP_LINK) > 0)
									event.detail = DND.DROP_LINK;
								else if ((event.operations & DND.DROP_COPY) > 0)
									event.detail = DND.DROP_COPY;
							}
						} else if (TextTransfer.getInstance().isSupportedType(
								event.currentDataType)) {
							event.detail = tv.getTableRowWithCursor() == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL;
							enterPoint = new Point(event.x, event.y);
						}
					}
					
					// @see org.eclipse.swt.dnd.DropTargetAdapter#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
					public void dragLeave(DropTargetEvent event) {
						super.dragLeave(event);

						tv.getComposite().redraw();
					}

					public void dragOver(DropTargetEvent event) {
						if (drag_drop_line_start >= 0) {
							if (drag_drop_rows.length > 0
									&& !(drag_drop_rows[0].getDataSource(true) instanceof DownloadManager)) {
								event.detail = DND.DROP_NONE;
								return;
							}
							TableRowCore row = tv.getTableRowWithCursor();
							if (row instanceof TableRowPainted) {
								boolean dragging_down = row.getIndex() > drag_drop_line_start;
	  							Rectangle bounds = ((TableRowPainted) row).getBounds();
	  							tv.getComposite().redraw();
	  							tv.getComposite().update();
	  							GC gc = new GC(tv.getComposite());
	  							gc.setLineWidth(2);
	  							int y_pos = bounds.y;
	  							if ( dragging_down ){
	  								y_pos +=bounds.height;
	  							}
	  							gc.drawLine(bounds.x, y_pos, bounds.x + bounds.width, y_pos );
	  							gc.dispose();
							}
							event.detail = row == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL
									| ((enterPoint != null && enterPoint.y > event.y)
											? DND.FEEDBACK_INSERT_BEFORE : DND.FEEDBACK_INSERT_AFTER);
						}
					}

					public void drop(DropTargetEvent event) {
						if (!(event.data instanceof String)) {
							TorrentOpener.openDroppedTorrents(event, true);
							return;
						}
						String data = (String) event.data;
						if (data.startsWith("DiskManagerFileInfo\n")) {
							return;
						}
						if (!data.startsWith("DownloadManager\n")) {
							TorrentOpener.openDroppedTorrents(event, true);
							return;
						}

						event.detail = DND.DROP_NONE;
						// Torrent file from shell dropped
						if (drag_drop_line_start >= 0) { // event.data == null
							event.detail = DND.DROP_NONE;
							TableRowCore row = tv.getRow(event);
							if (row == null)
								return;
							if (row.getParentRowCore() != null) {
								row = row.getParentRowCore();
							}
							int drag_drop_line_end = row.getIndex();
							if (drag_drop_line_end != drag_drop_line_start) {
								DownloadManager dm = (DownloadManager) row.getDataSource(true);
								moveRowsTo(drag_drop_rows, dm.getPosition());
								event.detail = DND.DROP_MOVE;
							}
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}
					}
				});
			}

		} catch (Throwable t) {
			Logger.log(new LogEvent(LOGID, "failed to init drag-n-drop", t));
		}
	}
  
  private void moveRowsTo(TableRowCore[] rows, int iNewPos) {
    if (rows == null || rows.length == 0) {
      return;
    }
    
    TableColumnCore sortColumn = tv.getSortColumn();
    boolean isSortAscending = sortColumn == null ? true
				: sortColumn.isSortAscending();

    for (int i = 0; i < rows.length; i++) {
			TableRowCore row = rows[i];
      Object ds = row.getDataSource(true);
      if (!(ds instanceof DownloadManager)) {
      	continue;
      }
      DownloadManager dm = (DownloadManager) ds;
      int iOldPos = dm.getPosition();
      globalManager.moveTo(dm, iNewPos);
      if (isSortAscending) {
        if (iOldPos > iNewPos)
          iNewPos++;
      } else {
        if (iOldPos < iNewPos)
          iNewPos--;
      }
    }

    boolean bForceSort = sortColumn == null ? false : sortColumn.getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

  // @see com.aelitis.azureus.ui.common.table.TableRefreshListener#tableRefresh()
  public void tableRefresh() {
    if (tv.isDisposed())
      return;
    
    refreshTorrentMenu();
  }


	// @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	public void keyPressed(KeyEvent e) {
		viewActive = true;
		int key = e.character;
		if (key <= 26 && key > 0)
			key += 'a' - 1;

		if (e.stateMask == (SWT.CTRL | SWT.SHIFT)) {
			// CTRL+SHIFT+S stop all Torrents
			if (key == 's') {
				ManagerUtils.asyncStopAll();
				e.doit = false;
				return;
			}

			// Can't capture Ctrl-PGUP/DOWN for moving up/down in chunks
			// (because those keys move through tabs), so use shift-ctrl-up/down
			if (e.keyCode == SWT.ARROW_DOWN) {
				moveSelectedTorrents(10);
				e.doit = false;
				return;
			}

			if (e.keyCode == SWT.ARROW_UP) {
				moveSelectedTorrents(-10);
				e.doit = false;
				return;
			}
		}
		
		if (e.stateMask == SWT.MOD1) {
			switch (key) {
				case 'a': // CTRL+A select all Torrents
					if (e.widget != txtFilter) {
						tv.selectAll();
						e.doit = false;
					}
					break;
				case 'c': // CTRL+C
					if (e.widget != txtFilter) {
						tv.clipboardSelected();
						e.doit = false;
					}
					break;
				case 'i': // CTRL+I Info/Details
					showSelectedDetails();
					e.doit = false;
					break;
			}

			if (!e.doit)
				return;
		}

		if (e.stateMask == SWT.CTRL) {
			switch (e.keyCode) {
				case SWT.ARROW_UP:
					moveSelectedTorrentsUp();
					e.doit = false;
					break;
				case SWT.ARROW_DOWN:
					moveSelectedTorrentsDown();
					e.doit = false;
					break;
				case SWT.HOME:
					moveSelectedTorrentsTop();
					e.doit = false;
					break;
				case SWT.END:
					moveSelectedTorrentsEnd();
					e.doit = false;
					break;
			}
			if (!e.doit)
				return;

			switch (key) {
				case 'r': // CTRL+R resume/start selected Torrents
					TorrentUtil.resumeTorrents(tv.getSelectedDataSources().toArray());
					e.doit = false;
					break;
				case 's': // CTRL+S stop selected Torrents
					Utils.getOffOfSWTThread(new AERunnable() {
						public void runSupport() {
							TorrentUtil.stopDataSources(tv.getSelectedDataSources().toArray());
						}
					});
					e.doit = false;
					break;
			}

			if (!e.doit)
				return;
		}
		
		if (e.keyCode == SWT.F2 && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
			FilesViewMenuUtil.rename(tv, tv.getSelectedDataSources(true), true, false);
			e.doit = false;
			return;
		}

		
		// DEL remove selected Torrents
		if (e.stateMask == 0 && e.keyCode == SWT.DEL && e.widget != txtFilter) {
			Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
					TorrentUtil.removeDataSources(tv.getSelectedDataSources().toArray());
				}
			});
			e.doit = false;
			return;
		}

		if (e.keyCode != SWT.BS) {
			if ((e.stateMask & (~SWT.SHIFT)) != 0 || e.character < 32)
				return;
		}
	}

	public void keyReleased(KeyEvent e) {
		// ignore
	}





  private void moveSelectedTorrentsDown() {
    // Don't use runForSelectDataSources to ensure the order we want
  	DownloadManager[] dms = getSelectedDownloads();
    Arrays.sort(dms, new Comparator<DownloadManager>() {
			public int compare(DownloadManager a, DownloadManager b) {
        return a.getPosition() - b.getPosition();
			}
    });
    for (int i = dms.length - 1; i >= 0; i--) {
      DownloadManager dm = dms[i];
      if (dm.getGlobalManager().isMoveableDown(dm)) {
        dm.getGlobalManager().moveDown(dm);
      }
    }

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

  private void moveSelectedTorrentsUp() {
    // Don't use runForSelectDataSources to ensure the order we want
  	DownloadManager[] dms = getSelectedDownloads();
    Arrays.sort(dms, new Comparator<DownloadManager>() {
    	public int compare(DownloadManager a, DownloadManager b) {
        return a.getPosition() - b.getPosition();
      }
    });
    for (int i = 0; i < dms.length; i++) {
      DownloadManager dm = dms[i];
      if (dm.getGlobalManager().isMoveableUp(dm)) {
        dm.getGlobalManager().moveUp(dm);
      }
    }

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

	private void moveSelectedTorrents(int by) {
		// Don't use runForSelectDataSources to ensure the order we want
  	DownloadManager[] dms = getSelectedDownloads();
		if (dms.length <= 0)
			return;

		int[] newPositions = new int[dms.length];

		if (by < 0) {
			Arrays.sort(dms, new Comparator<DownloadManager>() {
				public int compare(DownloadManager a, DownloadManager b) {
					return a.getPosition() - b.getPosition();
				}
			});
		} else {
			Arrays.sort(dms, new Comparator<DownloadManager>() {
				public int compare(DownloadManager a, DownloadManager b) {
					return b.getPosition() - a.getPosition();
				}
			});
		}

		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
			boolean complete = dm.isDownloadComplete(false);
			int count = globalManager.downloadManagerCount(complete); 
			int pos = dm.getPosition() + by;
			if (pos < i + 1)
				pos = i + 1;
			else if (pos > count - i)
				pos = count - i;

			newPositions[i] = pos;
		}

		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
			globalManager.moveTo(dm, newPositions[i]);
		}

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
	}

  private void moveSelectedTorrentsTop() {
    moveSelectedTorrentsTopOrEnd(true);
  }

  private void moveSelectedTorrentsEnd() {
    moveSelectedTorrentsTopOrEnd(false);
  }

  private void moveSelectedTorrentsTopOrEnd(boolean moveToTop) {
  	DownloadManager[] dms = getSelectedDownloads();
    if (dms.length == 0)
      return;

    if(moveToTop)
      globalManager.moveTop(dms);
    else
      globalManager.moveEnd(dms);

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    if (bForceSort) {
    	tv.columnInvalidate("#");
    	tv.refreshTable(bForceSort);
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
		if (parameterName == null || parameterName.equals("DND Always In Incomplete")) {
			bDNDalwaysIncomplete = COConfigurationManager.getBooleanParameter("DND Always In Incomplete");
		}
		
		if (parameterName != null && 
				( 	parameterName.equals("Library.ShowCatButtons") || 
					parameterName.equals("Library.ShowTagButtons" ) ||
					parameterName.equals("Library.ShowTagButtons.CompOnly" ))){
			
			createTabs();
		}
	}

	private MdiEntrySWT getActiveView() {
		TableViewSWT_TabsCommon tabsCommon = tv.getTabsCommon();
		if (tabsCommon != null) {
			return tabsCommon.getActiveSubView();
		}
		return null;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		ISelectedContent[] datasource = SelectedContentManager.getCurrentlySelectedContent();

		if (!isTableFocus()) {
  		UISWTViewCore active_view = getActiveView();
  		if (active_view != null) {
  			UIPluginViewToolBarListener l = active_view.getToolBarListener();
  			if (l != null) {
  				Map<String, Long> activeViewList = new HashMap<String, Long>();
  				l.refreshToolBarItems(activeViewList);
  				if (activeViewList.size() > 0) {
  					list.putAll(activeViewList);
  					return;
  				}
  			}
  		}
		}
  }  

  public boolean toolBarItemActivated(ToolBarItem item, long activationType, Object datasource) {
	  boolean isTableSelected = false;
	  if (tv instanceof TableViewImpl) {
	  	isTableSelected = ((TableViewImpl) tv).isTableSelected();
	  }
	  if (!isTableSelected) {
  		UISWTViewCore active_view = getActiveView();
  		if (active_view != null) {
  			UIPluginViewToolBarListener l = active_view.getToolBarListener();
  			if (l != null && l.toolBarItemActivated(item, activationType, datasource)) {
  				return true;
  			}
  		}
  		return false;
	  }

		String itemKey = item.getID();
  	if (activationType == UIToolBarActivationListener.ACTIVATIONTYPE_HELD) {
      if(itemKey.equals("up")) {
        moveSelectedTorrentsTop();
        return true;
      }
      if(itemKey.equals("down")){
        moveSelectedTorrentsEnd();
        return true;
      }
      return false;
  	}

  	if (activationType != UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL) {
  		return false;
  	}
    if(itemKey.equals("top")) {
      moveSelectedTorrentsTop();
      return true;
    }
    if(itemKey.equals("bottom")){
      moveSelectedTorrentsEnd();
      return true;
    }
    if(itemKey.equals("up")) {
      moveSelectedTorrentsUp();
      return true;
    }
    if(itemKey.equals("down")){
      moveSelectedTorrentsDown();
      return true;
    }
    if(itemKey.equals("run")){
      TorrentUtil.runDataSources(tv.getSelectedDataSources().toArray());
      return true;
    }
    if(itemKey.equals("start")){
      TorrentUtil.queueDataSources(tv.getSelectedDataSources().toArray(), true);
      return true;
    }
    if(itemKey.equals("stop")){
      TorrentUtil.stopDataSources(tv.getSelectedDataSources().toArray());
      return true;
    }
    if (itemKey.equals("startstop")) {
    	TorrentUtil.stopOrStartDataSources(tv.getSelectedDataSources().toArray());
    	return true;
    }
    if(itemKey.equals("remove")){
      TorrentUtil.removeDataSources(tv.getSelectedDataSources().toArray());
      return true;
    }
    return false;
  }
  
  // DownloadManagerListener Functions
  public void stateChanged(DownloadManager manager, int state) {
    final TableRowCore row = tv.getRow(manager);
    if (row != null) {
    	Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
		    	row.refresh(true);
		    	if (row.isSelected()) {
		    		updateSelectedContentRateLimited();
		    	}
				}
    	});
    }
  }

  // DownloadManagerListener
  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  	if (isOurDownloadManager(download)) {
    	Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					updateSelectedContent();
				}
    	});
  	}
  }
  
  // DownloadManagerListener
  public void filePriorityChanged(DownloadManager download,
			DiskManagerFileInfo file) {
	}

  // DownloadManagerListener
	public void completionChanged(DownloadManager manager, boolean bCompleted) {
		// manager has moved lists

		if (isOurDownloadManager(manager)) {
			tv.addDataSource(manager);
		} else {

			tv.removeDataSource(manager);
		}
	}

  // DownloadManagerListener
  public void downloadComplete(DownloadManager manager) {
  }


	@Override
	public void tagEventOccurred(TagEvent event ) {
		int	type = event.getEventType();
		Tag	tag = event.getTag();
		if ( type == TagEvent.ET_TAG_ADDED ){
			tagAdded( tag );
		}else if ( type == TagEvent.ET_TAG_CHANGED ){
			tagChanged( tag );
		}else if ( type == TagEvent.ET_TAG_REMOVED ){
			tagRemoved( tag );
		}
	}
	
	@Override
	public void tagTypeChanged(TagType tag_type) {		
	}
	
	public void
	tagAdded(
		Tag			tag )
	{
		createTabs();
	}
	
	private Set<Tag> pending_tag_changes = new HashSet<Tag>();

	private boolean currentTagsAny = true;
	
	public void
	tagChanged(
		Tag			tag )
	{	
			// we can get a lot of hits here, limit tab rebuilds somewhat
		
		synchronized( pending_tag_changes ){
			
			pending_tag_changes.add( tag );
		}
		
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
			
				if ( allTags != null ){
						
					boolean create_tabs = false;
					
					synchronized( pending_tag_changes ){
						
						for ( Tag t: pending_tag_changes ){
							
							boolean should_be_visible	= t.isVisible();
							boolean is_visible			= allTags.contains( t );
							
							if ( should_be_visible != is_visible ){
								
								create_tabs = true;
								
								break;
							}
						}
						
						pending_tag_changes.clear();
					}
					
					if ( create_tabs ){
						
						createTabs();
					}
				}
			}
		});
	}
	
	public void
	tagRemoved(
		Tag			tag )
	{
		if (currentTags == null) {
			return;
		}

		removeTagFromCurrent(tag);
		createTabs();
	}
	
  
	private void removeTagFromCurrent(Tag tag) {
		boolean found = false;
		for (int i = 0; i < currentTags.length; i++) {
			Tag curTag = currentTags[i];
			if (curTag.equals(tag)) {
				Tag[] tags;
				if (currentTags.length == 1) {
					tags = new Tag[] {
						CategoryManager.getCategory(Category.TYPE_ALL)
					};
				} else {
  				tags = new Tag[currentTags.length - 1];
  				if (i > 0) {
  					System.arraycopy(currentTags, 0, tags, 0, i);
  				}
  				if (tags.length - i > 0) {
  					System.arraycopy(currentTags, i + 1, tags, 0, tags.length - i);
  				}
				}

				setCurrentTags(tags);
				found = true;
				break;
			}
		}
		
		if (!found) {
			// always activate as deletion of this one might have
			// affected the current view 
			setCurrentTags(currentTags);
		}
	}
	
	private Object[] removeFromArray(Object[] array, Object o) {
		for (int i = 0; i < array.length; i++) {
			Object cur = array[i];
			if (cur.equals(o)) {
				Tag[] newArray = new Tag[array.length - 1];
				if (i > 0) {
					System.arraycopy(array, 0, newArray, 0, i);
				}
				if (newArray.length - i > 0) {
					System.arraycopy(array, i + 1, newArray, 0, newArray.length - i);
				}

				return newArray;
			}
		}
		
		return array;
	}

			// tags 
	
	public Tag[] getCurrentTags() {
		return currentTags;
	}

	protected void setCurrentTags(Tag[] tags) {
		if (currentTags != null) {
			for (Tag tag : currentTags) {
				tag.removeTagListener(this);
			}
		}

		currentTags = tags;
		if (currentTags != null) {
			Set<Tag> to_remove = null;
			for (Tag tag : currentTags) {
				if ( tag.getTaggableTypes() != Taggable.TT_DOWNLOAD ){
						// hmm, not a download related tag (e.g. peer-set), remove from the set. We can get this in the
						// TagsOverview 'torrents' sub-view when peer-sets are selected in the main tag table
					if (  to_remove == null ){
						to_remove = new HashSet<Tag>();
					}
					to_remove.add( tag );
				}else{
					tag.addTagListener(this, false);
				}
			}
			if ( to_remove != null ){
				Tag[] updated_tags = new Tag[currentTags.length-to_remove.size()];
				
				int	pos = 0;
				for (Tag tag : currentTags) {
					if ( !to_remove.contains( tag )){
						updated_tags[pos++] = tag;
					}
				}
				currentTags = updated_tags;
			}
		}
  	
  		tv.processDataSourceQueue();
  		Object[] managers = globalManager.getDownloadManagers().toArray();
  		List<DownloadManager> listRemoves = new ArrayList<DownloadManager>();
  		List<DownloadManager> listAdds = new ArrayList<DownloadManager>();

  		for (int i = 0; i < managers.length; i++) {
  			DownloadManager dm = (DownloadManager) managers[i];

  			boolean bHave = tv.isUnfilteredDataSourceAdded(dm);
  			if (!isOurDownloadManager(dm)) {
  				if (bHave) {
  					listRemoves.add(dm);
  				}
  			} else {
  				if (!bHave) {
  					listAdds.add(dm);
  				}
  			}
  		}
  		tv.removeDataSources(listRemoves.toArray(new DownloadManager[0]));
  		tv.addDataSources(listAdds.toArray(new DownloadManager[0]));

  		tv.processDataSourceQueue();
  		//tv.refreshTable(false);
  	}
  
  	private boolean 
  	isInTags(
  		DownloadManager		manager, 
  		Tag[] 				tags ) 
  	{
  		if ( tags == null ){
  			return true;
  		}
  		
  		if (currentTagsAny) {
    		for (Tag tag : tags) {
  				if (tag.hasTaggable(manager)) {
  					return true;
  				}
  			}
    		return false;
  		} else {
    		for (Tag tag : tags) {
  				if (!tag.hasTaggable(manager)) {
  					return false;
  				}
  			}
    		return true;
  		}
  	}
  	
  	public boolean 
  	isInCurrentTag(
  		DownloadManager 	manager ) 
  	{
  		return( isInTags(manager, currentTags ));
  	}

	public void
	taggableAdded(
		Tag			tag,
		Taggable	tagged )
	{
		DownloadManager	manager = (DownloadManager)tagged;
		
	 	if ( isOurDownloadManager(manager)){
	 		
	 		tv.addDataSource( manager );
	    }
	}
	
	public void 
	taggableSync(
		Tag 		tag ) 
	{
		// request to fully resync this tag
		
		List<DownloadManager> dataSources = tv.getDataSources();
		
		for ( DownloadManager dm : dataSources ){
			
			if ( !isOurDownloadManager(dm)){
				
				tv.removeDataSource(dm);
			}
		}

		for ( Taggable t: tag.getTagged()){
			
			DownloadManager	manager = (DownloadManager)t;
			
			if ( isOurDownloadManager( manager ) && !tv.dataSourceExists(manager)){
				
				tv.addDataSource(manager);
			}
		}
	}
	
	public void
	taggableRemoved(
		Tag			tag,
		Taggable	tagged )
	{
		DownloadManager	manager = (DownloadManager)tagged;

		tv.removeDataSource( manager );
	}
  
  
  
  // globalmanagerlistener Functions
  // @see org.gudy.azureus2.core3.global.GlobalManagerListener#downloadManagerAdded(org.gudy.azureus2.core3.download.DownloadManager)
  public void downloadManagerAdded( DownloadManager dm ) {
    dm.addListener( this );
  	if (isOurDownloadManager(dm)) {
      tv.addDataSource(dm);
    }
  }

  // @see org.gudy.azureus2.core3.global.GlobalManagerListener#downloadManagerRemoved(org.gudy.azureus2.core3.download.DownloadManager)
  public void downloadManagerRemoved( DownloadManager dm ) {
    dm.removeListener( this );
    DownloadBar.close(dm);
    tv.removeDataSource(dm);
  }

  public void destroyInitiated() {  }
  public void destroyed() { }
  public void seedingStatusChanged( boolean seeding_only_mode, boolean b ){}       

  // End of globalmanagerlistener Functions
  


	// @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#updateLanguage()
	public void updateLanguage() {
		super.updateLanguage();
		getComposite().layout(true, true);
	}

	public boolean isTableFocus() {
		return viewActive;
		//return tv.isTableFocus();
	}
	
	public Image obfusticatedImage(final Image image) {
		return tv.obfusticatedImage(image);
	}
	
	/**
	 * Creates and return an <code>TableViewSWT</code>
	 * Subclasses my override to return a different TableViewSWT if needed
	 * @param basicItems
	 * @return
	 */
	
	private static boolean registeredCoreSubViews = false;
	
	protected TableViewSWT<DownloadManager>
	createTableView(
		Class<?> 			forDataSourceType, 
		String 				tableID, 
		TableColumnCore[] 	basicItems )
	{
		int tableExtraStyle = COConfigurationManager.getIntParameter("MyTorrentsView.table.style");
		TableViewSWT<DownloadManager> table = 
			TableViewFactory.createTableViewSWT(forDataSourceType, tableID,
				getPropertiesPrefix(), basicItems, "#", tableExtraStyle | SWT.MULTI
						| SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.CASCADE);
		
			// config??
		
		boolean	enable_tab_views = 
			!Utils.isAZ2UI() &&
			supportsTabs &&
			COConfigurationManager.getBooleanParameter( "Library.ShowTabsInTorrentView" );

		List<String> restrictTo = new ArrayList<String>();
		restrictTo.addAll(Arrays.asList(
			GeneralView.MSGID_PREFIX,
			TrackerView.MSGID_PREFIX,
			PeersView.MSGID_PREFIX,
			PeersGraphicView.MSGID_PREFIX,
			PiecesView.MSGID_PREFIX,
			DownloadActivityView.MSGID_PREFIX,
			PieceInfoView.MSGID_PREFIX,
			FilesView.MSGID_PREFIX,
			TaggingView.MSGID_PREFIX,
			PrivacyView.MSGID_PREFIX
		));
				
		// sub-tab hacks
		restrictTo.add( "azbuddy.ui.menu.chat" );
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getPluginInterfaceByID("aercm", true);

		if (pi != null) {
			String pluginInfo = pi.getPluginconfig().getPluginStringParameter(
					"plugin.info", "");
			if (pluginInfo.equals("e")) {
				restrictTo.add("rcm.subview.torrentdetails.name");
			}
		}

		if ( Logger.isEnabled()){
			
			restrictTo.add( LoggerView.MSGID_PREFIX );
		}

		table.setEnableTabViews(enable_tab_views, false,
				restrictTo.toArray(new String[0]));
		
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
			
			registerPluginViews( pluginUI );
		}
		
		return( table );
	}

	public static void
	registerPluginViews(
		UISWTInstance pluginUI )
	{
		if ( pluginUI != null && !registeredCoreSubViews ){
			
			String[] views_with_tabs = { 
					TableManager.TABLE_MYTORRENTS_ALL_BIG,			// all simple views
					TableManager.TABLE_MYTORRENTS_INCOMPLETE,		// downloading view
					TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,	// downloading view
					TableManager.TABLE_MYTORRENTS_COMPLETE,			// bottom part of split views (hack of course)
			};
			
			boolean hasTags = false;
			try {
				// gotta be a simpler way?
				hasTags = TagManagerFactory.getTagManager().getTagType(TagType.TT_DOWNLOAD_MANUAL).getTags().size() > 0;
			} catch (Throwable t) {
			}

			for ( String id: views_with_tabs ){

				pluginUI.addView( id, GeneralView.MSGID_PREFIX, GeneralView.class, null);
				pluginUI.addView( id, TrackerView.MSGID_PREFIX, TrackerView.class, null);
				pluginUI.addView( id, PeersView.MSGID_PREFIX,	PeersView.class, null);
				pluginUI.addView( id, PeersGraphicView.MSGID_PREFIX, PeersGraphicView.class, null);
				pluginUI.addView( id, PiecesView.MSGID_PREFIX, PiecesView.class, null);
				pluginUI.addView( id, PieceInfoView.MSGID_PREFIX, PieceInfoView.class, null);
				pluginUI.addView( id, DownloadActivityView.MSGID_PREFIX, DownloadActivityView.class, null);
				pluginUI.addView( id, FilesView.MSGID_PREFIX,	FilesView.class, null);
				pluginUI.addView( id, TorrentInfoView.MSGID_PREFIX, TorrentInfoView.class, null);
				pluginUI.addView( id, TorrentOptionsView.MSGID_PREFIX, TorrentOptionsView.class, null);
				if (hasTags) {
					pluginUI.addView( id, TaggingView.MSGID_PREFIX, TaggingView.class, null);
				}
				pluginUI.addView( id, PrivacyView.MSGID_PREFIX, PrivacyView.class, null);

				if (Logger.isEnabled()) {
					pluginUI.addView( id, LoggerView.MSGID_PREFIX, LoggerView.class, null);
				}
			}
			
			registeredCoreSubViews = true;
		}
	}
	/**
	 * Returns the default row height for the table
	 * Subclasses my override to return a different height if needed; a height of -1 means use default
	 * @return
	 */
	protected int getRowDefaultHeight(){
		return -1;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener#rowRefresh(org.gudy.azureus2.plugins.ui.tables.TableRow)
	public void rowRefresh(TableRow row) {
		if (!(row instanceof TableRowCore)) {
			return;
		}

		TableRowCore rowCore = (TableRowCore) row;
		Object ds = rowCore.getDataSource(true);
		if (!(ds instanceof DownloadManager)) {
			return;
		}

		DownloadManager dm = (DownloadManager) ds;
		if (rowCore.getSubItemCount() == 0 && dm.getTorrent() != null
				&& !dm.getTorrent().isSimpleTorrent() && rowCore.isVisible()
				&& dm.getNumFileInfos() > 0) {
			DiskManagerFileInfoSet fileInfos = dm.getDiskManagerFileInfoSet();
			if (fileInfos != null) {
				DiskManagerFileInfo[] files = fileInfos.getFiles();
				boolean copied = false;
				int pos = 0;
				for (int i = 0; i < files.length; i++) {
					DiskManagerFileInfo fileInfo = files[i];
					if (fileInfo.isSkipped()
							&& (fileInfo.getStorageType() == DiskManagerFileInfo.ST_COMPACT || fileInfo.getStorageType() == DiskManagerFileInfo.ST_REORDER_COMPACT)) {
						continue;
					}
					if (pos != i) {
						if ( !copied ){
								// we *MUSTN'T* modify the returned array!!!!
							
							DiskManagerFileInfo[] oldFiles = files;
							files = new DiskManagerFileInfo[files.length];
							System.arraycopy(oldFiles, 0, files, 0, files.length);
							
							copied = true;
						}
						
						files[pos] = files[i];
					}
					pos++;
				}
				if (pos != files.length) {
					DiskManagerFileInfo[] oldFiles = files;
					files = new DiskManagerFileInfo[pos];
					System.arraycopy(oldFiles, 0, files, 0, pos);
				}
				rowCore.setSubItems(files);
			}
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		boolean b = super.eventOccurred(event);
		if (event.getType() == UISWTViewEvent.TYPE_FOCUSGAINED) {
			if (rebuildListOnFocusGain) {
  			List<?> dms = globalManager.getDownloadManagers();
  			List<DownloadManager> listAdds = new ArrayList<DownloadManager>();
  			List<DownloadManager> listRemoves = new ArrayList<DownloadManager>();
  			for (Iterator<?> iter = dms.iterator(); iter.hasNext();) {
  				DownloadManager dm = (DownloadManager) iter.next();
  
  				if (!isOurDownloadManager(dm)) {
  					listRemoves.add(dm);
  				} else {
  					listAdds.add(dm);
  				}
  			}
  			tv.removeDataSources(listRemoves.toArray(new DownloadManager[0]));
  			tv.addDataSources(listAdds.toArray(new DownloadManager[0]));
			}
	    updateSelectedContent(true);
		} else if (event.getType() == UISWTViewEvent.TYPE_FOCUSLOST) {
		}
		return b;
	}

	public void setRebuildListOnFocusGain(boolean rebuildListOnFocusGain) {
		this.rebuildListOnFocusGain = rebuildListOnFocusGain;
	}

	public void rowAdded(TableRowCore row) {
		if (row.getParentRowCore() == null) {
			DownloadManager dm = (DownloadManager) row.getDataSource(true);
			if ( dm.getDownloadState().getBooleanAttribute( DownloadManagerState.AT_FILES_EXPANDED )){
				row.setExpanded(true);
			}
		}
		//if (getRowDefaultHeight() > 0 && row.getParentRowCore() != null) {
		//	row.setHeight(20);
		//}
	}

	public void rowRemoved(TableRowCore row) {
	}
	
	public void 
	rowExpanded(
		TableRowCore 	row )
	{
		if ( row.getParentRowCore() == null ){
			
			DownloadManager dm = (DownloadManager) row.getDataSource(true);
				
			dm.getDownloadState().setBooleanAttribute( DownloadManagerState.AT_FILES_EXPANDED, true );
		}
	}
	
	public void 
	rowCollapsed(
		TableRowCore 	row )
	{
		if ( row.getParentRowCore() == null ){
			
			DownloadManager dm = (DownloadManager) row.getDataSource(true);
				
			dm.getDownloadState().setBooleanAttribute( DownloadManagerState.AT_FILES_EXPANDED, false );
		}
	}
	
	protected Class<?> getForDataSourceType() {
		return forDataSourceType;
	}
	
	private boolean isCurrent(Tag tag) {
		if (currentTags != null) {
  		for (Tag curTag : currentTags) {
  			if (tag.equals(curTag)) {
  				return true;
  			}
  		}
		}
		return false;
	}

	public boolean isCurrentTagsAny() {
		return currentTagsAny;
	}

	public void setCurrentTagsAny(boolean currentTagsAny) {
		if (this.currentTagsAny == currentTagsAny) {
			return;
		}
		this.currentTagsAny = currentTagsAny;
		setCurrentTags(currentTags);
	}
}
