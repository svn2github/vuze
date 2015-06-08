/*
 * File    : CategoryItem.java
 * Created : 01 feb. 2004
 * By      : TuxPaper
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.columns.tag;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnExtraInfoListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureExecOnAssign;
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
		
		info.setProficiency( TableColumnInfo.PROFICIENCY_INTERMEDIATE );
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
		
		if ( tag instanceof TagFeatureExecOnAssign ){
			
			TagFeatureExecOnAssign eoa = (TagFeatureExecOnAssign)tag;
			
			int	actions = eoa.getSupportedActions();
			
			if ( actions != TagFeatureExecOnAssign.ACTION_NONE ){
							
				String actions_str = "";
				
				if ( eoa.supportsAction( TagFeatureExecOnAssign.ACTION_DESTROY )){
					
					boolean enabled = eoa.isActionEnabled( TagFeatureExecOnAssign.ACTION_DESTROY );
					
					if ( enabled ){
					
						actions_str += MessageText.getString( "FileItem.delete") + "=Y";
					}
				}
				
				if ( actions_str.length() > 0 ){
					
					text += (text.length()==0?"":"; ") +  MessageText.getString( "label.exec.on.assign" ) + ": ";

					text += actions_str;
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
