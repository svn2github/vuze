/**
 * Created on May 10, 2013
 *
 * Copyright 2011 Vuze, LLC.  All rights reserved.
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

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.columns.tag.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

/**
 * @author TuxPaper
 * @created May 10, 2013
 *
 */
public class SBC_TagsOverview
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<Tag>, TagManagerListener, TagTypeListener
{

	private static final String TABLE_TAGS = "TagsView";

	TableViewSWT<Tag> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

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
		return "TagsView";
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				initColumns(core);
			}
		});

		return null;
	}

	protected void initColumns(AzureusCore core) {
		synchronized (SBC_TagsOverview.class) {

			if (columnsAdded) {

				return;
			}

			columnsAdded = true;
		}

		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();

		TableManager tableManager = uiManager.getTableManager();

		tableManager.registerColumn(Tag.class, ColumnTagCount.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagCount(column);
					}
				});
		tableManager.registerColumn(Tag.class, ColumnTagColor.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagColor(column);
					}
				});
		tableManager.registerColumn(Tag.class, ColumnTagName.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagName(column);
					}
				});
		tableManager.registerColumn(Tag.class, ColumnTagType.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagType(column);
					}
				});
		
		MenuItem menuItem = uiManager.getMenuManager().addMenuItem(MenuManager.MENU_MENUBAR, "tags.view.heading");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_TAGS);
			}
		});
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
		
		TagManager tagManager = TagManagerFactory.getTagManger();
		if (tagManager != null) {
			List<TagType> tagTypes = tagManager.getTagTypes();
			for (TagType tagType : tagTypes) {
				tagType.removeTagTypeListener(this);
			}
			tagManager.removeTagManagerListener(this);
		}


		return super.skinObjectHidden(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		SWTSkinObject so_list = getSkinObject("tags-list");

		if (so_list != null) {
			initTable((Composite) so_list.getControl());
		} else {
			System.out.println("NO tags-list");
			return null;
		}
		
		if (tv == null) {
			return null;
		}

		TagManager tagManager = TagManagerFactory.getTagManger();
		if (tagManager != null) {
			tagManager.addTagManagerListener(this, true);
		}

		return super.skinObjectShown(skinObject, params);
	}

	/**
	 * @param control
	 *
	 * @since 4.6.0.5
	 */
	private void initTable(Composite control) {
		tv = TableViewFactory.createTableViewSWT(Tag.class, TABLE_TAGS, TABLE_TAGS,
				new TableColumnCore[0], ColumnTagCount.COLUMN_ID, SWT.MULTI
						| SWT.FULL_SELECTION | SWT.VIRTUAL);
		if (txtFilter != null) {
			tv.enableFilterCheck(txtFilter, this);
		}
		tv.setRowDefaultHeight(16);

		TableColumnManager.getInstance().setDefaultColumnNames(TABLE_TAGS,
				new String[] {
					ColumnTagColor.COLUMN_ID,
					ColumnTagName.COLUMN_ID,
					ColumnTagCount.COLUMN_ID,
					ColumnTagType.COLUMN_ID
				});
		TableColumnManager.getInstance().setDefaultSortColumnName(TABLE_TAGS, ColumnTagName.COLUMN_ID);
		table_parent = new Composite(control, SWT.BORDER);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv.initialize(table_parent);
		control.layout(true);
	}

	// @see com.aelitis.azureus.ui.common.table.TableViewFilterCheck#filterCheck(java.lang.Object, java.lang.String, boolean)
	public boolean filterCheck(Tag ds, String filter, boolean regex) {
		return false;
	}

	// @see com.aelitis.azureus.core.tag.TagManagerListener#tagTypeAdded(com.aelitis.azureus.core.tag.TagManager, com.aelitis.azureus.core.tag.TagType)
	public void tagTypeAdded(TagManager manager, TagType tag_type) {
		tag_type.addTagTypeListener(this, true);
	}

	// @see com.aelitis.azureus.core.tag.TagManagerListener#tagTypeRemoved(com.aelitis.azureus.core.tag.TagManager, com.aelitis.azureus.core.tag.TagType)
	public void tagTypeRemoved(TagManager manager, TagType tag_type) {
		tag_type.removeTagTypeListener(this);
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagTypeChanged(com.aelitis.azureus.core.tag.TagType)
	public void tagTypeChanged(TagType tag_type) {
		tv.tableInvalidate();
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagAdded(com.aelitis.azureus.core.tag.Tag)
	public void tagAdded(Tag tag) {
		tv.addDataSource(tag);
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagChanged(com.aelitis.azureus.core.tag.Tag)
	public void tagChanged(Tag tag) {
		if (tv == null || tv.isDisposed()) {
			return;
		}
		TableRowCore row = tv.getRow(tag);
		if (row != null) {
			row.invalidate(true);
		}
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagRemoved(com.aelitis.azureus.core.tag.Tag)
	public void tagRemoved(Tag tag) {
		tv.removeDataSource(tag);
	}
}
