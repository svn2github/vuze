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
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;

public class ConfigSectionSharing implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "sharing";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;

    Composite gSharing = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gSharing.setLayoutData(gridData);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    gSharing.setLayout(layout);

    	// row
    
    new BooleanParameter(gSharing, "Sharing Use SSL", false, 
                         "ConfigView.section.sharing.usessl");
    
    new Label(gSharing, SWT.NULL);

    	// row
    
    new BooleanParameter(gSharing, "Sharing Add Hashes", true, 
                         "wizard.createtorrent.extrahashes");
    
    new Label(gSharing, SWT.NULL);

    	// row
    BooleanParameter rescan_enable = 
    	new BooleanParameter(gSharing, "Sharing Rescan Enable", false, 
    						"ConfigView.section.sharing.rescanenable");
    
    new Label(gSharing, SWT.NULL);

    	//row
    
    Label period_label = new Label(gSharing, SWT.NULL );
    Messages.setLanguageText(period_label, "ConfigView.section.sharing.rescanperiod");

    IntParameter rescan_period = new IntParameter(gSharing, "Sharing Rescan Period", 60 );
    rescan_period.setMinimumValue(1);
    gridData = new GridData();
    gridData.widthHint = 30;
    rescan_period.setLayoutData( gridData );
    
    rescan_enable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( rescan_period.getControls() ));
    rescan_enable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( new Control[]{period_label} ));
	
    return gSharing;
       
  }
}
