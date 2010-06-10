/*
 * Created on 30 juin 2003
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

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.ViewUtils.SpeedAdapter;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.impl.*;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

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
                  CategoryManagerListener,
                  CategoryListener,
                  KeyListener,
                  TableLifeCycleListener, 
                  TableViewSWTPanelCreator,
                  TableSelectionListener,
                  TableViewSWTMenuFillListener,
                  TableRefreshListener,
                  TableCountChangeListener,
                  TableViewFilterCheck<DownloadManager>,
                  TableRowSWTPaintListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
	
	private AzureusCore		azureus_core;

  private GlobalManager globalManager;
  protected boolean isSeedingView;

  private Composite cTablePanel;
  private Font fontButton = null;
  private Composite cCategories;
  private ControlAdapter catResizeAdapter;
  private DragSource dragSource = null;
  private DropTarget dropTarget = null;
  private Composite cHeader = null;
  private Label lblHeader = null;
  private Text txtFilter = null;
  private Label lblX = null;
  
  private Category currentCategory;

  // table item index, where the drag has started
  private int drag_drop_line_start = -1;
  private TableRowCore[] drag_drop_rows = null;

	private boolean bDNDalwaysIncomplete;
	private TableViewSWT<DownloadManager> tv;
	private Composite cTableParentPanel;
	protected boolean viewActive;
	private boolean forceHeaderVisible = false;
	private TableSelectionListener defaultSelectedListener;
	private Composite cFilterArea;
	protected boolean resizeHeaderEventQueued;
	private Button btnFilter;

	private Composite cSizer;

	public MyTorrentsView() {
		super("MyTorrentsView");
	}
	
  /**
   * Initialize
   * 
   * @param _azureus_core
   * @param isSeedingView
   * @param basicItems
   */
  public 
  MyTorrentsView(
  		AzureusCore				_azureus_core,
  		String						tableID,
  		boolean 					isSeedingView,
      TableColumnCore[]	basicItems) 
  {
		super("MyTorrentsView");
		init(_azureus_core, tableID, isSeedingView, isSeedingView
				? DownloadTypeComplete.class : DownloadTypeIncomplete.class, basicItems);
  }
  
  // @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
  public TableViewSWT initYourTableView() {
  	return tv;
  }
  
  public void init(AzureusCore _azureus_core, String tableID,
			boolean isSeedingView, Class forDataSourceType, TableColumnCore[] basicItems) {

  	this.isSeedingView 	= isSeedingView;
  	
    tv = createTableView(forDataSourceType, tableID, basicItems);
    tv.setRowDefaultIconSize(new Point(16, 16));
    
    /*
     * 'Big' table has taller row height
     */
    if (getRowDefaultHeight() > 0) {
			tv.setRowDefaultHeight(getRowDefaultHeight());
		}
    
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    

    currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);
    tv.addLifeCycleListener(this);
    tv.setMainPanelCreator(this);
    tv.addSelectionListener(this, false);
    tv.addMenuFillListener(this);
    tv.addRefreshListener(this, false);
    tv.addCountChangeListener(this);
    tv.addRowPaintListener(this);
    
    forceHeaderVisible = COConfigurationManager.getBooleanParameter("MyTorrentsView.alwaysShowHeader");

		//tv.setEnableTabViews(true);
		//IView views[] = { new GeneralView(), new PeersView(),
		//	new PeersGraphicView(), new PiecesView(), new FilesView(),
		//	new LoggerView() };
    //tv.setCoreTabViews(views);
	}

  // @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
  public void tableViewInitialized() {
 
  	tv.addKeyListener(this);

    createTabs();

    if (txtFilter == null) {
    	tv.enableFilterCheck(null, this);
    }

    createDragDrop();

    COConfigurationManager.addAndFireParameterListeners(new String[] {
			"DND Always In Incomplete",
			"Confirm Data Delete",
			"MyTorrentsView.alwaysShowHeader",
			"User Mode"
		}, this);

    if (currentCategory != null) {
    	currentCategory.addCategoryListener(this);
    }
    CategoryManager.addCategoryManagerListener(this);
    globalManager.addListener(this, false);
    DownloadManager[] dms = (DownloadManager[]) globalManager.getDownloadManagers().toArray(new DownloadManager[0]);
    for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
			dm.addListener(this);
			if (!isOurDownloadManager(dm)) {
				dms[i] = null;
			}
		}
    tv.addDataSources(dms);
    tv.processDataSourceQueue();
    
    cTablePanel.layout();
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
						fontButton
					});
					dragSource = null;
					dropTarget = null;
					fontButton = null;
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
    if (currentCategory != null) {
    	currentCategory.removeCategoryListener(this);
    }
    CategoryManager.removeCategoryManagerListener(this);
    globalManager.removeListener(this);
    COConfigurationManager.removeParameterListener("DND Always In Incomplete", this);
    COConfigurationManager.removeParameterListener("Confirm Data Delete", this);
    COConfigurationManager.removeParameterListener("User Mode", this);
  }
  
  
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator#createTableViewPanel(org.eclipse.swt.widgets.Composite)
  public Composite createTableViewPanel(Composite composite) {
  	composite.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event event) {
				viewActive = true;
		    updateSelectedContent();
		    refreshIconBar();
			}
		});
  	composite.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event event) {
				viewActive = false;
				// don't updateSelectedContent() because we may have switched
				// to a button or a text field, and we still want out content to be
				// selected
			}
		});
  	
    GridData gridData;
    cTableParentPanel = new Composite(composite, SWT.NULL);
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
    cTablePanel.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    cTablePanel.setForeground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

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
  
  public void setForceHeaderVisible(boolean forceHeaderVisible) {
		this.forceHeaderVisible  = forceHeaderVisible;
		if (cTablePanel != null && !cTablePanel.isDisposed()) {
			createTabs();
		}
  }

  private void createTabs() {
    GridData gridData;
    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);
    boolean showCat = tv.getFilterText().length() > 0;
    if (!showCat) {
	    for(int i = 0; i < categories.length; i++) {
	        if(categories[i].getType() == Category.TYPE_USER) {
	            showCat = true;
	            break;
	        }
	    }
    }
    boolean show = showCat || forceHeaderVisible;

    if (!showCat) {
    	if (cCategories != null && !cCategories.isDisposed()) {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
    	}
    }
    
    if (show) {
      if (cCategories == null || cCategories.isDisposed()) {
      	buildHeaderArea();
      } else {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
      }
      
      if (showCat) {
      	buildCat(categories);
      }
    } else {
    	if (cHeader != null && !cHeader.isDisposed()) {
    		cHeader.dispose();
    	}
    	if (cTableParentPanel != null && !cTableParentPanel.isDisposed()) {
    		cTableParentPanel.layout();
    	}
    }
  }
  
  /**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private void buildHeaderArea() {
		GridData gridData;
    Composite parent = cTableParentPanel;

    cHeader = new Composite(parent, SWT.NONE);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 5;
    cHeader.setLayoutData(gridData);
    cHeader.setLayout(new FormLayout());
    FormData fd;
    
    lblHeader = new Label(cHeader, SWT.WRAP);
    updateTableLabel();
    
    Label lblSep = new Label(cHeader, SWT.SEPARATOR | SWT.VERTICAL);
    gridData = new GridData(GridData.FILL_VERTICAL);
    gridData.heightHint = 5;
    lblSep.setLayoutData(gridData);
    
    btnFilter = new Button(cHeader, SWT.TOGGLE);
    Messages.setLanguageText(btnFilter, "MyTorrentsView.filter");
    btnFilter.addSelectionListener(new SelectionListener() {
		
			public void widgetSelected(SelectionEvent e) {
				boolean enable = btnFilter.getSelection();
        cFilterArea.setVisible(enable);
        if (enable) {
        	tv.setFilterText(txtFilter.getText());
        	txtFilter.setFocus();
        } else {
        	tv.setFilterText("");
        	tv.setFocus();
        }
        resizeHeader();
			}
		
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
    boolean hasLastSearch = tv.getFilterText().length() != 0;
    btnFilter.setSelection(hasLastSearch);
    

    cFilterArea = new Composite(cHeader, SWT.NONE);
    cFilterArea.setVisible(hasLastSearch);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.numColumns = 5;
    cFilterArea.setLayout(layout);
            
    cCategories = new Composite(cHeader, SWT.NONE);
    RowLayout rowLayout = new RowLayout();
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    rowLayout.marginLeft = 3;
    rowLayout.marginRight = 0;
    rowLayout.spacing = 0;
    rowLayout.wrap = true;
    cCategories.setLayout(rowLayout);
    
    cSizer = new Composite(cHeader, SWT.NONE);

    fd = new FormData();
    fd.top = new FormAttachment(cFilterArea, 0, SWT.CENTER);
    lblHeader.setLayoutData(fd);

    fd = new FormData();
    fd.left = new FormAttachment(lblSep, 10);
    fd.top = new FormAttachment(cFilterArea, 0, SWT.CENTER);
    btnFilter.setLayoutData(fd);

    fd = new FormData();
    fd.left = new FormAttachment(lblHeader, 10);
    fd.top = new FormAttachment(cFilterArea, 3, SWT.TOP);
    fd.bottom = new FormAttachment(cFilterArea, -3, SWT.BOTTOM);
    lblSep.setLayoutData(fd);

    fd = new FormData();
    fd.top = new FormAttachment(0, 2);
    fd.left = new FormAttachment(btnFilter, 0);
    fd.right = new FormAttachment(cCategories, -10);
    cFilterArea.setLayoutData(fd);

    fd = new FormData();
    fd.right = new FormAttachment(cSizer, -5); 
    fd.top = new FormAttachment(cFilterArea, 0, SWT.CENTER); 
    fd.bottom = new FormAttachment(cFilterArea, 0, SWT.BOTTOM);
    cCategories.setLayoutData(fd);

		int userMode = COConfigurationManager.getIntParameter("User Mode");

		boolean enableSizer = userMode >= 2 && Constants.IS_CVS_VERSION;
		
    fd = new FormData();
    fd.right = new FormAttachment(100, -1); 
    fd.top = new FormAttachment(cFilterArea, 0, SWT.CENTER); 
    fd.width = enableSizer ? 20 : 1;
    fd.height = enableSizer ? 20 : 1;
    cSizer.setLayoutData(fd);

    if (enableSizer) {
    	tv.enableSizeSlider(cSizer, 16, 96);
    }

    cHeader.addListener(SWT.Resize, new Listener(){
			public void handleEvent(Event event) {
				if (!resizeHeaderEventQueued) {
	        resizeHeaderEventQueued = true;
					Utils.execSWTThreadLater(0, new AERunnable(){
						public void runSupport() {
							resizeHeader();
							resizeHeaderEventQueued = false;
						}
					});
				}
			}
		});
    
    
    txtFilter = new Text(cFilterArea, SWT.BORDER);
    Messages.setLanguageTooltip(txtFilter, "MyTorrentsView.filter.tooltip");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    txtFilter.setLayoutData(gridData);
    

    lblX = new Label(cFilterArea, SWT.WRAP);
    Messages.setLanguageTooltip(lblX, "MyTorrentsView.clearFilter.tooltip");
    gridData = new GridData(SWT.TOP);
    lblX.setLayoutData(gridData);
    ImageLoader.getInstance().setLabelImage(lblX, "smallx-gray");
    lblX.setData("ImageID", "smallx-gray");
    lblX.addMouseListener(new MouseAdapter() {
    	public void mouseUp(MouseEvent e) {
    		if (e.y <= 10) {
    			tv.setFilterText("");
      		txtFilter.setText("");
    		}
    	}
    });
    

    lblSep = new Label(cFilterArea, SWT.SEPARATOR | SWT.VERTICAL);
    gridData = new GridData(GridData.FILL_VERTICAL);
    gridData.heightHint = 5;
    lblSep.setLayoutData(gridData);
    
    cHeader.moveAbove(null);
    parent.layout(true, true);

    tv.enableFilterCheck(txtFilter, this);
	}

  private void hideShowSlider() {
  	if (!Constants.IS_CVS_VERSION) {
  		return;
  	}
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				int userMode = COConfigurationManager.getIntParameter("User Mode");
				boolean isAdvanced = userMode >= 2;
				
				if (cSizer == null || cSizer.isDisposed()) {
					return;
				}
				
				FormData fd = (FormData) cSizer.getLayoutData();
				if (isAdvanced) {
					if (fd.width < 20) {
						fd.width = 20;
						fd.height = 16;
			    	tv.enableSizeSlider(cSizer, 16, 96);
			    	cSizer.setVisible(false);
						cSizer.getParent().layout(true);
					}
				} else {
					if (fd.width == 100) {
						fd.width = 1;
						fd.height = 1;
						tv.disableSizeSlider();
						cSizer.setVisible(true);
						cSizer.getParent().layout(true);
					}
				}
			}
		});

		
	}


	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	protected void showFilterArea() {
		if (cFilterArea != null && !cFilterArea.isDisposed() && !cFilterArea.isVisible()) {
			cFilterArea.setVisible(true);
		}
		if (btnFilter != null && !btnFilter.isDisposed() && !btnFilter.getSelection()) {
			btnFilter.setSelection(true);
		}
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	protected void resizeHeader() {
		if (cCategories == null || cCategories.isDisposed()) {
			return;
		}
		Point curCatPos = cCategories.getLocation();
		int posEndFilter = cFilterArea.getLocation().x + (cFilterArea.isVisible() ? 170 : 0);
		
		boolean onNewLine = ((FormData)cCategories.getLayoutData()).left != null;
		
		//System.out.println("posend=" + posEndFilter + ";curcat" + curCatPos);
		if (onNewLine) {
			Point prefSizeCat = cCategories.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			//System.out.println("pref=" + prefSizeCat + ";sz=" + cHeader.getClientArea());
			if (prefSizeCat.x + posEndFilter + cSizer.getSize().x < cHeader.getClientArea().width) {
				//System.out.println("MOVE UP");
				FormData fd = new FormData();
        fd.right = new FormAttachment(cSizer, -2); 
        fd.top = new FormAttachment(cFilterArea, 0, SWT.CENTER); 
        fd.bottom = new FormAttachment(cFilterArea, 0, SWT.BOTTOM); 
				cCategories.setLayoutData(fd);

        fd = new FormData();
        fd.top = new FormAttachment(0, 2);
        fd.left = new FormAttachment(btnFilter, 0);
        fd.right = new FormAttachment(cCategories, -10);
        cFilterArea.setLayoutData(fd);

				cHeader.getShell().layout(new Control[] {
					cFilterArea,
					cCategories
				});
			}
			// assume on new line
		} else {
			if (curCatPos.x < posEndFilter) {
				//System.out.println("MOVE DOWN");
				FormData fd = new FormData();
				fd.top = new FormAttachment(cFilterArea, 2);
				fd.bottom = new FormAttachment(100, -3);
				fd.left = new FormAttachment(0,0);
				fd.right = new FormAttachment(100,0);
				cCategories.setLayoutData(fd);

				fd = new FormData();
		    fd.top = new FormAttachment(0, 2);
        fd.left = new FormAttachment(btnFilter, 0);
        fd.right = new FormAttachment(cSizer, -10);
        cFilterArea.setLayoutData(fd);

				cHeader.getShell().layout(new Control[] {
					cFilterArea,
					cCategories
				});
			}
		}
	}

	/**
	 * 
	 *
	 * @param categories 
   * @since 3.1.1.1
	 */
	private void buildCat(Category[] categories) {
		int iFontPixelsHeight = 11;
		int iFontPointHeight = (iFontPixelsHeight * 72)
				/ cCategories.getDisplay().getDPI().y;
		for (int i = 0; i < categories.length; i++) {
			final Category category = categories[i];

			final Button catButton = new Button(cCategories, SWT.TOGGLE);
			catButton.addKeyListener(this);
			if (i == 0 && fontButton == null) {
				Font f = catButton.getFont();
				FontData fd = f.getFontData()[0];
				fd.setHeight(iFontPointHeight);
				fontButton = new Font(cCategories.getDisplay(), fd);
			}
			catButton.setText("|");
			catButton.setFont(fontButton);
			catButton.pack(true);
			if (catButton.computeSize(100, SWT.DEFAULT).y > 0) {
				RowData rd = new RowData();
				rd.height = catButton.computeSize(100, SWT.DEFAULT).y - 2
						+ catButton.getBorderWidth() * 2;
				catButton.setLayoutData(rd);
			}

			final String name = category.getName();
			if (category.getType() == Category.TYPE_USER)
				catButton.setText(name);
			else
				Messages.setLanguageText(catButton, name);

			catButton.setData("Category", category);
			if (category == currentCategory) {
				catButton.setSelection(true);
			}

			catButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Button curButton = (Button) e.widget;
					boolean isEnabled = curButton.getSelection();
					Control[] controls = cCategories.getChildren();
					if (!isEnabled) {
						for (int i = 0; i < controls.length; i++) {
							if (controls[i] instanceof Button) {
								curButton = (Button) controls[i];
								break;
							}
						}
					}

					for (int i = 0; i < controls.length; i++) {
						if (!(controls[i] instanceof Button)) {
							continue;
						}
						Button b = (Button) controls[i];
						if (b != curButton && b.getSelection())
							b.setSelection(false);
						else if (b == curButton && !b.getSelection())
							b.setSelection(true);
					}
					activateCategory((Category) curButton.getData("Category"));
				}
			});

			catButton.addListener(SWT.MouseHover, new Listener() {
				public void handleEvent(Event event) {
					Button curButton = (Button) event.widget;
					Category curCategory = (Category) curButton.getData("Category");
					List dms = curCategory.getDownloadManagers(globalManager.getDownloadManagers());

					long ttlActive = 0;
					long ttlSize = 0;
					long ttlRSpeed = 0;
					long ttlSSpeed = 0;
					int count = 0;
					for (Iterator iter = dms.iterator(); iter.hasNext();) {
						DownloadManager dm = (DownloadManager) iter.next();

						if (!isInCategory(dm, currentCategory))
							continue;

						count++;
						if (dm.getState() == DownloadManager.STATE_DOWNLOADING
								|| dm.getState() == DownloadManager.STATE_SEEDING)
							ttlActive++;
						ttlSize += dm.getSize();
						ttlRSpeed += dm.getStats().getDataReceiveRate();
						ttlSSpeed += dm.getStats().getDataSendRate();
					}

					String up_details = "";
					String down_details = "";

					if (category.getType() != Category.TYPE_ALL) {

						String up_str = MessageText.getString("GeneralView.label.maxuploadspeed");
						String down_str = MessageText.getString("GeneralView.label.maxdownloadspeed");
						String unlimited_str = MessageText.getString("MyTorrentsView.menu.setSpeed.unlimited");

						int up_speed = category.getUploadSpeed();
						int down_speed = category.getDownloadSpeed();

						up_details = up_str
								+ ": "
								+ (up_speed == 0 ? unlimited_str
										: DisplayFormatters.formatByteCountToKiBEtc(up_speed));
						down_details = down_str
								+ ": "
								+ (down_speed == 0 ? unlimited_str
										: DisplayFormatters.formatByteCountToKiBEtc(down_speed));
					}

					if (count == 0) {
						curButton.setToolTipText(down_details + "\n" + up_details
								+ "\nTotal: 0");
						return;
					}

					curButton.setToolTipText((up_details.length() == 0 ? ""
							: (down_details + "\n" + up_details + "\n"))
							+ "Total: "
							+ count
							+ "\n"
							+ "Downloading/Seeding: "
							+ ttlActive
							+ "\n"
							+ "\n"
							+ "Speed: "
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlRSpeed
									/ count)
							+ "/"
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlSSpeed
									/ count)
							+ "\n"
							+ "Size: "
							+ DisplayFormatters.formatByteCountToKiBEtc(ttlSize));
				}
			});

			final DropTarget tabDropTarget = new DropTarget(catButton,
					DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			Transfer[] types = new Transfer[] {
				TextTransfer.getInstance()
			};
			tabDropTarget.setTransfer(types);
			tabDropTarget.addDropListener(new DropTargetAdapter() {
				public void dragOver(DropTargetEvent e) {
					if (drag_drop_line_start >= 0)
						e.detail = DND.DROP_MOVE;
					else
						e.detail = DND.DROP_NONE;
				}

				public void drop(DropTargetEvent e) {
					e.detail = DND.DROP_NONE;
					//System.out.println("DragDrop on Button:" + drag_drop_line_start);
					if (drag_drop_line_start >= 0) {
						drag_drop_line_start = -1;
						drag_drop_rows = null;

						TorrentUtil.assignToCategory(tv.getSelectedDataSources().toArray(),
								(Category) catButton.getData("Category"));
					}
				}
			});

			catButton.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (tabDropTarget != null && !tabDropTarget.isDisposed()) {
						tabDropTarget.dispose();
					}
				}
			});

			final Menu menu = new Menu(catButton.getShell(), SWT.POP_UP);

			catButton.setMenu(menu);

			menu.addMenuListener(new MenuListener() {
				boolean bShown = false;

				public void menuHidden(MenuEvent e) {
					bShown = false;

					if (Constants.isOSX)
						return;

					// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
					// get fired (async workaround provided by Eclipse Bug #87678)

					e.widget.getDisplay().asyncExec(new AERunnable() {
						public void runSupport() {
							if (bShown || menu.isDisposed())
								return;
							MenuItem[] items = menu.getItems();
							for (int i = 0; i < items.length; i++) {
								items[i].dispose();
							}
						}
					});
				}

				public void menuShown(MenuEvent e) {
					MenuItem[] items = menu.getItems();
					for (int i = 0; i < items.length; i++)
						items[i].dispose();

					bShown = true;

					if (category.getType() == Category.TYPE_USER) {

						final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);

						Messages.setLanguageText(itemDelete,
								"MyTorrentsView.menu.category.delete");

						menu.setDefaultItem(itemDelete);

						itemDelete.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
								Category catToDelete = (Category) catButton.getData("Category");
								if (catToDelete != null) {
									java.util.List managers = catToDelete.getDownloadManagers(globalManager.getDownloadManagers());
									// move to array,since setcategory removed it from the category,
									// which would mess up our loop
									DownloadManager dms[] = (DownloadManager[]) managers.toArray(new DownloadManager[managers.size()]);
									for (int i = 0; i < dms.length; i++) {
										dms[i].getDownloadState().setCategory(null);
									}
									if (currentCategory == catToDelete) {

										activateCategory(CategoryManager.getCategory(Category.TYPE_ALL));

									} else {
										// always activate as deletion of this one might have
										// affected the current view 
										activateCategory(currentCategory);
									}
									CategoryManager.removeCategory(catToDelete);
								}
							}
						});
					}

					if (category.getType() != Category.TYPE_ALL) {

						long maxDownload = COConfigurationManager.getIntParameter(
								"Max Download Speed KBs", 0) * 1024;
						long maxUpload = COConfigurationManager.getIntParameter(
								"Max Upload Speed KBs", 0) * 1024;

						int down_speed = category.getDownloadSpeed();
						int up_speed = category.getUploadSpeed();

						ViewUtils.addSpeedMenu(menu.getShell(), menu, true, true, false,
								down_speed == 0, down_speed, down_speed, maxDownload, false,
								up_speed == 0, up_speed, up_speed, maxUpload, 1,
								new SpeedAdapter() {
									public void setDownSpeed(int val) {
										category.setDownloadSpeed(val);
									}

									public void setUpSpeed(int val) {
										category.setUploadSpeed(val);

									}
								});
					}

					java.util.List managers = category.getDownloadManagers(globalManager.getDownloadManagers());

					final DownloadManager dms[] = (DownloadManager[]) managers.toArray(new DownloadManager[managers.size()]);

					boolean start = false;
					boolean stop = false;

					for (int i = 0; i < dms.length; i++) {

						DownloadManager dm = dms[i];

						stop = stop || ManagerUtils.isStopable(dm);

						start = start || ManagerUtils.isStartable(dm);

					}

					// Queue

					final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue");
					Utils.setMenuItemImage(itemQueue, "start");
					itemQueue.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							TorrentUtil.queueTorrents(dms, menu.getShell());
						}
					});
					itemQueue.setEnabled(start);

					// Stop

					final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop");
					Utils.setMenuItemImage(itemStop, "stop");
					itemStop.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							TorrentUtil.stopTorrents(dms, menu.getShell());
						}
					});
					itemStop.setEnabled(stop);

					// share with friends
					
					PluginInterface bpi = PluginInitializer.getDefaultInterface().getPluginManager().getPluginInterfaceByClass( BuddyPlugin.class );
					
					int cat_type = category.getType();
					
					if ( bpi != null && cat_type != Category.TYPE_UNCATEGORIZED ){
						
						final BuddyPlugin	buddy_plugin = (BuddyPlugin)bpi.getPlugin();
						
						if ( buddy_plugin.isEnabled()){
							
							final Menu share_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
							final MenuItem share_item = new MenuItem(menu, SWT.CASCADE);
							Messages.setLanguageText(share_item, "azbuddy.ui.menu.cat.share" );
							share_item.setMenu(share_menu);

							List<BuddyPluginBuddy> buddies = buddy_plugin.getBuddies();

							if ( buddies.size() == 0 ){
								
								final MenuItem item = new MenuItem(share_menu, SWT.CHECK );
								
								item.setText( MessageText.getString( "general.add.friends" ));
								
								item.setEnabled( false );

							}else{
								final String cname;
								
								if ( cat_type == Category.TYPE_ALL ){
									
									cname = "All";
									
								}else{
									
									cname = category.getName();
								}
	
								final boolean is_public = buddy_plugin.isPublicCategory( cname );
							
								final MenuItem itemPubCat = new MenuItem(share_menu, SWT.CHECK );
								
								Messages.setLanguageText( itemPubCat, "general.all.friends" );
								
								itemPubCat.setSelection( is_public );
								
								itemPubCat.addListener(
									SWT.Selection, 
									new Listener() 
									{
										public void 
										handleEvent(
											Event event) 
										{
											if ( is_public ){
											
												buddy_plugin.removePublicCategory( cname );
												
											}else{
												
												buddy_plugin.addPublicCategory( cname );
											}
										}
									});
								
								new MenuItem(share_menu, SWT.SEPARATOR );
															
								for ( final BuddyPluginBuddy buddy: buddies ){
									
									if ( buddy.getNickName() == null ){
										
										continue;
									}
																	
									final boolean auth = buddy.isLocalRSSCategoryAuthorised( cname );
									
									final MenuItem itemShare = new MenuItem(share_menu, SWT.CHECK );
									
									itemShare.setText( buddy.getName());
									
									itemShare.setSelection( auth || is_public );
									
									if ( is_public ){
										
										itemShare.setEnabled( false );
									}
									
									itemShare.addListener(
										SWT.Selection, 
										new Listener() 
										{
											public void 
											handleEvent(
												Event event) 
											{
												if ( auth ){
												
													buddy.removeLocalAuthorisedRSSCategory( cname );
													
												}else{
													
													buddy.addLocalAuthorisedRSSCategory( cname );
												}
											}
										});
									
								}
							}
						}
					}
					
						// auto-transcode
					
					AZ3Functions.provider provider = AZ3Functions.getProvider();
					
					if ( provider != null && category.getType() != Category.TYPE_ALL ){
						
						AZ3Functions.provider.TranscodeTarget[] tts = provider.getTranscodeTargets();
						 
						if ( tts.length > 0 ){
							
							final Menu t_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
							final MenuItem t_item = new MenuItem(menu, SWT.CASCADE);
							Messages.setLanguageText(t_item, "cat.autoxcode" );
							t_item.setMenu(t_menu);
						
							String existing = category.getStringAttribute( Category.AT_AUTO_TRANSCODE_TARGET );
							
							for ( AZ3Functions.provider.TranscodeTarget tt: tts ){
							
								AZ3Functions.provider.TranscodeProfile[] profiles = tt.getProfiles();
							
								if ( profiles.length > 0 ){
									
									final Menu tt_menu = new Menu(t_menu.getShell(), SWT.DROP_DOWN);
									final MenuItem tt_item = new MenuItem(t_menu, SWT.CASCADE);
									tt_item.setText( tt.getName());
									tt_item.setMenu(tt_menu);
									
									for ( final AZ3Functions.provider.TranscodeProfile tp: profiles ){

										final MenuItem p_item = new MenuItem(tt_menu, SWT.CHECK );
										
										p_item.setText(tp.getName());
										
										p_item.setSelection( existing != null && existing.equals( tp.getUID()));
										
										p_item.addListener(
											SWT.Selection, 
											new Listener() 
											{
												public void 
												handleEvent(
													Event event) 
												{	
													category.setStringAttribute( Category.AT_AUTO_TRANSCODE_TARGET, p_item.getSelection()?tp.getUID():null );
												}
											});
									}
								}
							}
						}
					}
					
					// options

					MenuItem itemOptions = new MenuItem(menu, SWT.PUSH);

					Messages.setLanguageText(itemOptions,
							"MainWindow.menu.view.configuration");
					itemOptions.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();

							uiFunctions.openView(UIFunctions.VIEW_DM_MULTI_OPTIONS, dms);
						}
					});

					if (dms.length == 0) {

						itemOptions.setEnabled(false);
					}
				}
			});
		}

		cHeader.layout(true, true);
		Utils.execSWTThreadLater(0, new AERunnable(){
			public void runSupport() {
				resizeHeader();
			}
		});

		// layout hack - relayout
