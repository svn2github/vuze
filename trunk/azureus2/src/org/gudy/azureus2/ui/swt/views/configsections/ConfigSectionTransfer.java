
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
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;

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
    try {
      layout.spacing = 5;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    cTransfer.setLayout(layout);

    	// max upload speed
    
    	// store the initial d/l speed so we can do something sensible later
    
    final int[]	manual_max_download_speed = { COConfigurationManager.getIntParameter( "Max Download Speed KBs" )};
    
    final IntParameter paramMaxUploadSpeed = new IntParameter(cTransfer, "Max Upload Speed KBs", 1, -1, true);    
    formData = new FormData();
    formData.top = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(0,50);
    paramMaxUploadSpeed.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed");
    formData = new FormData();
    formData.top = new FormAttachment(0,5);
    formData.left = new FormAttachment(paramMaxUploadSpeed.getControl());
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    label.setLayoutData(formData);
    
    	// max download speed
    
    final IntParameter paramMaxDownSpeed = new IntParameter(cTransfer, "Max Download Speed KBs", 0, -1, true);    
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploadSpeed.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(0,50);
    paramMaxDownSpeed.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxdownloadspeed");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploadSpeed.getControl(),5);
    formData.left = new FormAttachment(paramMaxDownSpeed.getControl());
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    label.setLayoutData(formData);
    
    	// max upload/download limit dependencies
    
    paramMaxUploadSpeed.addChangeListener(
    	new ParameterChangeListener()
		{
    		public void
			parameterChanged(
				Parameter	p,
				boolean		internal )
			{
      			int	up_val 		= paramMaxUploadSpeed.getValue();
   				int	down_val 	= paramMaxDownSpeed.getValue();
    			   			
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
				Parameter	p,
				boolean		internal )
			{
       			int	up_val 		= paramMaxUploadSpeed.getValue();
   				int	down_val 	= paramMaxDownSpeed.getValue();
   	   		
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
        
    
    	// max uploads

    IntParameter paramMaxUploads = new IntParameter(cTransfer, "Max Uploads", 2, -1, false); 
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxDownSpeed.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(0,50);
    paramMaxUploads.setLayoutData(formData);

    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploads");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxDownSpeed.getControl(),5);
    formData.left = new FormAttachment(paramMaxUploads.getControl());
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    label.setLayoutData(formData);
    
    
    
    
    IntParameter paramMaxClients = new IntParameter(cTransfer, "Max.Peer.Connections.Per.Torrent");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploads.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(0,70);
    paramMaxClients.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.max_peers_per_torrent");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxUploads.getControl(),5);
    formData.left = new FormAttachment(paramMaxClients.getControl());
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    label.setLayoutData(formData);
    
    
    IntParameter paramMaxClientsTotal = new IntParameter(cTransfer, "Max.Peer.Connections.Total");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClients.getControl());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(0,70);
    paramMaxClientsTotal.setLayoutData(formData);
    
    label = new Label(cTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.max_peers_total");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClients.getControl(),5);
    formData.left = new FormAttachment(paramMaxClientsTotal.getControl());
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    label.setLayoutData(formData);


    BooleanParameter oldPolling = new BooleanParameter(cTransfer, "Old.Socket.Polling.Style", false, "ConfigView.label.oldpollingstyle");
    formData = new FormData();
    formData.top = new FormAttachment(paramMaxClientsTotal.getControl(), 10);
    oldPolling.setLayoutData(formData);
     
    BooleanParameter allowSameIP = new BooleanParameter(cTransfer, "Allow Same IP Peers", false, "ConfigView.label.allowsameip");
    formData = new FormData();
    formData.top = new FormAttachment(oldPolling.getControl(), 10);
    allowSameIP.setLayoutData(formData);
    
    BooleanParameter firstPiece = new BooleanParameter(cTransfer, "Prioritize First Piece", false, "ConfigView.label.prioritizefirstpiece");
    formData = new FormData();
    formData.top = new FormAttachment(allowSameIP.getControl());
    firstPiece.setLayoutData(formData);
    
    if(!Constants.isOSX) {
      BooleanParameter playSound = new BooleanParameter(cTransfer, "Play Download Finished", false, "ConfigView.label.playdownloadfinished");
      formData = new FormData();
      formData.top = new FormAttachment(firstPiece.getControl());
      playSound.setLayoutData(formData);
    }
    
    return cTransfer;
  }
}
