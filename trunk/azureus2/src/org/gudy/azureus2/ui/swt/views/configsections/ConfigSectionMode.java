
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
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;



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
    String initsMode = "";
    final String[] text = {""};
    
    int userMode = COConfigurationManager.getIntParameter("User Mode");

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
    Messages.setLanguageText(gRadio, "ConfigView.section.mode.title");
    gRadio.setLayoutData(gridData);
    gRadio.setLayout(new RowLayout(SWT.HORIZONTAL));

    Button button0 = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(button0, "ConfigView.section.mode.Beginner");
    button0.setData("iMode", "0");
    button0.setData("sMode", "Beginner.text");
    
    Button button1 = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(button1, "ConfigView.section.mode.Intermediate");
    button1.setData("iMode", "1");
    button1.setData("sMode", "Intermediate.text");
    
    Button button2 = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(button2, "ConfigView.section.mode.Advanced");
    button2.setData("iMode", "2");
    button2.setData("sMode", "Advanced.text");
    
    if ( userMode == 0) {
    	initsMode = "Beginner.text";
    	button0.setSelection(true);
    } else if ( userMode == 1) {
    	initsMode = "Intermediate.text";
    	button1.setSelection(true);
    } else {
    	initsMode = "Advanced.text";
    	button2.setSelection(true);
    }
    
    Composite cExplain = new Composite(cMode, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 2;
    cExplain.setLayout(layout);
    gridData = new GridData();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cExplain.setLayoutData(gridData);
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final Link link = new Link(cExplain, SWT.WRAP);
    gridData.horizontalSpan = 2;
    link.setLayoutData(gridData);
	text[0] = MessageText.getString("ConfigView.section.mode." + initsMode);
	link.setText(text[0]);
	link.addListener (SWT.Selection, new Listener () {
		public void handleEvent(Event event) {
			Program.launch(event.text);
		}
	});
    
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
		    text[0] = MessageText.getString("ConfigView.section.mode." + (String)button.getData("sMode"));
			link.setText(text[0]);
		    COConfigurationManager.setParameter("User Mode", Integer.parseInt((String)button.getData("iMode")));
		    }
    };
    
    button0.addListener (SWT.Selection, radioGroup);
    button1.addListener (SWT.Selection, radioGroup);
    button2.addListener (SWT.Selection, radioGroup);

    return cMode;
  }
}
