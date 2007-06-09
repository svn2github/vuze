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

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
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
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.ViewUtils.SpeeedAdapter;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

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
       extends TableViewTab
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
                  TableCountChangeListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
	private static final int ASYOUTYPE_MODE_FIND = 0;
	private static final int ASYOUTYPE_MODE_FILTER = 1;
	private static final int ASYOUTYPE_MODE = ASYOUTYPE_MODE_FILTER;
	private static final int ASYOUTYPE_UPDATEDELAY = 300;
	
	/** Expirimental Table UI.  When setting to true, some code needs 
	 *  uncommenting as well */
	private static final boolean EXPERIMENT = false;
	
	private AzureusCore		azureus_core;

  private GlobalManager globalManager;
  private boolean isSeedingView;

  private Composite cTablePanel;
  private Font fontButton = null;
  private Composite cCategories;
  private ControlAdapter catResizeAdapter;
  private Menu menuCategory;
  private MenuItem menuItemChangeDir = null;
  private DragSource dragSource = null;
  private DropTarget dropTarget = null;
  private Composite cHeader = null;
  private Label lblHeader = null;
  private Text txtFilter = null;
  private Label lblX = null;
  
  int userMode;
  boolean isTrackerOn;

  private Category currentCategory;

  // table item index, where the drag has started
  private int drag_drop_line_start = -1;

	private TimerEvent searchUpdateEvent;
  private String sLastSearch = "";
  private long lLastSearchTime;
  private boolean bRegexSearch = false;
	private boolean bDNDalwaysIncomplete;
	private TableViewSWT tv;
	private Composite cTableParentPanel;

  /**
   * Initialize
   * 
   * @param _azureus_core
   * @param isSeedingView
   * @param basicItems
   */
  public 
  MyTorrentsView(
  		AzureusCore			_azureus_core, 
		boolean 			isSeedingView,
        TableColumnCore[] 	basicItems) 
  {
  	if (EXPERIMENT) {
//      tv = new ListView(isSeedingView ? TableManager.TABLE_MYTORRENTS_COMPLETE
//  				: TableManager.TABLE_MYTORRENTS_INCOMPLETE, SWT.V_SCROLL);
//      tv.setColumnList(basicItems, "#", false);
  	} else {
      tv = new TableViewSWTImpl(isSeedingView
  				? TableManager.TABLE_MYTORRENTS_COMPLETE
  				: TableManager.TABLE_MYTORRENTS_INCOMPLETE, "MyTorrentsView",
  				basicItems, "#", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
  	}
    setTableView(tv);
    tv.setRowDefaultIconSize(new Point(16, 16));
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    this.isSeedingView 	= isSeedingView;

    currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);
    tv.addLifeCycleListener(this);
    tv.setMainPanelCreator(this);
    tv.addSelectionListener(this, false);
    tv.addMenuFillListener(this);
    tv.addRefreshListener(this, false);
    tv.addCountChangeListener(this);

    // experiment
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

    createDragDrop();

    COConfigurationManager.addAndFireParameterListeners(new String[] {
				"DND Always In Incomplete",
				"Confirm Data Delete",
				"User Mode" }, this);

    if (currentCategory != null) {
    	currentCategory.addCategoryListener(this);
    }
    CategoryManager.addCategoryManagerListener(this);
    globalManager.addListener(this, false);
    Object[] dms = globalManager.getDownloadManagers().toArray();
    for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = (DownloadManager) dms[i];
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
    CategoryManager.removeCategoryManagerListener(this);
    globalManager.removeListener(this);
    COConfigurationManager.removeParameterListener("DND Always In Incomplete", this);
    COConfigurationManager.removeParameterListener("Confirm Data Delete", this);
    COConfigurationManager.removeParameterListener("User Mode", this);
  }
  
  
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator#createTableViewPanel(org.eclipse.swt.widgets.Composite)
  public Composite createTableViewPanel(Composite composite) {
    GridData gridData;
    cTableParentPanel = new Composite(composite, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cTableParentPanel.setLayout(layout);
    if (composite.getLayout() instanceof GridLayout) {
    	cTableParentPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    
    if (EXPERIMENT) {
    	Composite cHeaders = new Composite(cTableParentPanel, SWT.NONE);
    	gridData = new GridData(GridData.FILL_HORIZONTAL);
    	gridData.horizontalSpan = 2;
    	GC gc = new GC(cHeaders);
    	int h = gc.textExtent("alyup").y + 2;
    	gc.dispose();
    	gridData.heightHint = h;
    	cHeaders.setLayoutData(gridData);
    	//((ListView)tv).setHeaderArea(cHeaders, null, null);
    }

    cTablePanel = new Composite(cTableParentPanel, SWT.NULL);
    cTablePanel.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    cTablePanel.setForeground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
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
    GridData gridData;
    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);
    boolean showCat = sLastSearch.length() > 0;
    if (!showCat)
	    for(int i = 0; i < categories.length; i++) {
	        if(categories[i].getType() == Category.TYPE_USER) {
	            showCat = true;
	            break;
	        }
	    }

    if (!showCat) {
    	if (cCategories != null && !cCategories.isDisposed()) {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
    	}
    } else {
      if (cCategories == null) {
        Composite parent = cTableParentPanel;

        cCategories = new Composite(parent, SWT.NONE);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        cCategories.setLayoutData(gridData);
        RowLayout rowLayout = new RowLayout();
        rowLayout.marginTop = 0;
        rowLayout.marginBottom = 0;
        rowLayout.marginLeft = 3;
        rowLayout.marginRight = 0;
        rowLayout.spacing = 0;
        rowLayout.wrap = true;
        cCategories.setLayout(rowLayout);

        cHeader = new Composite(parent, SWT.NONE);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalIndent = 5;
        cHeader.setLayoutData(gridData);
        GridLayout layout = new GridLayout();
        layout.numColumns = 6;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 2;
        layout.verticalSpacing = 0;
        cHeader.setLayout(layout);
        
        lblHeader = new Label(cHeader, SWT.WRAP);
        gridData = new GridData();
        lblHeader.setLayoutData(gridData);
        updateTableLabel();

        Label lblSep = new Label(cHeader, SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.FILL_VERTICAL);
        gridData.heightHint = 5;
        lblSep.setLayoutData(gridData);
        
        Label lblFilter = new Label(cHeader, SWT.WRAP);
        gridData = new GridData(GridData.BEGINNING);
        lblFilter.setLayoutData(gridData);
        Messages.setLanguageText(lblFilter, "MyTorrentsView.filter");

		lblX = new Label(cHeader, SWT.WRAP);
        Messages.setLanguageTooltip(lblX, "MyTorrentsView.clearFilter.tooltip");
        gridData = new GridData(SWT.TOP);
        lblX.setLayoutData(gridData);
        lblX.setImage(ImageRepository.getImage("smallx-gray"));
        lblX.addMouseListener(new MouseAdapter() {
        	public void mouseUp(MouseEvent e) {
        		if (e.y <= 10) {
          		sLastSearch = "";
          		updateLastSearch();
        		}
        	}
        });
        
        txtFilter = new Text(cHeader, SWT.BORDER);
        Messages.setLanguageTooltip(txtFilter, "MyTorrentsView.filter.tooltip");
        txtFilter.addKeyListener(this);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        txtFilter.setLayoutData(gridData);
        txtFilter.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		sLastSearch = ((Text)e.widget).getText();
        		updateLastSearch();
        	}
        });
        txtFilter.addKeyListener(new KeyAdapter() {
        	public void keyPressed(KeyEvent e) {
        		if (e.keyCode == SWT.ARROW_DOWN) {
        			tv.setFocus();
        			e.doit = false;
        		}
        	}
        });
        
        lblSep = new Label(cHeader, SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.FILL_VERTICAL);
        gridData.heightHint = 5;
        lblSep.setLayoutData(gridData);
        
        cHeader.moveAbove(null);
        cCategories.moveBelow(cHeader);
      } else {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
      }

      int iFontPixelsHeight = 11;
      int iFontPointHeight = (iFontPixelsHeight * 72) / cCategories.getDisplay().getDPI().y;
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
        if (catButton.computeSize(100,SWT.DEFAULT).y > 0) {
          RowData rd = new RowData();
          rd.height = catButton.computeSize(100,SWT.DEFAULT).y - 2 + catButton.getBorderWidth() * 2;
          catButton.setLayoutData(rd);
        }

        String name = category.getName();
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
            Button curButton = (Button)e.widget;
            boolean isEnabled = curButton.getSelection();
            Control[] controls = cCategories.getChildren();
            if (!isEnabled)
              curButton = (Button)controls[0];

            for (int i = 0; i < controls.length; i++) {
              Button b = (Button)controls[i];
              if (b != curButton && b.getSelection())
                b.setSelection(false);
              else if (b == curButton && !b.getSelection())
                b.setSelection(true);
            }
            activateCategory( (Category)curButton.getData("Category") );
          }
        });
        
        catButton.addListener(SWT.MouseHover, new Listener() {
        	public void handleEvent(Event event) {
            Button curButton = (Button)event.widget;
            Category curCategory = (Category)curButton.getData("Category");
            List dms = curCategory.getDownloadManagers();
            
            long ttlActive = 0;
            long ttlSize = 0;
            long ttlRSpeed = 0;
            long ttlSSpeed = 0;
            int count = 0;
            for (Iterator iter = dms.iterator(); iter.hasNext();) {
							DownloadManager dm = (DownloadManager) iter.next();
							
							if (!isInCategory(dm, currentCategory ))
								continue;
							
							count++;
							if (dm.getState() == DownloadManager.STATE_DOWNLOADING
									|| dm.getState() == DownloadManager.STATE_SEEDING)
								ttlActive++;
							ttlSize += dm.getSize();
							ttlRSpeed += dm.getStats().getDataReceiveRate();
							ttlSSpeed += dm.getStats().getDataSendRate();
						}

            String up_str 	= MessageText.getString( "GeneralView.label.maxuploadspeed" );
            String down_str = MessageText.getString( "GeneralView.label.maxdownloadspeed" );
            String unlimited_str = MessageText.getString( "MyTorrentsView.menu.setSpeed.unlimited" );
             
            int	up_speed 	= category.getUploadSpeed();
            int	down_speed 	= category.getDownloadSpeed();
            
            String up 	= up_str + ": " + (up_speed==0?unlimited_str:DisplayFormatters.formatByteCountToKiBEtc(up_speed));
            String down = down_str + ": " + (down_speed==0?unlimited_str:DisplayFormatters.formatByteCountToKiBEtc(down_speed));
            
            if (count == 0) {
            	curButton.setToolTipText( up + "\n" + down );
            	return;
            }
            
            curButton.setToolTipText(
            		up + "\n" + down + "\n" +
            		"Total: " + count + "\n"
            		+ "Downloading/Seeding: " + ttlActive + "\n"
            		+ "\n"
            		+ "Speed: "
            		+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlRSpeed / count) + "/" 
            		+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlSSpeed / count) + "\n"
            		+ "Size: " + DisplayFormatters.formatByteCountToKiBEtc(ttlSize));
        	}
        });

        final DropTarget tabDropTarget = new DropTarget(catButton, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
        Transfer[] types = new Transfer[] { TextTransfer.getInstance()};
        tabDropTarget.setTransfer(types);
        tabDropTarget.addDropListener(new DropTargetAdapter() {
          public void dragOver(DropTargetEvent e) {
            if(drag_drop_line_start >= 0)
              e.detail = DND.DROP_MOVE;
            else
              e.detail = DND.DROP_NONE;
          }

          public void drop(DropTargetEvent e) {
            e.detail = DND.DROP_NONE;
            //System.out.println("DragDrop on Button:" + drag_drop_line_start);
            if(drag_drop_line_start >= 0) {
              drag_drop_line_start = -1;

              assignSelectedToCategory((Category)catButton.getData("Category"));
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

        if ( category.getType() == Category.TYPE_USER ){
        	
        	final Menu menu = new Menu(getComposite().getShell(), SWT.POP_UP);
    
            catButton.setMenu(menu);
            
        	menu.addMenuListener(
        		new MenuListener() 
        		{
        	    	boolean bShown = false;
        	    	
        			public void 
        			menuHidden(
        				MenuEvent e )
        			{
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

        			public void 
        			menuShown(
        				MenuEvent e) 
        			{
        				MenuItem[] items = menu.getItems();
        				for (int i = 0; i < items.length; i++)
        					items[i].dispose();

        				bShown = true;

        		        final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
        		       
        		        Messages.setLanguageText(itemDelete, "MyTorrentsView.menu.category.delete");
        		        
        		        menu.setDefaultItem(itemDelete);

        				long maxDownload = COConfigurationManager.getIntParameter("Max Download Speed KBs", 0) * 1024;
        				long maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs", 0) * 1024;

           				int	down_speed 	= category.getDownloadSpeed();
           				int	up_speed 	= category.getUploadSpeed();
           			        				
        		        ViewUtils.addSpeedMenu( 
        		        		menu.getShell(), menu, true, 
        		        		false, down_speed==0, down_speed, down_speed, maxDownload, 
        		        		false, up_speed==0, up_speed, up_speed, maxUpload, 
        		        		1, 
        		        		new SpeeedAdapter()
        		        		{
        		        			public void 
        		        			setDownSpeed(int val) 
        		        			{
        		        				category.setDownloadSpeed( val );
        		        			}
        		        			public void 
        		        			setUpSpeed(int val) 
        		        			{
        		        				category.setUploadSpeed( val );

        		        			}
        		        		});

        		        itemDelete.addListener(SWT.Selection, new Listener() {
        		        	public void handleEvent(Event event) {
        		        		Category catToDelete = (Category)catButton.getData("Category");
        		        		if (catToDelete != null) {
        		        			java.util.List managers = catToDelete.getDownloadManagers();
        		        			// move to array,since setcategory removed it from the category,
        		        			// which would mess up our loop
        		        			DownloadManager dms[] = (DownloadManager [])managers.toArray(new DownloadManager[managers.size()]);
        		        			for (int i = 0; i < dms.length; i++) {
        		        				dms[i].getDownloadState().setCategory(null);
        		        			}
        		        			if (currentCategory == catToDelete){

        		        				activateCategory(CategoryManager.getCategory(Category.TYPE_ALL));

        		        			}else{
        		        				// always activate as deletion of this one might have
        		        				// affected the current view 
        		        				activateCategory(  currentCategory );
        		        			}
        		        			CategoryManager.removeCategory(catToDelete);
        		        		}
        			}
        		});
        				
 
            }
          });
        }
      }

      cCategories.layout();
      getComposite().layout();

      // layout hack - relayout
			if (catResizeAdapter == null) {
				catResizeAdapter = new ControlAdapter() {
					public void controlResized(ControlEvent event) {
						if (getComposite().isDisposed() || cCategories.isDisposed())
							return;

						GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);

						int parentWidth = cCategories.getParent().getClientArea().width;
						int catsWidth = cCategories.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
						// give text a 5 pixel right padding
						int textWidth = 5
								+ cHeader.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
								+ cHeader.getBorderWidth() * 2;

						Object layoutData = cHeader.getLayoutData();
						if (layoutData instanceof GridData) {
							GridData labelGrid = (GridData) layoutData;
							textWidth += labelGrid.horizontalIndent;
						}

						if (textWidth + catsWidth > parentWidth) {
							gridData.widthHint = parentWidth - textWidth;
						}
						cCategories.setLayoutData(gridData);
						cCategories.getParent().layout(true);

					}
				};

				tv.getTableComposite().addControlListener(catResizeAdapter);
			}

      catResizeAdapter.controlResized(null);
    }
  }
  
  private boolean isOurDownloadManager(DownloadManager dm) {
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

		if (bOurs && sLastSearch.length() > 0) {
			try {
				String[][] names = {	{"", 		dm.getDisplayName()},
												{"t:", 	dm.getTorrent().getAnnounceURL().getHost()},
												{"st:", 	"" + dm.getState()}
											};
				
				String name = names[0][1];
				String tmpSearch = sLastSearch;
				
				for(int i = 0; i < names.length; i++){
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

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void selected(TableRowCore[] rows) {
  	refreshIconBar();
  }

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
  	refreshIconBar();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
  	refreshIconBar();
	}

  private void refreshIconBar() {
  	computePossibleActions();
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.refreshIconBar();
  	}
  }

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void defaultSelected(TableRowCore[] rows) {
    DownloadManager dm = (DownloadManager)tv.getFirstSelectedDataSource();
    if (dm != null) {
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.openManagerView(dm);
	  	}
    }
  }

  private void fillTorrentMenu(final Menu menu, Object[] dms) {
		boolean hasSelection = (dms.length > 0);

		isTrackerOn = TRTrackerUtils.isTrackerEnabled();

		// Enable/Disable Logic


		boolean bChangeDir = hasSelection;

		boolean start, stop, changeUrl, barsOpened, forceStart;
		boolean forceStartEnabled, recheck, manualUpdate, fileMove, fileRescan;

		changeUrl = barsOpened = manualUpdate = fileMove = fileRescan = true;
		forceStart = forceStartEnabled = recheck = start = stop = false;

		boolean canSetSuperSeed	= false;
		boolean superSeedAllYes	= true;
		boolean superSeedAllNo	= true;
		
		boolean upSpeedDisabled = false;
		long totalUpSpeed = 0;
		boolean upSpeedUnlimited = false;
		long	upSpeedSetMax = 0;
		
		boolean downSpeedDisabled = false;
		long totalDownSpeed = 0;
		boolean downSpeedUnlimited = false;
		long	downSpeedSetMax	= 0;

		boolean	allScanSelected 	= true;
		boolean allScanNotSelected	= true;
		
		boolean	allStopped			= true;
		
		if (hasSelection) {
			for (int i = 0; i < dms.length; i++) {
				DownloadManager dm = (DownloadManager) dms[i];

				try {
					int maxul = dm.getStats().getUploadRateLimitBytesPerSecond();
					if (maxul == 0) {
						upSpeedUnlimited = true;
					}else{
						if ( maxul > upSpeedSetMax ){
							upSpeedSetMax	= maxul;
						}
					}
					if (maxul == -1) {
						maxul = 0;
						upSpeedDisabled = true;
					}
					totalUpSpeed += maxul;

					int maxdl = dm.getStats().getDownloadRateLimitBytesPerSecond();
					if (maxdl == 0) {
						downSpeedUnlimited = true;
					}else{
						if ( maxdl > downSpeedSetMax ){
							downSpeedSetMax	= maxdl;
						}
					}
					if (maxdl == -1) {
						maxdl = 0;
						downSpeedDisabled = true;
					}
					totalDownSpeed += maxdl;

				} catch (Exception ex) {
					Debug.printStackTrace(ex);
				}

				if (dm.getTrackerClient() == null) {
					changeUrl = false;
				}

				if (barsOpened && !DownloadBar.getManager().isOpen(dm)) {
					barsOpened = false;
				}

				stop = stop || ManagerUtils.isStopable(dm);

				start = start || ManagerUtils.isStartable(dm);

				recheck = recheck || dm.canForceRecheck();

				forceStartEnabled = forceStartEnabled
						|| ManagerUtils.isForceStartable(dm);

				forceStart = forceStart || dm.isForceStart();

				boolean	stopped = ManagerUtils.isStopped(dm);
				
				allStopped &= stopped;
					
				fileMove = fileMove && stopped && dm.isPersistent();

				if (userMode > 1) {
					TRTrackerAnnouncer trackerClient = dm.getTrackerClient();

					if (trackerClient != null) {
						boolean update_state = ((SystemTime.getCurrentTime() / 1000
								- trackerClient.getLastUpdateTime() >= TRTrackerAnnouncer.REFRESH_MINIMUM_SECS));
						manualUpdate = manualUpdate & update_state;
					}

				}
				int state = dm.getState();
				bChangeDir &= (state == DownloadManager.STATE_ERROR
						|| state == DownloadManager.STATE_STOPPED || state == DownloadManager.STATE_QUEUED)
						&& dm.isDownloadComplete(false);
				
				/**
				 * Only perform a test on disk if:
				 *    1) We are currently set to allow the "Change Data Directory" option, and
				 *    2) We've only got one item selected - otherwise, we may potentially end up checking massive
				 *       amounts of files across multiple torrents before we generate a menu.
				 */
				if (bChangeDir && dms.length == 1) {
					bChangeDir = !dm.filesExist();
				}
				
				boolean	scan = dm.getDownloadState().getFlag( DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES );
				
				// include DND files in incomplete stat, since a recheck may
				// find those files have been completed
				boolean	incomplete = !dm.isDownloadComplete(true);
				
				allScanSelected 	= incomplete && allScanSelected && scan;
				allScanNotSelected 	= incomplete && allScanNotSelected && !scan;
				
				PEPeerManager pm = dm.getPeerManager();
				
				if ( pm != null ){
					
					if ( pm.canToggleSuperSeedMode()){
						
						canSetSuperSeed	= true;
					}
					
					if ( pm.isSuperSeedMode()){
						
						superSeedAllYes = false;
						
					}else{
						
						superSeedAllNo	= false;
					}
				}else{
					superSeedAllYes = false;
					superSeedAllNo	= false;
				}
			}

			fileRescan	= allScanSelected || allScanNotSelected;
			
		} else { // empty right-click
			barsOpened = false;
			forceStart = false;
			forceStartEnabled = false;

			start = false;
			stop = false;
			fileMove = false;
			fileRescan	= false;
			upSpeedDisabled = true;
			downSpeedDisabled = true;
			changeUrl = false;
			recheck = false;
			manualUpdate = false;
		}

		// === Root Menu ===

		if (bChangeDir) {
			menuItemChangeDir = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(menuItemChangeDir,
					"MyTorrentsView.menu.changeDirectory");
			menuItemChangeDir.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					changeDirSelectedTorrents();
				}
			});
		}


		// Open Details
		final MenuItem itemDetails = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemDetails, "MyTorrentsView.menu.showdetails");
		menu.setDefaultItem(itemDetails);
		Utils.setMenuItemImage(itemDetails, "details");
		itemDetails.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
		  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		  	if (uiFunctions != null) {
					uiFunctions.openManagerView((DownloadManager) row.getDataSource(true));
				}
			}
		});
		itemDetails.setEnabled(hasSelection);

		// Open Bar
		final MenuItem itemBar = new MenuItem(menu, SWT.CHECK);
		Messages.setLanguageText(itemBar, "MyTorrentsView.menu.showdownloadbar");
		Utils.setMenuItemImage(itemBar, "downloadBar");
		itemBar.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				DownloadManager dm = (DownloadManager) row.getDataSource(true);
				if (DownloadBar.getManager().isOpen(dm)) {
					DownloadBar.close(dm);
				} else {
					DownloadBar.open(dm, cTablePanel.getShell());
				}
			} // run
		});
		itemBar.setEnabled(hasSelection);
		itemBar.setSelection(barsOpened);


		// ---
		new MenuItem(menu, SWT.SEPARATOR);

		// Run Data File
		final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemOpen, "MyTorrentsView.menu.open");
		Utils.setMenuItemImage(itemOpen, "run");
		itemOpen.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				runSelectedTorrents();
			}
		});
		itemOpen.setEnabled(hasSelection);

		// Explore
		final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemExplore, "MyTorrentsView.menu.explore");
		itemExplore.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				openSelectedTorrents();
			}
		});
		itemExplore.setEnabled(hasSelection);

		// === advanced menu ===

		final MenuItem itemAdvanced = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemAdvanced, "MyTorrentsView.menu.advancedmenu"); //$NON-NLS-1$
		itemAdvanced.setEnabled(hasSelection);

		final Menu menuAdvanced = new Menu(menu.getShell(), SWT.DROP_DOWN);
		itemAdvanced.setMenu(menuAdvanced);

		// advanced > Download Speed Menu //

		long maxDownload = COConfigurationManager.getIntParameter("Max Download Speed KBs", 0) * 1024;
		long maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs", 0) * 1024;
		
		ViewUtils.addSpeedMenu(
			menu.getShell(),
			menuAdvanced,
			hasSelection,
			downSpeedDisabled,
			downSpeedUnlimited,
			totalDownSpeed,
			downSpeedSetMax,
			maxDownload,
			upSpeedDisabled,
			upSpeedUnlimited,
			totalUpSpeed,
			upSpeedSetMax,
			maxUpload,
			dms.length,
			new ViewUtils.SpeeedAdapter()
			{
				public void 
				setDownSpeed(
					int speed ) 
				{
					setSelectedTorrentsDownSpeed( speed );	
				}
				
				public void 
				setUpSpeed(
					int speed ) 
				{
					setSelectedTorrentsUpSpeed( speed );
				}
			});
				
		// advanced > Tracker Menu //
		final Menu menuTracker = new Menu(menu.getShell(), SWT.DROP_DOWN);
		final MenuItem itemTracker = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemTracker, "MyTorrentsView.menu.tracker");
		itemTracker.setMenu(menuTracker);

		final MenuItem itemChangeTracker = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemChangeTracker,
				"MyTorrentsView.menu.changeTracker"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemChangeTracker, "add_tracker");
		itemChangeTracker.addListener(SWT.Selection,
				new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						TRTrackerAnnouncer tc = ((DownloadManager) row.getDataSource(true))
								.getTrackerClient();
						if (tc != null)
							new TrackerChangerWindow(getComposite().getDisplay(), tc);
					}
				});
		itemChangeTracker.setEnabled(changeUrl);


		final MenuItem itemEditTracker = new MenuItem(menuTracker, SWT.PUSH);
		Messages
				.setLanguageText(itemEditTracker, "MyTorrentsView.menu.editTracker");
		Utils.setMenuItemImage(itemEditTracker, "edit_trackers");
		itemEditTracker.addListener(SWT.Selection,
				new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
				final DownloadManager dm = (DownloadManager) row.getDataSource(true);
				if (dm.getTorrent() != null) {
					final TOTorrent torrent = dm.getTorrent();

					java.util.List group = TorrentUtils.announceGroupsToList(torrent);

					new MultiTrackerEditor(null, group, new TrackerEditorListener() {
						public void trackersChanged(String str, String str2,
								java.util.List group) {
							TorrentUtils.listToAnnounceGroups(group, torrent);

							try {
								TorrentUtils.writeToFile(torrent);
							} catch (Throwable e) {
								Debug.printStackTrace(e);
							}

							if (dm.getTrackerClient() != null)
								dm.getTrackerClient().resetTrackerUrl(true);
						}
					}, true);
				}
			} // run
		});
		itemEditTracker.setEnabled(hasSelection);

		final MenuItem itemManualUpdate = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemManualUpdate,
				"GeneralView.label.trackerurlupdate"); //$NON-NLS-1$
		//itemManualUpdate.setImage(ImageRepository.getImage("edit_trackers"));
		itemManualUpdate.addListener(SWT.Selection,
				new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						((DownloadManager) row.getDataSource(true)).requestTrackerAnnounce(false);
					}
				});
		itemManualUpdate.setEnabled(manualUpdate);

		boolean	scrape_enabled = COConfigurationManager.getBooleanParameter("Tracker Client Scrape Enable");
		
		boolean scrape_stopped = COConfigurationManager.getBooleanParameter("Tracker Client Scrape Stopped Enable");
		
		boolean manualScrape = 
			(!scrape_enabled) ||
			((!scrape_stopped) && allStopped );
		
		final MenuItem itemManualScrape = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemManualScrape,
				"GeneralView.label.trackerscrapeupdate");
		//itemManualUpdate.setImage(ImageRepository.getImage("edit_trackers"));
		itemManualScrape.addListener(SWT.Selection,
				new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						((DownloadManager) row.getDataSource(true)).requestTrackerScrape(true);
					}
				});
		itemManualScrape.setEnabled(manualScrape);

		// advanced > files

		final MenuItem itemFiles = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemFiles, "ConfigView.section.files");

		final Menu menuFiles = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		itemFiles.setMenu(menuFiles);

		final MenuItem itemFileMoveData = new MenuItem(menuFiles, SWT.PUSH);
		Messages.setLanguageText(itemFileMoveData, "MyTorrentsView.menu.movedata");
		itemFileMoveData.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Object[] dms = tv.getSelectedDataSources();

				if (dms != null && dms.length > 0) {

					DirectoryDialog dd = new DirectoryDialog(getComposite().getShell());

					dd.setFilterPath(TorrentOpener.getFilterPathData());

					dd.setText(MessageText
							.getString("MyTorrentsView.menu.movedata.dialog"));

					String path = dd.open();

					if (path != null) {

						TorrentOpener.setFilterPathData(path);

						File target = new File(path);

						for (int i = 0; i < dms.length; i++) {

							try {
								((DownloadManager) dms[i]).moveDataFiles(target);

							} catch (Throwable e) {

								Logger.log(new LogAlert(dms[i], LogAlert.REPEATABLE,
										"Download data move operation failed", e));
							}
						}
					}
				}
			}
		});
		itemFileMoveData.setEnabled(fileMove);
		
		final MenuItem itemFileMoveTorrent = new MenuItem(menuFiles, SWT.PUSH);
		Messages.setLanguageText(itemFileMoveTorrent,
				"MyTorrentsView.menu.movetorrent");
		itemFileMoveTorrent.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Object[] dms = tv.getSelectedDataSources();

				if (dms != null && dms.length > 0) {

					DirectoryDialog dd = new DirectoryDialog(getComposite().getShell());

					dd.setFilterPath(TorrentOpener.getFilterPathTorrent());

					dd.setText(MessageText
							.getString("MyTorrentsView.menu.movedata.dialog"));

					String path = dd.open();

					if (path != null) {

						File target = new File(path);

						TorrentOpener.setFilterPathTorrent(target.toString());

						for (int i = 0; i < dms.length; i++) {

							try {
								((DownloadManager) dms[i]).moveTorrentFile(target);

							} catch (Throwable e) {

								Logger.log(new LogAlert(dms[i], LogAlert.REPEATABLE,
										"Download torrent move operation failed", e));
							}
						}
					}
				}
			}
		});		
		itemFileMoveTorrent.setEnabled(fileMove);
		
		final MenuItem itemFileRescan = new MenuItem(menuFiles, SWT.CHECK );
		Messages.setLanguageText(itemFileRescan,
				"MyTorrentsView.menu.rescanfile");
		itemFileRescan.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				DownloadManager dm = (DownloadManager) row.getDataSource(true);
				
				dm.getDownloadState().setFlag( 
						DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES,
						itemFileRescan.getSelection());
			}
		});

		itemFileRescan.setSelection( allScanSelected );
		itemFileRescan.setEnabled( fileRescan );
		
		// === advanced > export ===
		// =========================

		if (userMode > 0) {
			final MenuItem itemExport = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemExport, "MyTorrentsView.menu.exportmenu"); //$NON-NLS-1$
			Utils.setMenuItemImage(itemExport, "export");
			itemExport.setEnabled(hasSelection);
	
			final Menu menuExport = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
			itemExport.setMenu(menuExport);
	
			// Advanced > Export > Export XML
			final MenuItem itemExportXML = new MenuItem(menuExport, SWT.PUSH);
			Messages.setLanguageText(itemExportXML, "MyTorrentsView.menu.export");
			itemExportXML.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					DownloadManager dm = (DownloadManager) tv.getFirstSelectedDataSource();
					if (dm != null)
						new ExportTorrentWizard(azureus_core, itemExportXML.getDisplay(), dm);
				}
			});
	
			// Advanced > Export > Export Torrent
			final MenuItem itemExportTorrent = new MenuItem(menuExport, SWT.PUSH);
			Messages.setLanguageText(itemExportTorrent,
					"MyTorrentsView.menu.exporttorrent");
			itemExportTorrent.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					DownloadManager dm = (DownloadManager) tv.getFirstSelectedDataSource();
					if (dm != null) {
						FileDialog fd = new FileDialog(getComposite().getShell(), SWT.SAVE );
	
						fd.setFileName(dm.getTorrentFileName());
	
						String path = fd.open();
	
						if (path != null) {
	
							try {
								File target = new File(path);
	
								if ( target.exists()){
									
									MessageBox mb = new MessageBox(getComposite().getShell(),SWT.ICON_QUESTION | SWT.YES | SWT.NO);
									
									mb.setText(MessageText.getString("exportTorrentWizard.process.outputfileexists.title"));
									
									mb.setMessage(MessageText.getString("exportTorrentWizard.process.outputfileexists.message"));
									
									int result = mb.open();
								
									if( result == SWT.NO ){
										
										return;
									}
									
									if ( !target.delete()){
										
										throw( new Exception( "Failed to delete file" ));
									}
								}
								
								// first copy the torrent - DON'T use "writeTorrent" as this amends the
								// "filename" field in the torrent
	
								TorrentUtils.copyToFile(dm.getDownloadState().getTorrent(),	target);
	
								// now remove the non-standard entries
	
								TOTorrent dest = TOTorrentFactory
										.deserialiseFromBEncodedFile(target);
	
								dest.removeAdditionalProperties();
	
								dest.serialiseToBEncodedFile(target);
	
							} catch (Throwable e) {
								Logger.log(new LogAlert(dm, LogAlert.UNREPEATABLE,
										"Torrent export failed", e));
							}
	
						}
					}
				}
			});
		} // export menu

		// === advanced > peer sources ===
		// ===============================

		if (userMode > 0) {
			final MenuItem itemPeerSource = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemPeerSource, "MyTorrentsView.menu.peersource"); //$NON-NLS-1$
	
			final Menu menuPeerSource = new Menu(getComposite().getShell(),
					SWT.DROP_DOWN);
			itemPeerSource.setMenu(menuPeerSource);
	
			for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
	
				final String p = PEPeerSource.PS_SOURCES[i];
				String msg_text = "ConfigView.section.connection.peersource." + p;
				final MenuItem itemPS = new MenuItem(menuPeerSource, SWT.CHECK);
				itemPS.setData("peerSource", p);
				Messages.setLanguageText(itemPS, msg_text); //$NON-NLS-1$
				itemPS.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						((DownloadManager) row.getDataSource(true)).getDownloadState()
								.setPeerSourceEnabled(p, itemPS.getSelection());
					}
				});
				itemPS.setSelection(true);
				
				boolean bChecked = hasSelection;
				boolean bEnabled = !hasSelection;
				if (bChecked) {
					bEnabled = true;
					
					// turn on check if just one dm is not enabled
					for (int j = 0; j < dms.length; j++) {
						DownloadManager dm = (DownloadManager) dms[j];
						
						if (!dm.getDownloadState().isPeerSourceEnabled(p)) {
							bChecked = false;
						}
						if (!dm.getDownloadState().isPeerSourcePermitted(p)) {
							bEnabled = false;
						}
					}
				}
				
				itemPS.setSelection(bChecked);
				itemPS.setEnabled(bEnabled);
			}
		}
		

		// === advanced > networks ===
		// ===========================

		if (userMode > 1) {
			final MenuItem itemNetworks = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemNetworks, "MyTorrentsView.menu.networks"); //$NON-NLS-1$
	
			final Menu menuNetworks = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
			itemNetworks.setMenu(menuNetworks);
	
			for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
				final String nn = AENetworkClassifier.AT_NETWORKS[i];
				String msg_text = "ConfigView.section.connection.networks." + nn;
				final MenuItem itemNetwork = new MenuItem(menuNetworks, SWT.CHECK);
				itemNetwork.setData("network", nn);
				Messages.setLanguageText(itemNetwork, msg_text); //$NON-NLS-1$
				itemNetwork.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						((DownloadManager) row.getDataSource(true)).getDownloadState()
								.setNetworkEnabled(nn, itemNetwork.getSelection());
					}
				});
				boolean bChecked = hasSelection;
				if (bChecked) {
					// turn on check if just one dm is not enabled
					for (int j = 0; j < dms.length; j++) {
						DownloadManager dm = (DownloadManager) dms[j];
						
						if (!dm.getDownloadState().isNetworkEnabled(nn)) {
							bChecked = false;
							break;
						}
					}
				}
				
				itemNetwork.setSelection(bChecked);
			}
		}

			// superseed
		if ( userMode > 1 && isSeedingView ){
			
			final MenuItem itemSuperSeed = new MenuItem(menuAdvanced, SWT.CHECK);

			Messages.setLanguageText(itemSuperSeed, "ManagerItem.superseeding"); 
			
			boolean enabled = canSetSuperSeed && ( superSeedAllNo || superSeedAllYes );
			
			itemSuperSeed.setEnabled( enabled );
			
			final boolean	selected = superSeedAllNo;
			
			if ( enabled ){
				
				itemSuperSeed.setSelection( selected );
				
				itemSuperSeed.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						DownloadManager dm = (DownloadManager) row.getDataSource(true);
						
						PEPeerManager pm = dm.getPeerManager();
						
						if ( pm != null ){
							
							if ( 	pm.isSuperSeedMode() == selected &&
									pm.canToggleSuperSeedMode()){
								
								pm.setSuperSeedMode( !selected );
							}
						}
					}
				});
			}
		}

		final MenuItem itemPositionManual = new MenuItem(menuAdvanced, SWT.PUSH);
		Messages.setLanguageText(itemPositionManual,
				"MyTorrentsView.menu.reposition.manual");
		itemPositionManual.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputShell is = new InputShell(
						"MyTorrentsView.dialog.setPosition.title",
						"MyTorrentsView.dialog.setPosition.text");

				String sReturn = is.open();
				if (sReturn == null)
					return;

				int newPosition = -1;
				try {
					newPosition = Integer.valueOf(sReturn).intValue();
				} catch (NumberFormatException er) {
					// Ignore
				}

				int size = globalManager.downloadManagerCount(isSeedingView);
				if (newPosition > size)
					newPosition = size;

				if (newPosition <= 0) {
					MessageBox mb = new MessageBox(getComposite().getShell(),
							SWT.ICON_ERROR | SWT.OK);
					mb.setText(MessageText
							.getString("MyTorrentsView.dialog.NumberError.title"));
					mb.setMessage(MessageText
							.getString("MyTorrentsView.dialog.NumberError.text"));

					mb.open();
					return;
				}

				moveSelectedTorrentsTo(newPosition);
			}
		});

		// back to main menu

		if (userMode > 0 && isTrackerOn) {
			// Host
			final MenuItem itemHost = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemHost, "MyTorrentsView.menu.host");
			Utils.setMenuItemImage(itemHost, "host");
			itemHost.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					hostSelectedTorrents();
				}
			});
	
			// Publish
			final MenuItem itemPublish = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemPublish, "MyTorrentsView.menu.publish");
			Utils.setMenuItemImage(itemPublish, "publish");
			itemPublish.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					publishSelectedTorrents();
				}
			});

			itemHost.setEnabled(hasSelection);
			itemPublish.setEnabled(hasSelection);
		}
