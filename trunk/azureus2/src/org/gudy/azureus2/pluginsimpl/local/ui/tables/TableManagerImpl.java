/*
 * Created on 19-Apr-2004
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ui.tables;


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableColumnImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;

/* Manage Tables
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class TableManagerImpl 
       implements TableManager
{	
	protected static TableManagerImpl	singleton;
	protected static AEMonitor 			class_mon 	= new AEMonitor( "TableManager" );

	public static TableManagerImpl getSingleton() {
		try{
			class_mon.enter();
		
			if (singleton == null)
				singleton = new TableManagerImpl();
			return singleton;
		}finally{
			
			class_mon.exit();
		}
	}

  public TableColumn createColumn(String tableID, String cellID) {
    return new TableColumnImpl(tableID, cellID);
  }

  public void addColumn(TableColumn tableColumn) {
    if (!(tableColumn instanceof TableColumnCore))
			throw(new UIRuntimeException("TableManager.addColumn(..) can only add columns created by createColumn(..)"));
    
    TableColumnManager.getInstance().addColumn((TableColumnCore)tableColumn);
  }
  
  public TableContextMenuItem addContextMenuItem(String tableID, String resourceKey) {
    TableContextMenuItemImpl item = new TableContextMenuItemImpl(tableID, resourceKey);
    TableContextMenuManager.getInstance().addContextMenuItem(item);
    return item;
  }
}
