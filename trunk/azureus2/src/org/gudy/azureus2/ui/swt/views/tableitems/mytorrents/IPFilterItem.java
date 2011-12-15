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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

/** Display Category torrent belongs to.
 *
 * @author TuxPaper
 */
public class IPFilterItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
	private static final IpFilter ipfilter = IpFilterManagerFactory.getSingleton().getIPFilter();
	
	public static final Class DATASOURCE_TYPE = Download.class;

  public static final String COLUMN_ID = "ipfilter";

	/** Default Constructor */
  public IPFilterItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 100, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONNECTION,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE );
	}

  public void refresh(TableCell cell) {
    String state = "";
    if( ipfilter.isEnabled()){
	    DownloadManager dm = (DownloadManager)cell.getDataSource();
	    if (dm != null) {
	       boolean excluded = dm.getDownloadState().getFlag( DownloadManagerState.FLAG_DISABLE_IP_FILTER );
	  
	       if ( excluded ){
	    	   
	    	   state = "\u2718";
	    	   
	       }else{
	    	   
	    	   state = "\u2714";
	       }
	    }
    }
    cell.setText( state );
  }
}
