/*
 * File    : ConfigPanelServer.java
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionServer implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "server";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    FormData formData;
    FormLayout layout;
    Label label;

    Composite cServer = new Composite(parent, SWT.NULL);

    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cServer.setLayoutData(gridData);
    layout = new FormLayout();   
    try {
      layout.spacing = 5;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    cServer.setLayout(layout);

 ///////////////////////
    
    IntParameter tcplisten = new IntParameter(cServer, "TCP.Listen.Port", 6881,false);
    formData = new FormData();
    formData.top = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 40;
    tcplisten.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.serverport");
    formData = new FormData();
    formData.top = new FormAttachment(0,5);
    formData.left = new FormAttachment(tcplisten.getControl());
    label.setLayoutData(formData);
    
//////////////////////
    
    BooleanParameter enableUDP = new BooleanParameter(cServer, "Server Enable UDP", true, "ConfigView.section.server.enableudp");
    formData = new FormData();
    formData.top = new FormAttachment( tcplisten.getControl());
    enableUDP.setLayoutData(formData);  
    
 ///////////////////////
    
    StringParameter overrideip = new StringParameter(cServer, "Override Ip", "");
    formData = new FormData();
    formData.top = new FormAttachment(enableUDP.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    overrideip.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.overrideip");
    formData = new FormData();
    formData.top = new FormAttachment(enableUDP.getControl(),5);
    formData.left = new FormAttachment(overrideip.getControl());
    label.setLayoutData(formData);
    
 //////////////////////
    
    StringParameter bindip = new StringParameter(cServer, "Bind IP", "");
    formData = new FormData();
    formData.top = new FormAttachment(overrideip.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    bindip.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.bindip");
    formData = new FormData();
    formData.top = new FormAttachment(overrideip.getControl(),5);
    formData.left = new FormAttachment(bindip.getControl());
    label.setLayoutData(formData);
    
 //////////////////////
    
    Label proxytext = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(proxytext, "ConfigView.section.proxy.description2");
    formData = new FormData();
    formData.top = new FormAttachment( bindip.getControl(), 10 );
    proxytext.setLayoutData(formData);
    
 //////////////////////
    
    BooleanParameter enableProxy = new BooleanParameter(cServer, "Enable.Proxy", false, "ConfigView.section.proxy.enable_proxy");
    formData = new FormData();
    formData.top = new FormAttachment( proxytext );
    enableProxy.setLayoutData(formData);  

 //////////////////////
    
    BooleanParameter enableSocks = new BooleanParameter(cServer, "Enable.SOCKS", false, "ConfigView.section.proxy.enable_socks");
    formData = new FormData();
    formData.top = new FormAttachment( enableProxy.getControl() );
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    enableSocks.setLayoutData(formData); 
    
 //////////////////////

    StringParameter pHost = new StringParameter(cServer, "Proxy.Host", "");
    formData = new FormData();
    formData.top = new FormAttachment(enableSocks.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    pHost.setLayoutData(formData);
    
    Label lHost = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(lHost, "ConfigView.section.proxy.host");
    formData = new FormData();
    formData.top = new FormAttachment(enableSocks.getControl(),5);
    formData.left = new FormAttachment(pHost.getControl());
    lHost.setLayoutData(formData);

 //////////////////////

    StringParameter pPort = new StringParameter(cServer, "Proxy.Port", "");
    formData = new FormData();
    formData.top = new FormAttachment(enableSocks.getControl());
    formData.left = new FormAttachment(lHost, 15);
    formData.width = 40;
    pPort.setLayoutData(formData);
    
    Label lPort = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(lPort, "ConfigView.section.proxy.port");
    formData = new FormData();
    formData.top = new FormAttachment(enableSocks.getControl(),5);
    formData.left = new FormAttachment(pPort.getControl());
    lPort.setLayoutData(formData);

 //////////////////////
    
    StringParameter pUser = new StringParameter(cServer, "Proxy.Username", "");
    formData = new FormData();
    formData.top = new FormAttachment(pPort.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    pUser.setLayoutData(formData);
    
    Label lUser = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(lUser, "ConfigView.section.proxy.username");
    formData = new FormData();
    formData.top = new FormAttachment(pPort.getControl(),5);
    formData.left = new FormAttachment(pUser.getControl());
    lUser.setLayoutData(formData);
    
 //////////////////////
    
    StringParameter pPass = new StringParameter(cServer, "Proxy.Password", "");
    formData = new FormData();
    formData.top = new FormAttachment(pUser.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    pPass.setLayoutData(formData);
    
    Label lPass = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(lPass, "ConfigView.section.proxy.password");
    formData = new FormData();
    formData.top = new FormAttachment(pUser.getControl(),5);
    formData.left = new FormAttachment(pPass.getControl());
    lPass.setLayoutData(formData);
    
 //////////////////////
    
    Control[] controls = new Control[9];
    controls[0] = enableSocks.getControl();
    controls[1] = lHost;
    controls[2] = pHost.getControl();
    controls[3] = lPort;
    controls[4] = pPort.getControl();
    controls[5] = lUser;
    controls[6] = pUser.getControl();
    controls[7] = lPass;
    controls[8] = pPass.getControl();
    IAdditionalActionPerformer proxyButton = new ChangeSelectionActionPerformer(controls);
    enableProxy.setAdditionalActionPerformer(proxyButton);
    
 //////////////////////
    

    return cServer;

  }
}