//		if (catResizeAdapter == null) {
//			catResizeAdapter = new ControlAdapter() {
//				public void controlResized(ControlEvent event) {
//					if (getComposite().isDisposed() || cCategories.isDisposed())
//						return;
//
//					GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
//
//					int parentWidth = cCategories.getParent().getClientArea().width;
//					int catsWidth = cCategories.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
//					// give text a 5 pixel right padding
//					int textWidth = 200;
//					System.out.println("TW=" + textWidth + ";" + (cHeader.computeSize(20, SWT.DEFAULT).x));
//
//					Object layoutData = cHeader.getLayoutData();
//					if (layoutData instanceof GridData) {
//						GridData labelGrid = (GridData) layoutData;
//						textWidth += labelGrid.horizontalIndent;
//					}
//
//					if (textWidth + catsWidth > parentWidth) {
//						gridData.widthHint = parentWidth - textWidth;
//					}
//					cCategories.setLayoutData(gridData);
//					cCategories.getParent().layout(true);
//
//				}
//			};
//
//			tv.getTableComposite().addControlListener(catResizeAdapter);
//		}
//
//		catResizeAdapter.controlResized(null);
	}

	public boolean isOurDownloadManager(DownloadManager dm) {
  	if (!isInCategory(dm, currentCategory)) {
  		return false;
  	}

		boolean bCompleted =  dm.isDownloadComplete(bDNDalwaysIncomplete);
		boolean bOurs = (bCompleted && isSeedingView)
				|| (!bCompleted && !isSeedingView);
		
//		System.out.println("ourDM? " + sTableID + "; " + dm.getDisplayName()
//				+ "; Complete=" + bCompleted + ";Ours=" + bOurs + ";bc"
//				+ dm.getStats().getDownloadCompleted(false) + ";"
//				+ dm.getStats().getDownloadCompleted(true));

		return bOurs;
	}

	public boolean filterCheck(DownloadManager dm, String sLastSearch, boolean bRegexSearch) {
		boolean bOurs = true;

		if (sLastSearch.length() > 0) {
			try {
				String[][] names = {
					{
						"",
						dm.getDisplayName()
					},
					{
						"t:",
						dm.getTorrent().getAnnounceURL().getHost()
					},
					{
						"st:",
						"" + dm.getState()
					}
				};

				String name = names[0][1];
				String tmpSearch = sLastSearch;

				for (int i = 0; i < names.length; i++) {
					if (tmpSearch.startsWith(names[i][0])) {
						tmpSearch = tmpSearch.substring(names[i][0].length());
						name = names[i][1];
					}
				}

				String s = bRegexSearch ? tmpSearch : "\\Q"
						+ tmpSearch.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
				Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);

				if (!pattern.matcher(name).find())
					bOurs = false;
			} catch (Exception e) {
				// Future: report PatternSyntaxException message to user.
			}
		}
		return bOurs;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(final String filter) {
		Utils.execSWTThread(new AERunnable(){
			public void runSupport() {
				if (lblX != null && !lblX.isDisposed()) {
					String oldID = (String) lblX.getData("ImageID");
					String id = filter.length() > 0 ? "smallx" : "smallx-gray";
					if (oldID == null || !oldID.equals(id)) {
						ImageLoader.getInstance().setLabelImage(lblX, id);
						lblX.setData("ImageID", id);
					}
				}

				if (filter.length() > 0) {
					if (txtFilter == null) {
				    createTabs();
					} else {
						showFilterArea();
					}
				}
			}
		});
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void selected(TableRowCore[] rows) {
  	updateSelectedContent();
  	refreshTorrentMenu();
  	refreshIconBar();
  }

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
  	updateSelectedContent();
  	refreshTorrentMenu();
  	refreshIconBar();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
  	refreshIconBar();
  	refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseEnter(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseEnter(TableRowCore row) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseExit(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseExit(TableRowCore row) {
	}

	public void updateSelectedContent() {
		if (cTablePanel == null || cTablePanel.isDisposed()) {
			return;
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
		SelectedContentManager.changeCurrentlySelectedContent(tv.getTableID(), listSelected.toArray(new SelectedContent[0]), tv);
	}
	
  private void refreshIconBar() {
  	computePossibleActions();
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.refreshIconBar();
  	}
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
				uiFunctions.openView(UIFunctions.VIEW_DM_DETAILS, dm_sources[i]);
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
  			FilesViewMenuUtil.fillMenu(tv, menu, firstFileDM,
  					listFileInfos.toArray(new DiskManagerFileInfo[0]));
  			return;
  		}
		}
		
		boolean hasSelection = (dms.length > 0);

		if (hasSelection) {
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
							// System.out.println("DragStart");
							drag_drop_line_start = rows[0].getIndex();
							drag_drop_rows = rows;
						} else {
							event.doit = false;
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}

						// Build eventData here because on OSX, selection gets cleared
						// by the time dragSetData occurs
						DownloadManager[] selectedDownloads = getSelectedDownloads();
						eventData = "DownloadManager\n";
						for (int i = 0; i < selectedDownloads.length; i++) {
							DownloadManager dm = selectedDownloads[i];
							
							TOTorrent torrent = dm.getTorrent();
							try {
								eventData += torrent.getHashWrapper().toBase32String() + "\n";
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

			dropTarget = tv.createDropTarget(DND.DROP_DEFAULT | DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
			if (dropTarget != null) {
				dropTarget.setTransfer(new Transfer[] { HTMLTransfer.getInstance(),
						URLTransfer.getInstance(), FileTransfer.getInstance(),
						TextTransfer.getInstance() });

				dropTarget.addDropListener(new DropTargetAdapter() {
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
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_BEFORE;
						}
					}

					public void dragOver(DropTargetEvent event) {
						if (drag_drop_line_start >= 0) {
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_BEFORE;
						}
					}

					public void drop(DropTargetEvent event) {
						if (!(event.data instanceof String)
								|| !((String) event.data).startsWith("DownloadManager\n")) {
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

    boolean bForceSort = sortColumn.getName().equals("#");
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
					TorrentUtil.stopTorrents(tv.getSelectedDataSources().toArray(),
							cTablePanel.getShell());
					e.doit = false;
					break;
			}

			if (!e.doit)
				return;
		}
		
		// DEL remove selected Torrents
		if (e.stateMask == 0 && e.keyCode == SWT.DEL && e.widget != txtFilter) {
			TorrentUtil.removeTorrents(tv.getSelectedDataSources().toArray(), cTablePanel.getShell());
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
    System.out.println("forcesort? " + bForceSort);
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
			Arrays.sort(dms, new Comparator() {
				public int compare(Object a, Object b) {
					return ((DownloadManager) a).getPosition()
							- ((DownloadManager) b).getPosition();
				}
			});
		} else {
			Arrays.sort(dms, new Comparator() {
				public int compare(Object a, Object b) {
					return ((DownloadManager) b).getPosition()
							- ((DownloadManager) a).getPosition();
				}
			});
		}

		int count = globalManager.downloadManagerCount(isSeedingView); 
		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
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
		if (parameterName == null
				|| parameterName.equals("DND Always In Incomplete")) {
			bDNDalwaysIncomplete = COConfigurationManager.getBooleanParameter("DND Always In Incomplete");
		}
		if (parameterName == null || parameterName.equals("MyTorrentsView.alwaysShowHeader")) {
			setForceHeaderVisible(COConfigurationManager.getBooleanParameter("MyTorrentsView.alwaysShowHeader"));
		}
		if (parameterName == null || parameterName.equals("User Mode")) {
			hideShowSlider();
		}
	}

	private boolean top,bottom,up,down,run,start,stop,remove;

  private void computePossibleActions() {
    Object[] dataSources = tv.getSelectedDataSources().toArray();
    // enable up and down so that we can do the "selection rotate trick"
    up = down = run =  remove = (dataSources.length > 0);
    top = bottom = start = stop = false;
    for (int i = 0; i < dataSources.length; i++) {
    	if (dataSources[i] instanceof DownloadManager) {
        DownloadManager dm = (DownloadManager)dataSources[i];
  
        if(!start && ManagerUtils.isStartable(dm))
          start =  true;
        if(!stop && ManagerUtils.isStopable(dm))
          stop = true;
        if(!top && dm.getGlobalManager().isMoveableUp(dm))
          top = true;
        if(!bottom && dm.getGlobalManager().isMoveableDown(dm))
          bottom = true;
    	}
    }
  }

  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("run"))
      return run;
    if(itemKey.equals("start"))
      return start;
    if(itemKey.equals("stop"))
      return stop;
    if(itemKey.equals("remove"))
      return remove;
    if(itemKey.equals("top"))
      return top;
    if(itemKey.equals("bottom"))
      return bottom;
    if(itemKey.equals("up"))
      return up;
    if(itemKey.equals("down"))
      return down;
    if(itemKey.equals("share"))
      return remove;
    if(itemKey.equals("transcode"))
      return remove;
    return super.isEnabled(itemKey);
  }

  public void itemActivated(String itemKey) {
    if(itemKey.equals("top")) {
      moveSelectedTorrentsTop();
      return;
    }
    if(itemKey.equals("bottom")){
      moveSelectedTorrentsEnd();
      return;
    }
    if(itemKey.equals("up")) {
      moveSelectedTorrentsUp();
      return;
    }
    if(itemKey.equals("down")){
      moveSelectedTorrentsDown();
      return;
    }
    if(itemKey.equals("run")){
      TorrentUtil.runTorrents(tv.getSelectedDataSources().toArray());
      return;
    }
    if(itemKey.equals("start")){
      TorrentUtil.queueTorrents(tv.getSelectedDataSources().toArray(),
					cTablePanel.getShell());
      return;
    }
    if(itemKey.equals("stop")){
      TorrentUtil.stopTorrents(tv.getSelectedDataSources().toArray(),
					cTablePanel.getShell());
      return;
    }
    if(itemKey.equals("remove")){
      TorrentUtil.removeTorrents(tv.getSelectedDataSources().toArray(),
					cTablePanel.getShell());
      return;
    }
    super.itemActivated(itemKey);
  }
  

  // categorymanagerlistener Functions
  public void downloadManagerAdded(Category category, final DownloadManager manager)
  {
  	if (isOurDownloadManager(manager)) {
      tv.addDataSource(manager);
    }
  }

  public void downloadManagerRemoved(Category category, DownloadManager removed)
  {
    tv.removeDataSource(removed);
  }


  // DownloadManagerListener Functions
  public void stateChanged(DownloadManager manager, int state) {
    final TableRowCore row = tv.getRow(manager);
    if (row != null) {
    	Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
		    	row.refresh(true);
		    	if (row.isSelected()) {
		    		refreshIconBar();
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
					refreshIconBar();
				}
    	});
  	}
  }
  
  // DownloadManagerListener
  public void filePriorityChanged(DownloadManager download,
			DiskManagerFileInfo file) {
	}

  // DownloadManagerListener
	public void completionChanged(final DownloadManager manager,
			boolean bCompleted) {
		// manager has moved lists

		if (isOurDownloadManager(manager)) {

			// only make the download visible if it satisfies the category selection

			if (currentCategory == null
					|| currentCategory.getType() == Category.TYPE_ALL) {

				tv.addDataSource(manager);

			} else {

				int catType = currentCategory.getType();

				Category manager_category = manager.getDownloadState().getCategory();

				if (manager_category == null) {

					if (catType == Category.TYPE_UNCATEGORIZED) {

						tv.addDataSource(manager);
					}
				} else {

					if (currentCategory.getName().equals(manager_category.getName()))

						tv.addDataSource(manager);
				}
			}
		} else if ((isSeedingView && !bCompleted) || (!isSeedingView && bCompleted)) {

			tv.removeDataSource(manager);
		}
	}

  // DownloadManagerListener
  public void downloadComplete(DownloadManager manager) {
  }


  /**
   * Rebuild the table based on the category activated
   * 
   * @param category
   */
  private void activateCategory(Category category) {
		if (category != currentCategory) {
			if (currentCategory != null)
				currentCategory.removeCategoryListener(this);
			if (category != null)
				category.addCategoryListener(this);

			currentCategory = category;
		}
		
		Object[] managers = globalManager.getDownloadManagers().toArray();
		Set existing = new HashSet(tv.getDataSources());
		List<DownloadManager> listRemoves = new ArrayList<DownloadManager>();
		List<DownloadManager> listAdds = new ArrayList<DownloadManager>();
		
		for (int i = 0; i < managers.length; i++) {
			DownloadManager dm = (DownloadManager) managers[i];
		
			boolean bHave = existing.contains(dm);
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
  
  public boolean isInCurrentCategory(DownloadManager manager) {
  	return isInCategory(manager, currentCategory);
  }

  private boolean isInCategory(DownloadManager manager, Category category) {
  	if (category == null) {
  		return true;
  	}
		int type = category.getType();
		if (type == Category.TYPE_ALL) {
			return true;
		}

  	Category dmCategory = manager.getDownloadState().getCategory();
  	if (dmCategory == null) {
  		return type == Category.TYPE_UNCATEGORIZED;
  	}
  	
  	return category.equals(dmCategory);
  }


  // CategoryManagerListener Functions
  public void categoryAdded(Category category) {
  	Utils.execSWTThread(
	  		new AERunnable() 
			{
	  			public void 
				runSupport() 
	  			{
	  				createTabs();
	  			}
			});
  }

  public void categoryRemoved(Category category) {
  	Utils.execSWTThread(
	  		new AERunnable() 
			{
	  			public void 
				runSupport() 
	  			{
	  				createTabs();
	  			}
			});
  }
  
  public void categoryChanged(Category category) {	
  }

  // globalmanagerlistener Functions
  public void downloadManagerAdded( DownloadManager dm ) {
    dm.addListener( this );
    downloadManagerAdded(null, dm);
  }

  public void downloadManagerRemoved( DownloadManager dm ) {
    dm.removeListener( this );
    DownloadBar.close(dm);
    downloadManagerRemoved(null, dm);
  }

  public void destroyInitiated() {  }
  public void destroyed() { }
  public void seedingStatusChanged( boolean seeding_only_mode, boolean b ){}       

  // End of globalmanagerlistener Functions
  


  // @see com.aelitis.azureus.ui.common.table.TableCountChangeListener#rowAdded(com.aelitis.azureus.ui.common.table.TableRowCore)
  public void rowAdded(TableRowCore row) {
  	if (tv.canHaveSubItems() && row.getParentRowCore() == null) {
    	DownloadManager dm = (DownloadManager) row.getDataSource(true);
    	if (dm != null) {
    		DiskManagerFileInfo[] fileInfos = dm.getDiskManagerFileInfo();
    		if (fileInfos != null && fileInfos.length > 0) {
    			row.setSubItems(fileInfos);
    		}
    	}
  	}
		updateTableLabel();
  }

  // @see com.aelitis.azureus.ui.common.table.TableCountChangeListener#rowRemoved(com.aelitis.azureus.ui.common.table.TableRowCore)
  public void rowRemoved(TableRowCore row) {
		updateTableLabel();
	}
	

	public void updateLanguage() {
		super.updateLanguage();
		updateTableLabel();
		getComposite().layout(true, true);
	}

	/**
	 * 
	 */
	private boolean refreshingTableLabel = false;
	private void updateTableLabel() {
		if (refreshingTableLabel || lblHeader == null || lblHeader.isDisposed()) {
			return;
		}
		refreshingTableLabel = true;
		lblHeader.getDisplay().asyncExec(new AERunnable() {
			public void runSupport() {
				try {
					if (lblHeader != null && !lblHeader.isDisposed()) {
						String sText = MessageText.getString(tv.getTableID() + "View"
								+ ".header")
								+ " (" + tv.size(true) + ")";
						lblHeader.setText(sText);
						lblHeader.getParent().layout();
					}
				} finally {
					refreshingTableLabel = false;
				}
			}
		});
	}

	public boolean isTableFocus() {
		return viewActive;
		//return tv.isTableFocus();
	}
	
	public Image obfusticatedImage(final Image image, Point shellOffset) {
		return tv.obfusticatedImage(image, shellOffset);
	}
	
	/**
	 * Creates and return an <code>TableViewSWT</code>
	 * Subclasses my override to return a different TableViewSWT if needed
	 * @param basicItems
	 * @return
	 */
	protected TableViewSWT createTableView(Class forDataSourceType, String tableID,
			TableColumnCore[] basicItems) {
		int tableExtraStyle = COConfigurationManager.getIntParameter("MyTorrentsView.table.style");
		return new TableViewSWTImpl(forDataSourceType, tableID, getPropertiesPrefix(),
				basicItems, "#", tableExtraStyle | SWT.MULTI | SWT.FULL_SELECTION
						| SWT.VIRTUAL | SWT.CASCADE) {
			public void setSelectedRows(TableRowCore[] rows) {
				super.setSelectedRows(rows);
				updateSelectedContent();
			}			
		};
	}

	/**
	 * Returns the default row height for the table
	 * Subclasses my override to return a different height if needed; a height of -1 means use default
	 * @return
	 */
	protected int getRowDefaultHeight(){
		return -1;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWTPaintListener#rowPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableRowSWT)
	public void rowPaint(GC gc, TableRowCore row, TableColumnCore column,
			Rectangle cellArea) {
		Object dataSource = row.getDataSource(true);
		if (dataSource instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) dataSource;

			if (column.getName().equals("name")) {
				final String CFG_SHOWPROGRAMICON = "NameColumn.showProgramIcon."	+ tv.getTableID();
				boolean showIcon = COConfigurationManager.getBooleanParameter(
						CFG_SHOWPROGRAMICON,
						COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon"));
				//System.out.println(cellArea);
				int padding = 10 + 20 + (showIcon ? cellArea.height : 0);
				cellArea.x += padding;
				cellArea.width -= padding;
				GCStringPrinter.printString(gc, fileInfo.getFile(true).getName(), cellArea, true, false, SWT.LEFT );
			} else if (column.getName().equals("size")) {
				String s = DisplayFormatters.formatByteCountToKiBEtc(fileInfo.getLength());
				cellArea.width -= 3;
				GCStringPrinter.printString(gc, s, cellArea, true, false, SWT.RIGHT);
			}
			//Rectangle bounds = row.getBounds();
			//bounds.x = 120;
			//bounds.width = row.getParent().getClientArea().width - bounds.x;
		
		}
	}
}
