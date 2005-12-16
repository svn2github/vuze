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
		return ConfigSection.SECTION_CONNECTION;
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

/////////////////////// NETWORKS GROUP ///////////////////
    
    Group networks_group = new Group( cServer, SWT.NULL );
    Messages.setLanguageText( networks_group, "ConfigView.section.connection.group.networks" );
    GridLayout networks_layout = new GridLayout();
    networks_layout.numColumns = 2;
    networks_group.setLayout( networks_layout );
    
    formData = new FormData();
    formData.left = new FormAttachment( 0, 0 );
    formData.right = new FormAttachment( 100, -5 );
    formData.top = new FormAttachment( bindip.getControl(), 5 );
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
	
    } // end userMode>1
    } // end userMode>0
    
    
 ///////////////////////   
 
    
    return cServer;

  }
}
