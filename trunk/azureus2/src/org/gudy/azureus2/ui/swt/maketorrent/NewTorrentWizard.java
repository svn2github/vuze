/*
 * File    : Wizard.java
 * Created : 12 oct. 2003 14:30:57
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
 
package org.gudy.azureus2.ui.swt.maketorrent;

import org.eclipse.swt.widgets.Display;

/**
 * @author Olivier
 * 
 */
public class NewTorrentWizard extends org.gudy.azureus2.ui.swt.wizard.Wizard {

  //false : singleMode, true:  directory
  boolean mode;
  String singlePath = "";
  String directoryPath = "";
  String savePath = "";
  
  boolean localTracker = true;
  String trackerURL = "http://";
  
  public NewTorrentWizard(Display display) {
    super(display,"wizard.title");
    ModePanel panel = new ModePanel(this,null);
    this.setFirstPanel(panel);
  }

}
