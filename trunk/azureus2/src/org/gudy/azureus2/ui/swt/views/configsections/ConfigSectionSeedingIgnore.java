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
import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;

import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;

/** Config Section for items that make us ignore torrents when seeding
 * @author TuxPaper
 * @created Jan 12, 2004
 */
public class ConfigSectionSeedingIgnore implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return "queue.seeding";
  }

  public String configSectionGetName() {
    return "queue.seeding.ignore";
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

    Composite cIgnore = new Composite(parent, SWT.NULL);
    cIgnore.addControlListener(new Utils.LabelWrapControlListener());

    layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginHeight = 0;
    cIgnore.setLayout(layout);

    label = new Label(cIgnore, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ConfigView.label.autoSeedingIgnoreInfo"); //$NON-NLS-1$

    label = new Label(cIgnore, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.ignoreSeeds"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 20;
    new IntParameter(cIgnore, "StartStopManager_iIgnoreSeedCount").setLayoutData(gridData);
    label = new Label(cIgnore, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeds");

    label = new Label(cIgnore, SWT.WRAP);
    Messages.setLanguageText(label, "ConfigView.label.seeding.ignoreRatioPeers"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 20;
    new IntParameter(cIgnore, "Stop Peers Ratio").setLayoutData(gridData);
    label = new Label(cIgnore, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.peers");

    Composite cArea = new Composite(cIgnore, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 4;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    cArea.setLayout(layout);
    gridData = new GridData();
    gridData.horizontalIndent = 15;
    gridData.horizontalSpan = 3;
    cArea.setLayoutData(gridData);

    label = new Label(cArea, SWT.NULL);
    Image img = ImageRepository.getImage("subitem");
    img.setBackground(label.getBackground());
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    label.setLayoutData(gridData);
    label.setImage(img);

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.fakeFullCopySeedStart");

    gridData = new GridData();
    gridData.widthHint = 20;
    new IntParameter(cArea, "StartStopManager_iIgnoreRatioPeersSeedStart").setLayoutData(gridData);
    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeds");

    label = new Label(cIgnore, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.ignoreShareRatio");
    gridData = new GridData();
    gridData.widthHint = 30;
    new FloatParameter(cIgnore, "Stop Ratio", 0, -1, true, 1).setLayoutData(gridData);
    label = new Label(cIgnore, SWT.NULL);
    label.setText(":1");

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    new BooleanParameter(cIgnore,
                         "StartStopManager_bIgnore0Peers",
                         "ConfigView.label.seeding.ignore0Peers").setLayoutData(gridData);

    return cIgnore;
  }
}

