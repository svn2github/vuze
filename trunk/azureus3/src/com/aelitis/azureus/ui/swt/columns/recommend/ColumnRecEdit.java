/*
 * Created on Jun 19, 2006 1:02:59 PM
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

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jun 19, 2006
 *
 */
public class ColumnRecEdit
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener
{
	/**
	 * 
	 */
	public ColumnRecEdit(String sTableID) {
		super("RecEdit", sTableID);
		initializeAsGraphic(POSITION_LAST, 18);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);

		if (Math.random() > 0.3) {
			Image img = ImageLoaderFactory.getInstance().getImage("icon.editpencil");
			Graphic graphic = new UISWTGraphicImpl(img);
			cell.setGraphic(graphic);
		}
	}

	public void refresh(TableCell cell) {

	}
}
