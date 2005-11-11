
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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;



public class ConfigSectionTransfer implements UISWTConfigSection {
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

    Composite cTransfer = new Composite(parent, SWT.WRAP);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cTransfer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    layout.marginHeight = 0;
    cTransfer.setLayout(layout);

    int userMode = COConfigurationManager.getIntParameter("User Mode");
    
    
    //  store the initial d/l speed so we can do something sensible later
    final int[] manual_max_download_speed = { COConfigurationManager.getIntParameter( "Max Download Speed KBs" )};
    
    //  max upload speed
    gridData = new GridData();
    gridData.widthHint = 35;
    final IntParameter paramMaxUploadSpeed = new IntParameter(cTransfer, "Max Upload Speed KBs", 1, -1, true, true);
    paramMaxUploadSpeed.setLayoutData( gridData );
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label = new Label(cTransfer, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed");


    //  max upload speed when seeding
    label = new Label(cTransfer, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalIndent = 20;
    BooleanParameter enable_seeding_rate = new BooleanParameter( cTransfer, "enable.seedingonly.upload.rate", false, "ConfigView.label.maxuploadspeedseeding" );
    enable_seeding_rate.setLayoutData( gridData );
    
    gridData = new GridData();
    gridData.widthHint = 35;
    IntParameter paramMaxUploadSpeedSeeding = new IntParameter(cTransfer, "Max Upload Speed Seeding KBs", 1, -1, true, false);   
    paramMaxUploadSpeedSeeding.setLayoutData( gridData );
    enable_seeding_rate.setAdditionalActionPerformer( new ChangeSelectionActionPerformer( paramMaxUploadSpeedSeeding.getControl() ) );
    label = new Label(cTransfer, SWT.NULL);

    if(userMode < 2) {
	    // wiki link
	    
	    Composite cWiki = new Composite(cTransfer, SWT.COLOR_GRAY);
	    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 4;
	    cWiki.setLayoutData(gridData);
	    layout = new GridLayout();
	    layout.numColumns = 4;
	    layout.marginHeight = 0;
	    cWiki.setLayout(layout);
	    
	    gridData = new GridData();
	    gridData.horizontalIndent = 6;
	    gridData.horizontalSpan = 2;
	    label = new Label(cWiki, SWT.NULL);
	    label.setLayoutData( gridData );
	    label.setText(MessageText.getString("Utils.link.visit") + ":");
	
	    
	    gridData = new GridData();
	    gridData.horizontalIndent = 10;
	    gridData.horizontalSpan = 2;
	    new LinkLabel( cWiki, gridData, "ConfigView.section.transfer.speeds.wiki", "http://azureus.aelitis.com/wiki/index.php/Good_settings" );
    }
    
    // max download speed
    gridData = new GridData();
    gridData.widthHint = 35;
    final IntParameter paramMaxDownSpeed = new IntParameter(cTransfer, "Max Download Speed KBs", 0, -1, true, true);
    paramMaxDownSpeed.setLayoutData( gridData );

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label = new Label(cTransfer, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.label.maxdownloadspeed");
    

    
    // max upload/download limit dependencies
    paramMaxUploadSpeed.addChangeListener(
        new ParameterChangeListener()
        {
            public void
            parameterChanged(
                Parameter   p,
                boolean     internal )
            {
                int up_val      = paramMaxUploadSpeed.getValue();
                int down_val    = paramMaxDownSpeed.getValue();
                            
                if ( up_val != 0 && up_val < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED ){
                    
                    if ( ( down_val==0 ) || down_val > (up_val*2) ){
                        
                        paramMaxDownSpeed.setValue( up_val*2 );
                    }
                }else{
                    
                    if ( down_val != manual_max_download_speed[0] ){
                        
                        paramMaxDownSpeed.setValue( manual_max_download_speed[0] );
                    }
                }
            }
        });
    
    paramMaxDownSpeed.addChangeListener(
        new ParameterChangeListener()
        {
            public void
            parameterChanged(
                Parameter   p,
                boolean     internal )
            {
                int up_val      = paramMaxUploadSpeed.getValue();
                int down_val    = paramMaxDownSpeed.getValue();
            
                if ( !internal ){
                    
                    manual_max_download_speed[0] = down_val;
                }
                                                        
                if ( up_val < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED ){
                    
                    if ( up_val != 0 && up_val < (down_val*2)){
                        
                        paramMaxUploadSpeed.setValue((down_val+1)/2 );
                        
                    }else if ( down_val == 0 ){
                        
                        paramMaxUploadSpeed.setValue( 0 );
                    }
                }               
            }
        });
        

    
    if( userMode > 0 ) {
    	
    // max uploads
    gridData = new GridData();
    gridData.widthHint = 35;
    IntParameter paramMaxUploads = new IntParameter(cTransfer, "Max Uploads", 2, -1, false, false);
    paramMaxUploads.setLayoutData( gridData );

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label = new Label(cTransfer, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.label.maxuploads");
    

    gridData = new GridData();
    gridData.widthHint = 35;
    IntParameter paramMaxClients = new IntParameter(cTransfer, "Max.Peer.Connections.Per.Torrent");
    paramMaxClients.setLayoutData( gridData );
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label = new Label(cTransfer, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.label.max_peers_per_torrent");
    
    
    gridData = new GridData();
    gridData.widthHint = 35;
    IntParameter paramMaxClientsTotal = new IntParameter(cTransfer, "Max.Peer.Connections.Total");
    paramMaxClientsTotal.setLayoutData( gridData );

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label = new Label(cTransfer, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.label.max_peers_total");
    
    
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    BooleanParameter allowSameIP = new BooleanParameter(cTransfer, "Allow Same IP Peers", false, "ConfigView.label.allowsameip");
    allowSameIP.setLayoutData( gridData );
    
    // prioritise 1st piece
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    BooleanParameter firstPiece = new BooleanParameter(cTransfer, "Prioritize First Piece", false, "ConfigView.label.prioritizefirstpiece");
    firstPiece.setLayoutData( gridData );
    
    /* BAD BAD BAD
		// prioritise most completed files
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    BooleanParameter mostCompletedFiles = new BooleanParameter(cTransfer, "Prioritize Most Completed Files", false, "ConfigView.label.prioritizemostcompletedfiles");
    mostCompletedFiles.setLayoutData(gridData);
    */
    
    // ignore ports
    
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label = new Label(cTransfer, SWT.NULL);
    label.setLayoutData( gridData );
    Messages.setLanguageText(label, "ConfigView.label.transfer.ignorepeerports");
    
    gridData = new GridData();
    gridData.widthHint = 125;
    StringParameter ignore_ports = new StringParameter(cTransfer, "Ignore.peer.ports","0"); 
    ignore_ports.setLayoutData( gridData );
    
    } //end usermode>0
    
    return cTransfer;
  }
}
