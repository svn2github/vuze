/*
 * Created on 9 juil. 2003
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.ui.components.UIComponent;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeEvent;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeListener;
import org.gudy.azureus2.pluginsimpl.local.ui.config.UITextAreaImpl;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;


public class 
TextAreaParameter 
	extends 	Parameter
	implements	UIPropertyChangeListener
{
	private UITextAreaImpl	ui_text_area;
	
	private StyledText	text_area;
  
	public 
	TextAreaParameter(
		Composite 			composite,
		UITextAreaImpl 		_ui_text_area) 
	{
		super( "" );
  	
		ui_text_area = _ui_text_area;
  	
		text_area = new StyledText(composite,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		
		ClipboardCopy.addCopyToClipMenu(
				text_area,
				new ClipboardCopy.copyToClipProvider()
				{
					public String 
					getText() 
					{
						return( text_area.getText().trim());
					}
				});
	
		text_area.setText(ui_text_area.getText());

		ui_text_area.addPropertyChangeListener(this);		
	}
	
	public void 
	setLayoutData(
		Object layoutData ) 
	{
		if ( layoutData instanceof GridData ){

			GridData gd = (GridData)layoutData;

			Integer hhint = (Integer)ui_text_area.getProperty( UIComponent.PT_HEIGHT_HINT );

			if ( hhint != null ){

				gd.heightHint = hhint;
			}
		}

		text_area.setLayoutData(layoutData);
	}


	public Control 
	getControl() 
	{
		return( text_area );
	}

	public void 
	setValue(
		Object value) 
	{
	}
	
	public void 
	propertyChanged(
		final UIPropertyChangeEvent ev ) 
	{
		if ( text_area.isDisposed() || !ui_text_area.isVisible()){
			
			ui_text_area.removePropertyChangeListener( this );
			
			return;
		}
		
		text_area.getDisplay().asyncExec(
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					if ( text_area.isDisposed() || !ui_text_area.isVisible()){
						
						ui_text_area.removePropertyChangeListener( TextAreaParameter.this );
						
						return;
					}

					String old_value = (String)ev.getOldPropertyValue();
					String new_value = (String) ev.getNewPropertyValue();

					ScrollBar bar = text_area.getVerticalBar();

					boolean max = bar.getSelection() == bar.getMaximum() - bar.getThumb();

					int lineOffset = text_area.getLineCount() - text_area.getTopIndex();

					if ( new_value.startsWith( old_value )){

						String toAppend = new_value.substring(old_value.length());

						if ( toAppend.length() == 0 ){

							return;
						}

						StringBuffer builder = new StringBuffer(toAppend.length());

						String[] lines = toAppend.split("\n");


						for( int i=0;i<lines.length;i++){

							String line = lines[i];

							builder.append("\n");
							builder.append(line);
						}

						text_area.append(builder.toString());

					}else{

						StringBuffer builder = new StringBuffer(new_value.length());

						String[] lines = new_value.split("\n");

						for( int i=0;i<lines.length;i++){

							String line = lines[i];

							if (line != lines[0] ){

								builder.append("\n");
							}

							builder.append(line);
						}

						text_area.setText(builder.toString());
					}

					if ( max ){

						bar.setSelection(bar.getMaximum()-bar.getThumb());

						text_area.setTopIndex(text_area.getLineCount()-lineOffset);

						text_area.redraw();
					}

				}
			});
	}
}
