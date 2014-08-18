/*
 * File    : TypeItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.tracker;

import java.util.Locale;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;


public class 
TypeItem
	extends CoreTableColumnSWT 
    implements TableCellRefreshListener
{
	private static final String[] js_resource_keys = {
		"SpeedView.stats.unknown",
		"MyTrackerView.tracker",
		"wizard.webseed.title",
		"tps.type.dht",
		"ConfigView.section.transfer.lan",
		"tps.type.pex",
		"tps.type.incoming",
		"tps.type.plugin",
	};

	private static String[] js_resources = new String[js_resource_keys.length];

	
	public 
	TypeItem(String tableID)
	{
		super( "type", ALIGN_LEAD, POSITION_LAST, 75, tableID );
    
		setRefreshInterval(INTERVAL_INVALID_ONLY);
		
		MessageText.addAndFireListener(new MessageTextListener() {
			public void localeChanged(Locale old_locale, Locale new_locale) {
				for (int i = 0; i < js_resources.length; i++) {
					js_resources[i] = MessageText.getString(js_resource_keys[i]);
				}
			}
		});
	}

	public void 
	fillTableColumnInfo(
		TableColumnInfo info ) 
	{
		info.addCategories( new String[]{
			CAT_ESSENTIAL,
		});
	}

	public void 
	refresh(
		TableCell cell ) 
	{
		TrackerPeerSource ps = (TrackerPeerSource)cell.getDataSource();
    
		int value = (ps==null)?TrackerPeerSource.TP_UNKNOWN:ps.getType();

		if (!cell.setSortValue(value) && cell.isValid()){
		
			return;
		}

		cell.setText( js_resources[value]);
	}
}
