
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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
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
    FormData formData;
    FormLayout layout;
    Label label;

    Composite cTransfer = new Composite(parent, SWT.NULL);

    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cTransfer.setLayoutData(gridData);
    layout = new FormLayout();   
    layout.spacing = 5;
    cTransfer.setLayout(layout);

    
    IntParameter paramMaxUploadSpeed = new IntParameter(cTransfer, "Max Upload Speed KBs", 5, -1, true);    
    formData = new FormData();
    formData.top = new FormAttachment(0);
    formData.left = new FormAttachment(0);
    formData.right = new FormAttachment(0,30);
    paramMaxUploadSpeed.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed");
    formData = new FormData();
    formData.top = new FormAttachment(0,5);
    formData.left = new FormAttachment(paramMaxUploadSpeed.getControl());
    formData.right = new FormAttachment(100);
    label.setLayoutData(formData);
    
    

    IntParameter paramMaxClients = new IntParameter(cTransfer, "Max.Peer.Connections.Per.Torrent", 100);
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploadSpeed.getControl());
    formData.left = new FormAttachment(0);
    formData.right = new FormAttachment(0,30);
    paramMaxClients.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.max_peers_per_torrent");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploadSpeed.getControl(),5);
    formData.left = new FormAttachment(paramMaxClients.getControl());
    formData.right = new FormAttachment(100);
    label.setLayoutData(formData);
    
    
    IntParameter paramMaxClientsTotal = new IntParameter(cTransfer, "Max.Peer.Connections.Total", 0);
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClients.getControl());
    formData.left = new FormAttachment(0);
    formData.right = new FormAttachment(0,30);
    paramMaxClientsTotal.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.max_peers_total");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClients.getControl(),5);
    formData.left = new FormAttachment(paramMaxClientsTotal.getControl());
    formData.right = new FormAttachment(100);
    label.setLayoutData(formData);
    
    
    
    IntParameter paramMaxUploads = new IntParameter(cTransfer, "Max Uploads", 2, -1, false); 
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClients.getControl());
    formData.left = new FormAttachment(0);
    formData.right = new FormAttachment(0,30);
    paramMaxUploads.setLayoutData(formData);

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploads");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClients.getControl(),5);
    formData.left = new FormAttachment(paramMaxUploads.getControl());
    formData.right = new FormAttachment(100);
    label.setLayoutData(formData);
     
    
    BooleanParameter slowConnect = new BooleanParameter(cTransfer, "Slow Connect", true, "ConfigView.label.slowconnect");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploads.getControl(), 10);
    slowConnect.setLayoutData(formData);    
    
    BooleanParameter oldPolling = new BooleanParameter(cTransfer, "Old.Socket.Polling.Style", false, "ConfigView.label.oldpollingstyle");
    formData = new FormData();
    formData.top = new FormAttachment(slowConnect.getControl());
    oldPolling.setLayoutData(formData);
    
    BooleanParameter allowSameIP = new BooleanParameter(cTransfer, "Allow Same IP Peers", false, "ConfigView.label.allowsameip");
    formData = new FormData();
    formData.top = new FormAttachment(oldPolling.getControl());
    allowSameIP.setLayoutData(formData);
    
    BooleanParameter firstPiece = new BooleanParameter(cTransfer, "Prioritize First Piece", false, "ConfigView.label.prioritizefirstpiece");
    formData = new FormData();
    formData.top = new FormAttachment(allowSameIP.getControl());
    firstPiece.setLayoutData(formData);
    
    if(!System.getProperty("os.name").equals("Mac OS X")) {
      BooleanParameter playSound = new BooleanParameter(cTransfer, "Play Download Finished", false, "ConfigView.label.playdownloadfinished");
      formData = new FormData();
      formData.top = new FormAttachment(firstPiece.getControl());
      playSound.setLayoutData(formData);
    }
    
    return cTransfer;
  }
}
