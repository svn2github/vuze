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
 *
 */

package com.aelitis.azureus.ui.swt.columns.searchsubs;


import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.swt.utils.SearchSubsResultBase;
import com.aelitis.azureus.ui.swt.utils.SearchSubsUtils;


public class ColumnSearchSubResultExisting
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{
	public static String COLUMN_ID = "in";

	private static final String[] messages = new String[5];
	
	static{
	   	COConfigurationManager.addAndFireListener(
    		new COConfigurationListener()
    		{
    			public void 
    			configurationSaved() 
    			{
    				messages[0]	= "";
       				messages[1]	= MessageText.getString( "label.library" );
       				messages[2]	= MessageText.getString( "label.archive" );
       				messages[3]	= MessageText.getString( "label.history" );		
       				messages[4] = "?";
    			}
    		});
	}
	
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnSearchSubResultExisting(TableColumn column) {
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_INVISIBLE, 60 );
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC );
		column.addListeners(this);
	}

	public void refresh(TableCell cell) {
		SearchSubsResultBase result = (SearchSubsResultBase)cell.getDataSource();
			
		int status = SearchSubsUtils.getHashStatus( result );
		
		if ( !cell.setSortValue(status) && cell.isValid()){
			
			return;
		}

		if (!cell.isShown()){
			
			return;
		}
		
		cell.setText(messages[status]);
	}
}
