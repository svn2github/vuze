/*
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;

/** Seeding Automation Specific options
 * @author TuxPaper
 * @created Jan 12, 2004
 */
public class ConfigSectionSeeding implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return "queue";
  }

  public String configSectionGetName() {
    return "queue.seeding";
  }

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }

  public Composite configSectionCreate(Composite parent) {
    // Seeding Automation Setup
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite cSeeding = new Composite(parent, SWT.NULL);

    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    cSeeding.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cSeeding.setLayoutData(gridData);

    // General Seeding Options
    label = new Label(cSeeding, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minSeedingTime");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(cSeeding, "StartStopManager_iMinSeedingTime").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cSeeding, "Disconnect Seed", true,
                         "ConfigView.label.disconnetseed").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cSeeding, "Use Super Seeding", false,
                         "ConfigView.label.userSuperSeeding").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cSeeding, "StartStopManager_bAutoReposition",
                         "ConfigView.label.seeding.autoReposition").setLayoutData(gridData);

    return cSeeding;
  }
}

