/**
 * Created on Sep 25, 2008
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
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.activities.*;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryCreationListener;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUIListener;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class SBC_ActivityTableView
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener, VuzeActivitiesListener
{
	private static final String TABLE_ID_PREFIX = "activity-";

	private TableViewSWT<VuzeActivitiesEntry> view;

	private String tableID;

	private Composite viewComposite;

	private int viewMode = SBC_ActivityView.MODE_SMALLTABLE;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		skinObject.addListener(new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
					SelectedContentManager.changeCurrentlySelectedContent(tableID,
							getCurrentlySelectedContent(), view);
				} else if (eventType == SWTSkinObjectListener.EVENT_HIDE) {
					SelectedContentManager.changeCurrentlySelectedContent(tableID, null,
							view);
				}
				return null;
			}
		});

		SWTSkinObject soParent = skinObject.getParent();

		Object data = soParent.getControl().getData("ViewMode");
		if (data instanceof Long) {
			viewMode = (int) ((Long) data).longValue();
		}

		boolean big = viewMode == SBC_ActivityView.MODE_BIGTABLE;

		tableID = big ? TableManager.TABLE_ACTIVITY_BIG
				: TableManager.TABLE_ACTIVITY;
		TableColumnCore[] columns = big
				? TableColumnCreatorV3.createActivityBig(tableID)
				: TableColumnCreatorV3.createActivitySmall(tableID);

		view = TableViewFactory.createTableViewSWT(VuzeActivitiesEntry.class,
				tableID, tableID, columns, "name", SWT.MULTI | SWT.FULL_SELECTION
						| SWT.VIRTUAL);

		view.setRowDefaultHeight(big ? 50 : 32);

		view.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					removeSelected();
				} else if (e.keyCode == SWT.F5) {
					if ((e.stateMask & SWT.SHIFT) > 0) {
						VuzeActivitiesManager.resetRemovedEntries();
					}
					if ((e.stateMask & SWT.CONTROL) > 0) {
						System.out.println("pull all vuze news entries");
						VuzeActivitiesManager.clearLastPullTimes();
						VuzeActivitiesManager.pullActivitiesNow(0, "^F5", true);
					} else {
						System.out.println("pull latest vuze news entries");
						VuzeActivitiesManager.pullActivitiesNow(0, "F5", true);
					}
				}
			}
		});

		view.addSelectionListener(new TableSelectionAdapter() {
			// @see com.aelitis.azureus.ui.common.table.TableSelectionAdapter#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
			public void selected(TableRowCore[] rows) {
				selectionChanged();
				for (int i = 0; i < rows.length; i++) {
					VuzeActivitiesEntry entry = (VuzeActivitiesEntry) rows[i].getDataSource(true);
					if (entry != null && !entry.isRead() && entry.canFlipRead()) {
						entry.setRead(true);
					}
				}
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
				if (rows.length == 1) {
					TorrentListViewsUtils.playOrStreamDataSource(rows[0].getDataSource(),
							false);
				}
			}

			public void deselected(TableRowCore[] rows) {
				selectionChanged();
			}

			public void selectionChanged() {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						ISelectedContent[] contents = getCurrentlySelectedContent();
						if (soMain.isVisible()) {
							SelectedContentManager.changeCurrentlySelectedContent(tableID,
									contents, view);
						}
					}
				});
			}

		}, false);

		view.addLifeCycleListener(new TableLifeCycleListener() {
			public void tableViewInitialized() {
				view.addDataSources(VuzeActivitiesManager.getAllEntries());
			}

			public void tableViewDestroyed() {
			}
		});

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

		VuzeActivitiesManager.addListener(this);

		return null;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		if (view != null) {
			view.delete();
		}
		return super.skinObjectDestroyed(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return tableID;
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (view != null) {
			view.refreshTable(false);
		}
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		list.put("remove",
				isVisible() && view != null && view.getSelectedRowsSize() > 0
						? UIToolBarItem.STATE_ENABLED : 0);
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if (item.getID().equals("remove")) {
			removeSelected();
			return true;
		}

		return false;
	}

	public ISelectedContent[] getCurrentlySelectedContent() {
		if (view == null) {
			return null;
		}
		List listContent = new ArrayList();
		Object[] selectedDataSources = view.getSelectedDataSources(true);
		for (int i = 0; i < selectedDataSources.length; i++) {

			VuzeActivitiesEntry ds = (VuzeActivitiesEntry) selectedDataSources[i];
			if (ds != null) {
				ISelectedContent currentContent;
				try {
					currentContent = ds.createSelectedContentObject();
					if (currentContent != null) {
						listContent.add(currentContent);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return (ISelectedContent[]) listContent.toArray(new ISelectedContent[listContent.size()]);
	}

	// @see com.aelitis.azureus.util.VuzeNewsListener#vuzeNewsEntriesAdded(com.aelitis.azureus.util.VuzeNewsEntry[])
	public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
		if (view != null) {
			view.addDataSources(entries);
		}
	}

	// @see com.aelitis.azureus.util.VuzeNewsListener#vuzeNewsEntriesRemoved(com.aelitis.azureus.util.VuzeNewsEntry[])
	public void vuzeNewsEntriesRemoved(VuzeActivitiesEntry[] entries) {
		if (view != null) {
			view.removeDataSources(entries);
			view.processDataSourceQueue();
		}
	}

	// @see com.aelitis.azureus.util.VuzeActivitiesListener#vuzeNewsEntryChanged(com.aelitis.azureus.util.VuzeActivitiesEntry)
	public void vuzeNewsEntryChanged(VuzeActivitiesEntry entry) {
		if (view == null) {
			return;
		}
		TableRowCore row = view.getRow(entry);
		if (row != null) {
			row.invalidate();
		}
	}

	private void removeEntries(final VuzeActivitiesEntry[] toRemove,
			final int startIndex) {
		final VuzeActivitiesEntry entry = toRemove[startIndex];
		if (entry == null
				|| VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID())) {
			int nextIndex = startIndex + 1;
			if (nextIndex < toRemove.length) {
				removeEntries(toRemove, nextIndex);
			}
			return;
		}

		MessageBoxShell mb = new MessageBoxShell(
				MessageText.getString("v3.activity.remove.title"),
				MessageText.getString("v3.activity.remove.text", new String[] {
					entry.getText()
				}));
		mb.setRemember(tableID + "-Remove", false,
				MessageText.getString("MessageBoxWindow.nomoreprompting"));

		if (startIndex == toRemove.length - 1) {
			mb.setButtons(0, new String[] {
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no"),
			}, new Integer[] {
				0,
				1
			});
			mb.setRememberOnlyIfButton(0);
		} else {
			mb.setButtons(1, new String[] {
				MessageText.getString("Button.removeAll"),
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no"),
			}, new Integer[] {
				2,
				0,
				1
			});
			mb.setRememberOnlyIfButton(1);
		}

		mb.setHandleHTML(false);
		mb.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 2) {
					int numToRemove = toRemove.length - startIndex;
					VuzeActivitiesEntry[] toGroupRemove = new VuzeActivitiesEntry[numToRemove];
					System.arraycopy(toRemove, startIndex, toGroupRemove, 0, numToRemove);
					VuzeActivitiesManager.removeEntries(toGroupRemove);
					return;
				} else if (result == 0) {
					VuzeActivitiesManager.removeEntries(new VuzeActivitiesEntry[] {
						entry
					});
				}

				int nextIndex = startIndex + 1;
				if (nextIndex < toRemove.length) {
					removeEntries(toRemove, nextIndex);
				}
			}
		});
	}

	protected void removeSelected() {
		if (view == null) {
			return;
		}
		VuzeActivitiesEntry[] selectedEntries = view.getSelectedDataSources().toArray(
				new VuzeActivitiesEntry[0]);
		removeEntries(selectedEntries, 0);
	}

	public TableViewSWT getView() {
		return view;
	}

	public static void setupSidebarEntry() {
		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

		// Put TitleInfo in another class
		final ViewTitleInfo titleInfoActivityView = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					int count = 0;
					VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
					for (int i = 0; i < allEntries.length; i++) {
						VuzeActivitiesEntry entry = allEntries[i];
						if (!entry.isRead()) {
							count++;
						}
					}
					if (count > 0) {
						return "" + count;
					}

					return null;
				} else if (propertyID == TITLE_IMAGEID) {
					return "image.sidebar.activity";
				}
				return null;
			}
		};
		VuzeActivitiesManager.addListener(new VuzeActivitiesListener() {
			public void vuzeNewsEntryChanged(VuzeActivitiesEntry entry) {
				ViewTitleInfoManager.refreshTitleInfo(titleInfoActivityView);
			}

			public void vuzeNewsEntriesRemoved(VuzeActivitiesEntry[] entries) {
				ViewTitleInfoManager.refreshTitleInfo(titleInfoActivityView);
			}

			public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
				ViewTitleInfoManager.refreshTitleInfo(titleInfoActivityView);
			}
		});
		
		MdiEntryCreationListener creationListener = new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				return mdi.createEntryFromSkinRef(MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
						MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES, "activity",
						"{sidebar." + MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES
								+ "}", titleInfoActivityView, null, false, null);
			}
		};
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES, creationListener);
		mdi.registerEntry("activities", creationListener);

		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();

		MenuItem menuItem = menuManager.addMenuItem("sidebar."
				+ MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES,
				"v3.activity.button.readall");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
				for (int i = 0; i < allEntries.length; i++) {
					VuzeActivitiesEntry entry = allEntries[i];
					entry.setRead(true);
				}
			}
		});

		if (Constants.isCVSVersion()) {
			menuItem = menuManager.addMenuItem("sidebar."
					+ MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES,
					"!test update expiry!");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					FeatureManagerUIListener.buildNotifications();
				}
			});
		}
	}
}
