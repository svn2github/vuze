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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;



public class ConfigSectionConnection implements UISWTConfigSection {
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
    GridData grid_data;
    
    int userMode = COConfigurationManager.getIntParameter("User Mode");

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
    
    final IntParameter tcplisten = new IntParameter(cServer, "TCP.Listen.Port", 1, 65535, false, false);
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
     
    tcplisten.addChangeListener( new ParameterChangeListener() {
      public void parameterChanged( Parameter p, boolean caused_internally ) {
        int val = tcplisten.getValue();
        
        if( val == 6880 || val == 6881 ) {
          tcplisten.setValue( 6881 );
        }
      }
    });
    
    if (userMode < 2) {
    // wiki link
    label = new Label(cServer, SWT.NULL);
    formData = new FormData();
    formData.top = new FormAttachment(tcplisten.getControl(), 0);  // 2 params for Pre SWT 3.0
    formData.left = new FormAttachment(0, 15);  // 2 params for Pre SWT 3.0
    label.setLayoutData(formData);
    label.setText(MessageText.getString("Utils.link.visit") + ":");
    
    final Label linkLabel = new Label(cServer, SWT.NULL);
    linkLabel.setText(MessageText.getString("ConfigView.section.connection.serverport.wiki"));
    linkLabel.setData("http://azureus.aelitis.com/wiki/index.php?title=Why_ports_like_6881_are_no_good_choice");
    linkLabel.setCursor(Cursors.handCursor);
    linkLabel.setForeground(Colors.blue);
    formData = new FormData();
    formData.top = new FormAttachment(tcplisten.getControl(), 0);  // 2 params for Pre SWT 3.0
    formData.left = new FormAttachment(label, 5);  // 2 params for Pre SWT 3.0
    linkLabel.setLayoutData(formData);
    linkLabel.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
        Program.launch((String) ((Label) arg0.widget).getData());
      }
      public void mouseDown(MouseEvent arg0) {
        Program.launch((String) ((Label) arg0.widget).getData());
      }
    });
    }
    
    if( userMode > 0 ) {
/////////////////////// PEER SOURCES GROUP ///////////////////

    	Group peer_sources_group = new Group( cServer, SWT.NULL );
    	Messages.setLanguageText( peer_sources_group, "ConfigView.section.connection.group.peersources" );
    	GridLayout peer_sources_layout = new GridLayout();
    	peer_sources_layout.numColumns = 2;
    	peer_sources_group.setLayout( peer_sources_layout );
    
    	formData = new FormData();
    	formData.top = new FormAttachment( label, 6 );
    	formData.left = new FormAttachment( 0, 0 );
    	formData.right = new FormAttachment( 100, -5 );
    	peer_sources_group.setLayoutData( formData );
        
    	label = new Label(peer_sources_group, SWT.NULL);
    	Messages.setLanguageText(label, "ConfigView.section.connection.group.peersources.info");
    	grid_data = new GridData();
    	grid_data.horizontalSpan = 2;
    	label.setLayoutData( grid_data );
    
    	for (int i=0;i<PEPeerSource.PS_SOURCES.length;i++){
		
    		String	p = PEPeerSource.PS_SOURCES[i];
	
    		String	config_name = "Peer Source Selection Default." + p;
    		String	msg_text	= "ConfigView.section.connection.peersource." + p;
		 
    		BooleanParameter peer_source = new BooleanParameter(peer_sources_group, config_name, msg_text );
				
    		grid_data = new GridData();
    		grid_data.horizontalSpan = 2;
    		peer_source.setLayoutData( grid_data );
    	}

 ///////////////////////
    
 
    
    IntParameter max_connects = new IntParameter(cServer, "network.max.simultaneous.connect.attempts", 1, 100, false, false );
    formData = new FormData();
    formData.top = new FormAttachment(peer_sources_group);
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 20;
    max_connects.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.connection.network.max.simultaneous.connect.attempts");
    formData = new FormData();
    formData.top = new FormAttachment(peer_sources_group,5);
    formData.left = new FormAttachment(max_connects.getControl());
    label.setLayoutData(formData);
    
    
 //////////////////////
    
    if (userMode > 1) {
    
    StringParameter bindip = new StringParameter(cServer, "Bind IP", "");
    formData = new FormData();
    formData.top = new FormAttachment(max_connects.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.width = 105;
    bindip.setLayoutData(formData);
    
    label = new Label(cServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.bindip");
    formData = new FormData();
    formData.top = new FormAttachment(max_connects.getControl(),5);
    formData.left = new FormAttachment(bindip.getControl());
    label.setLayoutData(formData);

 //////////////////////  PROXY GROUP /////////////////
    
    Group proxy_group = new Group( cServer, SWT.NULL );
    Messages.setLanguageText( proxy_group, "ConfigView.connection.group.proxy" );
    GridLayout proxy_layout = new GridLayout();
    proxy_layout.numColumns = 2;
    proxy_group.setLayout( proxy_layout );
    
    formData = new FormData();
    formData.left = new FormAttachment( 0, 0 );
    formData.right = new FormAttachment( 100, -5 );
    formData.top = new FormAttachment( bindip.getControl(), 5 );
    proxy_group.setLayoutData( formData );
    
    
    
    
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
     
/////////////////////// NETWORKS GROUP ///////////////////
    
    Group networks_group = new Group( cServer, SWT.NULL );
    Messages.setLanguageText( networks_group, "ConfigView.section.connection.group.networks" );
    GridLayout networks_layout = new GridLayout();
    networks_layout.numColumns = 2;
    networks_group.setLayout( networks_layout );
    
    formData = new FormData();
    formData.left = new FormAttachment( 0, 0 );
    formData.right = new FormAttachment( 100, -5 );
    formData.top = new FormAttachment( proxy_group, 6 );
    networks_group.setLayoutData( formData );
        
    label = new Label(networks_group, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.connection.group.networks.info");
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    label.setLayoutData( grid_data );
    
    for (int i=0;i<AENetworkClassifier.AT_NETWORKS.length;i++){
		
		String	nn = AENetworkClassifier.AT_NETWORKS[i];
	
		String	config_name = "Network Selection Default." + nn;
		String	msg_text	= "ConfigView.section.connection.networks." + nn;
		 
		BooleanParameter network = new BooleanParameter(networks_group, config_name, msg_text );
				
	    grid_data = new GridData();
	    grid_data.horizontalSpan = 2;
	    network.setLayoutData( grid_data );
	}
    
    label = new Label(networks_group, SWT.NULL);
    grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    label.setLayoutData( grid_data );
    
	BooleanParameter network_prompt = new BooleanParameter(networks_group, "Network Selection Prompt", "ConfigView.section.connection.networks.prompt" );
	
	grid_data = new GridData();
	grid_data.horizontalSpan = 2;
	network_prompt.setLayoutData( grid_data );
	

    
	
 ///////////////////////   ADVANCED NETWORK SETTINGS GROUP //////////
    
    final BooleanParameter enable_advanced = new BooleanParameter( cServer, "config.connection.show_advanced", false );
    formData = new FormData();
    formData.top = new FormAttachment( networks_group, 5 );
    enable_advanced.setLayoutData( formData );
    
    
 ///////////////////////
    
    Group advanced_group = new Group( cServer, SWT.NULL );
    Messages.setLanguageText( advanced_group, "ConfigView.connection.group.advanced" );
    GridLayout advanced_layout = new GridLayout();
    advanced_layout.numColumns = 2;
    advanced_group.setLayout( advanced_layout );
    
    formData = new FormData();
    formData.left = new FormAttachment( enable_advanced.getControl() );
    formData.right = new FormAttachment( 100, -5 );
    formData.top = new FormAttachment( networks_group, 6 );
    advanced_group.setLayoutData( formData );
    
    GridData advanced_grid_data;
    
    final IntParameter mtu_size = new IntParameter( advanced_group, "network.tcp.mtu.size" );
    mtu_size.setMaximumValue(512*1024);
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
    

    final StringParameter IPTOS = new StringParameter( advanced_group, "network.tcp.socket.IPTOS" );
    grid_data = new GridData();
    grid_data.widthHint = 30;
    IPTOS.setLayoutData( grid_data );
    Label ltos = new Label(advanced_group, SWT.NULL);
    Messages.setLanguageText(ltos, "ConfigView.section.connection.advanced.IPTOS");
    
    //do simple input verification, and registry key setting for TOS field
    IPTOS.addChangeListener( new ParameterChangeListener() {
      
      final Color obg = IPTOS.getControl().getBackground();
      final Color ofg = IPTOS.getControl().getForeground();
      
      public void parameterChanged( Parameter p, boolean caused_internally ) {
        String raw = IPTOS.getValue();
        int value = -1;
        
        try {
          value = Integer.decode( raw ).intValue();
        }
        catch( Throwable t ){}
        
        if( value < 0 || value > 255 ) {  //invalid or no value entered
          ConfigurationManager.getInstance().removeParameter( "network.tcp.socket.IPTOS" );

          if( raw != null && raw.length() > 0 ) {  //error state
            IPTOS.getControl().setBackground( Colors.red );
            IPTOS.getControl().setForeground( Colors.white );
          }
          else {  //no value state
            IPTOS.getControl().setBackground( obg );
            IPTOS.getControl().setForeground( ofg );
          }
          
          enableTOSRegistrySetting( false );  //disable registry setting if necessary
        }
        else { //passes test
          IPTOS.getControl().setBackground( obg );
          IPTOS.getControl().setForeground( ofg );
          
          enableTOSRegistrySetting( true );  //enable registry setting if necessary
        }
      }
    });
    
    
    
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
    enable_advanced.setAdditionalActionPerformer( new IAdditionalActionPerformer() {
      boolean checked;
      public void performAction() {
        if( !checked ) {  //revert all advanced options back to defaults
          ConfigurationManager.getInstance().removeParameter( "network.tcp.mtu.size" );
          ConfigurationManager.getInstance().removeParameter( "network.tcp.socket.SO_SNDBUF" );
          ConfigurationManager.getInstance().removeParameter( "network.tcp.socket.SO_RCVBUF" );
          ConfigurationManager.getInstance().removeParameter( "network.tcp.socket.IPTOS" );
        }
      }
      public void setSelected(boolean selected) {  checked = selected;  }
      public void setIntValue(int value) { }
      public void setStringValue(String value) {}
    });
    
    } // end userMode>1
    } // end userMode>0
    
    
 ///////////////////////   
 
    
    return cServer;

  }
  
  
  
  
  
  private void enableTOSRegistrySetting( boolean enable ) {
    PlatformManager mgr = PlatformManagerFactory.getPlatformManager();
    
    if( mgr.hasCapability( PlatformManagerCapabilities.SetTCPTOSEnabled ) ){  //see http://azureus.aelitis.com/wiki/index.php/AdvancedNetworkSettings
      try{
        mgr.setTCPTOSEnabled( enable );
      }
      catch( PlatformManagerException pe ) {  
        Debug.printStackTrace( pe );
      }
    }
  }
  
}
