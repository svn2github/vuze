/*
 * File    : TableRow.java
 * Created : 2004/Apr/30
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
 
package org.gudy.azureus2.plugins.ui.tables;


/**
 * This interface provides access to an Azureus table row.
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public interface TableRow {
  /** Retrieve the data object associated with the current table row.
   *
   * @return The return type is dependent upon which table the cell is for:<br>
   *   TABLE_MYTORRENTS_*: {@link org.gudy.azureus2.plugins.download.Download}
   *                       object for the current row<br>
   *   TABLE_TORRENT_PEERS: {@link org.gudy.azureus2.plugins.peers.Peer} 
   *                        object for the current row<br>
   *   TABLE_MYTRACKER: {@link org.gudy.azureus2.plugins.tracker.TrackerTorrent}
   *                    object for the current row<br>
   *   TABLE_MYSHARES: {@link org.gudy.azureus2.plugins.sharing.ShareResource}
   *                    object for the current row<br>
   *   remaining TABLE_* constants: undefined or null<br>
   */
  Object getDataSource();
  
  /** Returns which table the row is being displayed in.
   *
   * @return {@link TableManager}.TABLE_* constant
   */
  String getTableID();

  // Future
  // public void setForeground(int red, int green, int blue);


  /** Validility of the row's cells.
   *
   * @return True - Text is the same as last call.  You do not need to update
   *                unless you have new text to display. <br>
   *         False - Cell-to-Datasource link has changed, and the text is
   *                 definitely not valid.
   */
  boolean isValid();

  /** Retrieve a cell based on its column name
   *
   * @param sColumnName Name/ID of column
   * @return TableCell object related to this row and the column specified
   */
  TableCell getTableCell(String sColumnName);
}
