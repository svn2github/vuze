
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;



public class ConfigSectionMode implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "mode";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    String sUserMode = "";
    


    final Composite cMode = new Composite(parent, SWT.WRAP);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cMode.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    layout.marginHeight = 0;
    cMode.setLayout(layout);
    
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    final Group gRadio = new Group(cMode, SWT.WRAP);
    gRadio.setText("User Proficiency");
    gRadio.setLayoutData(gridData);
    gRadio.setLayout(new RowLayout(SWT.HORIZONTAL));

    Button button0 = new Button (gRadio, SWT.RADIO);
    button0.setText ("Beginner");
    button0.setData("0");
    
    Button button1 = new Button (gRadio, SWT.RADIO);
    button1.setText ("Intermediate");
    button1.setData("1");
    
    Button button2 = new Button (gRadio, SWT.RADIO);
    button2.setText ("Advanced");
    button2.setData("2");
    
    if (COConfigurationManager.getIntParameter("User Mode") == 0) {
    	sUserMode = "Beginner";
    	button0.setSelection(true);
    }
    if (COConfigurationManager.getIntParameter("User Mode") == 1) {
    	sUserMode = "Intermediate";
    	button1.setSelection(true);
    }
    if (COConfigurationManager.getIntParameter("User Mode") == 2) {
    	sUserMode = "Advanced";
    	button2.setSelection(true);
    }
    
    
    /*gridData = new GridData();
    gridData.horizontalSpan = 4;
    final Label label = new Label(gExplain, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.mode.beginner");*/
    
    Composite cExplain = new Composite(cMode, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 2;
    cExplain.setLayout(layout);
    gridData = new GridData();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cExplain.setLayoutData(gridData);
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final Label label = new Label(cExplain, SWT.WRAP);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ConfigView.section.mode." + sUserMode);

    
    Listener radioGroup = new Listener () {
    	public void handleEvent (Event event) {
    		
    		Control [] children = gRadio.getChildren ();
    		
    		for (int j=0; j<children.length; j++) {
    			 Control child = children [j];
    			 if (child instanceof Button) {
    				 Button button = (Button) child;
    				 if ((button.getStyle () & SWT.RADIO) != 0) button.setSelection (false);
    			 }
    		}

		    Button button = (Button) event.widget;
		    button.setSelection (true);
		    Messages.setLanguageText(label, "ConfigView.section.mode." + button.getText());
		    COConfigurationManager.setParameter("User Mode", Integer.parseInt((String)button.getData()));
		    }
    };
    
    button0.addListener (SWT.Selection, radioGroup);
    button1.addListener (SWT.Selection, radioGroup);
    button2.addListener (SWT.Selection, radioGroup);

    return cMode;
  }
}
