/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.util.*;

import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;

import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;

/**
 * @author TuxPaper
 * @created Feb 19, 2015
 *
 */
public class UIToolBarManagerImpl
	implements UIToolBarManagerCore
{
	private static UIToolBarManagerImpl instance;

	private Map<String, UIToolBarItem> items = new LinkedHashMap<String, UIToolBarItem>();
	
	private Map<String, List<String>> mapGroupToItemIDs = new HashMap<String, List<String>>();

	public List<ToolBarManagerListener> listListeners = new ArrayList<ToolBarManagerListener>();

	public static UIToolBarManager getInstance() {
		if (instance == null) {
			instance = new UIToolBarManagerImpl();
		}
		return instance;
	}

	public static interface ToolBarManagerListener
	{
		public void toolbarItemRemoved(UIToolBarItem item);

		public void toolbarItemAdded(UIToolBarItem item);
	}

	
	public UIToolBarManagerImpl() {
		SelectedContentManager.addCurrentlySelectedContentListener(new SelectedContentListener() {
			public void currentlySelectedContentChanged(
					ISelectedContent[] currentContent, String viewID) {
				if (viewID == null) {
  				ToolBarItem[] allSWTToolBarItems = getAllSWTToolBarItems();
  				for (ToolBarItem item : allSWTToolBarItems) {
  					item.setState(0);
  				}
				}
			}
		});
	}
	
	public void addListener(ToolBarManagerListener l) {
		synchronized (listListeners) {
			listListeners.add(l);
		}
	}

	public void removeListener(ToolBarManagerListener l) {
		synchronized (listListeners) {
			listListeners.remove(l);
		}
	}

	public UIToolBarItem getToolBarItem(String itemID) {
		return items.get(itemID);
	}

	public UIToolBarItem[] getAllToolBarItems() {
		return items.values().toArray(new UIToolBarItem[0]);
	}

	public ToolBarItem[] getAllSWTToolBarItems() {
		return items.values().toArray(new ToolBarItem[0]);
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager#createToolBarItem(java.lang.String)
	public UIToolBarItem createToolBarItem(String id) {
		UIToolBarItemImpl base = new UIToolBarItemImpl(id);
		return base;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager#addToolBarItem(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem)
	public void addToolBarItem(UIToolBarItem item) {
		addToolBarItem(item, true);
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager#addToolBarItem(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem)
	public void addToolBarItem(UIToolBarItem item, boolean trigger) {
		if (item == null) {
			return;
		}
		if (items.containsKey(item.getID())) {
			return;
		}
		
		items.put(item.getID(), item);

		String groupID = item.getGroupID();
		synchronized (mapGroupToItemIDs) {
			List<String> list = mapGroupToItemIDs.get(groupID);
			if (list == null) {
				list = new ArrayList<String>();
				mapGroupToItemIDs.put(groupID, list);
			}
			list.add(item.getID());
		}

		if (trigger) {
  		ToolBarManagerListener[] listeners = listListeners.toArray(new ToolBarManagerListener[0]);
  		for (ToolBarManagerListener l : listeners) {
  			l.toolbarItemAdded(item);
  		}
		}
	}

	public String[] getToolBarIDsByGroup(String groupID) {
		synchronized (mapGroupToItemIDs) {
			List<String> list = mapGroupToItemIDs.get(groupID);
			if (list == null) {
				return new String[0];
			}
			return list.toArray(new String[0]);
		}
	}

	public UIToolBarItem[] getToolBarItemsByGroup(String groupID) {
		synchronized (mapGroupToItemIDs) {
			List<String> list = mapGroupToItemIDs.get(groupID);
			if (list == null) {
				return new UIToolBarItem[0];
			}
			UIToolBarItem[] items = new UIToolBarItem[list.size()];
			int i = 0;
			for (String id : list) {
				items[i] = getToolBarItem(id);
				i++;
			}
			return items;
		}
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager#removeToolBarItem(java.lang.String)
	public void removeToolBarItem(String id) {
		UIToolBarItem toolBarItem = items.remove(id);
		if (toolBarItem != null) {

			synchronized (mapGroupToItemIDs) {
				List<String> list = mapGroupToItemIDs.get(toolBarItem.getGroupID());
				if (list != null) {
					list.remove(toolBarItem.getID());
				}
			}

			ToolBarManagerListener[] listeners = listListeners.toArray(new ToolBarManagerListener[0]);
			for (ToolBarManagerListener l : listeners) {
				l.toolbarItemRemoved(toolBarItem);
			}
		}
	}
	
	// @see org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerCore#getGroupIDs()
	public String[] getGroupIDs() {
		return mapGroupToItemIDs.keySet().toArray(new String[0]);
	}
}
