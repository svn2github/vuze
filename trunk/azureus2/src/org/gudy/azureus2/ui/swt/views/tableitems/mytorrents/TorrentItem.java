/*
 * File    : TorrentItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.ui.swt.components.BufferedTableItem;

/**
 * @author Olivier
 *
 */
public abstract class TorrentItem extends BufferedTableItem {

  protected TorrentRow torrentRow;
  
  /**
   * @param row
   * @param position
   *
   * @note Add 1 to position because we make a non resizable 0-sized 1st column
   *       to fix the 1st column gap problem (Eclipse Bug 43910)
   */
  public TorrentItem(TorrentRow torrentRow, int position) {
  		// remembering that columns that have not been selected for viewing 
  		// have a position of -1 and thus NOT incrementing this to 0!
  	
    super(torrentRow.getRow(), position >=0?position+1:position);
    this.torrentRow = torrentRow;
  }

}
