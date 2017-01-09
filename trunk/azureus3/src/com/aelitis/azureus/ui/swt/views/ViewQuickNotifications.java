/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.azureus.ui.swt.views;

import java.net.InetAddress;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class ViewQuickNotifications
	implements UISWTViewCoreEventListener
{
	private UISWTView swtView;
	
	private Composite			composite;
	private Label				notification_icon;
	private BufferedLabel		notification_text;
		
	public 
	ViewQuickNotifications() 
	{
	}


	private void 
	initialize(
		Composite parent) 
	{
		parent.setLayout( new GridLayout());
		
		composite = new Composite( parent, SWT.BORDER );
		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		
		Utils.setLayoutData(composite, gridData);
		
		GridLayout layout = new GridLayout(2, false);
		
		composite.setLayout(layout);
		
			// icon
		
		notification_icon = new Label(composite,SWT.NONE);
		gridData = new GridData();
		gridData.widthHint = 20;
		Utils.setLayoutData(notification_icon, gridData);
		
			// text
		
		notification_text = new BufferedLabel(composite,SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		Utils.setLayoutData(notification_text, gridData);

		MouseAdapter	listener = 
			new MouseAdapter()
			{
				@Override
				public void mouseDown(MouseEvent e) {
					UIFunctions uif = UIFunctionsManager.getUIFunctions();

					if ( uif != null ){

						uif.getMDI().showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES );
					}
				}
			};
			
		notification_icon.addMouseListener( listener );
		notification_text.addMouseListener( listener );
	}

	private void 
	delete() 
	{
		Utils.disposeComposite(composite);
	}

	private String 
	getFullTitle() 
	{
		return( MessageText.getString( "label.quick.notifications" ));
	}
	
	private Composite 
	getComposite() 
	{
		return composite;
	}

	private void refresh()
	{
		VuzeActivitiesEntry 	entry = VuzeActivitiesManager.getMostRecentUnseen();
		
		if ( entry == null ){
			
			notification_icon.setImage( null );
			
			notification_text.setText( "" );
			
		}else{
			
			notification_text.setText( entry.getText());
			
			String icon_id = entry.getIconID();
			
			if ( icon_id != null ){
				
				String existing = (String)notification_icon.getData();
				
				if ( existing == null || notification_icon.getImage() == null || !existing.equals( icon_id )){
					
					ImageLoader imageLoader = ImageLoader.getInstance();
					
					Image image = imageLoader.getImage(icon_id);
					
					notification_icon.setImage( image );
					
					notification_icon.setData( icon_id );
				}
			}else{
				
				notification_icon.setImage( null );
			}
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = event.getView();
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
      case UISWTViewEvent.TYPE_FOCUSGAINED:{
    	  composite.traverse( SWT.TRAVERSE_TAB_NEXT);
    	  break;
      }
    }

    return true;
  }
}
