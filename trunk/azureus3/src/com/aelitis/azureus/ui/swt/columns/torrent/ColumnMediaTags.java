/*
 * Created on Jun 29, 2006 11:02:58 PM
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
package com.aelitis.azureus.ui.swt.columns.torrent;

import java.util.ArrayList;
import java.util.Arrays;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jun 29, 2006
 *
 */
public class ColumnMediaTags extends CoreTableColumn implements
		TableCellRefreshListener, TableCellAddedListener
{
	final static String[] DEMO_TAGS = {
		"Funny",
		"Sad",
		"Long",
		"Short",
		"Horror",
		"Sci-Fi",
		"Romantic",
		"Frogs",
	};

	/**
	 * 
	 */
	public ColumnMediaTags(String sTableID) {
		super("MediaTags", sTableID);
		setWidth(80);
		setAlignment(ALIGN_CENTER);

	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		ArrayList list = new ArrayList();
		list.addAll(Arrays.asList(DEMO_TAGS));

		String s = "";
		do {
			if (s != "") {
				s += ", ";
			}
			int i = (int) (Math.random() * list.size());
			s += list.get(i);
			list.remove(i);
		} while (Math.random() > 0.5 && list.size() > 0);
		cell.setText(s);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
	}
}
