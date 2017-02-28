/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package com.aelitis.azureus.ui.swt.devices;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;


import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceTemplate;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class 
DevicesWizard 
{
	private DeviceManagerUI		device_manager_ui;
	
	private Display display;
	
	private Shell shell;
	
	
	private Label title;
	
	
	private Font boldFont;
	private Font titleFont;
	private Font subTitleFont;
	private Font textInputFont;
	
	
	private Composite main;
	
	
	private ImageLoader imageLoader;
	
	
	public 
	DevicesWizard(
		DeviceManagerUI		dm_ui )
	{
		device_manager_ui	= dm_ui;
		
		imageLoader = ImageLoader.getInstance();
				
		shell = ShellFactory.createMainShell(SWT.TITLE | SWT.CLOSE | SWT.ICON
				| SWT.RESIZE);
		
		shell.setSize(650,400);
			
		Utils.centreWindow(shell);
		
		shell.setMinimumSize(550,400);
		
		display = shell.getDisplay();
		
		Utils.setShellIcon(shell);
		
		
		createFonts();
		
		shell.setText(MessageText.getString("wizard.device.title"));
		
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {

				imageLoader.releaseImage("wizard_header_bg");
				
				if(titleFont != null && !titleFont.isDisposed()) {
					titleFont.dispose();
				}
				
				if(textInputFont != null && !textInputFont.isDisposed()) {
					textInputFont.dispose();
				}
				
				if(boldFont != null && !boldFont.isDisposed()) {
					boldFont.dispose();
				}
				
				if(subTitleFont != null && !subTitleFont.isDisposed()) {
					subTitleFont.dispose();
				}
			}
		});
		
		Composite header = new Composite(shell, SWT.NONE);
		header.setBackgroundMode(SWT.INHERIT_DEFAULT);
		header.setBackgroundImage(imageLoader.getImage("wizard_header_bg"));
		Label topSeparator = new Label(shell,SWT.SEPARATOR |SWT.HORIZONTAL);
		main = new Composite(shell, SWT.NONE);
		Label bottomSeparator = new Label(shell,SWT.SEPARATOR |SWT.HORIZONTAL);
		Composite footer = new Composite(shell, SWT.NONE);
		
		FormLayout layout = new FormLayout();
		shell.setLayout(layout);
		
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		//data.height = 50;
		header.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(header,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		topSeparator.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(topSeparator,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(bottomSeparator,0);
		main.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(footer,0);
		bottomSeparator.setLayoutData(data);
		
		data = new FormData();
		data.bottom = new FormAttachment(100,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		footer.setLayoutData(data);
		
		populateHeader(header);
		populateFooter(footer);
		
		shell.layout();
		shell.open();
	}
	
	private void 
	populateHeader(
		Composite header) 
	{
		header.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		
		title = new Label(header, SWT.WRAP);
		
		title.setFont(titleFont);
		
		title.setText( MessageText.getString("device.wizard.header") );
		
		FillLayout layout = new FillLayout();
		
		layout.marginHeight = 10;
		
		layout.marginWidth = 10;
		
		header.setLayout(layout);
	}
	

	

	
	private void 
	createFonts() 
	{	
		FontData[] fDatas = shell.getFont().getFontData();
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setStyle(SWT.BOLD);
		}
		boldFont = new Font(display,fDatas);
		
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(org.gudy.azureus2.core3.util.Constants.isOSX) {
				fDatas[i].setHeight(12);
			} else {
				fDatas[i].setHeight(10);
			}
		}
		subTitleFont = new Font(display,fDatas);
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(org.gudy.azureus2.core3.util.Constants.isOSX) {
				fDatas[i].setHeight(17);
			} else {
				fDatas[i].setHeight(14);
			}
		}
		titleFont = new Font(display,fDatas);
		
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(org.gudy.azureus2.core3.util.Constants.isOSX) {
				fDatas[i].setHeight(14);
			} else {
				fDatas[i].setHeight(12);
			}
			fDatas[i].setStyle(SWT.NONE);
		}
		textInputFont = new Font(display,fDatas);	
	}
	
	private void 
	populateFooter(
		Composite footer) 
	{
		Button cancelButton;
		Button createButton;

		cancelButton = new Button(footer,SWT.PUSH);
		cancelButton.setText(MessageText.getString("Button.cancel"));
			
		createButton = new Button(footer,SWT.PUSH);
		createButton.setText(MessageText.getString("device.wizard.create"));
		
		
		FormLayout layout = new FormLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.spacing = 5;
		
		footer.setLayout(layout);
		FormData data;
								
		data = new FormData();
		data.right = new FormAttachment(100);
		data.width = 100;
		cancelButton.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0);
		data.width = 175;
		createButton.setLayoutData(data);
		
		
		
		createButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				
				try{
					DeviceTemplate[] templates = device_manager_ui.getDeviceManager().getDeviceTemplates( Device.DT_MEDIA_RENDERER );
					
					for ( DeviceTemplate template: templates ){
						
						if ( !template.isAuto()){
					
							Device device = template.createInstance( template.getName() + " test!" );
					
							device.requestAttention();
							
							break;
						}
					}
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		});
				
		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				shell.close();
			}
		});
	}
	

	
	public static void 
	main(
		String args[]) 
	{
		final DevicesWizard sw = new DevicesWizard( null );
				
		while( ! sw.shell.isDisposed()) {
			if(! sw.display.readAndDispatch()) {
				sw.display.sleep();
			}
		}
		
		sw.display.dispose();		
	}
}
