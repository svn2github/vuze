/*
 * Created on Jun 18, 2006 2:37:58 PM
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

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.NameItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SizeItem;

import com.aelitis.azureus.ui.swt.columns.recommend.ColumnRecEdit;
import com.aelitis.azureus.ui.swt.columns.recommend.ColumnRecFrom;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnAzProduct;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnQuality;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnRate;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.views.list.ListView;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author TuxPaper
 * @created Jun 18, 2006
 *
 */
public class RecommendationsView extends ListView
{
	final static TableColumnCore[] tableItems = {
		new ColumnAzProduct(TableManager.TABLE_MYTORRENTS_INCOMPLETE),

		new NameItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
		new SizeItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
		new ColumnQuality(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
		new ColumnRecFrom(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
		new ColumnRecEdit(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
		new ColumnRate(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
	};

	//TODO REMOVE HACK HACK HACK HACK !!!!!
	private static final String[] recs = {
		"World's Greatest Kite Surfing Stunts",
		"Clarkson Family Reunion",
		"Play better golf - chapter 2"
	};

	private static final long[] sizes = { 489934545, 9421154, 2008484
	};

	private volatile static int pos_index = 0;

	/**
	 * @param properties
	 * @param headerArea 
	 * @param dataArea
	 */
	public RecommendationsView(SWTSkinProperties skinProperties,
			Composite headerArea, Composite dataArea) {
		super("Recommendations", skinProperties, dataArea);

		// XXX Temporary width change
		tableItems[2].setWidth(190);

		updateColumnList(tableItems, null);

		if (headerArea != null) {
			setupHeader(headerArea);
		}
	}
}
