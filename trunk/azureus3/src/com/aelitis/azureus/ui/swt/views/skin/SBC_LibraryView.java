/**
 * Created on Jul 2, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Map;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView.ToolBarViewListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;

/**
 * @author TuxPaper
 * @created Jul 2, 2008
 *
 */
public class SBC_LibraryView
	extends SkinView implements ToolBarEnabler
{
	private final static String ID = "library-list";

	public final static int MODE_BIGTABLE = 0;

	public final static int MODE_SMALLTABLE = 1;

	public static final int TORRENTS_ALL = 0;

	public static final int TORRENTS_COMPLETE = 1;

	public static final int TORRENTS_INCOMPLETE = 2;

	public static final int TORRENTS_UNOPENED = 3;

	private final static String[] modeViewIDs = {
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_BIG,
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_SMALL
	};

	private final static String[] modeIDs = {
		"library.table.big",
		"library.table.small"
	};

	private int viewMode = -1;

	private SWTSkinButtonUtility btnSmallTable;

	private SWTSkinButtonUtility btnBigTable;

	private SWTSkinObject soListArea;

	private int torrentFilterMode = TORRENTS_ALL;

	private String torrentFilter;

	private SWTSkinObject soWait;

	private SWTSkinObject soWaitProgress;

	private SWTSkinObjectText soWaitTask;

	private int waitProgress = 0;

	private SWTSkinObjectText soLibraryInfo;

	private Object datasource;

	public void setViewMode(int viewMode, boolean save) {
		if (viewMode >= modeViewIDs.length || viewMode < 0
				|| viewMode == this.viewMode) {
			return;
		}

		int oldViewMode = this.viewMode;

		this.viewMode = viewMode;

		if (oldViewMode >= 0 && oldViewMode < modeViewIDs.length) {
			SWTSkinObject soOldViewArea = getSkinObject(modeViewIDs[oldViewMode]);
			//SWTSkinObject soOldViewArea = skin.getSkinObjectByID(modeIDs[oldViewMode]);
			if (soOldViewArea != null) {
				soOldViewArea.setVisible(false);
			}
		}

		SelectedContentManager.clearCurrentlySelectedContent();

		SWTSkinObject soViewArea = getSkinObject(modeViewIDs[viewMode]);
		if (soViewArea == null) {
			soViewArea = skin.createSkinObject(modeIDs[viewMode] + torrentFilterMode,
					modeIDs[viewMode], soListArea);
			skin.layout();
			soViewArea.setVisible(true);
			soViewArea.getControl().setLayoutData(Utils.getFilledFormData());
		} else {
			soViewArea.setVisible(true);
		}

		if (save) {
			COConfigurationManager.setParameter(torrentFilter + ".viewmode", viewMode);
		}

		String entryID = null;
		if (torrentFilterMode == TORRENTS_ALL) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY;
		} else if (torrentFilterMode == TORRENTS_COMPLETE) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY_CD;
		} else if (torrentFilterMode == TORRENTS_INCOMPLETE) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY_DL;
		} else if (torrentFilterMode == TORRENTS_UNOPENED) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED;
		}

		if (entryID != null) {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			MdiEntry entry = mdi.getEntry(entryID);
			if (entry != null) {
				entry.setLogID(entryID + "-" + viewMode);
			}
		}

		SB_Transfers.triggerCountRefreshListeners();

		setupModeButtons();
	}


	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		soWait = null;
		try {
			soWait = getSkinObject("library-wait");
			soWaitProgress = getSkinObject("library-wait-progress");
			soWaitTask = (SWTSkinObjectText) getSkinObject("library-wait-task");
			if (soWaitProgress != null) {
				soWaitProgress.getControl().addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent e) {
						Control c = (Control) e.widget;
						Point size = c.getSize();
						e.gc.setBackground(ColorCache.getColor(e.display, "#23a7df"));
						int breakX = size.x * waitProgress / 100;
						e.gc.fillRectangle(0, 0, breakX, size.y);
						e.gc.setBackground(ColorCache.getColor(e.display, "#cccccc"));
						e.gc.fillRectangle(breakX, 0, size.x - breakX, size.y);
					}
				});
			}

			soLibraryInfo = (SWTSkinObjectText) getSkinObject("library-info");
			if (soLibraryInfo != null) {
				SB_Transfers.addCountRefreshListener(new SB_Transfers.countRefreshListener() {
					// @see com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.countRefreshListener#countRefreshed(com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.stats, com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.stats)
					public void countRefreshed(SB_Transfers.stats statsWithLowNoise,
							SB_Transfers.stats statsNoLowNoise) {
						SB_Transfers.stats stats = viewMode == MODE_SMALLTABLE
						? statsWithLowNoise : statsNoLowNoise;
						if (torrentFilterMode == TORRENTS_INCOMPLETE) {
							String id = "library.incomplete.header";
							if (stats.numDownloading != 1) {
								id += ".p";
							}
							String s = MessageText.getString(id,
									new String[] {
								String.valueOf(stats.numDownloading),
								String.valueOf(stats.numIncomplete - stats.numDownloading),
							});
							soLibraryInfo.setText(s);
						} else if (torrentFilterMode == TORRENTS_ALL) {
							if (datasource instanceof Category) {
								Category cat = (Category) datasource;
								
								String id = "library.category.header";
								String s = MessageText.getString(id,
										new String[] {
									cat.getName()
								});
								soLibraryInfo.setText(s);

							} else {
  							String id = "library.all.header";
  							if (stats.numComplete + stats.numIncomplete != 1) {
  								id += ".p";
  							}
  							String s = MessageText.getString(id,
  									new String[] {
  								String.valueOf(stats.numComplete + stats.numIncomplete),
  								String.valueOf(stats.numSeeding + stats.numDownloading),
  							});
  							soLibraryInfo.setText(s);
							}
						} else if (torrentFilterMode == TORRENTS_UNOPENED) {
							String id = "library.unopened.header";
							if (stats.numUnOpened != 1) {
								id += ".p";
							}
							String s = MessageText.getString(id,
									new String[] {
								String.valueOf(stats.numUnOpened),
							});
							soLibraryInfo.setText(s);
						}
					}
				});
			}
		} catch (Exception e) {
		}

		AzureusCore core = AzureusCoreFactory.getSingleton();
		if (!AzureusCoreFactory.isCoreRunning()) {
			if (soWait != null) {
				soWait.setVisible(true);
				//soWait.getControl().getParent().getParent().getParent().layout(true, true);
			}
			final Initializer initializer = Initializer.getLastInitializer();
			if (initializer != null) {
				initializer.addListener(new InitializerListener() {
					public void reportPercent(final int percent) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if (soWaitProgress != null && !soWaitProgress.isDisposed()) {
									waitProgress = percent;
									soWaitProgress.getControl().redraw();
									soWaitProgress.getControl().update();
								}
							}
						});
						if (percent > 100) {
							initializer.removeListener(this);
						}
					}

					public void reportCurrentTask(String currentTask) {
						if (soWaitTask != null && !soWaitTask.isDisposed()) {
							soWaitTask.setText(currentTask);
						}
					}
				});
			}
		}

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (soWait != null) {
							soWait.setVisible(false);
						}
					}
				});
			}
		});

		torrentFilter = skinObject.getSkinObjectID();
		if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_DL)) {
			torrentFilterMode = TORRENTS_INCOMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_CD)) {
			torrentFilterMode = TORRENTS_COMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED)) {
			torrentFilterMode = TORRENTS_UNOPENED;
		}

		soListArea = getSkinObject(ID + "-area");

		soListArea.getControl().setData("TorrentFilterMode",
				new Long(torrentFilterMode));
		soListArea.getControl().setData("DataSource",
				datasource);

		setViewMode(
				COConfigurationManager.getIntParameter(torrentFilter + ".viewmode"),
				false);

		SWTSkinObject so;
		so = getSkinObject(ID + "-button-smalltable");
		if (so != null) {
			btnSmallTable = new SWTSkinButtonUtility(so);
			btnSmallTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					setViewMode(MODE_SMALLTABLE, true);
				}
			});
		}

		so = getSkinObject(ID + "-button-bigtable");
		if (so != null) {
			btnBigTable = new SWTSkinButtonUtility(so);
			btnBigTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					setViewMode(MODE_BIGTABLE, true);
				}
			});
		}
		
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntry entry = mdi.getEntryFromSkinObject(skinObject);
			if (entry != null) {
				entry.addToolbarEnabler(this);
			}
		}

		SkinViewManager.addListener(ToolBarView.class,
				new SkinViewManager.SkinViewManagerListener() {
					public void skinViewAdded(SkinView skinview) {
						if (skinview instanceof ToolBarView) {
							ToolBarView tbv = (ToolBarView) skinview;
							tbv.addListener(new ToolBarViewListener() {
								public void toolbarViewInitialized(ToolBarView tbv) {
									setupModeButtons();
								}
							});
						}
					}
				});

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				SB_Transfers.setupViewTitleWithCore(core);
			}
		});
		return null;
	}

	public void refreshToolBar(Map<String, Boolean> list) {
		list.put("modeSmall", true);
		list.put("modeBig", true);
	}
	
	public boolean toolBarItemActivated(String itemKey) {
		if (itemKey.equals("modeSmall")) {
			if (isVisible()) {
				setViewMode(MODE_SMALLTABLE, true);
			}
		}
		if (itemKey.equals("modeBig")) {
			if (isVisible()) {
				setViewMode(MODE_BIGTABLE, true);
			}
		}
		return false;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);

		setupModeButtons();
		return null;
	}

	private void setupModeButtons() {
		
		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb != null) {
			ToolBarItem itemModeSmall = tb.getToolBarItem("modeSmall");
			if (itemModeSmall != null) {
				itemModeSmall.getSkinButton().getSkinObject().switchSuffix(
						viewMode == MODE_BIGTABLE ? "" : "-down");
			}
			ToolBarItem itemModeBig = tb.getToolBarItem("modeBig");
			if (itemModeBig != null) {
				itemModeBig.getSkinButton().getSkinObject().switchSuffix(
						viewMode == MODE_BIGTABLE ? "-down" : "");
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		return super.skinObjectHidden(skinObject, params);
	}
	
	public Object dataSourceChanged(SWTSkinObject skinObject, Object params) {
		datasource = params;
		if (soListArea != null) {
  		soListArea.getControl().setData("DataSource",
  				params);
		}
		
		return null;
	}

	public int getViewMode() {
		return viewMode;
	}

}
