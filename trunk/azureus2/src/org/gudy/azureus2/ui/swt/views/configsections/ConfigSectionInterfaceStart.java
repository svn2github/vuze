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

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionInterfaceStart implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_INTERFACE;
  }

	public String configSectionGetName() {
		return "start";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    // "Start" Sub-Section
    // -------------------
    Label label;
    GridLayout layout;
    Composite cStart = new Composite(parent, SWT.NULL);

    cStart.setLayoutData(new GridData(GridData.FILL_BOTH));
    layout = new GridLayout();
    layout.numColumns = 2;
    cStart.setLayout(layout);

    label = new Label(cStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.showsplash"); //$NON-NLS-1$
    new BooleanParameter(cStart, "Show Splash", true); //$NON-NLS-1$

    label = new Label(cStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.autoupdate"); //$NON-NLS-1$
    new BooleanParameter(cStart, "Auto Update", true); //$NON-NLS-1$

    label = new Label(cStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.openconsole"); //$NON-NLS-1$
    new BooleanParameter(cStart, "Open Console", false); //$NON-NLS-1$

    label = new Label(cStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.openconfig"); //$NON-NLS-1$
    new BooleanParameter(cStart, "Open Config", false); //$NON-NLS-1$

    label = new Label(cStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.startminimized"); //$NON-NLS-1$
    new BooleanParameter(cStart, "Start Minimized", false); //$NON-NLS-1$
    
    return cStart;
  }
}
