/*
 * Created on Jul 11, 2006 3:06:44 PM
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
package com.aelitis.azureus.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

import com.aelitis.azureus.ui.swt.columns.torrent.ColumnRate;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.views.list.ListView;

/**
 * @author TuxPaper
 * @created Jul 11, 2006
 *
 */
public class RateItListView extends ListView
{
	final String[] ICON_NAMES = {
		"icon.frogfingers.0",
		"icon.frogfingers.1",
		"icon.frogfingers.2",
		"icon.frogfingers.3",
		"icon.frogfingers.4",
		"icon.frogfingers.5",
		"icon.frogfingers.6",
	};

	/**
	 * @param sTableID
	 * @param skinProperties
	 * @param parent
	 */
	public RateItListView(SWTSkinProperties skinProperties, Composite parent) {
		super("RateItList", skinProperties, parent, SWT.V_SCROLL);

		updateColumnList(new TableColumnCore[] { new ColumnRate("RateItList")
		}, null);

		addDataSources(ICON_NAMES, false);
	}
}
