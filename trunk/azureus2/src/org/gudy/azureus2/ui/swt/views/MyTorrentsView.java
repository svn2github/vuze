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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryListener;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.CategoryManagerListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;

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
       extends TableView
       implements GlobalManagerListener,
                  ParameterListener,
                  DownloadManagerListener,
                  CategoryManagerListener,
                  CategoryListener,
                  KeyListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
	private static final int ASYOUTYPE_MODE_FIND = 0;
	private static final int ASYOUTYPE_MODE_FILTER = 1;
	private static final int ASYOUTYPE_MODE = ASYOUTYPE_MODE_FILTER; 
	
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
  private Label tableLabel = null;
  
  int userMode;
  boolean isTrackerOn;

  private Map downloadBars;
  private AEMonitor				downloadBars_mon	= new AEMonitor( "MyTorrentsView:DL" );

  private Category currentCategory;
  private boolean skipDMAdding = true;

  // table item index, where the drag has started
  private int drag_drop_line_start = -1;

  private boolean confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete", true);
  
  private String sLastSearch = "";
  private long lLastSearchTime;
  private boolean bRegexSearch = false;

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
    super((isSeedingView) ? TableManager.TABLE_MYTORRENTS_COMPLETE
                          : TableManager.TABLE_MYTORRENTS_INCOMPLETE,
          "MyTorrentsView", basicItems, "#", 
          SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
    setRowDefaultIconSize(new Point(16, 16));
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    this.isSeedingView 	= isSeedingView;

    downloadBars = MainWindow.getWindow().getDownloadBars();
    currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);  
	}

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite0) {
    if(cTablePanel != null) {
      return;
    }

    super.initialize(composite0);

    createTabs();

    createDragDrop();

    COConfigurationManager.addParameterListener("Confirm Data Delete", this);

    activateCategory(currentCategory);
    CategoryManager.addCategoryManagerListener(this);
    // globalManager.addListener sends downloadManagerAdded()'s when you addListener
    // we don't need them..
    skipDMAdding = true;
    globalManager.addListener(this);
    skipDMAdding = false;
  }


  public void tableStructureChanged() {
    super.tableStructureChanged();

    createDragDrop();
    activateCategory(currentCategory);
  }

  public Composite createMainPanel(Composite composite) {
    GridData gridData;
    Composite panel = new Composite(composite, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    panel.setLayout(layout);
    panel.setLayoutData(new GridData(GridData.FILL_BOTH));

    cTablePanel = new Composite(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    cTablePanel.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    cTablePanel.setLayout(layout);

    return panel;
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

    if(cCategories != null && !showCat) {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
    }

    if (categories.length > 0 && showCat) {
      if (cCategories == null) {
        Composite parent = getComposite();

        cCategories = new Composite(parent, SWT.NONE);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        cCategories.setLayoutData(gridData);
        RowLayout rowLayout = new RowLayout();
        final int uniformPadding = 0;
        rowLayout.marginTop = uniformPadding;
        rowLayout.marginBottom = uniformPadding;
        rowLayout.marginLeft = uniformPadding;
        rowLayout.marginRight = uniformPadding;
        rowLayout.spacing = uniformPadding;
        rowLayout.wrap = true;
        cCategories.setLayout(rowLayout);

        tableLabel = new Label(parent, SWT.WRAP);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalIndent = 5;
        tableLabel.setLayoutData(gridData);
        updateTableLabel();
        tableLabel.moveAbove(null);
        cCategories.moveBelow(tableLabel);
      } else {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
      }

      int iFontPixelsHeight = 11;
      int iFontPointHeight = (iFontPixelsHeight * 72) / cCategories.getDisplay().getDPI().y;
      for (int i = 0; i < categories.length; i++) {
        final Button catButton = new Button(cCategories, SWT.TOGGLE);
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

        String name = categories[i].getName();
        if (categories[i].getType() == Category.TYPE_USER)
          catButton.setText(name);
        else
          Messages.setLanguageText(catButton, name);

        catButton.setData("Category", categories[i]);
        if (categories[i] == currentCategory) {
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
							
							if (!isOurDownloadManager(dm))
								continue;
							
							count++;
							if (dm.getState() == DownloadManager.STATE_DOWNLOADING
									|| dm.getState() == DownloadManager.STATE_SEEDING)
								ttlActive++;
							ttlSize += dm.getSize();
							ttlRSpeed += dm.getStats().getDataReceiveRate();
							ttlSSpeed += dm.getStats().getDataSendRate();
						}

            if (count == 0) {
            	curButton.setToolTipText(null);
            	return;
            }
            
            curButton.setToolTipText("Total: " + count + "\n"
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

        Menu menu = new Menu(getComposite().getShell(), SWT.POP_UP);

        final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
        Messages.setLanguageText(itemDelete, "MyTorrentsView.menu.category.delete");
        menu.setDefaultItem(itemDelete);

        if (categories[i].getType() == Category.TYPE_USER) {
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
          itemDelete.setEnabled(true);
        }
        else {
          itemDelete.setEnabled(false);
        }
        catButton.setMenu(menu);
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
								+ tableLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
								+ tableLabel.getBorderWidth() * 2;

						Object layoutData = tableLabel.getLayoutData();
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

				getTableComposite().addControlListener(catResizeAdapter);
			}

      catResizeAdapter.controlResized(null);
    }
  }
  
  private boolean isOurDownloadManager(DownloadManager dm) {
    boolean bCompleted = dm.getStats().getDownloadCompleted(false) == 1000;
    boolean bOurs = ((bCompleted && isSeedingView) || (!bCompleted && !isSeedingView));
    
    if (bOurs && sLastSearch.length() > 0) {
    	try {
	    	String name = dm.getDisplayName();
				String s = bRegexSearch ? sLastSearch : "\\Q" + sLastSearch + "\\E"; 
				Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
				
				if (!pattern.matcher(name).find())
					bOurs = false;
    	} catch (Exception e) {
    		// Future: report PatternSyntaxException message to user.
    	}
    }

    return bOurs;
  }

  public Table createTable(Composite panel) {
    Table table = new Table(cTablePanel, iTableStyle);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    
    table.addKeyListener(this);

    table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
		    Utils.execSWTThread(new AERunnable() {
			    public void runSupport() {
			    	computePossibleActions();
			      MainWindow.getWindow().refreshIconBar();  
			    }
		    });    
			}
    });

    cTablePanel.layout();
    return table;
  }
  
  public void runDefaultAction() {
    DownloadManager dm = (DownloadManager)getFirstSelectedDataSource();
    if (dm != null)
      MainWindow.getWindow().openManagerView(dm);
  }

  public void fillMenu(final Menu menu) {
		Object[] dms = getSelectedDataSources();
		boolean hasSelection = (dms.length > 0);

		userMode = COConfigurationManager.getIntParameter("User Mode");
		isTrackerOn = TRTrackerUtils.isTrackerEnabled();

		// Enable/Disable Logic


		boolean moveUp, moveDown, bChangeDir;
		moveUp = moveDown = bChangeDir = hasSelection;

		boolean start, stop, changeUrl, barsOpened, forceStart;
		boolean forceStartEnabled, recheck, manualUpdate, changeSpeed, fileMove, fileRescan;

		changeUrl = barsOpened = manualUpdate = changeSpeed = fileMove = fileRescan = true;
		forceStart = forceStartEnabled = recheck = start = stop = false;

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
			bChangeDir = true;

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

				} catch (NullPointerException ex) {
					changeSpeed = false;
				} catch (Exception ex) {
					Debug.printStackTrace(ex);
				}

				if (dm.getTrackerClient() == null) {
					changeUrl = false;
				}

				if (!downloadBars.containsKey(dm)) {
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

				if (!dm.getGlobalManager().isMoveableDown(dm)) {
					moveDown = false;
				}

				if (!dm.getGlobalManager().isMoveableUp(dm)) {
					moveUp = false;
				}

				if (userMode > 1) {
					TRTrackerAnnouncer trackerClient = dm.getTrackerClient();

					if (trackerClient != null) {
						boolean update_state = ((SystemTime.getCurrentTime() / 1000
								- trackerClient.getLastUpdateTime() >= TRTrackerAnnouncer.REFRESH_MINIMUM_SECS));
						manualUpdate = manualUpdate & update_state;
					}

				}
				bChangeDir &= (dm.getState() == DownloadManager.STATE_ERROR && !dm
						.filesExist());
				
				boolean	scan = dm.getDownloadState().getFlag( DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES );
				
				boolean	incomplete = !dm.isDownloadComplete();
				
				allScanSelected 	= incomplete && allScanSelected && scan;
				allScanNotSelected 	= incomplete && allScanNotSelected && !scan;
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
		itemDetails.addListener(SWT.Selection, new SelectedTableRowsListener() {
			public void run(TableRowCore row) {
				MainWindow.getWindow().openManagerView(
						(DownloadManager) row.getDataSource(true));
			}
		});
		itemDetails.setEnabled(hasSelection);

		// Open Bar
		final MenuItem itemBar = new MenuItem(menu, SWT.CHECK);
		Messages.setLanguageText(itemBar, "MyTorrentsView.menu.showdownloadbar");
		Utils.setMenuItemImage(itemBar, "downloadBar");
		itemBar.addListener(SWT.Selection, new SelectedTableRowsListener() {
			public void run(TableRowCore row) {
				DownloadManager dm = (DownloadManager) row.getDataSource(true);
				try {
					downloadBars_mon.enter();

					if (downloadBars.containsKey(dm)) {
						MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(dm);
						mw.close();
					} else {
						MinimizedWindow mw = new MinimizedWindow(dm, cTablePanel.getShell());
						downloadBars.put(dm, mw);
					}
				} finally {

					downloadBars_mon.exit();
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

		final Menu menuAdvanced = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		itemAdvanced.setMenu(menuAdvanced);

		// advanced > Download Speed Menu //
		final MenuItem itemDownSpeed = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemDownSpeed, "MyTorrentsView.menu.setDownSpeed"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemDownSpeed, "speed");

		final Menu menuDownSpeed = new Menu(getComposite().getShell(),
				SWT.DROP_DOWN);
		itemDownSpeed.setMenu(menuDownSpeed);

		final MenuItem itemCurrentDownSpeed = new MenuItem(menuDownSpeed, SWT.PUSH);
		itemCurrentDownSpeed.setEnabled(false);
		StringBuffer speedText = new StringBuffer();
		String separator = "";
		//itemDownSpeed.                   
		if (downSpeedDisabled) {
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.disabled"));
			separator = " / ";
		}
		if (downSpeedUnlimited) {
			speedText.append(separator);
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.unlimited"));
			separator = " / ";
		}
		if (totalDownSpeed > 0) {
			speedText.append(separator);
			speedText.append(DisplayFormatters
					.formatByteCountToKiBEtcPerSec(totalDownSpeed));
		}
		itemCurrentDownSpeed.setText(speedText.toString());

		new MenuItem(menuDownSpeed, SWT.SEPARATOR);

		final MenuItem itemsDownSpeed[] = new MenuItem[12];
		Listener itemsDownSpeedListener = new Listener() {
			public void handleEvent(Event e) {
				if (e.widget != null && e.widget instanceof MenuItem) {
					MenuItem item = (MenuItem) e.widget;
					int speed = item.getData("maxdl") == null ? 0 : ((Integer) item
							.getData("maxdl")).intValue();
					setSelectedTorrentsDownSpeed(speed);
				}
			}
		};

		itemsDownSpeed[1] = new MenuItem(menuDownSpeed, SWT.PUSH);
		Messages.setLanguageText(itemsDownSpeed[1],
				"MyTorrentsView.menu.setSpeed.unlimit");
		itemsDownSpeed[1].setData("maxdl", new Integer(0));
		itemsDownSpeed[1].addListener(SWT.Selection, itemsDownSpeedListener);

		if (hasSelection) {
			long maxDownload = COConfigurationManager.getIntParameter(
					"Max Download Speed KBs", 0) * 1024;
			//using 200KiB/s as the default limit when no limit set.
			if (maxDownload == 0){		
				if ( downSpeedSetMax == 0 ){
					maxDownload = 200 * 1024;
				}else{
					maxDownload	= 4 * ( downSpeedSetMax/1024 ) * 1024;
				}
			}

			for (int i = 2; i < 12; i++) {
				itemsDownSpeed[i] = new MenuItem(menuDownSpeed, SWT.PUSH);
				itemsDownSpeed[i].addListener(SWT.Selection, itemsDownSpeedListener);
	
				// dms.length has to be > 0 when hasSelection
				int limit = (int)(maxDownload / (10 * dms.length) * (12 - i));
				StringBuffer speed = new StringBuffer();
				speed.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit
						* dms.length));
				if (dms.length > 1) {
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.in"));
					speed.append(" ");
					speed.append(dms.length);
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.slots"));
					speed.append(" ");
					speed
							.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit));
				}
				itemsDownSpeed[i].setText(speed.toString());
				itemsDownSpeed[i].setData("maxdl", new Integer(limit));
			}
		}

		// ---
		new MenuItem(menuDownSpeed, SWT.SEPARATOR);

		final MenuItem itemDownSpeedManual = new MenuItem(menuDownSpeed, SWT.PUSH);
		Messages.setLanguageText(itemDownSpeedManual, "MyTorrentsView.menu.manual");
		itemDownSpeedManual.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputShell is = new InputShell(
						"MyTorrentsView.dialog.setSpeed.title",
						new String[] { MessageText
								.getString("MyTorrentsView.dialog.setNumber.download") },
						"MyTorrentsView.dialog.setNumber.text",
						new String[] {
								MessageText.getString("MyTorrentsView.dialog.setNumber.inKbps"),
								MessageText
										.getString("MyTorrentsView.dialog.setNumber.download") });

				String sReturn = is.getText();
				if (sReturn == null)
					return;

				int newSpeed;
				try {
					newSpeed = (int) (Double.valueOf(sReturn).doubleValue() * 1024);
				} catch (NumberFormatException er) {
					MessageBox mb = new MessageBox(MainWindow.getWindow().getShell(),
							SWT.ICON_ERROR | SWT.OK);
					mb.setText(MessageText
							.getString("MyTorrentsView.dialog.NumberError.title"));
					mb.setMessage(MessageText
							.getString("MyTorrentsView.dialog.NumberError.text"));

					mb.open();
					return;
				}
				setSelectedTorrentsDownSpeed(newSpeed);
			}
		});

		// advanced >Upload Speed Menu //
		final MenuItem itemUpSpeed = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemUpSpeed, "MyTorrentsView.menu.setUpSpeed"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemUpSpeed, "speed");

		final Menu menuUpSpeed = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		itemUpSpeed.setMenu(menuUpSpeed);

		final MenuItem itemCurrentUpSpeed = new MenuItem(menuUpSpeed, SWT.PUSH);
		itemCurrentUpSpeed.setEnabled(false);
		separator = "";
		speedText = new StringBuffer();
		//itemUpSpeed.                   
		if (upSpeedDisabled) {
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.disabled"));
			separator = " / ";
		}
		if (upSpeedUnlimited) {
			speedText.append(separator);
			speedText.append(MessageText
					.getString("MyTorrentsView.menu.setSpeed.unlimited"));
			separator = " / ";
		}
		if (totalUpSpeed > 0) {
			speedText.append(separator);
			speedText.append(DisplayFormatters
					.formatByteCountToKiBEtcPerSec(totalUpSpeed));
		}
		itemCurrentUpSpeed.setText(speedText.toString());

		// ---
		new MenuItem(menuUpSpeed, SWT.SEPARATOR);

		final MenuItem itemsUpSpeed[] = new MenuItem[12];
		Listener itemsUpSpeedListener = new Listener() {
			public void handleEvent(Event e) {
				if (e.widget != null && e.widget instanceof MenuItem) {
					MenuItem item = (MenuItem) e.widget;
					int speed = item.getData("maxul") == null ? 0 : ((Integer) item
							.getData("maxul")).intValue();
					setSelectedTorrentsUpSpeed(speed);
				}
			}
		};

		itemsUpSpeed[1] = new MenuItem(menuUpSpeed, SWT.PUSH);
		Messages.setLanguageText(itemsUpSpeed[1],
				"MyTorrentsView.menu.setSpeed.unlimit");
		itemsUpSpeed[1].setData("maxul", new Integer(0));
		itemsUpSpeed[1].addListener(SWT.Selection, itemsUpSpeedListener);

		if (hasSelection) {
			long maxUpload = COConfigurationManager.getIntParameter(
					"Max Upload Speed KBs", 0) * 1024;
			//using 75KiB/s as the default limit when no limit set.
			if (maxUpload == 0){
				maxUpload = 75 * 1024;
			}else{
				if ( upSpeedSetMax == 0 ){
					maxUpload = 200 * 1024;
				}else{
					maxUpload = 4 * ( upSpeedSetMax/1024 ) * 1024;
				}
			}
			for (int i = 2; i < 12; i++) {
				itemsUpSpeed[i] = new MenuItem(menuUpSpeed, SWT.PUSH);
				itemsUpSpeed[i].addListener(SWT.Selection, itemsUpSpeedListener);

				int limit = (int)( maxUpload / (10 * dms.length) * (12 - i));
				StringBuffer speed = new StringBuffer();
				speed.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit
						* dms.length));
				if (dms.length > 1) {
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.in"));
					speed.append(" ");
					speed.append(dms.length);
					speed.append(" ");
					speed.append(MessageText
							.getString("MyTorrentsView.menu.setSpeed.slots"));
					speed.append(" ");
					speed
							.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit));
				}

				itemsUpSpeed[i].setText(speed.toString());
				itemsUpSpeed[i].setData("maxul", new Integer(limit));
			}
		}

		new MenuItem(menuUpSpeed, SWT.SEPARATOR);

		final MenuItem itemUpSpeedManual = new MenuItem(menuUpSpeed, SWT.PUSH);
		Messages.setLanguageText(itemUpSpeedManual, "MyTorrentsView.menu.manual");
		itemUpSpeedManual.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputShell is = new InputShell(
						"MyTorrentsView.dialog.setSpeed.title",
						new String[] { MessageText
								.getString("MyTorrentsView.dialog.setNumber.upload") },
						"MyTorrentsView.dialog.setNumber.text",
						new String[] {
								MessageText.getString("MyTorrentsView.dialog.setNumber.inKbps"),
								MessageText.getString("MyTorrentsView.dialog.setNumber.upload") });

				String sReturn = is.getText();
				if (sReturn == null)
					return;

				int newSpeed;
				try {
					newSpeed = (int) (Double.valueOf(sReturn).doubleValue() * 1024);
				} catch (NumberFormatException er) {
					MessageBox mb = new MessageBox(MainWindow.getWindow().getShell(),
							SWT.ICON_ERROR | SWT.OK);
					mb.setText(MessageText
							.getString("MyTorrentsView.dialog.NumberError.title"));
					mb.setMessage(MessageText
							.getString("MyTorrentsView.dialog.NumberError.text"));

					mb.open();
					return;
				}
				setSelectedTorrentsUpSpeed(newSpeed);
			}
		});

		// advanced > Tracker Menu //
		final Menu menuTracker = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
		final MenuItem itemTracker = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemTracker, "MyTorrentsView.menu.tracker");
		itemTracker.setMenu(menuTracker);

		final MenuItem itemChangeTracker = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemChangeTracker,
				"MyTorrentsView.menu.changeTracker"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemChangeTracker, "add_tracker");
		itemChangeTracker.addListener(SWT.Selection,
				new SelectedTableRowsListener() {
					public void run(TableRowCore row) {
						TRTrackerAnnouncer tc = ((DownloadManager) row.getDataSource(true))
								.getTrackerClient();
						if (tc != null)
							new TrackerChangerWindow(MainWindow.getWindow().getDisplay(), tc);
					}
				});
		itemChangeTracker.setEnabled(changeUrl);


		final MenuItem itemEditTracker = new MenuItem(menuTracker, SWT.PUSH);
		Messages
				.setLanguageText(itemEditTracker, "MyTorrentsView.menu.editTracker");
		Utils.setMenuItemImage(itemEditTracker, "edit_trackers");
		itemEditTracker.addListener(SWT.Selection, new SelectedTableRowsListener() {
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
				new SelectedTableRowsListener() {
					public void run(TableRowCore row) {
						((DownloadManager) row.getDataSource(true)).checkTracker(false);
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
				new SelectedTableRowsListener() {
					public void run(TableRowCore row) {
						((DownloadManager) row.getDataSource(true)).scrapeTracker(true);
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
				Object[] dms = getSelectedDataSources();

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

								Logger.log(new LogAlert(LogAlert.REPEATABLE,
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
				Object[] dms = getSelectedDataSources();

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

								Logger.log(new LogAlert(LogAlert.REPEATABLE,
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
		itemFileRescan.addListener(SWT.Selection, new SelectedTableRowsListener() {
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
					DownloadManager dm = (DownloadManager) getFirstSelectedDataSource();
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
					DownloadManager dm = (DownloadManager) getFirstSelectedDataSource();
					if (dm != null) {
						FileDialog fd = new FileDialog(getComposite().getShell(), SWT.SAVE );
	
						fd.setFileName(dm.getTorrentFileName());
	
						String path = fd.open();
	
						if (path != null) {
	
							try {
								File target = new File(path);
	
								// first copy the torrent - DON'T use "writeTorrent" as this amends the
								// "filename" field in the torrent
	
								TorrentUtils.copyToFile(dm.getDownloadState().getTorrent(),
										target);
	
								// now remove the non-standard entries
	
								TOTorrent dest = TOTorrentFactory
										.deserialiseFromBEncodedFile(target);
	
								dest.removeAdditionalProperties();
	
								dest.serialiseToBEncodedFile(target);
	
							} catch (Throwable e) {
								Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
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
				itemPS.addListener(SWT.Selection, new SelectedTableRowsListener() {
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
				itemNetwork.addListener(SWT.Selection, new SelectedTableRowsListener() {
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

		final MenuItem itemPositionManual = new MenuItem(menuAdvanced, SWT.PUSH);
		Messages.setLanguageText(itemPositionManual,
				"MyTorrentsView.menu.reposition.manual");
		itemPositionManual.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputShell is = new InputShell(
						"MyTorrentsView.dialog.setPosition.title",
						"MyTorrentsView.dialog.setPosition.text");

				String sReturn = is.getText();
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
					MessageBox mb = new MessageBox(MainWindow.getWindow().getShell(),
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
			itemForceStart.addListener(SWT.Selection, new SelectedTableRowsListener() {
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

		// Remove
		final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemRemove, "delete");
		itemRemove.addListener(SWT.Selection, new SelectedTableRowsListener() {
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
				new SelectedTableRowsListener() {
					public void run(TableRowCore row) {
						removeTorrent((DownloadManager) row.getDataSource(true), true,
								false);
					}
				});

		// Remove And > Delete Data
		final MenuItem itemDeleteData = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteData,
				"MyTorrentsView.menu.removeand.deletedata");
		itemDeleteData.addListener(SWT.Selection, new SelectedTableRowsListener() {
			public void run(TableRowCore row) {
				removeTorrent((DownloadManager) row.getDataSource(true), false, true);
			}
		});

		// Remove And > Delete Both
		final MenuItem itemDeleteBoth = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteBoth,
				"MyTorrentsView.menu.removeand.deleteboth");
		itemDeleteBoth.addListener(SWT.Selection, new SelectedTableRowsListener() {
			public void run(TableRowCore row) {
				removeTorrent((DownloadManager) row.getDataSource(true), true, true);
			}
		});

		// Force Recheck
		final MenuItem itemRecheck = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRecheck, "MyTorrentsView.menu.recheck");
		Utils.setMenuItemImage(itemRecheck, "recheck");
		itemRecheck.addListener(SWT.Selection, new SelectedTableRowsListener() {
			public void run(TableRowCore row) {
				DownloadManager dm = (DownloadManager) row.getDataSource(true);

				if (dm.canForceRecheck()) {

					dm.forceRecheck();
				}
			}
		});
		itemRecheck.setEnabled(recheck);

		// ---
		new MenuItem(menu, SWT.SEPARATOR);

		super.fillMenu(menu);
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
        if (categories[i].getType() == Category.TYPE_USER) {
          final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
          itemCategory.setText(categories[i].getName());
          itemCategory.setData("Category", categories[i]);

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
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
    final Table table = getTable();

    if (sColumnName.equals("health")) {
      MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
      Messages.setLanguageText(item, "MyTorrentsView.menu.health");
      Utils.setMenuItemImage(item, "st_explain");
      item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          HealthHelpWindow.show(table.getDisplay());
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
                         new SelectedTableRowsListener() {
          public void run(TableRowCore row) {
            DownloadManager dm = (DownloadManager)row.getDataSource(true);
            MenuItem item = (MenuItem)event.widget;
            if (item != null) {
              int value = ((Long)item.getData("MaxUploads")).intValue();
              dm.getStats().setMaxUploads(value);
            }
          } // run
        }); // listener
      } // for
    }
  }

  private void createDragDrop() {
    try{
      
    Transfer[] types = new Transfer[] { TextTransfer.getInstance()};

    if (dragSource != null && !dragSource.isDisposed()) {
    	dragSource.dispose();
    }
    
    if (dropTarget != null && !dropTarget.isDisposed()) {
    	dropTarget.dispose();
    }

    dragSource = new DragSource(getTable(), DND.DROP_MOVE);
    dragSource.setTransfer(types);
    dragSource.addDragListener(new DragSourceAdapter() {
      public void dragStart(DragSourceEvent event) {
        Table table = getTable();
        if (table.getSelectionCount() != 0 &&
           table.getSelectionCount() != table.getItemCount())
        {
          event.doit = true;
        	//System.out.println("DragStart"); 
          drag_drop_line_start = table.getSelectionIndex();
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

    dropTarget = new DropTarget(getTable(), 
                                DND.DROP_DEFAULT | DND.DROP_MOVE |
                                DND.DROP_COPY | DND.DROP_LINK |
                                DND.DROP_TARGET_MOVE);
    dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(),
                                            FileTransfer.getInstance(),
                                            TextTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
			public void dropAccept(DropTargetEvent event) {
				event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
						event.currentDataType);
			}

			public void dragEnter(DropTargetEvent event) {
				//System.out.println("DragEnter " + event.operations);
      	// no event.data on dragOver, use drag_drop_line_start to determine if
      	// ours
        if(drag_drop_line_start < 0) {
          if(event.detail != DND.DROP_COPY) {
          	if ((event.operations & DND.DROP_LINK) > 0)
          		event.detail = DND.DROP_LINK;
          	else if ((event.operations & DND.DROP_COPY) > 0)
          		event.detail = DND.DROP_COPY;
          }
        } else if(TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
          event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL | DND.FEEDBACK_SELECT | DND.FEEDBACK_INSERT_BEFORE | DND.FEEDBACK_INSERT_AFTER;
          event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
        }
	  }

      public void drop(DropTargetEvent event) {
      	if (!(event.data instanceof String) || !((String)event.data).equals("moveRow")) {
          TorrentOpener.openDroppedTorrents(azureus_core, event);
      		return;
      	}

        // Torrent file from shell dropped
        if(drag_drop_line_start >= 0) { // event.data == null
          event.detail = DND.DROP_NONE;
          if(event.item == null)
            return;
          int drag_drop_line_end = getTable().indexOf((TableItem)event.item);
          moveSelectedTorrents(drag_drop_line_start, drag_drop_line_end);
          drag_drop_line_start = -1;
        }
      }
    });
    
    }
    catch( Throwable t ) {
    	Logger.log(new LogEvent(LOGID, "failed to init drag-n-drop", t));
    }
  }

  private void moveSelectedTorrents(int drag_drop_line_start, int drag_drop_line_end) {
    if (drag_drop_line_end == drag_drop_line_start)
      return;

    TableItem ti = getTable().getItem(drag_drop_line_end);
    TableRowCore row = (TableRowCore)ti.getData("TableRow");
    DownloadManager dm = (DownloadManager)row.getDataSource(true);
    
    moveSelectedTorrentsTo(dm.getPosition());
  }
  
  private void moveSelectedTorrentsTo(int iNewPos) {
    java.util.List list = getSelectedRowsList();
    if (list.size() == 0)
      return;

    for (Iterator iter = list.iterator(); iter.hasNext();) {
      TableRowCore row = (TableRowCore)iter.next();
      DownloadManager dm = (DownloadManager)row.getDataSource(true);
      int iOldPos = dm.getPosition();
      
      globalManager.moveTo(dm, iNewPos);
      if (rowSorter.bAscending) {
        if (iOldPos > iNewPos)
          iNewPos++;
      } else {
        if (iOldPos < iNewPos)
          iNewPos--;
      }
    }

    boolean bForceSort = rowSorter.sColumnName.equals("#");
    columnInvalidate("#");
    refresh(bForceSort);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh(boolean bForceSort) {
    if (getComposite() == null || getComposite().isDisposed())
      return;
    
    userMode = COConfigurationManager.getIntParameter("User Mode");
    isTrackerOn = TRTrackerUtils.isTrackerEnabled();
    
    computePossibleActions();
    MainWindow.getWindow().refreshIconBar();

    super.refresh(bForceSort);
  }


  public void delete() {
    super.delete();

    if (dragSource != null && !dragSource.isDisposed()) {
    	dragSource.dispose();
    	dragSource = null;
    }
    	
    if (dropTarget != null && !dropTarget.isDisposed()) {
    	dropTarget.dispose();
    	dropTarget = null;
    }
    
    if (fontButton != null && !fontButton.isDisposed()) {
      fontButton.dispose();
      fontButton = null;
    }
    CategoryManager.removeCategoryManagerListener(this);
    globalManager.removeListener(this);
    COConfigurationManager.removeParameterListener("Confirm Data Delete", this);
  }

	public void keyPressed(KeyEvent e) {
		if (e.stateMask == (SWT.CTRL | SWT.SHIFT)) {
			// CTRL+SHIFT+S stop all Torrents
			if (e.character == 0x13) {
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

			switch (e.character) {
				case 0x1: // CTRL+A select all Torrents
					getTable().selectAll();
					e.doit = false;
					break;
				case 0x03: // CTRL+C
					clipboardSelected();
					e.doit = false;
					break;
				case 0x12: // CTRL+R resume/start selected Torrents
					resumeSelectedTorrents();
					e.doit = false;
					break;
				case 0x13: // CTRL+S stop selected Torrents
					stopSelectedTorrents();
					e.doit = false;
					break;
				case 0x18: // CTRL-X: RexEx search switch
					bRegexSearch = !bRegexSearch;
					e.doit = false;
					break;
			}

			if (!e.doit && e.character != 0x18)
				return;
		}

		// DEL remove selected Torrents
		if (e.stateMask == 0 && e.keyCode == SWT.DEL) {
			removeSelectedTorrents();
			e.doit = false;
			return;
		}

		if (e.character < 32 && e.keyCode != SWT.BS && e.keyCode != 0x18)
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
			if (tableLabel == null || tableLabel.isDisposed())
				createTabs();
			
			activateCategory(currentCategory);
		} else {
			Table table = getTable();

			TableCellCore[] cells = getColumnCells("name");

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
				int iTableIndex = cells[index].getTableRowCore().getIndex();
				if (iTableIndex >= 0) {
					table.setSelection(iTableIndex);
				}
			}
			lLastSearchTime = System.currentTimeMillis();
			updateTableLabel();
		}
		e.doit = false;
	}

	public void keyReleased(KeyEvent e) {
		// ignore
	}

  private void changeDirSelectedTorrents() {
    Object[] dataSources = getSelectedDataSources();
    if (dataSources.length <= 0)
      return;

    String sDefPath = COConfigurationManager.getBooleanParameter("Use default data dir") ?
                      COConfigurationManager.getStringParameter("Default save path", "") :
                      "";
    
    if ( sDefPath.length() > 0 ){
	    File	f = new File(sDefPath);
	    
	    if ( !f.exists()){
	    	f.mkdirs();
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
    
    if( COConfigurationManager.getBooleanParameter( "confirm_torrent_removal" ) ) {
    	
      MessageBox mb = new MessageBox(cTablePanel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
      
      mb.setText(MessageText.getString("deletedata.title"));
      
      mb.setMessage(MessageText.getString("deletetorrent.message1")
            + dm.getDisplayName() + " :\n"
            + dm.getTorrentFileName()
            + MessageText.getString("deletetorrent.message2"));
      
      if( mb.open() == SWT.NO ) {
        return;
      }
    }
    
    int choice;
    if (confirmDataDelete && bDeleteData) {
      String path = dm.getSaveLocation().toString();
      
      MessageBox mb = new MessageBox(cTablePanel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
      
      mb.setText(MessageText.getString("deletedata.title"));
      
      mb.setMessage(MessageText.getString("deletedata.message1")
          + dm.getDisplayName() + " :\n"
          + path
          + MessageText.getString("deletedata.message2"));

      choice = mb.open();
    } else {
      choice = SWT.YES;
    }

    if (choice == SWT.YES) {
      	new AEThread( "asyncStop", true )
		{
    		public void
			runSupport()
    		{

		      try {
		        dm.stopIt( DownloadManager.STATE_STOPPED, bDeleteTorrent, bDeleteData );
		        dm.getGlobalManager().removeDownloadManager( dm );
		      }
		      catch (GlobalManagerDownloadRemovalVetoException f) {
		        Alerts.showErrorMessageBoxUsingResourceString("globalmanager.download.remove.veto", f);
		      }
		      catch (Exception ex) {
		        Debug.printStackTrace( ex );
		      }
    		}
		}.start();
    }
  }

  private void removeSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), false, false);
      }
    });
  }

  private void stopSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.stop((DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  }

  private void queueSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.queue((DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  }

  private void resumeSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.start((DownloadManager)row.getDataSource(true));
      }
    });
  }

  private void hostSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.host(azureus_core, (DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
    MainWindow.getWindow().showMyTracker();
  }

  private void publishSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.publish(azureus_core, (DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
    MainWindow.getWindow().showMyTracker();
  }

  private void runSelectedTorrents() {
    Object[] dataSources = getSelectedDataSources();
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm != null) {
        ManagerUtils.run(dm);
      }
    }
  }
  
  private void openSelectedTorrents() {
    Object[] dataSources = getSelectedDataSources();
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm != null) {
        ManagerUtils.open(dm);
      }
    }
  }

  private void moveSelectedTorrentsDown() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = getSelectedDataSources();
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

    boolean bForceSort = rowSorter.sColumnName.equals("#");
    columnInvalidate("#");
    refresh(bForceSort);
  }

  private void moveSelectedTorrentsUp() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = getSelectedDataSources();
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

    boolean bForceSort = rowSorter.sColumnName.equals("#");
    columnInvalidate("#");
    refresh(bForceSort);
  }

	private void moveSelectedTorrents(int by) {
		// Don't use runForSelectDataSources to ensure the order we want
		Object[] dataSources = getSelectedDataSources();
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

		boolean bForceSort = rowSorter.sColumnName.equals("#");
		columnInvalidate("#");
		refresh(bForceSort);
	}

  private void moveSelectedTorrentsTop() {
    moveSelectedTorrentsTopOrEnd(true);
  }

  private void moveSelectedTorrentsEnd() {
    moveSelectedTorrentsTopOrEnd(false);
  }

  private void moveSelectedTorrentsTopOrEnd(boolean moveToTop) {
    DownloadManager[] downloadManagers = (DownloadManager[])getSelectedDataSources(new DownloadManager[0]);
    if (downloadManagers.length == 0)
      return;
    if(moveToTop)
      globalManager.moveTop(downloadManagers);
    else
      globalManager.moveEnd(downloadManagers);
    if (rowSorter.sColumnName.equals("#")) {
      columnInvalidate("#");
      refresh(true);
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    super.parameterChanged(parameterName);
    confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete", true);
  }

  private boolean top,bottom,up,down,run,host,publish,start,stop,remove;

  private void computePossibleActions() {
    Object[] dataSources = getSelectedDataSources();
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



  public void  removeDownloadBar(DownloadManager dm) {
    try{
    	downloadBars_mon.enter();
    
    	downloadBars.remove(dm);
    }finally{
    	
    	downloadBars_mon.exit();
    }
  }

  private Category addCategory() {
    CategoryAdderWindow adderWindow = new CategoryAdderWindow(MainWindow.getWindow().getDisplay());
    Category newCategory = adderWindow.getNewCategory();
    if (newCategory != null)
      assignSelectedToCategory(newCategory);
    return newCategory;
  }

  // categorymanagerlistener Functions
  public void downloadManagerAdded(Category category, final DownloadManager manager)
  {
  	if (isOurDownloadManager(manager)) {
      addDataSource(manager, true);
    }
  }

  public void downloadManagerRemoved(Category category, DownloadManager removed)
  {
    removeDataSource(removed, true);
  }


  // DownloadManagerListener Functions
  public void stateChanged(DownloadManager manager, int state) {
    final TableRowCore row = getRow(manager);
    if (row != null) {
    	Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
		    	row.refresh(true);
				}
    	});
    }
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }
  
  public void completionChanged(final DownloadManager manager, boolean bCompleted) {
    // manager has moved lists
	  
    if ((isSeedingView && bCompleted) || (!isSeedingView && !bCompleted)) {
    	
    		// only make the download visible if it satisfies the category selection
    	
    	if ( currentCategory == null || currentCategory.getType() == Category.TYPE_ALL ){
    		
    		addDataSource(manager, true);
    		
    	}else{
    	
    		int catType = currentCategory.getType();
    	
    		Category	manager_category = manager.getDownloadState().getCategory();
    		
   	        if ( manager_category == null ){
   	         
   	        	if ( catType == Category.TYPE_UNCATEGORIZED){
  	
    	        	addDataSource(manager, true);
    	        }
    		}else{
    			
    			if ( currentCategory.getName().equals( manager_category.getName()))
   
    				addDataSource(manager, true);
    		}
    	}
    }else if ((isSeedingView && !bCompleted) || (!isSeedingView && bCompleted)) {
     
    	removeDataSource(manager, true);
    }
  }

  public void downloadComplete(DownloadManager manager) {
  }

  // Category Stuff
  private void assignSelectedToCategory(final Category category) {
    runForSelectedRows(new GroupTableRowRunner() {
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

		int catType = (currentCategory == null) ? Category.TYPE_ALL
				: currentCategory.getType();
		java.util.List managers;
		if (catType == Category.TYPE_USER)
			managers = currentCategory.getDownloadManagers();
		else
			managers = globalManager.getDownloadManagers();

		java.util.List managersToAdd = new ArrayList();
		removeAllTableRows();

		for (int i = 0; i < managers.size(); i++) {
			DownloadManager manager = (DownloadManager) managers.get(i);
			if (isOurDownloadManager(manager)
					&& (catType != Category.TYPE_UNCATEGORIZED || manager
							.getDownloadState().getCategory() == null)) {
				managersToAdd.add(manager);
			}
		}
    addDataSources(managersToAdd.toArray(), true);
    refreshTable(false);
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

    if (skipDMAdding ||
        (currentCategory != null && currentCategory.getType() == Category.TYPE_USER))
      return;
    Category cat = dm.getDownloadState().getCategory();
    if (cat == null)
      downloadManagerAdded(null, dm);
  }

  public void downloadManagerRemoved( DownloadManager dm ) {
    dm.removeListener( this );

    MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(dm);
    if (mw != null) mw.close();

    if (skipDMAdding ||
        (currentCategory != null && currentCategory.getType() == Category.TYPE_USER))
      return;
    downloadManagerRemoved(null, dm);
  }

  public void destroyInitiated() {  }
  public void destroyed() { }
  public void seedingStatusChanged( boolean seeding_only_mode ){}       

  // End of globalmanagerlistener Functions
  
  private void setSelectedTorrentsUpSpeed(int speed) {      
    Object[] dms = getSelectedDataSources();
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
    Object[] dms = getSelectedDataSources();
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

	public synchronized void addDataSources(Object[] dataSources, boolean bImmediate) {
		super.addDataSources(dataSources, bImmediate);
		if (bImmediate && tableLabel != null && !tableLabel.isDisposed()) {
			updateTableLabel();
		}
	}

	public synchronized void removeDataSources(Object[] dataSources, boolean bImmediate) {
		super.removeDataSources(dataSources, bImmediate);
		
		if (bImmediate && tableLabel != null && !tableLabel.isDisposed()) {
			updateTableLabel();
		}
	}
	

	public void updateLanguage() {
		super.updateLanguage();
		updateTableLabel();
	}

	/**
	 * 
	 */
	private void updateTableLabel() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (tableLabel != null && !tableLabel.isDisposed()) {
					String sText = MessageText.getString(sTableID + "View.header") + " ("
							+ getRowCount() + ")";
					if (sLastSearch.length() > 0) {
						sText += " "
								+ MessageText.getString("MyTorrentsView.filter",
										new String[] { sLastSearch });
						if (bRegexSearch) {
							try {
								Pattern.compile(sLastSearch, Pattern.CASE_INSENSITIVE);
							} catch (Exception e) {
								sText += " " + e.getMessage();
							}
						}
					}
					tableLabel.setText(sText);
				}
			}
		});
	}
}
