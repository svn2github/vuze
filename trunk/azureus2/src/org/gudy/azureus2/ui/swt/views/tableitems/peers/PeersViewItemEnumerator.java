/*
 * File    : PeersViewItemEnumerator.java
 * Created : 29 nov. 2003
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.pluginsimpl.ui.tables.peers.PeersTableExtensions;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ConfigBasedItemEnumerator;


public class PeersViewItemEnumerator {
  
  private static final String[] basicItems = {
    "ip;L;S;100;0"
   ,"port;L;I;40;-1"
   ,"T;L;I;20;1"
   ,"I1;C;I;20;2"
   ,"C1;C;I;20;3"
   ,"pieces;C;I;100;4"
   ,"%;R;I;55;5"
   ,"downloadspeed;R;I;65;6"
   ,"download;R;I;70;7"
   ,"I2;C;I;20;8"
   ,"C2;C;I;20;9"
   ,"optunchoke;C;I;20;10"
   ,"uploadspeed;R;I;65;11"
   ,"upload;R;I;70;12"
   ,"statup;R;I;65;-1"
   ,"S;C;I;20;13"
   ,"downloadspeedoverall;R;I;65;14"    
   ,"client;L;S;105;15"
   ,"discarded;R;I;60;16"
   ,"uniquepiece;L;I;60;-1"};
  
  public static ConfigBasedItemEnumerator getItemEnumerator() {
    List items = new ArrayList(basicItems.length);
    for(int i=0 ; i < basicItems.length ; i++) {
      items.add(basicItems[i]);
    }
    Map extensions = PeersTableExtensions.getInstance().getExtensions();
    Iterator iter = extensions.keySet().iterator();
    while(iter.hasNext()) {
      String name = (String) iter.next();
      PluginPeerItemFactory ppif = (PluginPeerItemFactory) extensions.get(name);
      items.add(ppif.getName() + ";L;" + ppif.getType() + ";" + ppif.getDefaultSize() + ";-1");
    }
    return ConfigBasedItemEnumerator.getInstance("Peers",(String[])items.toArray(new String[items.size()]));
  }
}
