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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.torrent.HasBeenOpenedListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItemListener;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView.ToolBarViewListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarVitalityImageSWT;

/**
 * @author TuxPaper
 * @created Jul 2, 2008
 *
 */
public class SBC_LibraryView
	extends SkinView
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

	private static final String ID_VITALITY_ACTIVE = "image.sidebar.vitality.dl";

	private static final String ID_VITALITY_ALERT = "image.sidebar.vitality.alert";

	private static final long DL_VITALITY_REFRESH_RATE = 15000;

	private static final boolean DL_VITALITY_CONSTANT = true;

	private static class stats
	{
		private int numSeeding = 0;

		private int numDownloading = 0;

		private int numComplete = 0;

		private int numIncomplete = 0;

		private int numErrorComplete = 0;

		private String errorInCompleteTooltip;

		private int numErrorInComplete = 0;

		private String errorCompleteTooltip;

		private int numUnOpened = 0;

		private int numStopped = 0;

		public boolean includeLowNoise;
	};

	private static stats statsWithLowNoise = new stats();

	private static stats statsNoLowNoise = new stats();

	private static List<countRefreshListener> listeners = new ArrayList<countRefreshListener>();

	private static boolean first = true;

	private int viewMode = -1;

	private SWTSkinButtonUtility btnSmallTable;

	private SWTSkinButtonUtility btnBigTable;

	private SWTSkinObject soListArea;

	private int torrentFilterMode = TORRENTS_ALL;

	private String torrentFilter;

	private ToolBarItem itemModeSmall;

	private ToolBarItem itemModeBig;

	private SWTSkinObject soWait;

	private SWTSkinObject soWaitProgress;

	private SWTSkinObjectText soWaitTask;

	private int waitProgress = 0;

	private SWTSkinObjectText soLibraryInfo;

	static {
		statsNoLowNoise.includeLowNoise = false;
		statsWithLowNoise.includeLowNoise = true;
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
				addCountRefreshListener(new countRefreshListener() {
					// @see com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.countRefreshListener#countRefreshed(com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.stats, com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.stats)
					public void countRefreshed(stats statsWithLowNoise,
							stats statsNoLowNoise) {
						stats stats = viewMode == MODE_SMALLTABLE
								? statsWithLowNoise : statsNoLowNoise;
						int total = stats.numComplete + stats.numIncomplete;
						soLibraryInfo.setText(total + " items: " + stats.numSeeding
								+ " seeding, " + stats.numDownloading + " downloading, "
								+ stats.numStopped + " stopped");
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

		SkinViewManager.addListener(ToolBarView.class,
				new SkinViewManager.SkinViewManagerListener() {
					public void skinViewAdded(SkinView skinview) {
						if (skinview instanceof ToolBarView) {
							ToolBarView tbv = (ToolBarView) skinview;
							tbv.addListener(new ToolBarViewListener() {
								public void toolbarViewInitialized(ToolBarView tbv) {
									initToolBarView(tbv);
								}
							});
						}
					}
				});

		if (first) {
			AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
				public void azureusCoreRunning(AzureusCore core) {
					setupViewTitleWithCore(core);
				}
			});
			first = false;
		}
		return null;
	}

	protected void initToolBarView(ToolBarView tb) {
		itemModeSmall = tb.getToolBarItem("modeSmall");
		if (itemModeSmall != null) {
			itemModeSmall.addListener(new ToolBarItemListener() {
				public void pressed(ToolBarItem toolBarItem) {
					if (isVisible()) {
						setViewMode(MODE_SMALLTABLE, true);
					}
				}

				public boolean held(ToolBarItem toolBarItem) {
					return false;
				}
			});
		}
		itemModeBig = tb.getToolBarItem("modeBig");
		if (itemModeBig != null) {
			itemModeBig.addListener(new ToolBarItemListener() {
				public void pressed(ToolBarItem toolBarItem) {
					if (isVisible()) {
						setViewMode(MODE_BIGTABLE, true);
					}
				}

				public boolean held(ToolBarItem toolBarItem) {
					return false;
				}
			});
		}

		if (isVisible()) {
			setupModeButtons();
		}
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
				itemModeSmall.setEnabled(true);
				itemModeSmall.getSkinButton().getSkinObject().switchSuffix(
						viewMode == MODE_BIGTABLE ? "" : "-down");
			}
			ToolBarItem itemModeBig = tb.getToolBarItem("modeBig");
			if (itemModeBig != null) {
				itemModeBig.setEnabled(true);
				itemModeBig.getSkinButton().getSkinObject().switchSuffix(
						viewMode == MODE_BIGTABLE ? "-down" : "");
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		return super.skinObjectHidden(skinObject, params);
	}

	public int getViewMode() {
		return viewMode;
	}

	public void setViewMode(int viewMode, boolean save) {
		if (viewMode >= modeViewIDs.length || viewMode < 0
				|| viewMode == this.viewMode) {
			return;
		}

		if (itemModeSmall != null) {
			itemModeSmall.getSkinButton().getSkinObject().switchSuffix(
					viewMode == MODE_BIGTABLE ? "" : "-down");
		}
		if (itemModeBig != null) {
			itemModeBig.getSkinButton().getSkinObject().switchSuffix(
					viewMode == MODE_BIGTABLE ? "-down" : "");
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

		for (countRefreshListener l : listeners) {
			l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		}
	}

	public static void setupViewTitle() {

		final ViewTitleInfo titleInfoDownloading = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					if (statsNoLowNoise.numIncomplete > 0)
						return statsNoLowNoise.numIncomplete + ""; // + " of " + numIncomplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + statsNoLowNoise.numIncomplete
							+ " incomplete torrents, " + statsNoLowNoise.numDownloading
							+ " of which are currently downloading";
				}

				return null;
			}
		};
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		MdiEntry infoDL = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		if (infoDL != null) {
			MdiEntryVitalityImage vitalityImage = infoDL.addVitalityImage(ID_VITALITY_ACTIVE);
			vitalityImage.setVisible(false);

			vitalityImage = infoDL.addVitalityImage(ID_VITALITY_ALERT);
			vitalityImage.setVisible(false);

			infoDL.setViewTitleInfo(titleInfoDownloading);

			if (!DL_VITALITY_CONSTANT) {
				SimpleTimer.addPeriodicEvent("DLVitalityRefresher",
						DL_VITALITY_REFRESH_RATE, new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
								MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
								for (int i = 0; i < vitalityImages.length; i++) {
									MdiEntryVitalityImage vitalityImage = vitalityImages[i];
									if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
										refreshDLSpinner((SideBarVitalityImageSWT) vitalityImage);
									}
								}
							}
						});
			}
		}

		final ViewTitleInfo titleInfoSeeding = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return null; //numSeeding + " of " + numComplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + statsNoLowNoise.numComplete
							+ " complete torrents, " + statsNoLowNoise.numSeeding
							+ " of which are currently seeding";
				}
				return null;
			}
		};
		MdiEntry infoCD = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		if (infoCD != null) {
			MdiEntryVitalityImage vitalityImage = infoCD.addVitalityImage(ID_VITALITY_ALERT);
			vitalityImage.setVisible(false);

			infoCD.setViewTitleInfo(titleInfoSeeding);
		}

		MdiEntry infoLibraryUn = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED);
		if (infoLibraryUn != null) {
			infoLibraryUn.setViewTitleInfo(new ViewTitleInfo() {
				public Object getTitleInfoProperty(int propertyID) {
					if (propertyID == TITLE_INDICATOR_TEXT
							&& statsNoLowNoise.numUnOpened > 0) {
						return "" + statsNoLowNoise.numUnOpened;
					}
					return null;
				}
			});
		}

		if (first) {
			AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
				public void azureusCoreRunning(AzureusCore core) {
					setupViewTitleWithCore(core);
				}
			});
			first = false;
		}
		PlatformTorrentUtils.addHasBeenOpenedListener(new HasBeenOpenedListener() {
			public void hasBeenOpenedChanged(DownloadManager dm, boolean opened) {
				recountUnopened();
				refreshAllLibraries();
			}
		});
	}

	protected static void setupViewTitleWithCore(AzureusCore core) {
		final GlobalManager gm = core.getGlobalManager();
		final DownloadManagerListener dmListener = new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager dm, int state) {
				stateChanged(dm, state, statsNoLowNoise);
				stateChanged(dm, state, statsWithLowNoise);
			}

			public void stateChanged(DownloadManager dm, int state, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				updateDMCounts(dm);

				boolean complete = dm.getAssumedComplete();
				Boolean wasErrorStateB = (Boolean) dm.getUserData("wasErrorState");
				boolean wasErrorState = wasErrorStateB == null ? false
						: wasErrorStateB.booleanValue();
				boolean isErrorState = state == DownloadManager.STATE_ERROR;
				if (isErrorState != wasErrorState) {
					int rel = isErrorState ? 1 : -1;
					if (complete) {
						stats.numErrorComplete += rel;
					} else {
						stats.numErrorInComplete += rel;
					}
					updateErrorTooltip(stats);
					dm.setUserData("wasErrorState", new Boolean(isErrorState));
				}
				refreshAllLibraries();
			}

			public void completionChanged(DownloadManager dm, boolean completed) {
				completionChanged(dm, completed, statsNoLowNoise);
				completionChanged(dm, completed, statsWithLowNoise);
			}

			public void completionChanged(DownloadManager dm, boolean completed,
					stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				updateDMCounts(dm);
				if (completed) {
					stats.numComplete++;
					stats.numIncomplete--;
					if (dm.getState() == DownloadManager.STATE_ERROR) {
						stats.numErrorComplete++;
						stats.numErrorInComplete--;
					}
				} else {
					stats.numComplete--;
					stats.numIncomplete++;

					if (dm.getState() == DownloadManager.STATE_ERROR) {
						stats.numErrorComplete--;
						stats.numErrorInComplete++;
					}
				}
				recountUnopened();
				updateErrorTooltip(stats);
				refreshAllLibraries();
			}

			protected void updateErrorTooltip(stats stats) {
				if (stats.numErrorComplete < 0) {
					stats.numErrorComplete = 0;
				}
				if (stats.numErrorInComplete < 0) {
					stats.numErrorInComplete = 0;
				}

				if (stats.numErrorComplete > 0 || stats.numErrorInComplete > 0) {

					String comp_error = null;
					String incomp_error = null;

					List downloads = gm.getDownloadManagers();

					for (int i = 0; i < downloads.size(); i++) {

						DownloadManager download = (DownloadManager) downloads.get(i);

						if (download.getState() == DownloadManager.STATE_ERROR) {

							if (download.getAssumedComplete()) {

								if (comp_error == null) {

									comp_error = download.getDisplayName() + ": "
											+ download.getErrorDetails();
								} else {

									comp_error += "...";
								}
							} else {
								if (incomp_error == null) {

									incomp_error = download.getDisplayName() + ": "
											+ download.getErrorDetails();
								} else {

									incomp_error += "...";
								}
							}
						}
					}

					stats.errorCompleteTooltip = comp_error;
					stats.errorInCompleteTooltip = incomp_error;
				}
			}
		};

		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				downloadManagerRemoved(dm, statsNoLowNoise);
				downloadManagerRemoved(dm, statsWithLowNoise);
			}

			public void downloadManagerRemoved(DownloadManager dm, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				recountUnopened();
				if (dm.getAssumedComplete()) {
					stats.numComplete--;
					Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
					if (wasDownloadingB != null && wasDownloadingB.booleanValue()) {
						stats.numDownloading--;
					}
				} else {
					stats.numIncomplete--;
					Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
					if (wasSeedingB != null && wasSeedingB.booleanValue()) {
						stats.numSeeding--;
					}
				}

				Boolean wasStoppedB = (Boolean) dm.getUserData("wasStopped");
				boolean wasStopped = wasStoppedB == null ? false
						: wasStoppedB.booleanValue();
				if (wasStopped) {
					stats.numStopped--;
				}

				refreshAllLibraries();
				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				downloadManagerAdded(dm, statsNoLowNoise);
				downloadManagerAdded(dm, statsWithLowNoise);
			}

			public void downloadManagerAdded(DownloadManager dm, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				dm.addListener(dmListener, false);

				recountUnopened();
				if (dm.getAssumedComplete()) {
					stats.numComplete++;
					if (dm.getState() == DownloadManager.STATE_SEEDING) {
						stats.numSeeding++;
					}
				} else {
					stats.numIncomplete++;
					if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
						dm.setUserData("wasDownloading", new Boolean(true));
						stats.numSeeding++;
					} else {
						dm.setUserData("wasDownloading", new Boolean(false));
					}
				}
				refreshAllLibraries();
			}
		}, false);
		List downloadManagers = gm.getDownloadManagers();
		for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			boolean lowNoise = PlatformTorrentUtils.isAdvancedViewOnly(dm);
			dm.addListener(dmListener, false);
			int state = dm.getState();
			if (state == DownloadManager.STATE_STOPPED) {
				statsWithLowNoise.numStopped++;
				if (!lowNoise) {
					statsNoLowNoise.numStopped++;
				}
			}
			if (dm.getAssumedComplete()) {
				statsWithLowNoise.numComplete++;
				if (!lowNoise) {
					statsNoLowNoise.numComplete++;
				}
				if (state == DownloadManager.STATE_SEEDING) {
					dm.setUserData("wasSeeding", new Boolean(true));
					statsWithLowNoise.numSeeding++;
					if (!lowNoise) {
						statsNoLowNoise.numSeeding++;
					}
				} else {
					dm.setUserData("wasSeeding", new Boolean(false));
				}
			} else {
				statsWithLowNoise.numIncomplete++;
				if (!lowNoise) {
					statsNoLowNoise.numIncomplete++;
				}
				if (state == DownloadManager.STATE_DOWNLOADING) {
					statsWithLowNoise.numSeeding++;
					if (!lowNoise) {
						statsNoLowNoise.numSeeding++;
					}
				}
			}
		}

		recountUnopened();
		refreshAllLibraries();
	}

	protected static void updateDMCounts(DownloadManager dm) {
		boolean isSeeding;
		boolean isDownloading;
		boolean isStopped;

		Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
		boolean wasSeeding = wasSeedingB == null ? false
				: wasSeedingB.booleanValue();
		Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
		boolean wasDownloading = wasDownloadingB == null ? false
				: wasDownloadingB.booleanValue();
		Boolean wasStoppedB = (Boolean) dm.getUserData("wasStopped");
		boolean wasStopped = wasStoppedB == null ? false
				: wasStoppedB.booleanValue();

		if (dm.getAssumedComplete()) {
			isSeeding = dm.getState() == DownloadManager.STATE_SEEDING;
			isDownloading = false;
		} else {
			isDownloading = dm.getState() == DownloadManager.STATE_DOWNLOADING;
			isSeeding = false;
		}

		isStopped = dm.getState() == DownloadManager.STATE_STOPPED;
		boolean lowNoise = PlatformTorrentUtils.isAdvancedViewOnly(dm);

		if (isDownloading != wasDownloading) {
			if (isDownloading) {
				statsWithLowNoise.numDownloading++;
				if (!lowNoise) {
					statsNoLowNoise.numDownloading++;
				}
			} else {
				statsWithLowNoise.numDownloading--;
				if (!lowNoise) {
					statsNoLowNoise.numDownloading--;
				}
			}
			dm.setUserData("wasDownloading", new Boolean(isDownloading));
		}

		if (isSeeding != wasSeeding) {
			if (isSeeding) {
				statsWithLowNoise.numSeeding++;
				if (!lowNoise) {
					statsNoLowNoise.numSeeding++;
				}
			} else {
				statsWithLowNoise.numSeeding--;
				if (!lowNoise) {
					statsNoLowNoise.numSeeding--;
				}
			}
			dm.setUserData("wasSeeding", new Boolean(isSeeding));
		}

		if (isStopped != wasStopped) {
			if (isStopped) {
				statsWithLowNoise.numStopped++;
				if (!lowNoise) {
					statsNoLowNoise.numStopped++;
				}
			} else {
				statsWithLowNoise.numStopped--;
				if (!lowNoise) {
					statsNoLowNoise.numStopped--;
				}
			}
			dm.setUserData("wasStopped", new Boolean(isStopped));
		}

	}

	private static void recountUnopened() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return;
		}
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		List dms = gm.getDownloadManagers();
		statsNoLowNoise.numUnOpened = 0;
		for (Iterator iter = dms.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (!PlatformTorrentUtils.getHasBeenOpened(dm) && dm.getAssumedComplete()) {
				statsNoLowNoise.numUnOpened++;
			}
		}
		statsWithLowNoise.numUnOpened = statsNoLowNoise.numUnOpened;
	}

	protected static void addCountRefreshListener(countRefreshListener l) {
		l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		listeners.add(l);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	protected static void refreshAllLibraries() {
		for (countRefreshListener l : listeners) {
			l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		}
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}
		MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		if (entry != null) {
			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				MdiEntryVitalityImage vitalityImage = vitalityImages[i];
				if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
					vitalityImage.setVisible(statsNoLowNoise.numDownloading > 0);

					refreshDLSpinner((SideBarVitalityImageSWT) vitalityImage);

				} else if (vitalityImage.getImageID().equals(ID_VITALITY_ALERT)) {
					vitalityImage.setVisible(statsNoLowNoise.numErrorInComplete > 0);
					if (statsNoLowNoise.numErrorInComplete > 0) {
						vitalityImage.setToolTip(statsNoLowNoise.errorInCompleteTooltip);
					}
				}
			}
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		}

		entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		if (entry != null) {
			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				MdiEntryVitalityImage vitalityImage = vitalityImages[i];
				if (vitalityImage.getImageID().equals(ID_VITALITY_ALERT)) {
					vitalityImage.setVisible(statsNoLowNoise.numErrorComplete > 0);
					if (statsNoLowNoise.numErrorComplete > 0) {
						vitalityImage.setToolTip(statsNoLowNoise.errorCompleteTooltip);
					}
				}
			}
		}

		entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED);
		if (entry != null) {
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		}
	}

	public static void refreshDLSpinner(SideBarVitalityImageSWT vitalityImage) {
		if (DL_VITALITY_CONSTANT) {
			return;
		}

		if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
			if (!vitalityImage.isVisible()) {
				return;
			}
			SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();
			if (sm != null) {
				GlobalManagerStats stats = AzureusCoreFactory.getSingleton().getGlobalManager().getStats();

				int delay = 100;
				int limit = NetworkManager.getMaxDownloadRateBPS();
				if (limit <= 0) {
					limit = sm.getEstimatedDownloadCapacityBytesPerSec().getBytesPerSec();
				}

				// smoothing
				int current = stats.getDataReceiveRate() / 10;
				limit /= 10;

				if (limit > 0) {
					if (current > limit) {
						delay = 25;
					} else {
						// 40 incrememnts of 5.. max 200
						current += 39;
						delay = (40 - (current * 40 / limit)) * 5;
						if (delay < 35) {
							delay = 35;
						} else if (delay > 200) {
							delay = 200;
						}
					}
					if (vitalityImage instanceof SideBarVitalityImageSWT) {
						SideBarVitalityImageSWT viSWT = (SideBarVitalityImageSWT) vitalityImage;
						if (viSWT.getDelayTime() != delay) {
							viSWT.setDelayTime(delay);
							//System.out.println("new delay: " + delay + "; via " + current + " / " + limit);
						}
					}
				}
			}
		}
	}

	public static String getTableIdFromFilterMode(int torrentFilterMode,
			boolean big) {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_COMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_COMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_INCOMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			return big ? TableManager.TABLE_MYTORRENTS_UNOPENED_BIG
					: TableManager.TABLE_MYTORRENTS_UNOPENED;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return TableManager.TABLE_MYTORRENTS_ALL_BIG;
		}
		return null;
	}

	protected static interface countRefreshListener
	{
		public void countRefreshed(stats statsWithLowNoise, stats statsNoLowNoise);
	}
}
