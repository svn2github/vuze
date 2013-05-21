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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.tag.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

/**
 * @author TuxPaper
 * @created May 10, 2013
 *
 */
public class SBC_TagsOverview
	extends SkinView
	implements 	UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<Tag>, TagManagerListener, TagTypeListener,
				TableViewSWTMenuFillListener, TableSelectionListener
{

	private static final String TABLE_TAGS = "TagsView";

	TableViewSWT<Tag> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

	private boolean tm_listener_added;
	
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
		initColumns();

		return null;
	}

	protected void initColumns() {
		synchronized (SBC_TagsOverview.class) {

			if (columnsAdded) {

				return;
			}

			columnsAdded = true;
		}

		TableColumnManager tableManager = TableColumnManager.getInstance();

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

		tableManager.registerColumn(Tag.class, ColumnTagPublic.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagPublic(column);
					}
				});
		
		tableManager.registerColumn(Tag.class, ColumnTagUpRate.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagUpRate(column);
					}
				});
		
		tableManager.registerColumn(Tag.class, ColumnTagDownRate.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagDownRate(column);
					}
				});
		
		tableManager.registerColumn(Tag.class, ColumnTagUpLimit.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagUpLimit(column);
					}
				});

		tableManager.registerColumn(Tag.class, ColumnTagDownLimit.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagDownLimit(column);
					}
				});

		tableManager.registerColumn(Tag.class, ColumnTagRSSFeed.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagRSSFeed(column);
					}
				});

		tableManager.registerColumn(Tag.class, ColumnTagUploadPriority.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagUploadPriority(column);
					}
				});
		
		tableManager.registerColumn(Tag.class, ColumnTagXCode.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagXCode(column);
					}
				});

		tableManager.registerColumn(Tag.class, ColumnTagMoveOnComp.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTagMoveOnComp(column);
					}
				});

		tableManager.setDefaultColumnNames(TABLE_TAGS,
				new String[] {
					ColumnTagColor.COLUMN_ID,
					ColumnTagName.COLUMN_ID,
					ColumnTagCount.COLUMN_ID,
					ColumnTagType.COLUMN_ID,
					ColumnTagPublic.COLUMN_ID,
					ColumnTagUpRate.COLUMN_ID,
					ColumnTagDownRate.COLUMN_ID,
					ColumnTagUpLimit.COLUMN_ID,
					ColumnTagDownLimit.COLUMN_ID,
				});
		
		tableManager.setDefaultSortColumnName(TABLE_TAGS, ColumnTagName.COLUMN_ID);
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
		
		TagManager tagManager = TagManagerFactory.getTagManager();
		if (tagManager != null) {
			List<TagType> tagTypes = tagManager.getTagTypes();
			for (TagType tagType : tagTypes) {
				tagType.removeTagTypeListener(this);
			}
			tagManager.removeTagManagerListener(this);
			
			tm_listener_added = false;
		}


		return super.skinObjectHidden(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
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

		TagManager tagManager = TagManagerFactory.getTagManager();
		if (tagManager != null) {
			
			if ( !tm_listener_added ){
				
				tm_listener_added = true;
			
				tagManager.addTagManagerListener(this, true);
			}
		}

		return null;
	}

	@Override
	public Object 
	skinObjectDestroyed(
		SWTSkinObject skinObject, 
		Object params) 
	{
		if ( tm_listener_added ){
		
			tm_listener_added = false;
			
			TagManager tagManager = TagManagerFactory.getTagManager();
			
			tagManager.removeTagManagerListener( this );

			for ( TagType tt: tagManager.getTagTypes()){
				
				tt.removeTagTypeListener( this );
			}
		}			
		
		return super.skinObjectDestroyed(skinObject, params);
	}
	
	/**
	 * @param control
	 *
	 * @since 4.6.0.5
	 */
	private void initTable(Composite control) {
		if ( tv == null ){
			
			tv = TableViewFactory.createTableViewSWT(Tag.class, TABLE_TAGS, TABLE_TAGS,
					new TableColumnCore[0], ColumnTagName.COLUMN_ID, SWT.MULTI
							| SWT.FULL_SELECTION | SWT.VIRTUAL);
			if (txtFilter != null) {
				tv.enableFilterCheck(txtFilter, this);
			}
			tv.setRowDefaultHeight(16);
	
			table_parent = new Composite(control, SWT.BORDER);
			table_parent.setLayoutData(Utils.getFilledFormData());
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
			table_parent.setLayout(layout);
	
			tv.addMenuFillListener( this );
			tv.addSelectionListener(this, false);
			
			tv.initialize(table_parent);
		}
		
		control.layout(true);
	}

	public void 
	fillMenu(
		String 	sColumnName, 
		Menu 	menu )
	{
		List<Object>	ds = tv.getSelectedDataSources();
		
		List<Tag>	tags = new ArrayList<Tag>();
		
		for ( Object obj: ds ){
			
			if ( obj instanceof Tag ){
				
				tags.add((Tag)obj);
			}
		}
		
		if ( tags.size() == 1 ){
			TagUIUtils.createSideBarMenuItems( menu, tags.get(0) );
		}
	}

	public void 
	addThisColumnSubMenu(
		String 	sColumnName, 
		Menu	menuThisColumn )
	{
		
	}
	
	public void 
	selected(
		TableRowCore[] row )
	{
	}

	public void 
	deselected(
		TableRowCore[] rows )
	{
	}
	
	public void 
	focusChanged(
		TableRowCore focus )
	{
		
	}

	public void 
	defaultSelected(
		TableRowCore[] 	rows, 
		int 			stateMask )
	{
		if ( rows.length == 1 ){
			
			Object obj = rows[0].getDataSource();
			
			if ( obj instanceof Tag ){
				
				Tag tag = (Tag)obj;
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();

				if ( uiFunctions != null ){
					
					if ( !COConfigurationManager.getBooleanParameter("Library.TagInSideBar")){
						
						COConfigurationManager.setParameter("Library.TagInSideBar", true );
					}
					
					if ( !tag.isVisible()){
						
						tag.setVisible( true );
					}
					
					uiFunctions.openView( UIFunctions.VIEW_TAG, tag );
				}
			}
		}
	}

	public void 
	mouseEnter(
		TableRowCore row )
	{
	}

	public void 
	mouseExit(
		TableRowCore row)
	{	
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
