/*
 * Created on Jun 19, 2006 12:56:15 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.columns.recommend;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;

/**
 * @author TuxPaper
 * @created Jun 19, 2006
 *
 */
public class ColumnRecFrom extends CoreTableColumn implements
		TableCellAddedListener
{
	String[] SAMPLES = new String[] { "Froglegs", "Lilypad", "Tadpol",
	};

	public ColumnRecFrom(String sTableID) {
		super("RecFrom", sTableID);
		setWidth(60);
		setAlignment(ALIGN_CENTER);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		int i = (int) (Math.random() * SAMPLES.length);
		cell.setText(SAMPLES[i]);
	}
}