/*  Do we really need the Move submenu?  There's shortcut keys and toolbar
 *  buttons..

		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem itemMove = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemMove, "MyTorrentsView.menu.move");
		Utils.setMenuItemImage(itemMove, "move");
		itemMove.setEnabled(hasSelection);

		final Menu menuMove = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		itemMove.setMenu(menuMove);

		final MenuItem itemMoveTop = new MenuItem(menuMove, SWT.PUSH);
		Messages.setLanguageText(itemMoveTop, "MyTorrentsView.menu.moveTop");
		Utils.setMenuItemImage(itemMoveTop, "top");
		itemMoveTop.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				moveSelectedTorrentsTop();
			}
		});
		itemMoveTop.setEnabled(moveUp);

		final MenuItem itemMoveUp = new MenuItem(menuMove, SWT.PUSH);
		Messages.setLanguageText(itemMoveUp, "MyTorrentsView.menu.moveUp");
		Utils.setMenuItemImage(itemMoveUp, "up");
		itemMoveUp.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				moveSelectedTorrentsUp();
			}
		});

		final MenuItem itemMoveDown = new MenuItem(menuMove, SWT.PUSH);
		Messages.setLanguageText(itemMoveDown, "MyTorrentsView.menu.moveDown");
		Utils.setMenuItemImage(itemMoveDown, "down");
		itemMoveDown.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				moveSelectedTorrentsDown();
			}
		});

		final MenuItem itemMoveEnd = new MenuItem(menuMove, SWT.PUSH);
		Messages.setLanguageText(itemMoveEnd, "MyTorrentsView.menu.moveEnd");
		Utils.setMenuItemImage(itemMoveEnd, "bottom");
		itemMoveEnd.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				moveSelectedTorrentsEnd();
			}
		});
		itemMoveEnd.setEnabled(moveDown);
*/
		/*  //TODO ensure that all limits combined don't go under the min 5kbs ?
		 //Disable at the end of the list, thus the first item of the array is instanciated last.
		 itemsSpeed[0] = new MenuItem(menuSpeed,SWT.PUSH);
		 Messages.setLanguageText(itemsSpeed[0],"MyTorrentsView.menu.setSpeed.disable");
		 itemsSpeed[0].setData("maxul", new Integer(-1));    
		 itemsSpeed[0].addListener(SWT.Selection,itemsSpeedListener);
		 */

		// Category
		menuCategory = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
		//itemCategory.setImage(ImageRepository.getImage("speed"));
		itemCategory.setMenu(menuCategory);
		itemCategory.setEnabled(hasSelection);

		addCategorySubMenu();

        // Rename
        final MenuItem itemRename = new MenuItem(menu, SWT.CASCADE);
        Messages.setLanguageText(itemRename, "MyTorrentsView.menu.rename");
        itemRename.setEnabled(hasSelection);
        
        final Menu menuRename = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
        itemRename.setMenu(menuRename);
        
        DownloadManager first_selected = ((DownloadManager)tv.getFirstSelectedDataSource());
        
        // Rename -> Displayed Name
        final MenuItem itemRenameDisplayed = new MenuItem(menuRename, SWT.CASCADE);
        Messages.setLanguageText(itemRenameDisplayed, "MyTorrentsView.menu.rename.displayed");
        itemRenameDisplayed.setEnabled(hasSelection);
        if (itemRenameDisplayed.isEnabled()) {
        	itemRenameDisplayed.setData("suggested_text", first_selected.getDisplayName());
        	itemRenameDisplayed.setData("display_name", Boolean.valueOf(true));
        	itemRenameDisplayed.setData("save_name", Boolean.valueOf(false));
        	itemRenameDisplayed.setData("msg_key", "displayed");
        }
        
        // Rename -> Save Name
        final MenuItem itemRenameSavePath = new MenuItem(menuRename, SWT.CASCADE);
        Messages.setLanguageText(itemRenameSavePath, "MyTorrentsView.menu.rename.save_path");
        itemRenameSavePath.setEnabled(fileMove && dms.length == 1);
        if (itemRenameSavePath.isEnabled()) {
        	itemRenameSavePath.setData("suggested_text", first_selected.getAbsoluteSaveLocation().getName());
        	itemRenameSavePath.setData("display_name", Boolean.valueOf(false));
        	itemRenameSavePath.setData("save_name", Boolean.valueOf(true));
        	itemRenameSavePath.setData("msg_key", "save_path");
        }

        
        // Rename -> Both
        final MenuItem itemRenameBoth = new MenuItem(menuRename, SWT.CASCADE);
        Messages.setLanguageText(itemRenameBoth, "MyTorrentsView.menu.rename.displayed_and_save_path");
        itemRenameBoth.setEnabled(fileMove && dms.length == 1);
        if (itemRenameBoth.isEnabled()) {
        	itemRenameBoth.setData("suggested_text", first_selected.getAbsoluteSaveLocation().getName());
        	itemRenameBoth.setData("display_name", Boolean.valueOf(true));
        	itemRenameBoth.setData("save_name", Boolean.valueOf(true));
        	itemRenameBoth.setData("msg_key", "displayed_and_save_path");
        }
        
        Listener rename_listener = new Listener() {
        	public void handleEvent(Event event) {
        		MenuItem mi = (MenuItem)event.widget;
        		String suggested = (String)mi.getData("suggested_text");
        		final boolean change_displayed_name = ((Boolean)mi.getData("display_name")).booleanValue();
        		final boolean change_save_name = ((Boolean)mi.getData("save_name")).booleanValue();
        		String msg_key_prefix = "MyTorrentsView.menu.rename." + (String)mi.getData("msg_key") + ".enter.";
        		SimpleTextEntryWindow text_entry = new SimpleTextEntryWindow(getComposite().getDisplay());
        		text_entry.setTitle(msg_key_prefix + "title");
        		text_entry.setMessages(new String[]{msg_key_prefix + "message", msg_key_prefix + "message.2"});
        		text_entry.setPreenteredText(suggested, false);
        		text_entry.prompt();
        		if (text_entry.hasSubmittedInput()) {
        			String value = text_entry.getSubmittedInput();
        			final String value_to_set = (value.length() == 0) ? null : value;
        			tv.runForSelectedRows(new TableGroupRowRunner() {
                        public void run(TableRowCore row) {
                        	DownloadManager dm = (DownloadManager)row.getDataSource(true);
                        	if (change_displayed_name) {
                        		dm.getDownloadState().setDisplayName(value_to_set);
                        	}
                            if (change_save_name) {
                            	try {dm.renameDownload((value_to_set==null) ? dm.getDisplayName() : value_to_set);}
                            	catch (Exception e) {
                                    Logger.log(new LogAlert(dm, LogAlert.REPEATABLE,
                                            "Download data rename operation failed", e));
                            	}
                            }
                        }
                    });
        		}
        	}
        };
        
        itemRenameDisplayed.addListener(SWT.Selection, rename_listener);
        itemRenameSavePath.addListener(SWT.Selection, rename_listener);
        itemRenameBoth.addListener(SWT.Selection, rename_listener);

        // Edit Comment
        final MenuItem itemEditComment = new MenuItem(menu, SWT.CASCADE);
        Messages.setLanguageText(itemEditComment, "MyTorrentsView.menu.edit_comment");
        itemEditComment.setEnabled(dms.length > 0);
        if (itemEditComment.isEnabled()) {
        	itemEditComment.setData("suggested_text", first_selected.getDownloadState().getUserComment());
        }
        
        Listener edit_comment_listener = new Listener() {
        	public void handleEvent(Event event) {
        		MenuItem mi = (MenuItem)event.widget;
        		String suggested = (String)mi.getData("suggested_text");
        		String msg_key_prefix = "MyTorrentsView.menu.edit_comment.enter.";
        		SimpleTextEntryWindow text_entry = new SimpleTextEntryWindow(getComposite().getDisplay());
        		text_entry.setTitle(msg_key_prefix + "title");
        		text_entry.setMessage(msg_key_prefix + "message");
        		text_entry.setPreenteredText(suggested, false);
        		text_entry.prompt();
        		if (text_entry.hasSubmittedInput()) {
        			String value = text_entry.getSubmittedInput();
        			final String value_to_set = (value.length() == 0) ? null : value;
        			tv.runForSelectedRows(new TableGroupRowRunner() {
                        public void run(TableRowCore row) {
                        	((DownloadManager)row.getDataSource(true)).getDownloadState().setUserComment(value_to_set);
                        }
        			});
        		}
        	}
        };  
        
        itemEditComment.addListener(SWT.Selection, edit_comment_listener);
        
		// ---
		new MenuItem(menu, SWT.SEPARATOR);

		// Queue
		final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemQueue, "start");
		itemQueue.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				queueSelectedTorrents();
			}
		});
		itemQueue.setEnabled(start);

		// Force Start
		if (userMode > 0) {
			final MenuItem itemForceStart = new MenuItem(menu, SWT.CHECK);
			Messages.setLanguageText(itemForceStart, "MyTorrentsView.menu.forceStart");
			Utils.setMenuItemImage(itemForceStart, "forcestart");
			itemForceStart.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
				public void run(TableRowCore row) {
					DownloadManager dm = (DownloadManager) row.getDataSource(true);
	
					if (ManagerUtils.isForceStartable(dm)) {
						dm.setForceStart(itemForceStart.getSelection());
					}
				}
			});
			itemForceStart.setSelection(forceStart);
			itemForceStart.setEnabled(forceStartEnabled);
		}

		// Stop
		final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemStop, "stop");
		itemStop.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				stopSelectedTorrents();
			}
		});
		itemStop.setEnabled(stop);

		// Force Recheck
		final MenuItem itemRecheck = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRecheck, "MyTorrentsView.menu.recheck");
		Utils.setMenuItemImage(itemRecheck, "recheck");
		itemRecheck.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				DownloadManager dm = (DownloadManager) row.getDataSource(true);

				if (dm.canForceRecheck()) {

					dm.forceRecheck();
				}
			}
		});
		itemRecheck.setEnabled(recheck);
		
		// Remove
		final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemRemove, "delete");
		itemRemove.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				removeTorrent((DownloadManager) row.getDataSource(true), false, false);
			}
		});
		itemRemove.setEnabled(hasSelection);

		// === Remove And ===
		// ==================
		
		final MenuItem itemRemoveAnd = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemRemoveAnd, "MyTorrentsView.menu.removeand"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemRemoveAnd, "delete");
		itemRemoveAnd.setEnabled(hasSelection);

		final Menu menuRemove = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		itemRemoveAnd.setMenu(menuRemove);

		// Remove And > Delete Torrent
		final MenuItem itemDeleteTorrent = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteTorrent,
				"MyTorrentsView.menu.removeand.deletetorrent"); //$NON-NLS-1$
		itemDeleteTorrent.addListener(SWT.Selection,
				new TableSelectedRowsListener(tv) {
					public void run(TableRowCore row) {
						removeTorrent((DownloadManager) row.getDataSource(true), true,
								false);
					}
				});

		// Remove And > Delete Data
		final MenuItem itemDeleteData = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteData,
				"MyTorrentsView.menu.removeand.deletedata");
		itemDeleteData.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				removeTorrent((DownloadManager) row.getDataSource(true), false, true);
			}
		});

		// Remove And > Delete Both
		final MenuItem itemDeleteBoth = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteBoth,
				"MyTorrentsView.menu.removeand.deleteboth");
		itemDeleteBoth.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				removeTorrent((DownloadManager) row.getDataSource(true), true, true);
			}
		});
  }

  // @see org.gudy.azureus2.ui.swt.views.TableViewSWTMenuFillListener#fillMenu(org.eclipse.swt.widgets.Menu)
  public void fillMenu(final Menu menu) {
		Object[] dms = tv.getSelectedDataSources();
		boolean hasSelection = (dms.length > 0);

		if (hasSelection) {
			fillTorrentMenu(menu, dms);

			// ---
			new MenuItem(menu, SWT.SEPARATOR);
		}
		
		final MenuItem itemFilter = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemFilter, "MyTorrentsView.menu.filter");
		itemFilter.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				openFilterDialog();
			}
		});
	} 

  private void addCategorySubMenu() {
    MenuItem[] items = menuCategory.getItems();
    int i;
    for (i = 0; i < items.length; i++) {
      items[i].dispose();
    }

    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);

    if (categories.length > 0) {
      Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
      if (catUncat != null) {
        final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
        Messages.setLanguageText(itemCategory, catUncat.getName());
        itemCategory.setData("Category", catUncat);
        itemCategory.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event event) {
            MenuItem item = (MenuItem)event.widget;
            assignSelectedToCategory((Category)item.getData("Category"));
          }
        });

        new MenuItem(menuCategory, SWT.SEPARATOR);
      }

      for (i = 0; i < categories.length; i++) {
    	Category category = categories[i];
        if (category.getType() == Category.TYPE_USER) {
          final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
          itemCategory.setText(category.getName());
          itemCategory.setData("Category", category);

          itemCategory.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
              MenuItem item = (MenuItem)event.widget;
              assignSelectedToCategory((Category)item.getData("Category"));
            }
          });
        }
      }

      new MenuItem(menuCategory, SWT.SEPARATOR);
    }

    final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
    Messages.setLanguageText(itemAddCategory,
                             "MyTorrentsView.menu.setCategory.add");

    itemAddCategory.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        addCategory();
      }
    });

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

    } else if (sColumnName.equals("maxuploads")) {
      int iStart = COConfigurationManager.getIntParameter("Max Uploads") - 2;
      if (iStart < 2) iStart = 2;
      for (int i = iStart; i < iStart + 6; i++) {
        MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
        item.setText(String.valueOf(i));
        item.setData("MaxUploads", new Long(i));
        item.addListener(SWT.Selection,
                         new TableSelectedRowsListener(tv) {
          public void run(TableRowCore row) {
            DownloadManager dm = (DownloadManager)row.getDataSource(true);
            MenuItem item = (MenuItem)event.widget;
            if (item != null) {
              int value = ((Long)item.getData("MaxUploads")).intValue();
              dm.setMaxUploads(value);
            }
          } // run
        }); // listener
      } // for
    }
  }

  private void createDragDrop() {
    try {

			Transfer[] types = new Transfer[] { TextTransfer.getInstance()
			};

			if (dragSource != null && !dragSource.isDisposed()) {
				dragSource.dispose();
			}

			if (dropTarget != null && !dropTarget.isDisposed()) {
				dropTarget.dispose();
			}

			dragSource = tv.createDragSource(DND.DROP_MOVE);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					public void dragStart(DragSourceEvent event) {
						TableRowCore[] rows = tv.getSelectedRows();
						if (rows.length != 0) {
							event.doit = true;
							//System.out.println("DragStart"); 
							drag_drop_line_start = rows[0].getIndex();
						} else {
							event.doit = false;
							drag_drop_line_start = -1;
						}
					}

					public void dragSetData(DragSourceEvent event) {
						//System.out.println("DragSetData"); 
						event.data = "moveRow";
					}
				});
			}

			dropTarget = tv.createDropTarget(DND.DROP_DEFAULT | DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
			if (dropTarget != null) {
				if (SWT.getVersion() >= 3107) {
					dropTarget.setTransfer(new Transfer[] {
						HTMLTransfer.getInstance(),
						URLTransfer.getInstance(),
						FileTransfer.getInstance(),
						TextTransfer.getInstance()
					});
				} else {
					dropTarget.setTransfer(new Transfer[] {
						URLTransfer.getInstance(),
						FileTransfer.getInstance(),
						TextTransfer.getInstance()
					});
				}

				dropTarget.addDropListener(new DropTargetAdapter() {
					public void dropAccept(DropTargetEvent event) {
						event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
								event.currentDataType);
					}

					public void dragEnter(DropTargetEvent event) {
						// no event.data on dragOver, use drag_drop_line_start to determine if
						// ours
						if (drag_drop_line_start < 0) {
							if (event.detail != DND.DROP_COPY) {
								if ((event.operations & DND.DROP_LINK) > 0)
									event.detail = DND.DROP_LINK;
								else if ((event.operations & DND.DROP_COPY) > 0)
									event.detail = DND.DROP_COPY;
							}
						} else if (TextTransfer.getInstance().isSupportedType(
								event.currentDataType)) {
							event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL
									| DND.FEEDBACK_SELECT | DND.FEEDBACK_INSERT_BEFORE
									| DND.FEEDBACK_INSERT_AFTER;
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
						}
					}

					public void drop(DropTargetEvent event) {
						if (!(event.data instanceof String)
								|| !((String) event.data).equals("moveRow")) {
							TorrentOpener.openDroppedTorrents(azureus_core, event, true);
							return;
						}

						// Torrent file from shell dropped
						if (drag_drop_line_start >= 0) { // event.data == null
							event.detail = DND.DROP_NONE;
							TableRowCore row = tv.getRow(event);
							if (row == null)
								return;
							int drag_drop_line_end = row.getIndex();
					    if (drag_drop_line_end != drag_drop_line_start) {
								moveSelectedTorrentsTo(row);
					    }
							drag_drop_line_start = -1;
						}
					}
				});
			}

		}
    catch( Throwable t ) {
    	Logger.log(new LogEvent(LOGID, "failed to init drag-n-drop", t));
    }
  }

  private void moveSelectedTorrentsTo(TableRowCore row) {
    DownloadManager dm = (DownloadManager)row.getDataSource(true);
    moveSelectedTorrentsTo(dm.getPosition());
  }
  
  private void moveSelectedTorrentsTo(int iNewPos) {
    TableRowCore[] rows = tv.getSelectedRows();
    if (rows.length == 0) {
      return;
    }
    
    TableColumnCore sortColumn = tv.getSortColumn();
    boolean isSortAscending = sortColumn == null ? true
				: sortColumn.isSortAscending();

    for (int i = 0; i < rows.length; i++) {
			TableRowCore row = rows[i];
      DownloadManager dm = (DownloadManager)row.getDataSource(true);
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
    
    isTrackerOn = TRTrackerUtils.isTrackerEnabled();
    
    refreshIconBar();
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
				case 'f': // CTRL+F Find/Filter
					openFilterDialog();
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
					System.out.println("MOVEND");
					moveSelectedTorrentsEnd();
					e.doit = false;
					break;
			}
			if (!e.doit)
				return;

			switch (key) {
				case 'r': // CTRL+R resume/start selected Torrents
					resumeSelectedTorrents();
					e.doit = false;
					break;
				case 's': // CTRL+S stop selected Torrents
					stopSelectedTorrents();
					e.doit = false;
					break;
				case 'x': // CTRL+X: RegEx search switch
					bRegexSearch = !bRegexSearch;
					e.doit = false;
					updateLastSearch();
					break;
			}

			if (!e.doit)
				return;
		}

		// DEL remove selected Torrents
		if (e.stateMask == 0 && e.keyCode == SWT.DEL && e.widget != txtFilter) {
			removeSelectedTorrents();
			e.doit = false;
			return;
		}

		if (e.keyCode != SWT.BS) {
			if ((e.stateMask & (~SWT.SHIFT)) != 0 || e.character < 32)
				return;
		}
		
		if (e.widget == txtFilter)
			return;

		// normal character: jump to next item with a name beginning with this character
		if (ASYOUTYPE_MODE == ASYOUTYPE_MODE_FIND) {
			if (System.currentTimeMillis() - lLastSearchTime > 3000)
				sLastSearch = "";
		}

		if (e.keyCode == SWT.BS) {
			if (e.stateMask == SWT.CONTROL)
				sLastSearch = "";
			else if (sLastSearch.length() > 0)
				sLastSearch = sLastSearch.substring(0, sLastSearch.length() - 1);
		} else
			sLastSearch += String.valueOf(e.character);

		if (ASYOUTYPE_MODE == ASYOUTYPE_MODE_FILTER) {
			if (txtFilter != null && !txtFilter.isDisposed()) {
				txtFilter.setFocus();
			}
			updateLastSearch();
		} else {
			TableCellCore[] cells = tv.getColumnCells("name");

			//System.out.println(sLastSearch);

			Arrays.sort(cells, TableCellImpl.TEXT_COMPARATOR);
			int index = Arrays.binarySearch(cells, sLastSearch,
					TableCellImpl.TEXT_COMPARATOR);
			if (index < 0) {

				int iEarliest = -1;
				String s = bRegexSearch ? sLastSearch : "\\Q" + sLastSearch + "\\E"; 
				Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
				for (int i = 0; i < cells.length; i++) {
					Matcher m = pattern.matcher(cells[i].getText());
					if (m.find() && (m.start() < iEarliest || iEarliest == -1)) {
						iEarliest = m.start();
						index = i;
					}
				}

				if (index < 0)
					// Insertion Point (best guess)
					index = -1 * index - 1;
			}

			if (index >= 0) {
				if (index >= cells.length)
					index = cells.length - 1;
				TableRowCore row = cells[index].getTableRowCore();
				int iTableIndex = row.getIndex();
				if (iTableIndex >= 0) {
					tv.setSelectedRows(new TableRowCore[] { row });
				}
			}
			lLastSearchTime = System.currentTimeMillis();
			updateTableLabel();
		}
		e.doit = false;
	}

	private void openFilterDialog() {
		InputShell is = new InputShell("MyTorrentsView.dialog.setFilter.title",
				"MyTorrentsView.dialog.setFilter.text");
		is.setTextValue(sLastSearch);
		is.setLabelParameters(new String[] { MessageText.getString(tv.getTableID() + "View"
				+ ".header")
		});

		String sReturn = is.open();
		if (sReturn == null)
			return;
		
		sLastSearch = sReturn;
		updateLastSearch();
	}
	
	private void updateLastSearch() {
		if (lblHeader == null || lblHeader.isDisposed())
			createTabs();

		if (txtFilter != null && !txtFilter.isDisposed()) {
			if (!sLastSearch.equals(txtFilter.getText())) { 
				txtFilter.setText(sLastSearch);
				txtFilter.setSelection(sLastSearch.length());
			}

			if (sLastSearch.length() > 0) {
				if (bRegexSearch) {
					try {
						Pattern.compile(sLastSearch, Pattern.CASE_INSENSITIVE);
						txtFilter.setBackground(Colors.colorAltRow);
						Messages.setLanguageTooltip(txtFilter, "MyTorrentsView.filter.tooltip");
					} catch (Exception e) {
						txtFilter.setBackground(Colors.colorErrorBG);
						txtFilter.setToolTipText(e.getMessage());
					}
				} else {
					txtFilter.setBackground(null);
					Messages.setLanguageTooltip(txtFilter, "MyTorrentsView.filter.tooltip");
				}
			}
		}
		if (lblX != null && !lblX.isDisposed()) {
			Image img = ImageRepository.getImage(sLastSearch.length() > 0 ? "smallx"
					: "smallx-gray");

			lblX.setImage(img);
		}

		if (searchUpdateEvent != null) {
			searchUpdateEvent.cancel();
		}
		searchUpdateEvent = SimpleTimer.addEvent("SearchUpdate",
				SystemTime.getOffsetTime(ASYOUTYPE_UPDATEDELAY),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						searchUpdateEvent = null;
						activateCategory(currentCategory);
					}
				});
	}

	public void keyReleased(KeyEvent e) {
		// ignore
	}

  private void changeDirSelectedTorrents() {
    Object[] dataSources = tv.getSelectedDataSources();
    if (dataSources.length <= 0)
      return;

    String sDefPath = COConfigurationManager.getBooleanParameter("Use default data dir") ?
                      COConfigurationManager.getStringParameter("Default save path") :
                      "";
    
    if ( sDefPath.length() > 0 ){
	    File	f = new File(sDefPath);
	    
	    if ( !f.exists()){
	    	FileUtil.mkdirs(f);
	    }
    }
    
    DirectoryDialog dDialog = new DirectoryDialog(cTablePanel.getShell(),
                                                  SWT.SYSTEM_MODAL);
    dDialog.setFilterPath(sDefPath);
    dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath"));
    String sSavePath = dDialog.open();
    if (sSavePath != null) {
      for (int i = 0; i < dataSources.length; i++) {
        DownloadManager dm = (DownloadManager)dataSources[i];
        if (dm.getState() == DownloadManager.STATE_ERROR ){
        	
            dm.setTorrentSaveDir(sSavePath);
        	
            if ( dm.filesExist()) {
            	
            	dm.stopIt( DownloadManager.STATE_STOPPED,false,false);
            	
            	ManagerUtils.queue(dm, cTablePanel);
            }
        }
      }
    }
  }

  private void removeTorrent(final DownloadManager dm, final boolean bDeleteTorrent, final boolean bDeleteData) {
  	ManagerUtils.remove(dm, cTablePanel.getShell(), bDeleteTorrent, bDeleteData);
  }

  private void removeSelectedTorrents() {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), false, false);
      }
    });
  }

  private void stopSelectedTorrents() {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.stop((DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  }

  private void queueSelectedTorrents() {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.queue((DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  }

  private void resumeSelectedTorrents() {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.start((DownloadManager)row.getDataSource(true));
      }
    });
  }

  private void hostSelectedTorrents() {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.host(azureus_core, (DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.showMyTracker();
  	}
  }

  private void publishSelectedTorrents() {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.publish(azureus_core, (DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.showMyTracker();
  	}
  }

  private void runSelectedTorrents() {
    Object[] dataSources = tv.getSelectedDataSources();
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm != null) {
        ManagerUtils.run(dm);
      }
    }
  }
  
  private void openSelectedTorrents() {
    Object[] dataSources = tv.getSelectedDataSources();
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm != null) {
        ManagerUtils.open(dm);
      }
    }
  }

  private void moveSelectedTorrentsDown() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = tv.getSelectedDataSources();
    Arrays.sort(dataSources, new Comparator() {
      public int compare (Object a, Object b) {
        return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
      }
    });
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
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
    Object[] dataSources = tv.getSelectedDataSources();
    Arrays.sort(dataSources, new Comparator() {
      public int compare (Object a, Object b) {
        return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
      }
    });
    for (int i = 0; i < dataSources.length; i++) {
      DownloadManager dm = (DownloadManager)dataSources[i];
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
		Object[] dataSources = tv.getSelectedDataSources();
		if (dataSources.length <= 0)
			return;

		int[] newPositions = new int[dataSources.length];

		if (by < 0) {
			Arrays.sort(dataSources, new Comparator() {
				public int compare(Object a, Object b) {
					return ((DownloadManager) a).getPosition()
							- ((DownloadManager) b).getPosition();
				}
			});
		} else {
			Arrays.sort(dataSources, new Comparator() {
				public int compare(Object a, Object b) {
					return ((DownloadManager) b).getPosition()
							- ((DownloadManager) a).getPosition();
				}
			});
		}

		int count = globalManager.downloadManagerCount(isSeedingView); 
		for (int i = 0; i < dataSources.length; i++) {
			DownloadManager dm = (DownloadManager) dataSources[i];
			int pos = dm.getPosition() + by;
			if (pos < i + 1)
				pos = i + 1;
			else if (pos > count - i)
				pos = count - i;

			newPositions[i] = pos;
		}

		for (int i = 0; i < dataSources.length; i++) {
			DownloadManager dm = (DownloadManager) dataSources[i];
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
  	Object[] datasources = tv.getSelectedDataSources();
    if (datasources.length == 0)
      return;
  	DownloadManager[] downloadManagers = new DownloadManager[datasources.length];
  	System.arraycopy(datasources, 0, downloadManagers, 0, datasources.length);

    if(moveToTop)
      globalManager.moveTop(downloadManagers);
    else
      globalManager.moveEnd(downloadManagers);

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
		if (parameterName == null || parameterName.equals("User Mode")) {
			userMode = COConfigurationManager.getIntParameter("User Mode");
		}

		if (parameterName == null
				|| parameterName.equals("DND Always In Incomplete")) {
			bDNDalwaysIncomplete = COConfigurationManager.getBooleanParameter("DND Always In Incomplete");
		}
	}

  private boolean top,bottom,up,down,run,host,publish,start,stop,remove;

  private void computePossibleActions() {
    Object[] dataSources = tv.getSelectedDataSources();
    // enable up and down so that we can do the "selection rotate trick"
    up = down = run =  remove = (dataSources.length > 0);
    top = bottom = start = stop = host = publish = false;
    for (int i = 0; i < dataSources.length; i++) {
      DownloadManager dm = (DownloadManager)dataSources[i];

      if(!start && ManagerUtils.isStartable(dm))
        start =  true;
      if(!stop && ManagerUtils.isStopable(dm))
        stop = true;
      if(!top && dm.getGlobalManager().isMoveableUp(dm))
        top = true;
      if(!bottom && dm.getGlobalManager().isMoveableDown(dm))
        bottom = true;
      
      if(userMode>0 && isTrackerOn)
    	  host = publish = true;
    }
  }

  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("run"))
      return run;
    if(itemKey.equals("host"))
      return host;
    if(itemKey.equals("publish"))
      return publish;
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
    return false;
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
      runSelectedTorrents();
      return;
    }
    if(itemKey.equals("host")){
      hostSelectedTorrents();
      return;
    }
    if(itemKey.equals("publish")){
      publishSelectedTorrents();
      return;
    }
    if(itemKey.equals("start")){
      queueSelectedTorrents();
      return;
    }
    if(itemKey.equals("stop")){
      stopSelectedTorrents();
      return;
    }
    if(itemKey.equals("remove")){
      removeSelectedTorrents();
      return;
    }
  }
  
  private Category addCategory() {
    CategoryAdderWindow adderWindow = new CategoryAdderWindow(getComposite().getDisplay());
    Category newCategory = adderWindow.getNewCategory();
    if (newCategory != null)
      assignSelectedToCategory(newCategory);
    return newCategory;
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
    	Utils.execSWTThread(new AERunnable() {
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
  		refreshIconBar();
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

  // Category Stuff
  private void assignSelectedToCategory(final Category category) {
    tv.runForSelectedRows(new TableGroupRowRunner() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).getDownloadState().setCategory(category);
      }
    });
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
		List list = Arrays.asList(tv.getDataSources());
		List listRemoves = new ArrayList();
		List listAdds = new ArrayList();
		
		for (int i = 0; i < managers.length; i++) {
			DownloadManager dm = (DownloadManager) managers[i];
		
			boolean bHave = list.contains(dm);
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
		tv.removeDataSources(listRemoves.toArray());
		tv.addDataSources(listAdds.toArray());
		
    tv.refreshTable(false);
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
  public void seedingStatusChanged( boolean seeding_only_mode ){}       

  // End of globalmanagerlistener Functions
  
  private void setSelectedTorrentsUpSpeed(int speed) {      
    Object[] dms = tv.getSelectedDataSources();
    if(dms.length > 0) {            
      for (int i = 0; i < dms.length; i++) {
        try {
          DownloadManager dm = (DownloadManager)dms[i];
          dm.getStats().setUploadRateLimitBytesPerSecond(speed);
        } catch (Exception e) {
        	Debug.printStackTrace( e );
        }
      }
    }
  }
  
  private void setSelectedTorrentsDownSpeed(int speed) {      
    Object[] dms = tv.getSelectedDataSources();
    if(dms.length > 0) {            
      for (int i = 0; i < dms.length; i++) {
        try {
          DownloadManager dm = (DownloadManager)dms[i];
          dm.getStats().setDownloadRateLimitBytesPerSecond(speed);
        } catch (Exception e) {
          Debug.printStackTrace( e );
        }
      }
    }
  }

  // @see com.aelitis.azureus.ui.common.table.TableCountChangeListener#rowAdded(com.aelitis.azureus.ui.common.table.TableRowCore)
  public void rowAdded(TableRowCore row) {
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
	private void updateTableLabel() {
		if (lblHeader == null || lblHeader.isDisposed()) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (lblHeader != null && !lblHeader.isDisposed()) {
					String sText = MessageText.getString(tv.getTableID() + "View"
							+ ".header")
							+ " (" + tv.size(true) + ")";
					lblHeader.setText(sText);
					lblHeader.getParent().layout();
				}
			}
		});
	}

	public boolean isTableFocus() {
		return tv.isTableFocus();
	}
	
	public Image obfusticatedImage(final Image image, Point shellOffset) {
		return tv.obfusticatedImage(image, shellOffset);
	}
}
