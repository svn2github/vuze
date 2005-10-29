/*
 * File    : TrackerStatusItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * @author Olivier
 *
 */
public class TrackerStatusItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  public TrackerStatusItem(String sTableID) {
    super("tracker", POSITION_LAST, 90, sTableID);
    setRefreshInterval(10);		// slow refresh as getTrackerStatus is relatively expensive
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    
    String	status = dm == null ? "" : dm.getTrackerStatus();
    
    	// status sometimes contains multiline text (e.g. HTML) on failure - trim to end of first
    	// line break if present (see bug 1337563)
    
    int	nl_pos = status.indexOf( '\n' );
    
    if ( nl_pos >= 0 ){
    	
    	status = status.substring(0,nl_pos);
    }
    
    cell.setText( status );
  }
}
