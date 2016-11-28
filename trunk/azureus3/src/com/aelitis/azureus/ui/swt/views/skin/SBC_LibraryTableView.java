/**
 * Created on Jul 3, 2008
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.TableRowSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.ISelectedVuzeFileContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.PlayUtils;

/**
 * Classic My Torrents view wrapped in a SkinView
 * 
 * @author TuxPaper
 * @created Jul 3, 2008
 *
 */
public class SBC_LibraryTableView
	extends SkinView
	implements UIUpdatable, ObfusticateImage, UIPluginViewToolBarListener
{
	private final static String ID = "SBC_LibraryTableView";

	private Composite viewComposite;
	
	private TableViewSWT<?> tv;

	protected int torrentFilterMode = SBC_LibraryView.TORRENTS_ALL;

	private SWTSkinObject soParent;

	private MyTorrentsView torrentView;
	
	private UISWTViewEventListener swtViewListener;

	private UISWTViewImpl view;
	
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		soParent = skinObject.getParent();
		
  	AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (soParent == null || soParent.isDisposed()) {
							return;
						}
						initShow(core);
					}
				});
			}
  	});

		return null;
	}

	public void initShow(AzureusCore core) {
		Object data = soParent.getControl().getData("TorrentFilterMode");
		if (data instanceof Long) {
			torrentFilterMode = (int) ((Long) data).longValue();
		}
		
		data = soParent.getControl().getData("DataSource");
		
		boolean useBigTable = useBigTable();
		
		SWTSkinObjectTextbox soFilter = (SWTSkinObjectTextbox) skin.getSkinObject(
				"library-filter", soParent.getParent());
		Text txtFilter = soFilter == null ? null : soFilter.getTextControl();
		
		SWTSkinObjectContainer soCats = (SWTSkinObjectContainer) skin.getSkinObject(
				"library-categories", soParent.getParent());
		Composite cCats = soCats == null ? null : soCats.getComposite();

		// columns not needed for small mode, all torrents
		TableColumnCore[] columns = useBigTable
				|| torrentFilterMode != SBC_LibraryView.TORRENTS_ALL ? getColumns()
				: null;

		if (null != columns) {
			TableColumnManager tcManager = TableColumnManager.getInstance();
			tcManager.addColumns(columns);
		}

		if (useBigTable) {
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE
					|| torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE
					|| torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {

				swtViewListener = torrentView = new MyTorrentsView_Big(core, torrentFilterMode,
						columns, txtFilter, cCats);

			} else {
				swtViewListener = torrentView = new MyTorrentsView_Big(core, torrentFilterMode,
						columns, txtFilter, cCats);
			}

		} else {
			String tableID = SB_Transfers.getTableIdFromFilterMode(
					torrentFilterMode, false);
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
				swtViewListener = torrentView = new MyTorrentsView(core, tableID, true, columns, txtFilter,
						cCats,true);

			} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
				swtViewListener = torrentView = new MyTorrentsView(core, tableID, false, columns, txtFilter,
						cCats,true);

			} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
				swtViewListener = torrentView = new MyTorrentsView(core, tableID, true, columns, txtFilter,
						cCats, true) {
					public boolean isOurDownloadManager(DownloadManager dm) {
						if (PlatformTorrentUtils.getHasBeenOpened(dm)) {
							return false;
						}
						return super.isOurDownloadManager(dm);
					}
				};
			} else {
				swtViewListener = new MyTorrentsSuperView(txtFilter, cCats) {
					public void initializeDone() {
						MyTorrentsView seedingview = getSeedingview();
						if (seedingview != null) {
							seedingview.overrideDefaultSelected(new TableSelectionAdapter() {
								public void defaultSelected(TableRowCore[] rows, int stateMask) {
									doDefaultClick(rows, stateMask, false);
								}
							});
							MyTorrentsView torrentview = getTorrentview();
							if (torrentview != null) {
								torrentview.overrideDefaultSelected(new TableSelectionAdapter() {
									public void defaultSelected(TableRowCore[] rows, int stateMask) {
										doDefaultClick(rows, stateMask, false);
									}
								});
							}
						}
					}
				};
			}
			
			if (torrentView != null) {
				torrentView.overrideDefaultSelected(new TableSelectionAdapter() {
					public void defaultSelected(TableRowCore[] rows, int stateMask) {
						doDefaultClick(rows, stateMask, false);
					}
				});
			}
		}

		if (torrentView != null) {
			tv = torrentView.getTableView();
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
				torrentView.setRebuildListOnFocusGain(true);
			}
		}

		try {
			view = new UISWTViewImpl(ID + torrentFilterMode, UISWTInstance.VIEW_MAIN, false);
			view.setDatasource(data);
			view.setEventListener(swtViewListener, true);
		} catch (Exception e) {
			Debug.out(e);
		}

		SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
				skin.getSkinProperties(), getUpdateUIName(), "", soMain);

		skin.layout();

		viewComposite = soContents.getComposite();
		viewComposite.setLayoutData(Utils.getFilledFormData());
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
		viewComposite.setLayout(gridLayout);

		view.initialize(viewComposite);


		SWTSkinObject soSizeSlider = skin.getSkinObject("table-size-slider", soParent.getParent());
		if (soSizeSlider instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer so = (SWTSkinObjectContainer) soSizeSlider;
			if (tv != null && !tv.enableSizeSlider(so.getComposite(), 16, 100)) {
				so.setVisible(false);
			}
		}

		
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL
				&& tv != null) {
			tv.addRefreshListener(new TableRowRefreshListener() {
				public void rowRefresh(TableRow row) {
					TableRowSWT rowCore = (TableRowSWT)row;
					Object ds = rowCore.getDataSource(true);
					if (!(ds instanceof DownloadManager)) {
						return;
					}
					DownloadManager dm = (DownloadManager) ds;
					boolean changed = false;
					boolean assumedComplete = dm.getAssumedComplete();
					if (!assumedComplete) {
						changed |= rowCore.setAlpha(160);
					} else if (!PlatformTorrentUtils.getHasBeenOpened(dm)) {
						changed |= rowCore.setAlpha(255);
					} else {
						changed |= rowCore.setAlpha(255);
					}
				}
			});
		}
		
		viewComposite.getParent().layout(true);
	}

	public static void 
	doDefaultClick(
		final TableRowCore[] 	rows, 
		final int 				stateMask,
		final boolean 			neverPlay) 
	{
		if ( rows == null || rows.length != 1 ){
			return;
		}
		
		final Object ds = rows[0].getDataSource(true);

		boolean webInBrowser = COConfigurationManager.getBooleanParameter( "Library.LaunchWebsiteInBrowser" );
		
		if ( webInBrowser ){
			
			DiskManagerFileInfo fileInfo = DataSourceUtils.getFileInfo(ds);
			
			if ( fileInfo != null ){
				
				if ( ManagerUtils.browseWebsite( fileInfo )){
					
					return;
				}
			}else{
			
				DownloadManager dm = DataSourceUtils.getDM( ds);
				
				if ( dm != null ){
					
					if ( ManagerUtils.browseWebsite( dm )){
						
						return;
					}
				}
			}
		}
		
		String mode = COConfigurationManager.getStringParameter("list.dm.dblclick");
		
		if (mode.equals("1")) {
			
				// show detailed view
			
			if ( UIFunctionsManager.getUIFunctions().getMDI().showEntryByID( MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_DETAILS, ds)){
			
				return;
			}
		}else if (mode.equals("2")) {
			
				// Show in explorer
			
			boolean openMode = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
			DiskManagerFileInfo file = DataSourceUtils.getFileInfo(ds);
			if (file != null) {
				ManagerUtils.open(file, openMode);
				return;
			}
			DownloadManager dm = DataSourceUtils.getDM(ds);
			if (dm != null) {
				ManagerUtils.open(dm, openMode);
				return;
			}
		}else if (mode.equals("3") || mode.equals("4")){
		
			// Launch
			DiskManagerFileInfo file = DataSourceUtils.getFileInfo(ds);
			if (file != null) {
				if (	mode.equals("4") &&
						file.getDownloaded() == file.getLength() &&
						Utils.isQuickViewSupported( file )){
					
					Utils.setQuickViewActive( file, true );
				}else{
					TorrentUtil.runDataSources(new Object[]{ file });
				}
				return;
			}
			DownloadManager dm = DataSourceUtils.getDM(ds);
			if (dm != null) {
				TorrentUtil.runDataSources(new Object[]{ dm });
				return;
			}
		}else if (mode.equals("5")) {
			DiskManagerFileInfo fileInfo = DataSourceUtils.getFileInfo(ds);
			if ( fileInfo != null ){
				ManagerUtils.browse( fileInfo );
				return;
			}
			DownloadManager dm = DataSourceUtils.getDM(ds);
			if (dm != null) {
				ManagerUtils.browse( dm );
				return;
			}
		}
		
		if (neverPlay) {
			return;
		}
		
			// fallback
		
		if (PlayUtils.canPlayDS(ds, -1,true) || (stateMask & SWT.CONTROL) != 0) {
			TorrentListViewsUtils.playOrStreamDataSource(ds,
					DLReferals.DL_REFERAL_DBLCLICK, false, true );
			return;
		}

		if (PlayUtils.canStreamDS(ds, -1,true)) {
			TorrentListViewsUtils.playOrStreamDataSource(ds,
					DLReferals.DL_REFERAL_DBLCLICK, true, false );
			return;
		}
		
		DownloadManager dm = DataSourceUtils.getDM(ds);
		DiskManagerFileInfo file = DataSourceUtils.getFileInfo(ds);
		TOTorrent torrent = DataSourceUtils.getTorrent(ds);
		if (torrent == null && file != null) {
			DownloadManager dmFile = file.getDownloadManager();
			if (dmFile != null) {
				torrent = dmFile.getTorrent();
			}
		}
		if (file != null && file.getDownloaded() == file.getLength()) {
			TorrentUtil.runDataSources(new Object[] { file });
		} else if (dm != null) {
			TorrentUtil.runDataSources(new Object[] { dm });
		}
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return ID;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (viewComposite == null || viewComposite.isDisposed()
				|| !viewComposite.isVisible() || view == null) {
			return;
		}
		view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);

		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
		}
		
		Utils.execSWTThreadLater(0, new AERunnable() {
			
			public void runSupport() {
				updateUI();
			}
		});

		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
		}

		return super.skinObjectHidden(skinObject, params);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		if (!isVisible()) {
			return;
		}
		if (view != null) {
			view.refreshToolBarItems(list);
		}
		if (tv == null) {
			return;
		}
		ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
		boolean has1Selection = currentContent.length == 1;
		list.put(
				"play",
				has1Selection
						&& (!(currentContent[0] instanceof ISelectedVuzeFileContent))
						&& PlayUtils.canPlayDS(currentContent[0],
								currentContent[0].getFileIndex(),false)
						? UIToolBarItem.STATE_ENABLED : 0);
		list.put(
				"stream",
				has1Selection
						&& (!(currentContent[0] instanceof ISelectedVuzeFileContent))
						&& PlayUtils.canStreamDS(currentContent[0],
								currentContent[0].getFileIndex(),false)
						? UIToolBarItem.STATE_ENABLED : 0);
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType, Object datasource) {
		// currently stream and play are handled by ToolbarView..
		if (isVisible() && view != null) {
			return view.toolBarItemActivated(item, activationType, datasource);
		}
		return false;
	}

	/**
	 * Return either MODE_SMALLTABLE or MODE_BIGTABLE
	 * Subclasses may override
	 * @return
	 */
	protected int getTableMode() {
		return SBC_LibraryView.MODE_SMALLTABLE;
	}

	/**
	 * Returns whether the big version of the tables should be used
	 * Subclasses may override
	 * @return
	 */
	protected boolean useBigTable() {
		return false;
	}

	/**
	 * Returns the appropriate set of columns for the completed or incomplete torrents views
	 * Subclasses may override to return different sets of columns
	 * @return
	 */
	protected TableColumnCore[] getColumns() {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE);
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return TableColumnCreator.createIncompleteDM(TableManager.TABLE_MYTORRENTS_INCOMPLETE);
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			return TableColumnCreatorV3.createUnopenedDM(
					TableManager.TABLE_MYTORRENTS_UNOPENED, false);
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_ALL_BIG);
		}

		return null;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		if (view != null) {
  		view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
		}
		return super.skinObjectDestroyed(skinObject, params);
	}
	
	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateImage#obfusticatedImage(org.eclipse.swt.graphics.Image, org.eclipse.swt.graphics.Point)
	public Image obfusticatedImage(Image image) {
		if (view instanceof ObfusticateImage) {
			ObfusticateImage oi = (ObfusticateImage) view;
			return oi.obfusticatedImage(image);
		}
		return image;
	}
}
