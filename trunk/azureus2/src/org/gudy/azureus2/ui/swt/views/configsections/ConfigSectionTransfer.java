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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.views.ConfigView;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionTransfer implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "transfer";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite cTransfer = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cTransfer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cTransfer.setLayout(layout);

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxclients"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 30;
    new IntParameter(cTransfer, "Max Clients", 100).setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.slowconnect"); //$NON-NLS-1$
    new BooleanParameter(cTransfer, "Slow Connect", false); //$NON-NLS-1$

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploads"); //$NON-NLS-1$
    final String upLabels[] = new String[499];
    final int upValues[] = new int[upLabels.length];
    for (int i = 0; i < upLabels.length; i++) {
      upLabels[i] = " " + (i + 2); //$NON-NLS-1$
      upValues[i] = i + 2;
    }
    new IntListParameter(cTransfer, "Max Uploads", 4, upLabels, upValues); //$NON-NLS-1$

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed"); //$NON-NLS-1$
    final String upsLabels[] = new String[ConfigView.upRates.length];
    final int upsValues[] = new int[ConfigView.upRates.length];
    upsLabels[0] = MessageText.getString("ConfigView.unlimited"); //$NON-NLS-1$
    upsValues[0] = 0;
    for (int i = 1; i < ConfigView.upRates.length; i++) {
      upsLabels[i] = " " + ConfigView.upRates[i] + " KB/s"; //$NON-NLS-1$ //$NON-NLS-2$
      upsValues[i] = 1024 * ConfigView.upRates[i];
    }
    new IntListParameter(cTransfer, "Max Upload Speed", 0, upsLabels, upsValues); //$NON-NLS-1$

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.allowsameip"); //$NON-NLS-1$
    new BooleanParameter(cTransfer, "Allow Same IP Peers", false); //$NON-NLS-1$


    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.prioritizefirstpiece"); //$NON-NLS-1$
    new BooleanParameter(cTransfer, "Prioritize First Piece", false); //$NON-NLS-1$


    if(!System.getProperty("os.name").equals("Mac OS X")) {
      label = new Label(cTransfer, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.playdownloadfinished"); //$NON-NLS-1$
      new BooleanParameter(cTransfer, "Play Download Finished", false); //$NON-NLS-1$
    }
    return cTransfer;
  }
}
