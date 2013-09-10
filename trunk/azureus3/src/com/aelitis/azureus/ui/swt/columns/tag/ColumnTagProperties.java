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

package com.aelitis.azureus.ui.swt.columns.tag;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnExtraInfoListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureProperties;


public class 
ColumnTagProperties
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{	
	public static String COLUMN_ID = "tag.properties";

	public void 
	fillTableColumnInfo(
		TableColumnInfo info ) 
	{
		info.addCategories (new String[]{ TableColumn.CAT_SETTINGS });
		
		info.setProficiency( TableColumnInfo.PROFICIENCY_BEGINNER );
	}
	
	public 
	ColumnTagProperties(
		TableColumn column ) 
	{
		column.setWidth(160);
		column.setRefreshInterval(TableColumn.INTERVAL_LIVE);
		column.addListeners(this);
	}

	public void 
	refresh(
		TableCell cell) 
	{
		Tag tag = (Tag)cell.getDataSource();
		
		String text = "";
		
		if ( tag instanceof TagFeatureProperties ){
			
			TagFeatureProperties tp = (TagFeatureProperties)tag;
			
			TagFeatureProperties.TagProperty[] props = tp.getSupportedProperties();
			
			if ( props.length > 0 ){
				
				for ( TagFeatureProperties.TagProperty prop: props ){
					
					String prop_str = prop.getString();
					
					if ( prop_str.length() > 0 ){
						
						text += (text.length()==0?"":"; ") + prop_str;
					}
				}
			}
		}
		

		if ( !cell.setSortValue( text ) && cell.isValid()){
			
			return;
		}

		if ( !cell.isShown()){
			
			return;
		}
		
		cell.setText( text );
	}
}
