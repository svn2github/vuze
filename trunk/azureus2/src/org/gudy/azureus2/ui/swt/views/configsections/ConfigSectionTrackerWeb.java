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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionTrackerWeb implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return "tracker";
  }

	public String configSectionGetName() {
    return "tracker.web";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gWebTab = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gWebTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gWebTab.setLayout(layout);

    // **** web tab ****
    // row

    BooleanParameter enablePublish = 
      new BooleanParameter(gWebTab, "Tracker Publish Enable", true, 
                           "ConfigView.section.tracker.publishenable");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enablePublish.setLayoutData( gridData );

    BooleanParameter enablePublishDetails = 
      new BooleanParameter(gWebTab, "Tracker Publish Enable Details", true,
                           "ConfigView.section.tracker.publishenabledetails");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enablePublishDetails.setLayoutData( gridData );

    Control[] publish_controls = new Control[1];
    publish_controls[0] = enablePublishDetails.getControl();

    enablePublish.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( publish_controls ));

    // row

    label = new Label(gWebTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.torrentsperpage");

    final IntParameter tracker_skip = new IntParameter(gWebTab, "Tracker Skip", 0 );

    gridData = new GridData();
    gridData.widthHint = 100;
    tracker_skip.setLayoutData( gridData );

    return gWebTab;
  }
}
