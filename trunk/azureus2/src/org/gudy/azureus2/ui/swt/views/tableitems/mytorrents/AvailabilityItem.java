/*
 * File    : AvailabilityItem.java
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

import org.gudy.azureus2.core3.peer.PEPeerManager;


/**
 * @author TuxPaper
 *
 */
public class AvailabilityItem extends TorrentItem {
  // If you want more decimals, just add a zero
  private static final String zeros = "0000";
  // # decimal places == numZeros - 1
  private static final int numZeros = zeros.length();
  private int iTimesBy;
  public AvailabilityItem(
    TorrentRow torrentRow,
    int position) {
    super(torrentRow, position);
    iTimesBy = 1;
    for (int i = 1; i < numZeros; i++)
      iTimesBy *= 10;
  }

  public void refresh() {
    String sText = "";
    PEPeerManager pm = torrentRow.getManager().getPeerManager();
    if (pm != null) {
      float f = pm.getMinAvailability();
      if (f != 0) {
        int numZeros = zeros.length();
        sText = String.valueOf((int)(f * iTimesBy));
        if (numZeros - sText.length() > 0)
          sText = zeros.substring(0, numZeros - sText.length()) + sText;
        sText = sText.substring(0, sText.length() - numZeros + 1) + "." + 
                sText.substring(sText.length() - numZeros + 1);
      }
    }
    setText(sText);
  }
}
