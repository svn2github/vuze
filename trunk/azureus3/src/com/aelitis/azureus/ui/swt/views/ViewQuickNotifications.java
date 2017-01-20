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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
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
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
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
	private Label				notification_text;
	private BufferedLabel		more_text;
		
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
		layout.marginLeft = layout.marginRight = layout.marginTop = layout.marginBottom = 0;
		
		composite.setLayout(layout);
		
			// icon
		
		notification_icon = new Label(composite,SWT.NONE);
		gridData = new GridData();
		gridData.widthHint = 20;
		Utils.setLayoutData(notification_icon, gridData);
		
			// text
		
		notification_text = new Label(composite,SWT.DOUBLE_BUFFERED);
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
		
			// text
			
		more_text = new BufferedLabel(composite,SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		Utils.setLayoutData(more_text, gridData);
		notification_text.setData( "" );

		Control[] controls = { composite, notification_icon, notification_text, more_text.getControl() };
		
		for( Control c: controls ){
			
			c.addMouseListener( listener );
			
			Messages.setLanguageTooltip( c, "label.click.to.view" );
		}	
		
		notification_text.addPaintListener(
			new PaintListener() {
				
				@Override
				public void paintControl(PaintEvent e) {
					String text = (String)notification_text.getData();
					
					int style = SWT.LEFT;

					Rectangle bounds = notification_text.getBounds();
					
					bounds.x		= 4;
					bounds.y		= 0;
					bounds.width 	-= 8;
					
					GCStringPrinter sp = new GCStringPrinter(e.gc, text, bounds, true, true, style );

					sp.calculateMetrics();
					
					sp.printString();
				}
			});
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
		Object[] 	temp = VuzeActivitiesManager.getMostRecentUnseen();
		
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry)temp[0];
		
		String old_text = (String)notification_text.getData();

		if ( entry == null ){
			
			notification_icon.setImage( null );
						
			if ( old_text.length() > 0 ){
				
				notification_text.setData( "" );
				
				notification_text.redraw();
			}
			
			more_text.setText( "" );
			
		}else{
			
			String cur_text = entry.getText();
			
			if ( !old_text.equals( cur_text )){
			
				notification_text.setData(cur_text );
				
				notification_text.redraw();
			}
			
			String icon_id = entry.getIconID();
			
			if ( icon_id != null ){
				
				String existing = (String)notification_icon.getData();
				
				if ( existing == null || notification_icon.getImage() == null || !existing.equals( icon_id )){
					
					ImageLoader imageLoader = ImageLoader.getInstance();
					
					if ( existing != null){
						
						imageLoader.releaseImage( existing );
					}
					
					Image image = imageLoader.getImage(icon_id);
					
					notification_icon.setImage( image );
					
					notification_icon.setData( icon_id );
				}
			}else{
				
				notification_icon.setImage( null );
			}
			
			int	num = (Integer)temp[1];
			
			if ( num <= 1 ){
				
				more_text.setText( "" );
				
			}else{
				
				more_text.setText(
					MessageText.getString(
						"popup.more.waiting",
						new String[]{ String.valueOf( num-1 )} ));
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
