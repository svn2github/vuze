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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionConnection implements ConfigSectionSWT {
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
     
    
    
 ///////////////////////
    
    StringParameter bindip = new StringParameter(cServer, "Bind IP", "");
    formData = new FormData();
    formData.top = new FormAttachment(tcplisten.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    bindip.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.bindip");
    formData = new FormData();
    formData.top = new FormAttachment(tcplisten.getControl(),5);
    formData.left = new FormAttachment(bindip.getControl());
    label.setLayoutData(formData);
    
 //////////////////////
    
    IntParameter max_connects = new IntParameter(cServer, "network.max.simultaneous.connect.attempts");
    formData = new FormData();
    formData.top = new FormAttachment(bindip.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 20;
    max_connects.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.connection.network.max.simultaneous.connect.attempts");
    formData = new FormData();
    formData.top = new FormAttachment(bindip.getControl(),5);
    formData.left = new FormAttachment(max_connects.getControl());
    label.setLayoutData(formData);
    
    
 //////////////////////  PROXY GROUP /////////////////
    
    Group proxy_group = new Group( cServer, SWT.NULL );
    Messages.setLanguageText( proxy_group, "ConfigView.connection.group.proxy" );
    GridLayout proxy_layout = new GridLayout();
    proxy_layout.numColumns = 2;
    proxy_group.setLayout( proxy_layout );
    
    formData = new FormData();
    formData.top = new FormAttachment( max_connects.getControl(), 5 );
    proxy_group.setLayoutData( formData );
    
    GridData grid_data;
    
    
    final BooleanParameter enableProxy = new BooleanParameter(proxy_group, "Enable.Proxy", false, "ConfigView.section.proxy.enable_proxy");
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    enableProxy.setLayoutData( grid_data );
    
    
    final BooleanParameter enableSocks = new BooleanParameter(proxy_group, "Enable.SOCKS", false, "ConfigView.section.proxy.enable_socks");
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    enableSocks.setLayoutData( grid_data );
    
    
    StringParameter pHost = new StringParameter(proxy_group, "Proxy.Host", "");
    grid_data = new GridData();
    grid_data.widthHint = 105;
    pHost.setLayoutData( grid_data );
    Label lHost = new Label( proxy_group, SWT.NULL );
    Messages.setLanguageText( lHost, "ConfigView.section.proxy.host" );

    
    StringParameter pPort = new StringParameter(proxy_group, "Proxy.Port", "");
    grid_data = new GridData();
    grid_data.widthHint = 40;
    pPort.setLayoutData( grid_data );
    Label lPort = new Label( proxy_group, SWT.NULL );
    Messages.setLanguageText( lPort, "ConfigView.section.proxy.port" );
  

    StringParameter pUser = new StringParameter(proxy_group, "Proxy.Username", "");
    grid_data = new GridData();
    grid_data.widthHint = 105;
    pUser.setLayoutData( grid_data );
    Label lUser = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lUser, "ConfigView.section.proxy.username");


    StringParameter pPass = new StringParameter(proxy_group, "Proxy.Password", "");
    grid_data = new GridData();
    grid_data.widthHint = 105;
    pPass.setLayoutData( grid_data );
    Label lPass = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lPass, "ConfigView.section.proxy.password");


    final BooleanParameter enableSocksPeer = new BooleanParameter(proxy_group, "Proxy.Data.Enable", false, "ConfigView.section.proxy.enable_socks.peer");
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    enableSocksPeer.setLayoutData( grid_data );

    
    final BooleanParameter socksPeerInform = new BooleanParameter(proxy_group, "Proxy.Data.SOCKS.inform", true, "ConfigView.section.proxy.peer.informtracker");
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    socksPeerInform.setLayoutData( grid_data );
    

    String[] socks_types = { "V4", "V4a", "V5" };
    String dropLabels[] = new String[socks_types.length];
    String dropValues[] = new String[socks_types.length];
    for (int i = 0; i < socks_types.length; i++) {
      dropLabels[i] = socks_types[i];
      dropValues[i] = socks_types[i];
    }
    final StringListParameter	socksType  = new StringListParameter(proxy_group, "Proxy.Data.SOCKS.version", "V4", dropLabels, dropValues);
    Label lSocksVersion = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lSocksVersion, "ConfigView.section.proxy.socks.version");


    final BooleanParameter sameConfig = new BooleanParameter(proxy_group, "Proxy.Data.Same", true, "ConfigView.section.proxy.peer.same");
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    sameConfig.setLayoutData( grid_data );
    

    StringParameter pDataHost = new StringParameter(proxy_group, "Proxy.Data.Host", "");
    grid_data = new GridData();
    grid_data.widthHint = 105;
    pDataHost.setLayoutData( grid_data );
    Label lDataHost = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lDataHost, "ConfigView.section.proxy.host");


    StringParameter pDataPort = new StringParameter(proxy_group, "Proxy.Data.Port", "");
    grid_data = new GridData();
    grid_data.widthHint = 40;
    pDataPort.setLayoutData( grid_data );
    Label lDataPort = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lDataPort, "ConfigView.section.proxy.port");

    
    StringParameter pDataUser = new StringParameter(proxy_group, "Proxy.Data.Username", "");
    grid_data = new GridData();
    grid_data.widthHint = 105;
    pDataUser.setLayoutData( grid_data );
    Label lDataUser = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lDataUser, "ConfigView.section.proxy.username");

    
    StringParameter pDataPass = new StringParameter(proxy_group, "Proxy.Data.Password", "");
    grid_data = new GridData();
    grid_data.widthHint = 105;
    pDataPass.setLayoutData( grid_data );
    Label lDataPass = new Label(proxy_group, SWT.NULL);
    Messages.setLanguageText(lDataPass, "ConfigView.section.proxy.password");

    
    
    final Control[] proxy_controls = new Control[]
    {	enableSocks.getControl(),
	    lHost,
	    pHost.getControl(),
	    lPort,
	    pPort.getControl(),
	    lUser,
	    pUser.getControl(),
	    lPass,
	    pPass.getControl(),
    };
    
    IAdditionalActionPerformer proxy_enabler =
        new GenericActionPerformer(new Control[]{}) {
          public void performAction()
          {
          	for (int i=0;i<proxy_controls.length;i++){
          		
          		proxy_controls[i].setEnabled( enableProxy.isSelected());
          	}
          }
        };
        
        
        final Control[] proxy_peer_controls = new Control[]
			    {	lDataHost,
				    pDataHost.getControl(),
				    lDataPort,
				    pDataPort.getControl(),
				    lDataUser,
				    pDataUser.getControl(),
				    lDataPass,
				    pDataPass.getControl()
			    };
        
        final Control[] proxy_peer_details = new Control[]
				{
        			sameConfig.getControl(),
        			socksPeerInform.getControl(),
        			socksType.getControl(),
        			lSocksVersion
				};
        
        IAdditionalActionPerformer proxy_peer_enabler =
            new GenericActionPerformer(new Control[]{}) {
              public void performAction()
              {
                for (int i=0;i<proxy_peer_controls.length;i++){
             	  
                  proxy_peer_controls[i].setEnabled( enableSocksPeer.isSelected() && !sameConfig.isSelected());
                }
                
                for (int i=0;i<proxy_peer_details.length;i++){
              		
                  proxy_peer_details[i].setEnabled( enableSocksPeer.isSelected());
                }
              }
            };
    
    enableSocks.setAdditionalActionPerformer( proxy_enabler );
    enableProxy.setAdditionalActionPerformer( proxy_enabler );
    enableSocksPeer.setAdditionalActionPerformer( proxy_peer_enabler );
    sameConfig.setAdditionalActionPerformer( proxy_peer_enabler );
     
    
 ///////////////////////   
    
    final BooleanParameter enable_advanced = new BooleanParameter( cServer, "config.connection.show_advanced", false );
    formData = new FormData();
    formData.top = new FormAttachment( proxy_group, 5 );
    enable_advanced.setLayoutData( formData );
    
    
 ///////////////////////
    
    Group advanced_group = new Group( cServer, SWT.NULL );
    Messages.setLanguageText( advanced_group, "ConfigView.connection.group.advanced" );
    GridLayout advanced_layout = new GridLayout();
    advanced_layout.numColumns = 2;
    advanced_group.setLayout( advanced_layout );
    
    formData = new FormData();
    formData.left = new FormAttachment( enable_advanced.getControl() );
    formData.top = new FormAttachment( proxy_group, 6 );
    advanced_group.setLayoutData( formData );
    
    GridData advanced_grid_data;
    
    final IntParameter mtu_size = new IntParameter( advanced_group, "network.tcp.mtu.size" );
    advanced_grid_data = new GridData();
    advanced_grid_data.widthHint = 40;
    mtu_size.setLayoutData( advanced_grid_data );
    Label lmtu = new Label(advanced_group, SWT.NULL);
    Messages.setLanguageText(lmtu, "ConfigView.section.connection.advanced.mtu");
    
    
    final IntParameter SO_SNDBUF = new IntParameter( advanced_group, "network.tcp.socket.SO_SNDBUF" );
    advanced_grid_data = new GridData();
    advanced_grid_data.widthHint = 40;
    SO_SNDBUF.setLayoutData( advanced_grid_data );
    Label lsend = new Label(advanced_group, SWT.NULL);
    Messages.setLanguageText(lsend, "ConfigView.section.connection.advanced.SO_SNDBUF");
    
    
    final IntParameter SO_RCVBUF = new IntParameter( advanced_group, "network.tcp.socket.SO_RCVBUF" );
    advanced_grid_data = new GridData();
    advanced_grid_data.widthHint = 40;
    SO_RCVBUF.setLayoutData( advanced_grid_data );
    Label lreceiv = new Label(advanced_group, SWT.NULL);
    Messages.setLanguageText(lreceiv, "ConfigView.section.connection.advanced.SO_RCVBUF");
    

    StringParameter IPTOS = new StringParameter( advanced_group, "network.tcp.socket.IPTOS" );
    grid_data = new GridData();
    grid_data.widthHint = 30;
    IPTOS.setLayoutData( grid_data );
    Label ltos = new Label(advanced_group, SWT.NULL);
    Messages.setLanguageText(ltos, "ConfigView.section.connection.advanced.IPTOS");
    

    Control[] advanced_controls = { advanced_group,
                                    mtu_size.getControl(),
                                    lmtu,
                                    SO_SNDBUF.getControl(),
                                    lsend,
                                    SO_RCVBUF.getControl(),
                                    lreceiv,
                                    IPTOS.getControl(),
                                    ltos
                                   };
    enable_advanced.setAdditionalActionPerformer( new ChangeSelectionActionPerformer( advanced_controls ) );
    

 ///////////////////////   
 
    
    return cServer;

  }
}
