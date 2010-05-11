/**
 * Created on May 3, 2010
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

package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.views.table.TableColumnOrTreeColumn;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * @author TuxPaper
 * @created May 3, 2010
 *
 */
public class TableOrTreeUtils
{
	public static TableItemOrTreeItem getEventItem(Widget item) {
		if (item instanceof TreeItem) {
			return new TreeItemDelegate((TreeItem) item);
		}
		if (item instanceof TableItem) {
			return new TableItemDelegate((TableItem) item);
		}
		return null;
	}

	public static TableColumnOrTreeColumn getTableColumnEventItem(Widget item) {
		if (item instanceof TreeColumn) {
			return new TreeColumnDelegate((TreeColumn) item);
		}
		if (item instanceof TableColumn) {
			return new TableColumnDelegate((TableColumn) item);
		}
		return null;
	}

	public static TableOrTreeSWT createGrid(Composite parent, int style,
			boolean tree) {
		return tree ? new TreeDelegate(parent, style) : new TableDelegate(parent,
				style);
	}

	public static ControlEditor createTableOrTreeEditor(TableOrTreeSWT tableOrTree) {
		return (tableOrTree instanceof TreeDelegate) ? new TreeEditor(
				(Tree) tableOrTree.getComposite()) : new TableEditor(
				(Table) tableOrTree.getComposite());
	}

	/**
	 * @since 4.4.0.5
	 */
	public static void setEditorItem(ControlEditor editor, Control input,
			int column, TableItemOrTreeItem item) {
		if (item instanceof TreeItemDelegate) {
			((TreeEditor)editor).setEditor(input, (TreeItem) item.getItem(), column);
		} else {
			((TableEditor)editor).setEditor(input, (TableItem) item.getItem(), column);
		}
	}
}
