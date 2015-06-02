/**
 * Created on Oct 3, 2014
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableSelectedRowsListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedAttributeLookupListener;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.columns.tag.ColumnTagName;
import com.aelitis.azureus.ui.swt.columns.tagdiscovery.*;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.skin.*;

/**
 * @author TuxPaper
 */
public class SBC_TagDiscovery
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener,
	TableViewFilterCheck<TagDiscovery>, TableViewSWTMenuFillListener,
	TableSelectionListener, ViewTitleInfo
{

	private static final String TABLE_TAGDISCOVERY = "TagDiscoveryView";

	private static final boolean DEBUG = false;

	private static final String CONFIG_FILE = "tag-discovery.config";

	private static final String ID_VITALITY_ACTIVE = "image.sidebar.vitality.dots";

	TableViewSWT<TagDiscovery> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

	private int scansRemaining = 0;

	private AEMonitor2 mon_scansRemaining = new AEMonitor2("scansRemaining");

	private Map<String, TagDiscovery> mapTagDiscoveries = new HashMap<String, TagDiscovery>();

	private MdiEntry entry;

	private SWTSkinObjectText soTitle;

	private MdiEntryVitalityImage vitalityImage;

	private Map mapConfig;

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if (tv == null || !tv.isVisible()) {
			return (false);
		}
		if (item.getID().equals("remove")) {

			Object[] datasources = tv.getSelectedDataSources().toArray();

			if (datasources.length > 0) {

				for (Object object : datasources) {
					if (object instanceof TagDiscovery) {
						TagDiscovery discovery = (TagDiscovery) object;

					}
				}

				return true;
			}
		}

		return false;
	}

	// @see com.aelitis.azureus.ui.common.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(String filter) {
	}

	// @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	public void refreshToolBarItems(Map<String, Long> list) {
		if (tv == null || !tv.isVisible()) {
			return;
		}

		list.put("remove", tv.getSelectedDataSources().size() > 0
				? UIToolBarItem.STATE_ENABLED : 0);
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

		mapConfig = FileUtil.readResilientConfigFile(CONFIG_FILE);

		soTitle = (SWTSkinObjectText) getSkinObject("title");

		SWTSkinObjectButton soScanButton = (SWTSkinObjectButton) getSkinObject("scan-button");
		if (soScanButton != null) {
			soScanButton.addSelectionListener(new ButtonListenerAdapter() {
				// @see com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter#pressed(com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility, com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int)
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					startScan();
				}
			});
		}
		
		final SWTSkinObject soFilterArea = getSkinObject("filterarea");
		if (soFilterArea != null) {
			
			SWTSkinObjectToggle soFilterButton = (SWTSkinObjectToggle) getSkinObject("filter-button");
			if (soFilterButton != null) {
				soFilterButton.addSelectionListener(new SWTSkinToggleListener() {
					public void toggleChanged(SWTSkinObjectToggle so, boolean toggled) {
						soFilterArea.setVisible(toggled);
						Utils.relayout(soFilterArea.getControl().getParent());
					}
				});
			}
			
		}


		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {
			entry = mdi.getEntry(MultipleDocumentInterface.SIDEBAR_SECTION_TAG_DISCOVERY);
			if (entry != null) {
				entry.setViewTitleInfo(this);
				vitalityImage = entry.addVitalityImage(ID_VITALITY_ACTIVE);
				if ( vitalityImage != null ){
					vitalityImage.setVisible(false);
				}
			}
		}

		initColumns();

		return null;
	}

	public Object getTitleInfoProperty(int propertyID) {
		if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT) {
			int num = mapTagDiscoveries.size();
			if (num > 0) {
				return "" + num;
			}
		}
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
		tableManager.registerColumn(TagDiscovery.class,
				ColumnTagDiscoveryAddedOn.COLUMN_ID,
				new TableColumnCoreCreationListener() {
					public TableColumnCore createTableColumnCore(
							Class<?> forDataSourceType, String tableID, String columnID) {
						return new ColumnDateSizer(TagDiscovery.class, columnID,
								TableColumnCreator.DATE_COLUMN_WIDTH, tableID) {
						};
					}

					public void tableColumnCreated(TableColumn column) {
						new ColumnTagDiscoveryAddedOn(column);
					}
				});
		tableManager.registerColumn(TagDiscovery.class,
				ColumnTagDiscoveryNetwork.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagDiscoveryNetwork(column);
					}
				});

		tableManager.setDefaultColumnNames(TABLE_TAGDISCOVERY, new String[] {
			ColumnTagDiscoveryName.COLUMN_ID,
			ColumnTagDiscoveryTorrent.COLUMN_ID,
			ColumnTagDiscoveryAddedOn.COLUMN_ID,
		});

		tableManager.setDefaultSortColumnName(TABLE_TAGDISCOVERY,
				ColumnTagDiscoveryAddedOn.COLUMN_ID);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {

		if (mapConfig != null) {
			FileUtil.writeResilientConfigFile(CONFIG_FILE, mapConfig);
		}

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

		TagDiscovery[] tagDiscoveries = mapTagDiscoveries.values().toArray(
				new TagDiscovery[0]);
		tv.addDataSources(tagDiscoveries);

		return null;
	}

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
						if ( tv == null ){
							return;
						}
						TOTorrent torrent = dm.getTorrent();
						if (torrent == null) {
							continue;
						}
						try {
							final byte[] hash = torrent.getHash();
							try {
								mon_scansRemaining.enter();

								scansRemaining++;

								if (vitalityImage != null && scansRemaining == 1) {
									vitalityImage.setVisible(true);
								}

								if (soTitle != null) {
									soTitle.setText(MessageText.getString("tag.discovery.view.heading")
											+ " : Scanning " + scansRemaining);
								}
							} finally {
								mon_scansRemaining.exit();
							}

							try {
								rcm.lookupAttributes(hash, dm.getDownloadState().getNetworks(),
										new RelatedAttributeLookupListener() {
											public void tagFound(String tag, String network) {
												if (DEBUG) {
													System.out.println("Tag Search: Found Tag " + tag
															+ " for " + dm.getDisplayName());
												}
												if ( tv == null ){
													return;
												}
												String key = Base32.encode(hash) + tag;

												TagManager tm = TagManagerFactory.getTagManager();
												TagType tt_manual = tm.getTagType(TagType.TT_DOWNLOAD_MANUAL);
												List<Tag> existingDMTags = tt_manual.getTagsForTaggable(dm);
												for (Tag existingTag : existingDMTags) {
													if (existingTag.getTagName(true).equalsIgnoreCase(tag)) {
														return;
													}
												}
												synchronized (mapTagDiscoveries) {
													if (!mapTagDiscoveries.containsKey(key)) {
														TagDiscovery tagDiscovery = new TagDiscovery(tag,
																network, dm.getDisplayName(), hash);
														mapTagDiscoveries.put(key, tagDiscovery);
														ViewTitleInfoManager.refreshTitleInfo(SBC_TagDiscovery.this);
														tv.addDataSource(tagDiscovery);

													}
												}
											}

											public void lookupStart() {
												if (DEBUG) {
													System.out.println("Tag Search: Start" + " for "
															+ dm.getDisplayName());
												}
											}

											public void lookupFailed(ContentException error) {
												if (DEBUG) {
													System.out.println("Tag Search: Failed "
															+ error.getMessage() + " for "
															+ dm.getDisplayName());
												}
											}

											public void lookupComplete() {
												decreaseScansRemaining();

												if (DEBUG) {
													System.out.println("Tag Search: Complete" + " for "
															+ dm.getDisplayName());
												}
											}
										});
							} catch (Throwable e) {

								// can get here if the scan never gets kicked off (dht unavailable for network etc)
								decreaseScansRemaining();
							}
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

	protected void decreaseScansRemaining() {
		try {
			mon_scansRemaining.enter();

			scansRemaining--;

			if (soTitle != null) {
				if (scansRemaining <= 0) {
					soTitle.setTextID("tag.discovery.view.heading");
				} else {
					soTitle.setText(MessageText.getString("tag.discovery.view.heading")
							+ " : Scanning " + scansRemaining);
				}
			}
			if (vitalityImage != null && scansRemaining <= 0) {
				vitalityImage.setVisible(false);
			}
		} finally {
			mon_scansRemaining.exit();
		}
	}

	@Override
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		return super.skinObjectDestroyed(skinObject, params);
	}

	private void initTable(Composite control) {
		if (tv == null) {

			tv = TableViewFactory.createTableViewSWT(TagDiscovery.class,
					TABLE_TAGDISCOVERY, TABLE_TAGDISCOVERY, new TableColumnCore[0],
					ColumnTagName.COLUMN_ID, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
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

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener#fillMenu(java.lang.String, org.eclipse.swt.widgets.Menu)
	public void fillMenu(String sColumnName, Menu menu) {
		List<Object> ds = tv.getSelectedDataSources();

		final MenuItem menuTagIt = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(menuTagIt, "TagDiscoveriesView.menu.tagit");
		menuTagIt.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				TagDiscovery tagDiscovery = (TagDiscovery) row.getDataSource(true);
				TagManager tm = TagManagerFactory.getTagManager();
				TagType manual_tt = tm.getTagType(TagType.TT_DOWNLOAD_MANUAL);
				Tag tag = manual_tt.getTag(tagDiscovery.getName(), true);
				if (tag == null) {
					try {
						tag = manual_tt.createTag(tagDiscovery.getName(), true);
						tag.setPublic(true);
						tag.setGroup("Discovery");
						tag.setVisible(true);
					} catch (TagException e) {
						return;
					}
				}
				byte[] hash = tagDiscovery.getHash();
				DownloadManager dm = AzureusCoreFactory.getSingleton().getGlobalManager().getDownloadManager(
						new HashWrapper(hash));
				tag.addTaggable(dm);

				String key = Base32.encode(hash) + tag.getTagName(true);
				mapTagDiscoveries.remove(key);
				tv.removeDataSource(tagDiscovery);
				ViewTitleInfoManager.refreshTitleInfo(SBC_TagDiscovery.this);
			}
		});
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

				TagDiscovery tag = (TagDiscovery) obj;

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
