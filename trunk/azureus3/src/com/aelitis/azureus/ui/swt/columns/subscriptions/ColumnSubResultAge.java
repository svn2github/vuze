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

package com.aelitis.azureus.ui.swt.columns.subscriptions;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.subscriptions.SBC_SubscriptionResult;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.plugins.ui.tables.*;

public class ColumnSubResultAge
	implements TableCellRefreshListener
{
	public static final String COLUMN_ID = "age";

	/**
	 * 
	 * @param sTableID
	 */
	public ColumnSubResultAge(TableColumn column) {
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_INVISIBLE, 60 );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		
		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
		}
	}

	public void refresh(TableCell cell) {
		SBC_SubscriptionResult rc = (SBC_SubscriptionResult) cell.getDataSource();
		if (rc == null) {
			return;
		}

		long time = rc.getTimeFound();
				
		long age_secs = (SystemTime.getCurrentTime() - time)/1000;

		if ( cell.setSortValue( age_secs )){
		
			cell.setToolTip(time <= 0?"--":DisplayFormatters.formatCustomDateOnly( time ));
			cell.setText( age_secs <= 0?"--":TimeFormatter.format( age_secs ));
		}
	}
}
