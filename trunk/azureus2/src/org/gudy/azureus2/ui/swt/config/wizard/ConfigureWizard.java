/*
 * File    : ConfigureWizard.java
 * Created : 12 oct. 2003 16:06:44
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
 
package org.gudy.azureus2.ui.swt.config.wizard;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author Olivier
 * 
 */
public class ConfigureWizard extends Wizard {

  //Transfer settings
  int upSpeed = 1;
  int maxUpSpeed = 5;
  int maxActiveTorrents = 1;
  int maxDownloads = 1;
  int nbUploadsPerTorrent = 3;
  
  //Server / NAT Settings
  int serverMinPort = 6881;
  int serverMaxPort = 6889;
 

  public ConfigureWizard(Display display) {
    super(display,"configureWizard.title");
    WelcomePanel panel = new WelcomePanel(this,null);
    this.setFirstPanel(panel);
  }
}
