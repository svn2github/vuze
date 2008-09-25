/**
 * Created on Jul 3, 2008
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * Classic My Torrents view wrapped in a SkinView
 * 
 * @author TuxPaper
 * @created Jul 3, 2008
 *
 */
public class SBC_LibraryTableView
	extends SkinView
	implements UIUpdatable, IconBarEnabler
{
	private final static String ID = "SBC_LibraryTableView";

	private IView view;

	private Composite viewComposite;

	protected int torrentFilterMode = SBC_LibraryView.TORRENTS_ALL;

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		SWTSkinObject soParent = skinObject.getParent();
		
		Object data = soParent.getControl().getData(
				"TorrentFilterMode");
		if (data instanceof Long) {
			torrentFilterMode = (int) ((Long) data).longValue();
		}

		TableColumnCore[] columns = getColumns();

		if (null != columns) {
			TableColumnManager tcManager = TableColumnManager.getInstance();
			tcManager.addColumns(columns);
		}

		if (true == useBigTable()) {
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE
					|| torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE
					|| torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {

				view = new MyTorrentsView_Big(AzureusCoreFactory.getSingleton(),
						torrentFilterMode, columns);

			} else {
				view = new MyTorrentsSuperView_Big();
			}

		} else {
			String tableID = SBC_LibraryView.getTableIdFromFilterMode(torrentFilterMode, false);
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
				view = new MyTorrentsView(AzureusCoreFactory.getSingleton(), tableID,
						true, columns);
				
				((MyTorrentsView) view).overrideDefaultSelected(new TableSelectionAdapter() {
					public void defaultSelected(TableRowCore[] rows) {
						if (rows == null || rows.length > 1) {
							return;
						}
						Object ds = rows[0].getDataSource(true);
						TorrentListViewsUtils.playOrStreamDataSource(ds, null, "dblclick");
					}
				});

			} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
				view = new MyTorrentsView(AzureusCoreFactory.getSingleton(), tableID,
						false, columns);

			} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
				view = new MyTorrentsView(AzureusCoreFactory.getSingleton(), tableID,
						true, columns) {
					public boolean isOurDownloadManager(DownloadManager dm) {
						if (PlatformTorrentUtils.getHasBeenOpened(dm.getTorrent())) {
							return false;
						}
						return super.isOurDownloadManager(dm);
					}
				};
			} else {
				view = new MyTorrentsSuperView() {
					public void initialize(Composite parent) {
						super.initialize(parent);
						MyTorrentsView seedingview = getSeedingview();
						if (seedingview != null) {
							seedingview.overrideDefaultSelected(new TableSelectionAdapter() {
		  					public void defaultSelected(TableRowCore[] rows) {
		  						if (rows == null || rows.length > 1) {
		  							return;
		  						}
		  						Object ds = rows[0].getDataSource(true);
		  						TorrentListViewsUtils.playOrStreamDataSource(ds, null, "dblclick");
		  					}
		  				});
						}
					}
				};
			}
		}
		SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
				skin.getSkinProperties(), getUpdateUIName(), "", soMain);

		skin.layout();

		viewComposite = soContents.getComposite();
		viewComposite.setBackground(viewComposite.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));
		viewComposite.setForeground(viewComposite.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_FOREGROUND));
		viewComposite.setLayoutData(Utils.getFilledFormData());
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
		viewComposite.setLayout(gridLayout);

		view.initialize(viewComposite);

		if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
  		SWTSkinObject so = skin.getSkinObject("library-list-button-right",
					soParent.getParent());
  		if (so != null) {
  			so.setVisible(true);
  			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so);
  			btn.setTextID("Mark All UnNew");
  			btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
  				public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject) {
  					TableViewSWT tv = ((MyTorrentsView) view).getTableView();
  					Object[] dataSources = tv.getDataSources();
  					for (int i = 0; i < dataSources.length; i++) {
							Object ds = dataSources[i];
							if (ds instanceof DownloadManager) {
								PlatformTorrentUtils.setHasBeenOpened(
										((DownloadManager) ds).getTorrent(), true);
								// give user visual indication right away 
								tv.removeDataSource(ds);
							}
						}
  				}
  			});
  		}
		}

		return null;
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
		view.refresh();
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			if (view instanceof MyTorrentsView) {
  			MyTorrentsView torrentsView = (MyTorrentsView) view;
  			TableViewSWT tv = torrentsView.getTableView();
  			Object[] dataSources = tv.getDataSources();
  			for (int i = 0; i < dataSources.length; i++) {
  				DownloadManager dm = (DownloadManager) dataSources[i];
  				if (!torrentsView.isOurDownloadManager(dm)) {
  					tv.removeDataSource(dm);
  				} else {
  					tv.addDataSource(dm);
  				}
  			}
			}
		}
		
		updateUI();

		return null;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		try {
			if (view != null) {
				return view.isEnabled(itemKey);
			}
		} catch (Throwable t) {
			Debug.out(t);
		}
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		try {
			if (view != null) {
				return view.isSelected(itemKey);
			}
		} catch (Throwable t) {
			Debug.out(t);
		}
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#itemActivated(java.lang.String)
	public void itemActivated(String itemKey) {
		try {
			if (view != null) {
				view.itemActivated(itemKey);
			}
		} catch (Throwable t) {
			Debug.out(t);
		}
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
			return TableColumnCreatorV3.createUnopenedDM(TableManager.TABLE_MYTORRENTS_UNOPENED, false);
		}

		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		if (view != null) {
			view.delete();
		}
		return super.skinObjectDestroyed(skinObject, params);
	}
}
