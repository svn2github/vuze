
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
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
    Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed");
    gridData = new GridData();
    gridData.widthHint = 30;
    IntParameter paramMaxUploadSpeed = new IntParameter(cTransfer, "Max Upload Speed KBs", 5, -1, true);
    paramMaxUploadSpeed.setLayoutData(gridData);

    
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxclients");
    gridData = new GridData();
    gridData.widthHint = 30;
    new IntParameter(cTransfer, "Max Clients", 100).setLayoutData(gridData);

    

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploads");
    gridData = new GridData();
    gridData.widthHint = 30;
    IntParameter paramMaxUploads = new IntParameter(cTransfer, "Max Uploads", 2, -1, false); 
    paramMaxUploads.setLayoutData(gridData);

    
    Composite cTransfer2 = new Composite(cTransfer, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 1;
    cTransfer2.setLayout(layout);
    cTransfer2.setLayoutData(new GridData());
    
    new BooleanParameter(cTransfer2, "Slow Connect", false, "ConfigView.label.slowconnect");
    new BooleanParameter(cTransfer2, "Old.Socket.Polling.Style", false, "ConfigView.label.oldpollingstyle");
    new BooleanParameter(cTransfer2, "Allow Same IP Peers", false, "ConfigView.label.allowsameip");
    new BooleanParameter(cTransfer2, "Prioritize First Piece", false, "ConfigView.label.prioritizefirstpiece");
    
    if(!System.getProperty("os.name").equals("Mac OS X")) {
      new BooleanParameter(cTransfer2, "Play Download Finished", false, "ConfigView.label.playdownloadfinished");
    }
    
    return cTransfer;
  }
}
