/*
 * File    : CategoryImpl.java
 * Created : 09 feb. 2004
 * By      : TuxPaper
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.core3.category.impl;

import java.util.List;
import java.util.ArrayList;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryListener;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

public class CategoryImpl implements Category, Comparable {
  private String sName;
  private int type;
	private List managers = new ArrayList();

  private static final int LDT_CATEGORY_DMADDED     = 1;
  private static final int LDT_CATEGORY_DMREMOVED   = 2;
	private ListenerManager	category_listeners = ListenerManager.createManager(
		"CatListenDispatcher",
		new ListenerManagerDispatcher()
		{
			public void
			dispatch(Object		_listener,
               int			type,
               Object		value )
			{
				CategoryListener target = (CategoryListener)_listener;
		
				if ( type == LDT_CATEGORY_DMADDED )
					target.downloadManagerAdded((Category) CategoryImpl.this, (DownloadManager)value);
				else if ( type == LDT_CATEGORY_DMREMOVED )
					target.downloadManagerRemoved(CategoryImpl.this, (DownloadManager)value);
			}
		});

  public CategoryImpl(String sName) {
    this.sName = sName;
    this.type = Category.TYPE_USER;
  }

  public CategoryImpl(String sName, int type) {
    this.sName = sName;
    this.type = type;
  }

	public void addCategoryListener(CategoryListener l) {
		category_listeners.addListener( l );
	}
	
	public void removeCategoryListener(CategoryListener l) {
		category_listeners.removeListener( l );
	}

  public String getName() {
    return sName;
  }
  
  public int getType() {
    return type;
  }
  
  public List getDownloadManagers() {
    return managers;
  }
  
  public void addManager(DownloadManager manager) {
    if (manager.getCategory() != this) {
      manager.setCategory(this);
      // we will be called again by CategoryManager.categoryChange
      return;
    }
    if (!managers.contains(manager)) {
      managers.add(manager);
      category_listeners.dispatch(LDT_CATEGORY_DMADDED, manager);
    }
  }

  public void removeManager(DownloadManager manager) {
    if (manager.getCategory() == this) {
      manager.setCategory(null);
      // we will be called again by CategoryManager.categoryChange
      return;
    }
    if (managers.contains(manager)) {
      managers.remove(manager);
      category_listeners.dispatch( LDT_CATEGORY_DMREMOVED, manager );
    }
  }

  public int compareTo(Object b)
  {
    boolean aTypeIsUser = type == Category.TYPE_USER;
    boolean bTypeIsUser = ((Category)b).getType() == Category.TYPE_USER;
    if (aTypeIsUser == bTypeIsUser)
      return sName.compareToIgnoreCase(((Category)b).getName());
    if (aTypeIsUser)
      return 1;
    return -1;
  }
}
