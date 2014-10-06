/**
 * Created on Oct 3, 2014
 *
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.AEMonitor2;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedAttributeLookupListener;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.tag.ColumnTagName;
import com.aelitis.azureus.ui.swt.columns.tag.TagDiscovery;
import com.aelitis.azureus.ui.swt.columns.tagdiscovery.ColumnTagDiscoveryName;
import com.aelitis.azureus.ui.swt.columns.tagdiscovery.ColumnTagDiscoveryTorrent;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;

/**
 * @author TuxPaper
 */
public class SBC_TagDiscovery
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener,
	TableViewFilterCheck<TagDiscovery>,
	TableViewSWTMenuFillListener, TableSelectionListener
{

	private static final String TABLE_TAGDISCOVERY = "TagDiscoveryView";
	
	private static final boolean DEBUG = false;

	TableViewSWT<TagDiscovery> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;
	
	private int scansRemaining = 0;
	
	private AEMonitor2 mon_scansRemaining = new AEMonitor2("scansRemaining");
	
	private Map<String, TagDiscovery> mapTagDiscoveries = new HashMap<String, TagDiscovery>();

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		return false;
	}

	// @see com.aelitis.azureus.ui.common.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(String filter) {
	}

	// @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	public void refreshToolBarItems(Map<String, Long> list) {
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (tv != null) {
			tv.refreshTable(false);
		}
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return TABLE_TAGDISCOVERY;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		initColumns();

		return null;
	}

	protected void initColumns() {
		synchronized (SBC_TagDiscovery.class) {

			if (columnsAdded) {

				return;
			}

			columnsAdded = true;
		}

		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.registerColumn(TagDiscovery.class,
				ColumnTagDiscoveryName.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagDiscoveryName(column);
					}
				});
		tableManager.registerColumn(TagDiscovery.class,
				ColumnTagDiscoveryTorrent.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagDiscoveryTorrent(column);
					}
				});

		tableManager.setDefaultColumnNames(TABLE_TAGDISCOVERY, new String[] {
			ColumnTagDiscoveryName.COLUMN_ID,
			ColumnTagDiscoveryTorrent.COLUMN_ID,
		});

		tableManager.setDefaultSortColumnName(TABLE_TAGDISCOVERY,
				ColumnTagDiscoveryName.COLUMN_ID);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {

		if (tv != null) {

			tv.delete();

			tv = null;
		}

		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		return super.skinObjectHidden(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		SWTSkinObject so_list = getSkinObject("tag-discovery-list");

		if (so_list != null) {
			initTable((Composite) so_list.getControl());
		} else {
			System.out.println("NO tag-discovery-list");
			return null;
		}

		if (tv == null) {
			return null;
		}
		
		TagDiscovery[] tagDiscoveries = mapTagDiscoveries.values().toArray(new TagDiscovery[0]);
		tv.addDataSources(tagDiscoveries);

		// TODO: Not this		
		startScan();

		return null;
	}

	/**
	 * 
	 *
	 * @since 5.0.0.1
	 */
	private void startScan() {
		try {
			mon_scansRemaining.enter();

			if (scansRemaining > 0) {
				return;
			}
		} finally {
			mon_scansRemaining.exit();
		}

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				GlobalManager gm = core.getGlobalManager();
				try {
					try {
						mon_scansRemaining.enter();

						scansRemaining = 0;
					} finally {
						mon_scansRemaining.exit();
					}

					RelatedContentManager rcm = RelatedContentManager.getSingleton();
					List<DownloadManager> dms = gm.getDownloadManagers();

					for (final DownloadManager dm : dms) {
						TOTorrent torrent = dm.getTorrent();
						if (torrent == null) {
							continue;
						}
						try {
							final byte[] hash = torrent.getHash();
							try {
								mon_scansRemaining.enter();

								scansRemaining++;
								
								if (scansRemaining == 1) {
			  					SWTSkinObjectText soTitle = (SWTSkinObjectText) getSkinObject("title");
			  					if (soTitle != null) {
			  						soTitle.setText(MessageText.getString("tag.discovery.view.heading") + " : Scanning");
			  					}
								}
							} finally {
								mon_scansRemaining.exit();
							}
							rcm.lookupAttributes(hash, dm.getDownloadState().getNetworks(), new RelatedAttributeLookupListener() {
								public void tagFound(String tag) {
									if (DEBUG) {
										System.out.println("Tag Search: Found Tag " + tag + " for " + dm.getDisplayName());
									}
									String key = Base32.encode(hash) + tag;
									synchronized (mapTagDiscoveries) {
										if (!mapTagDiscoveries.containsKey(key)) {
											
										}
										TagDiscovery tagDiscovery = new TagDiscovery(tag, dm.getDisplayName(), hash);
										mapTagDiscoveries.put(key, tagDiscovery);
										tv.addDataSource(tagDiscovery);
									}
								}
								
								public void lookupStart() {
									if (DEBUG) {
										System.out.println("Tag Search: Start" + " for " + dm.getDisplayName());
									}
								}
								
								public void lookupFailed(ContentException error) {
									if (DEBUG) {
										System.out.println("Tag Search: Failed " + error.getMessage()  + " for " + dm.getDisplayName());
									}
								}
								
								public void lookupComplete() {
									try {
										mon_scansRemaining.enter();

										scansRemaining--;
										
										if (scansRemaining <= 0) {
											SWTSkinObjectText soTitle = (SWTSkinObjectText) getSkinObject("title");
											if (soTitle != null) {
												soTitle.setTextID("tag.discovery.view.heading");
											}
										}
									} finally {
										mon_scansRemaining.exit();
									}
									if (DEBUG) {
										System.out.println("Tag Search: Complete" + " for " + dm.getDisplayName());
									}
								}
							});
						} catch (TOTorrentException e) {
							e.printStackTrace();
						}
					}
				} catch (ContentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		return super.skinObjectDestroyed(skinObject, params);
	}

	/**
	 * @param control
	 *
	 * @since 4.6.0.5
	 */
	private void initTable(Composite control) {
		if (tv == null) {

			tv = TableViewFactory.createTableViewSWT(TagDiscovery.class, TABLE_TAGDISCOVERY,
					TABLE_TAGDISCOVERY, new TableColumnCore[0], ColumnTagName.COLUMN_ID,
					SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
			if (txtFilter != null) {
				tv.enableFilterCheck(txtFilter, this);
			}
			tv.setRowDefaultHeight(16);

			table_parent = new Composite(control, SWT.BORDER);
			table_parent.setLayoutData(Utils.getFilledFormData());
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
			table_parent.setLayout(layout);

			tv.addMenuFillListener(this);
			tv.addSelectionListener(this, false);

			tv.initialize(table_parent);
		}

		control.layout(true);
	}

	public void fillMenu(String sColumnName, Menu menu) {
		List<Object> ds = tv.getSelectedDataSources();
	}

	public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {

	}

	public void selected(TableRowCore[] row) {
	}

	public void deselected(TableRowCore[] rows) {
	}

	public void focusChanged(TableRowCore focus) {

	}

	public void defaultSelected(TableRowCore[] rows, int stateMask) {
		if (rows.length == 1) {

			Object obj = rows[0].getDataSource();

			if (obj instanceof TagDiscovery) {

				TagDiscovery  tag = (TagDiscovery) obj;

				// do something on double click
			}
		}
	}

	public void mouseEnter(TableRowCore row) {
	}

	public void mouseExit(TableRowCore row) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableViewFilterCheck#filterCheck(java.lang.Object, java.lang.String, boolean)
	public boolean filterCheck(TagDiscovery ds, String filter, boolean regex) {
		return false;
	}

}
