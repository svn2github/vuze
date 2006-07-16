/*
 * File    : TableRowComparator.java
 * Created : Nov 14, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Comparator;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import org.gudy.azureus2.plugins.ui.tables.TableColumn;


/**
 * Comparator for TableRow objects based on the sort value of TableCell 
 * in the row.
 * 
 * @author TuxPaper
 */
public class TableRowComparator implements Comparator {
	/** Name of column sort will use */
	private String sColumnName;
	
	private TableColumnCore tc;
	
	/** Order in which sort will use */
	private boolean bAscending;
	
	/**
	 * Default Constructor
	 * 
	 * @param sSortColumnName
	 * @param bAscending
	 */
	public TableRowComparator(TableColumnCore tc, boolean bAscending) {
		this.tc = tc;
		this.sColumnName = tc.getName();
		this.bAscending = bAscending;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object arg0, Object arg1) {
		TableCellCore cell0 = ((TableRowImpl)arg0).getTableCellCore(sColumnName);
		TableCellCore cell1 = ((TableRowImpl)arg1).getTableCellCore(sColumnName);
		
		Comparable c0 = (cell0 == null) ? "" : cell0.getSortValue();
		Comparable c1 = (cell1 == null) ? "" : cell1.getSortValue();
		
		try {
			boolean c0isString = c0 instanceof String; 
			boolean c1isString = c1 instanceof String; 
			if (c0isString && c1isString) {
				if (bAscending)
					return ((String)c0).compareToIgnoreCase((String)c1);

				return ((String)c1).compareToIgnoreCase((String)c0);
			}
			
			if (bAscending) {
				if (c0isString && !c1isString) {
					return -1;
				}
				return c0.compareTo(c1);
			}
			
			if (c1isString && !c0isString) {
				return 1;
			}
			return c1.compareTo(c0);
		} catch (ClassCastException e) {
			System.err.println("Can't compare " + c0.getClass().getName()
					+ "(" + c0.toString() + ") from row #" 
					+ cell0.getTableRowCore().getIndex() + " to "
					+ c1.getClass().getName()
					+ "(" + c1.toString() + ") from row #"
					+ cell1.getTableRowCore().getIndex() 
					+ " while sorting column " + sColumnName);
			e.printStackTrace();
			return 0;
		}
	}
	
	public void setColumn(TableColumnCore tc) {
		if (tc == this.tc) {
			return;
		}
		this.tc = tc;
		tc.setLastSortValueChange(SystemTime.getCurrentTime());
		sColumnName = tc.getName();
	}

	/**
	 * @return Returns the columnName.
	 */
	public String getColumnName() {
		return sColumnName;
	}

	public TableColumnCore getColumn() {
		return tc;
	}

	/**
	 * @param bAscending The bAscending to set.
	 */
	public void setAscending(boolean bAscending) {
		if (this.bAscending == bAscending) {
			return;
		}
		tc.setLastSortValueChange(SystemTime.getCurrentTime());
		
		this.bAscending = bAscending;
	}

	/**
	 * @return Returns the bAscending.
	 */
	public boolean isAscending() {
		return bAscending;
	}
}
