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
 
package org.gudy.azureus2.plugins.ui.tables.peers;

/**
 * This interface represents a table item in the Peers view.<br>
 * @author Olivier
 *
 */
public interface PluginPeerItem {
  
  /**
   * Called by the GUI whenever a refresh is needed.<br>
   * The Item should have been created by its associated factory,
   * and been initialiazed with references to a PeerTableItem.
   * This class only needs to implement the refresh method using methods
   * from PeerTableItem.
   */
  public void refresh();
}
