/*
 * File    : ConfigSection*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
//import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionTMP implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

  /* Name of section will be pulled from 
   * ConfigView.section.<i>configSectionGetName()</i>
   */
	public String configSectionGetName() {
		return "newsectionname";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;

    Composite cSection = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cSection.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cSection.setLayout(layout);

    // Place SWT config items here

    return cSection;
  }
}
