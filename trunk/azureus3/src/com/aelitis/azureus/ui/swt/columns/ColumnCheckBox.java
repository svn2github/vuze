/*
 * File    : CategoryItem.java
 * Created : 01 feb. 2004
 * By      : TuxPaper
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package com.aelitis.azureus.ui.swt.columns;

import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnExtraInfoListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;


public abstract class 
ColumnCheckBox
	implements TableCellRefreshListener, TableColumnExtraInfoListener, TableCellMouseListener
{	
	private static final UISWTGraphic tick_icon;
	private static final UISWTGraphic cross_icon;
	
	static {
		tick_icon = new UISWTGraphicImpl(ImageLoader.getInstance().getImage("check_yes"));
		cross_icon = new UISWTGraphicImpl(ImageLoader.getInstance().getImage("check_no"));
	}
	
	public void 
	fillTableColumnInfo(
		TableColumnInfo info) 
	{
		info.addCategories(new String[] {
				TableColumn.CAT_ESSENTIAL,
		});
		
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public 
	ColumnCheckBox(
		TableColumn column ) 
	{
		column.setWidth(40);
		column.setType( TableColumn.TYPE_GRAPHIC );
		column.addListeners(this);
	}
	
	protected abstract Boolean
	getCheckBoxState(
		Object		datasource );
	
	protected abstract void
	setCheckBoxState(
		Object		datasource,
		boolean		set );
	
	public void 
	cellMouseTrigger(
		TableCellMouseEvent event )
	{
		if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEUP ){
			
			TableCell cell = event.cell;
			
			int	event_x 		= event.x;
			int	event_y 		= event.y;
			int	cell_width 		= cell.getWidth();
			int	cell_height 	= cell.getHeight();
			
			Rectangle icon_bounds = tick_icon.getImage().getBounds();
			
			int x_pad = ( cell_width - icon_bounds.width ) / 2;
			int y_pad = ( cell_height - icon_bounds.height ) / 2;
			
			if ( 	event_x >= x_pad && event_x <= cell_width - x_pad &&
					event_y >= y_pad && event_y <= cell_height - y_pad ){
				
				Object datasource = cell.getDataSource();
				
				Boolean state = getCheckBoxState( datasource );
				
				if ( state != null ){
																
					setCheckBoxState( datasource, !state );
						
					cell.invalidate();
						
					if ( cell instanceof TableCellCore ){
							
						((TableCellCore)cell).refresh( true );
					}
				}
			}
		}
	}
	
	public void 
	refresh(
		TableCell cell )
	{
		Boolean state = getCheckBoxState( cell.getDataSource());
		
		int 			sortVal = 0;
		UISWTGraphic	icon 	= null;

		if ( state != null ){
			
			if ( state ){
				
				sortVal = 2;
				icon 	= tick_icon;
				
			}else{
				
				sortVal = 1;
				icon 	= cross_icon;
			}
		}
		
		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		if ( cell.getGraphic() != icon ){
    	
			cell.setGraphic( icon );
		}
	}
}
