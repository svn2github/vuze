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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.PeerTableItem;
import org.gudy.azureus2.plugins.ui.tables.PluginPeerItem;
import org.gudy.azureus2.plugins.ui.tables.PluginPeerItemFactory;

/**
 * @author Olivier
 *
 */
public class PluginItem extends PeerItem implements PeerTableItem {

  PluginPeerItem pluginItem;
  
  public PluginItem(PeerRow peerRow, int position,PluginPeerItemFactory factory) {
    super(peerRow, position);
    this.pluginItem = factory.getInstance(this);        
  }
  
  public PEPeer getPeer() {
    return peerRow.getPeerSocket();
  }
  
  public void refresh() {
    try {
      pluginItem.refresh();
    } catch(Exception e) {
      LGLogger.log(LGLogger.ERROR,"Plugin in PeersView generated an exception : " + e );
    }
  }

}
