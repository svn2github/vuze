/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * Legacy support for EMP.  Class moved to
 * {@link org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell}
 * 
 * @author TuxPaper
 * @created Jan 1, 2008
 *
 */
public class FakeTableCell
	extends org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell
{
	public FakeTableCell(TableColumn column) {
		super(column);
	}

	public FakeTableCell(TableColumnCore column) {
		super(column);
	}
}
