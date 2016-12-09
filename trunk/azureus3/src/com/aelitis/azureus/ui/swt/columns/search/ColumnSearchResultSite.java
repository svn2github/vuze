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

package com.aelitis.azureus.ui.swt.columns.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader.ImageDownloaderListener;
import com.aelitis.azureus.ui.swt.search.SBC_SearchResult;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnSearchResultSite
	implements TableCellSWTPaintListener, TableCellAddedListener,
	TableCellRefreshListener
{
	public static final String COLUMN_ID = "site";

	private static int WIDTH = 38; // enough to fit title

	private static ImageLoader	image_loader = new ImageLoader( null, null );

	private static Map<String,Object[]>	image_map = new HashMap<String,Object[]>();

	public ColumnSearchResultSite(TableColumn column ) {
	
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_LAST, WIDTH );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if ( column instanceof TableColumnCore ){
			
			((TableColumnCore)column).addCellOtherListener("SWTPaint", this );
		}
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		SBC_SearchResult entry = (SBC_SearchResult) cell.getDataSource();

		Rectangle cellBounds = cell.getBounds();
		
		final String icon = entry.getEngine().getIcon();
		
		Image img = null;
		
		if ( icon != null ){
			
			Object[] x = image_map.get( icon );
			
			if ( x == null ){
				
				Set<SBC_SearchResult>	waiters = new HashSet<SBC_SearchResult>();
				
				final Object[] f_x = new Object[]{ null, waiters, SystemTime.getMonotonousTime() };
				
				waiters.add( entry );
				
				image_map.put( icon, f_x );
				
				image_loader.getUrlImage( icon, 
					new ImageDownloaderListener() {
						
						public void imageDownloaded(Image image, boolean returnedImmediately) {
							
							f_x[0]	= image;
							
							Set<SBC_SearchResult> set = (Set<SBC_SearchResult>)f_x[1];
			
							for ( SBC_SearchResult result: set ){
								
								result.invalidate();
							}
							
							f_x[1] = null;
						}
					});
			}else{
				
				if ( x[1] instanceof Set ){
					
					((Set<SBC_SearchResult>)x[1]).add( entry );
					
				}else{
					
					img = (Image)x[0];
					
					if ( img == null ){
						
						if ( SystemTime.getMonotonousTime() - (Long)x[2] > 120*1000 ){
							
							image_map.remove( icon );
						}
					}
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
		
		if ( cell instanceof TableCellSWT ){
		
			((TableCellSWT)cell).setCursorID( SWT.CURSOR_HAND );
		}
	}

	public void refresh(TableCell cell) {
		SBC_SearchResult entry = (SBC_SearchResult)cell.getDataSource();

		if ( entry != null ){
			
			long sortVal = entry.getEngine().getId();
			
			if (!cell.setSortValue(sortVal) && cell.isValid()) {
				return;
			}
		}
	}
}
