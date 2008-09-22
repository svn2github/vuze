/**
 * Created on Sep 20, 2008
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
 
package org.gudy.azureus2.ui.swt.views.table;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;

/**
 * @author TuxPaper
 * @created Sep 20, 2008
 *
 */
public interface TableColumnCoreCreationListener
	extends TableColumnCreationListener
{
	public TableColumnCore createTableColumnCore(String tableID, String columnID);
}
