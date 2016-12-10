/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.columns.searchsubs;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.SearchSubsResultBase;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnSearchSubResultType
	implements TableCellSWTPaintListener, TableCellAddedListener,
	TableCellRefreshListener
{
	public static final String COLUMN_ID = "type";

	private static int WIDTH = 45;

	private static Image imgVideo;
	private static Image imgAudio;
	private static Image imgGame;
	private static Image imgOther;


	public ColumnSearchSubResultType(TableColumn column ) {
	
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_LAST, WIDTH );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if ( column instanceof TableColumnCore ){
			
			((TableColumnCore)column).addCellOtherListener("SWTPaint", this );
		}
		
		imgVideo = ImageLoader.getInstance().getImage("column.image.ct_video");
		imgAudio = ImageLoader.getInstance().getImage("column.image.ct_audio");
		imgGame = ImageLoader.getInstance().getImage("column.image.ct_game");
		imgOther = ImageLoader.getInstance().getImage("column.image.ct_other");
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		SearchSubsResultBase entry = (SearchSubsResultBase) cell.getDataSource();

		Rectangle cellBounds = cell.getBounds();
		Image img;
		if ( entry == null ){
			img = imgOther;
		}else{
			int ct = entry.getContentType();
			switch( ct ){
				case 0:{
					img = imgOther;
					break;
				}
				case 1:{
					img = imgVideo;
					break;
				}
				case 2:{
					img = imgAudio;
					break;
				}
				case 3:{
					img = imgGame;
					break;
				}
				default:{
					img = imgOther;
					break;
				}
			}	
		}

		if (img != null && !img.isDisposed()) {
			Rectangle imgBounds = img.getBounds();
			gc.drawImage(img, cellBounds.x
					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
					+ ((cellBounds.height - imgBounds.height) / 2));
		}
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void refresh(TableCell cell) {
		SearchSubsResultBase entry = (SearchSubsResultBase)cell.getDataSource();

		if ( entry != null ){
							
			if (!cell.setSortValue( entry.getContentType()) && cell.isValid()){
				
				return;
			}
		}
	}

}
