/*
 * File    : NameItem.java
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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.core3.download.DownloadManager;


/**
 * @author Olivier
 *
 */
public class DownSpeedItem extends TorrentItem implements ParameterListener {
  private int iMinActiveSpeed;
  private int iLastState;
  
  
  public DownSpeedItem(
    TorrentRow torrentRow,
    int position) {
    super(torrentRow, position);

    iMinActiveSpeed = COConfigurationManager.getIntParameter("StartStopManager_iMinSpeedForActiveDL");
    COConfigurationManager.addParameterListener("StartStopManager_iMinSpeedForActiveDL", this);
  }

  public void refresh() {
    long iDLAverage = torrentRow.getManager().getStats().getDownloadAverage();
    int iState = torrentRow.getManager().getState();
    if (setText(DisplayFormatters.formatByteCountToKiBEtcPerSec(iDLAverage)) || 
        (iState != iLastState)) {
      changeColor(iDLAverage, iState);
    }
  }
  
  private void changeColor() {
    try {
      changeColor(torrentRow.getManager().getStats().getDownloadAverage(), torrentRow.getManager().getState());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void changeColor(long iDLAverage, int iState) {
    try {
      setItemForeground((iDLAverage < iMinActiveSpeed && 
                         iState == DownloadManager.STATE_DOWNLOADING) ? MainWindow.colorWarning : null);
      iLastState = iState;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void parameterChanged(String parameterName) {
    iMinActiveSpeed = COConfigurationManager.getIntParameter("StartStopManager_iMinSpeedForActiveDL");
    changeColor();
  }

  public void dispose() {
    COConfigurationManager.removeParameterListener("StartStopManager_iMinSpeedForActiveDL", this);
  }
}
