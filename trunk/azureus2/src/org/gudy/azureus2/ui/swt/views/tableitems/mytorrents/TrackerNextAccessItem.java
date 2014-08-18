/*
 * File    : TrackerStatusItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.HashMap;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.TimeFormatter;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

/**
 * @author Olivier
 *
 */
public class TrackerNextAccessItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener, TableCellDisposeListener,
                  TableCellToolTipListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "trackernextaccess";
	HashMap map = new HashMap();
	
  public TrackerNextAccessItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 70, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_TRACKER,
			CAT_TIME,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    if (cell.isValid() && map.containsKey(dm)) {
    	long lNextUpdate = ((Long)map.get(dm)).longValue();
    	if (System.currentTimeMillis() < lNextUpdate)
    		return;
    }
    long value = (dm == null) ? 0 : dm.getTrackerTime();
    
    if (value < -1)
      value = -1;

    long lNextUpdate = System.currentTimeMillis()
				+ (((value > 60) ? (value % 60) : 1) * 1000);
		map.put(dm, new Long(lNextUpdate));

    if (!cell.setSortValue(value) && cell.isValid())
      return;

    String sText = TimeFormatter.formatColon(value);
    
    if (value > 60)
    	sText = "< " + sText;
    
  	TrackerCellUtils.updateColor(cell, dm, false);
    cell.setText(sText);
  }

	public void cellHover(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		cell.setToolTip(TrackerCellUtils.getTooltipText(cell, dm, false));
	}

	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}

	public void dispose(TableCell cell) {
		map.remove(cell.getDataSource());
	}
}
