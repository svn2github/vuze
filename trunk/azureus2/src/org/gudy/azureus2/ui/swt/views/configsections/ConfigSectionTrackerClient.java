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
import org.eclipse.swt.widgets.Group;
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
    Composite gMainTab = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gMainTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gMainTab.setLayout(layout);

    // row
    
    Label  label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.client.connecttimeout");
    gridData = new GridData();
    gridData.widthHint = 40;
    IntParameter	connect_timeout = new IntParameter(gMainTab, "Tracker Client Connect Timeout" );
    connect_timeout.setLayoutData(gridData);
    label = new Label(gMainTab, SWT.NULL);

    // row
    
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.client.readtimeout");
    gridData = new GridData();
    gridData.widthHint = 40;
    IntParameter	read_timeout = new IntParameter(gMainTab, "Tracker Client Read Timeout" );
    read_timeout.setLayoutData(gridData);
    label = new Label(gMainTab, SWT.NULL);

    // row

    gridData = new GridData();
    gridData.horizontalSpan = 2;
  
    new BooleanParameter(gMainTab, "Tracker Key Enable Client", true,
                         "ConfigView.section.tracker.enablekey").setLayoutData(gridData);

    label = new Label(gMainTab, SWT.NULL);
    
 
//////////////////////
    
    BooleanParameter enableUDP = new BooleanParameter(gMainTab, "Server Enable UDP", true, "ConfigView.section.server.enableudp");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableUDP.setLayoutData(gridData); 
    
    label = new Label(gMainTab, SWT.NULL);
    
//////////////////////OVERRIDE GROUP ///////////////////
    
    Group overrideGroup = new Group(gMainTab,SWT.NULL);
    Messages.setLanguageText(overrideGroup,"ConfigView.group.override");
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    overrideGroup.setLayout(gridLayout);
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    overrideGroup.setLayoutData( gridData );
    
    
    StringParameter overrideip = new StringParameter(overrideGroup, "Override Ip", "");
    GridData data = new GridData();
    data.widthHint = 100;
    overrideip.setLayoutData(data);
    label = new Label(overrideGroup, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.overrideip");
    
    
    StringParameter tcpAnnounce = new StringParameter(overrideGroup, "TCP.Announce.Port", "");
    data = new GridData();
    data.widthHint = 40;
    tcpAnnounce.setLayoutData(data);
    label = new Label(overrideGroup, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.announceport");
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 2;
 
    new BooleanParameter(gMainTab, "Tracker Separate Peer IDs", false,
                         "ConfigView.section.tracker.separatepeerids").setLayoutData(gridData);
  
    label = new Label(gMainTab, SWT.WRAP);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.separatepeerids.info");


    return gMainTab;
  }
}
