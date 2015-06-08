/**
 * Created on May 10, 2013
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

package com.aelitis.azureus.plugins.net.buddy.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

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
import com.aelitis.azureus.ui.swt.views.skin.InfoBarUtil;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginUtils;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.*;
import com.aelitis.azureus.plugins.net.buddy.swt.columns.*;

/**
 * @author TuxPaper
 * @created May 10, 2013
 *
 */
public class SBC_ChatOverview
	extends SkinView
	implements 	UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<ChatInstance>, 
				ChatManagerListener, TableViewSWTMenuFillListener, TableSelectionListener
{

	private static final String TABLE_CHAT = "ChatsView";

	TableViewSWT<ChatInstance> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

	private boolean listener_added;
	
	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if ( tv == null || !tv.isVisible()){
			return( false );
		}
		if (item.getID().equals("remove")) {
			
			Object[] datasources = tv.getSelectedDataSources().toArray();
			
			if ( datasources.length > 0 ){
				
				for (Object object : datasources) {
					if (object instanceof ChatInstance) {
						ChatInstance chat = (ChatInstance) object;
						chat.destroy();
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
		if ( tv == null || !tv.isVisible()){
			return;
		}

		boolean canEnable = false;
		Object[] datasources = tv.getSelectedDataSources().toArray();
		
		if ( datasources.length > 0 ){
			
			for (Object object : datasources) {
				if (object instanceof ChatInstance ) {
					
				}
			}
		}

		list.put("remove", canEnable ? UIToolBarItem.STATE_ENABLED : 0);
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

		new InfoBarUtil(skinObject, "chatsview.infobar", false,
				"chats.infobar", "chats.view.infobar") {
			public boolean allowShow() {
				return true;
			}
		};

		return null;
	}

	protected void initColumns() {
		synchronized (SBC_ChatOverview.class) {

			if (columnsAdded) {

				return;
			}

			columnsAdded = true;
		}

		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.registerColumn(ChatInstance.class, ColumnChatName.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatName(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatMessageCount.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatMessageCount(column);
					}
				});
		
		tableManager.registerColumn(ChatInstance.class, ColumnChatUserCount.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatUserCount(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatFavorite.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatFavorite(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatMsgOutstanding.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatMsgOutstanding(column);
					}
				});

		
			// last
		
		tableManager.registerColumn(ChatInstance.class, ColumnChatStatus.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatStatus(column);
					}
				});

		tableManager.setDefaultColumnNames(TABLE_CHAT,
			new String[] {
				
				ColumnChatName.COLUMN_ID,
				ColumnChatMessageCount.COLUMN_ID,
				ColumnChatUserCount.COLUMN_ID,
				ColumnChatFavorite.COLUMN_ID,
				ColumnChatMsgOutstanding.COLUMN_ID,
				
				ColumnChatStatus.COLUMN_ID,

			});
		
		tableManager.setDefaultSortColumnName(TABLE_CHAT, ColumnTagName.COLUMN_ID);
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
		
		BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();
		
		if ( beta != null) {
			
			beta.removeListener( this );
			
			listener_added = false;
		}


		return super.skinObjectHidden(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		SWTSkinObject so_list = getSkinObject("chats-list");

		if (so_list != null) {
			initTable((Composite) so_list.getControl());
		} else {
			System.out.println("NO chats-list");
			return null;
		}
		
		if (tv == null) {
			return null;
		}

		BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();
		
		if ( beta != null) {
			
			if ( !listener_added ){
				
				listener_added = true;
			
				beta.addListener(this, true);
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
		if ( listener_added ){
		
			listener_added = false;
			
			BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();
			
			if ( beta != null) {
				
				beta.removeListener( this );
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
			
			tv = TableViewFactory.createTableViewSWT(ChatInstance.class, TABLE_CHAT, TABLE_CHAT,
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
		
		final List<ChatInstance>	chats = new ArrayList<ChatInstance>();
		
		for ( Object obj: ds ){
			
			if ( obj instanceof ChatInstance ){
				
				chats.add((ChatInstance)obj);
			}
		}
					
		MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
		
		Messages.setLanguageText(itemRemove, "MySharesView.menu.remove");
		
		Utils.setMenuItemImage(itemRemove, "delete");

		itemRemove.setEnabled(chats.size() > 0);

		itemRemove.addListener(SWT.Selection, new Listener() {
			public void 
			handleEvent(
				Event e ) 
			{
				for ( ChatInstance chat: chats ){
					
					chat.remove();
				}
			}   
		});
		
		new MenuItem( menu, SWT.SEPARATOR );
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
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}

	public void 
	deselected(
		TableRowCore[] rows )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}
	
	public void 
	focusChanged(
		TableRowCore focus )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}

	public void 
	defaultSelected(
		TableRowCore[] 	rows, 
		int 			stateMask )
	{
		if ( rows.length == 1 ){
			
			Object obj = rows[0].getDataSource();
			
			if ( obj instanceof ChatInstance ){
				
				ChatInstance chat = (ChatInstance)obj;
				
				BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();
				
				if ( beta != null) {
					
					try{
						beta.showChat( chat.getClone());
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
	}

	public void chatAdded(ChatInstance chat) {
		if ( !chat.isInvisible()){
			tv.addDataSource(chat);
		}
	}

	public void chatChanged(ChatInstance chat) {
		if (tv == null || tv.isDisposed()) {
			return;
		}
		TableRowCore row = tv.getRow(chat);
		if (row != null) {
			row.invalidate(true);
		}
	}

	public void chatRemoved(ChatInstance chat) {
		tv.removeDataSource(chat);
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
	public boolean filterCheck(ChatInstance ds, String filter, boolean regex) {
		return false;
	}
}
