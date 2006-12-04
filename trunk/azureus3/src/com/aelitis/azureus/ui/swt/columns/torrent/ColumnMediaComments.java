/*
 * Created on Jun 29, 2006 11:04:37 PM
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

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;

/**
 * @author TuxPaper
 * @created Jun 29, 2006
 *
 */
public class ColumnMediaComments extends CoreTableColumn implements
		TableCellAddedListener
{
	private static String[] DEMO_COMMENTS = {
		"My Comments",
		"A very cute flick",
		"Totally Awesome Dude!",
		"Ribbiting!!",
		"Hot and Sexy, a must watch",
		"I peed my pants watching this!",
		"Larm ipsum dolar sit amot, consecteteur adipising dolor sit",
		"I want more!",
		"Pure enjoyment",
		"Sucks"
	};

	/**
	 * 
	 */
	public ColumnMediaComments(String sTableID) {
		super("MediaComments", sTableID);
		setWidth(120);
		setAlignment(ALIGN_CENTER);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		if (Math.random() > 0.4) {
			cell.setText(DEMO_COMMENTS[(int) (Math.random() * DEMO_COMMENTS.length)]);
		}
	}
}
