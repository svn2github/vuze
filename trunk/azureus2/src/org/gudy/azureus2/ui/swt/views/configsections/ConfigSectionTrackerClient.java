/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.gudy.azureus2.ui.swt.Messages;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;

public class 
ConfigSectionTrackerClient 
	implements ConfigSectionSWT 
{
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_TRACKER;
  }

	public String configSectionGetName() {
		return "tracker.client";
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


 
    // row

    new BooleanParameter(gExtTab, "Tracker Key Enable Client", true,
                         "ConfigView.section.tracker.enablekey");

    Label label = new Label(gExtTab, SWT.NULL);
    
    // row

    new BooleanParameter(gExtTab, "Tracker Separate Peer IDs", false,
                         "ConfigView.section.tracker.separatepeerids");
  
    label = new Label(gExtTab, SWT.WRAP);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.separatepeerids.info");

    return gExtTab;
  }
}
