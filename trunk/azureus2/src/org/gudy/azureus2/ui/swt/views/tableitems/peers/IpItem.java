/*
 * File    : IpItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/19: modified to TableCellAdapter)
 */
public class IpItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
	static boolean	show_host_names = false;
	
	static{
		show_host_names = COConfigurationManager.getBooleanParameter("config.style.showHostNames", false );
		
		COConfigurationManager.addParameterListener(
				"config.style.showHostNames",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	str )
					{
						show_host_names = COConfigurationManager.getBooleanParameter("config.style.showHostNames", false );						
					}
				});
	}
	
  /** Default Constructor */
  public IpItem() {
    super("ip", POSITION_LAST, 100, TableManager.TABLE_TORRENT_PEERS);
  }

  public void refresh(TableCell cell) {
    PEPeer peer = (PEPeer)cell.getDataSource();
    String sText = (peer == null) ? "" : (show_host_names?peer.getIPHostName():peer.getIp());

    cell.setText(sText);
  }
}
