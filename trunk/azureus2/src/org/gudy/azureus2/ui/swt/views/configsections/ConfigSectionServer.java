/*
 * File    : ConfigPanelServer.java
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
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gServer = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gServer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gServer.setLayout(layout);

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.overrideip"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 113;
    gridData.horizontalSpan = 3;
    new StringParameter(gServer, "Override Ip", "").setLayoutData(gridData); //$NON-NLS-1$ //$NON-NLS-2$

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.bindip"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 113;
    gridData.horizontalSpan = 3;
    new StringParameter(gServer, "Bind IP", "").setLayoutData(gridData); //$NON-NLS-1$ //$NON-NLS-2$

    new Label(gServer, SWT.NULL);
    new Label(gServer, SWT.NULL);
    new Label(gServer, SWT.NULL);
    new Label(gServer, SWT.NULL);
    
    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.serverport"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "TCP.Listen.Port", 6881).setLayoutData(gridData); //$NON-NLS-1$
    
    
    return gServer;
    
    // this will have to be put in a file of it's own..
    // Sub-Section: Server -> Proxy
    // ----------------------------
    /*
    Composite cProxy = createConfigSection(treeServer, "proxy");

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cProxy.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cProxy.setLayout(layout);
    
    new Label(cProxy, SWT.NULL);
    
    label = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.proxy.description");
    
    new Label(cProxy, SWT.NULL);
    new Label(cProxy, SWT.NULL);
    
    BooleanParameter enableProxy = new BooleanParameter(cProxy, "Enable.Proxy", false);
    label = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.proxy.enable_proxy");
    
    BooleanParameter enableSocks = new BooleanParameter(cProxy, "Enable.SOCKS", true);
    Label lSocks = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(lSocks, "ConfigView.section.proxy.enable_socks");
    
    new Label(cProxy, SWT.NULL);
    new Label(cProxy, SWT.NULL);
    
    Label lHost = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(lHost, "ConfigView.section.proxy.host");
    gridData = new GridData();
    gridData.widthHint = 150;
    StringParameter pHost = new StringParameter(cProxy, "Proxy.Host", "");
    pHost.setLayoutData(gridData);

    Label lPort = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(lPort, "ConfigView.section.proxy.port");
    gridData = new GridData();
    gridData.widthHint = 30;
    StringParameter pPort = new StringParameter(cProxy, "Proxy.Port", "");
    pPort.setLayoutData(gridData);
    
    new Label(cProxy, SWT.NULL);
    new Label(cProxy, SWT.NULL);
    
    Label lUser = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(lUser, "ConfigView.section.proxy.username");
    gridData = new GridData();
    gridData.widthHint = 100;
    StringParameter pUser = new StringParameter(cProxy, "Proxy.Username", "");
    pUser.setLayoutData(gridData);

    Label lPass = new Label(cProxy, SWT.NULL);
    Messages.setLanguageText(lPass, "ConfigView.section.proxy.password");
    gridData = new GridData();
    gridData.widthHint = 100;
    StringParameter pPass = new StringParameter(cProxy, "Proxy.Password", "");
    pPass.setLayoutData(gridData);
    
    Control[] controls = new Control[10];
    controls[0] = lSocks;
    controls[1] = enableSocks.getControl();
    controls[2] = lHost;
    controls[3] = pHost.getControl();
    controls[4] = lPort;
    controls[5] = pPort.getControl();
    controls[6] = lUser;
    controls[7] = pUser.getControl();
    controls[8] = lPass;
    controls[9] = pPass.getControl();
    IAdditionalActionPerformer proxyButton = new ChangeSelectionActionPerformer(controls);
    enableProxy.setAdditionalActionPerformer(proxyButton);
    */
  }
}
