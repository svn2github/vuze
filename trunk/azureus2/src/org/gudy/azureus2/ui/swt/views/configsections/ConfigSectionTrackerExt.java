/*
 * File    : ConfigPanel*.java
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.gudy.azureus2.ui.swt.Messages;

import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;

public class ConfigSectionTrackerExt implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return "tracker";
  }

	public String configSectionGetName() {
		return "tracker.extensions";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;

    // extensions tab set up
    Composite gExtTab = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gExtTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gExtTab.setLayout(layout);


    // **** extensions tab ****
    // row
    new BooleanParameter(gExtTab, "Tracker Send Peer IDs", true, 
                         "ConfigView.section.tracker.sendpeerids");

    Label label = new Label(gExtTab, SWT.NULL);
    
    // row
    new BooleanParameter(gExtTab, "Tracker Port UDP Enable", false, 
                         "ConfigView.section.tracker.enableudp");

    label = new Label(gExtTab, SWT.NULL);
    // row
    
    label = new Label(gExtTab, SWT.NULL);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.udpversion");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gExtTab, "Tracker Port UDP Version", 2).setLayoutData(gridData);

    // row
    new BooleanParameter(gExtTab, "Tracker Compact Enable", true,
                         "ConfigView.section.tracker.enablecompact");

    label = new Label(gExtTab, SWT.NULL);
    // row

    new BooleanParameter(gExtTab, "Tracker Key Enable", true,
                         "ConfigView.section.tracker.enablekey");

    label = new Label(gExtTab, SWT.NULL);
    
    return gExtTab;
  }
}
