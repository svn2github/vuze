/*
 * Created on 01-Dec-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.update;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.*;

import com.aelitis.azureus.core.AzureusCore;
import org.gudy.azureus2.plugins.update.*;

/**
 * @author parg
 *
 */

public class 
UpdateProgressWindow 
{
  static AzureusCore	azureus_core;
  
  public static void 
  show(
 	UpdateCheckInstance[]		instances,
	Shell						shell )
 {
  	Display	display = shell.getDisplay();
  	
    final Shell window = new Shell(display,SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    Messages.setLanguageText(window,"updater.progress.window.title");
    if(! Constants.isOSX) {
      window.setImage(ImageRepository.getImage("azureus"));
    }
    FormLayout layout = new FormLayout();
    try {
      layout.spacing = 5;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    layout.marginHeight = 5;
    layout.marginWidth = 5;
    window.setLayout(layout);
    FormData formData;
    
    	// text blocked area
    
    final StyledText textBlocked = new StyledText(window,SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
       
    textBlocked.setEditable(false);
    
    Button btnOk = new Button(window,SWT.PUSH);
    Button btnReset = new Button(window,SWT.PUSH);
    
            
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(0,0);   
    formData.bottom = new FormAttachment(90,0);   
    textBlocked.setLayoutData(formData);
    
    
    // label blocked area
    
    Label	blockedInfo = new Label(window, SWT.NULL);
    Messages.setLanguageText(blockedInfo,"updater.progress.window.info");
    formData = new FormData();
    formData.top = new FormAttachment(textBlocked);    
    formData.right = new FormAttachment(btnReset);    
    formData.left = new FormAttachment(0,0);    
    blockedInfo.setLayoutData( formData );    
   
    
    	// reset button
    
    Messages.setLanguageText(btnReset,"Button.abort");
    formData = new FormData();
    formData.right = new FormAttachment(btnOk);    
    formData.bottom = new FormAttachment(100,0);    
    formData.width = 70;
    btnReset.setLayoutData(formData);
    btnReset.addListener(
    		SWT.Selection,
			new Listener() 
			{
			    public void 
				handleEvent(
					Event e) 
			    {
	
			   	}
			});
    
    	// ok button
    
    Messages.setLanguageText(btnOk,"Button.ok");
    formData = new FormData();
    formData.right = new FormAttachment(95,0);    
    formData.bottom = new FormAttachment(100,0);    
    formData.width = 70;
    btnOk.setLayoutData(formData);
    btnOk.addListener(
    		SWT.Selection,
			new Listener() 
			{
    			public void 
				handleEvent(
					Event e) 
    			{
    				window.dispose();
    			}
		    });
        
    window.setDefaultButton( btnOk );
    
    window.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
			     window.dispose();
			 }
		}
    });
    
    window.setSize(620,450);
    window.layout();
    
    Utils.centreWindow( window );
    
    window.open();    
  }
  
}
