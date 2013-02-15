/**
 * Created on Mar 7, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.devices;


import java.net.URLEncoder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.core.devices.DeviceManagerException;
import com.aelitis.azureus.core.devices.DeviceOfflineDownloader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.util.ConstantsVuze;

/**
 * @author TuxPaper
 * @created Mar 7, 2009
 *
 */
public class 
DevicesODFTUX
{
	private static final String URL_LEARN_MORE = "/devices/offlinedownloader.start";

	private DeviceOfflineDownloader		device;
	
	private Display display;

	private Shell shell;
		
	private Font boldFont;
	private Font titleFont;
	private Font subTitleFont;
	private Font textInputFont;
	
	private Button	turnOnButton;
	private Label	noSpaceWarning;
	
	private String		dev_image_key;
	private ImageLoader imageLoader;

	protected
	DevicesODFTUX(
		DeviceOfflineDownloader		_device )
	
		throws DeviceManagerException
	{
		device	= _device;
		
		final long avail = device.getSpaceAvailable( false );
			
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void
				runSupport()
				{
					open( avail == 0 );
				}
			});
	}


	private void 
	open(
		boolean		no_space_available )
	{
		imageLoader = ImageLoader.getInstance();

		shell = ShellFactory.createMainShell(SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
		
		shell.setSize(650,400);
		
		Utils.centreWindow(shell);
		
		shell.setMinimumSize(550,400);
		
		display = shell.getDisplay();
		
		Utils.setShellIcon(shell);
		
		createFonts();
		
		shell.setText(MessageText.getString("devices.activation"));
		
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {

				imageLoader.releaseImage("wizard_header_bg");
				
				if ( dev_image_key != null ){
					
					imageLoader.releaseImage( dev_image_key );
				}
				
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
		
		Composite main = new Composite(shell, SWT.NONE);
		
		main.setBackground( Colors.white );
		
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
		
		populateMain( main, no_space_available );
		
		populateFooter( footer, no_space_available );
			
		shell.setDefaultButton(turnOnButton);

		shell.layout();
		
		Utils.centreWindow(shell);
		
		turnOnButton.setFocus();
		
		shell.open();
	}

	private void 
	populateHeader(
		Composite header) 
	{
		header.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		
		Label title = new Label(header, SWT.WRAP);
		
		title.setFont(titleFont);
		
		title.setText( MessageText.getString("devices.turnon.title") );
		
		FillLayout layout = new FillLayout();
		
		layout.marginHeight = 10;
		
		layout.marginWidth = 10;
		
		header.setLayout(layout); 
	}
	

	private void 
	populateMain(
		Composite 	main,
		boolean		no_space_available )
	{
		String manufacturer = device.getManufacturer();
		
		boolean	is_belkin = manufacturer.toLowerCase().contains( "belkin");
				
		Label image_area = new Label(main, SWT.NONE);
		
		String	router_text;
		
		if ( is_belkin ){
			
			dev_image_key = "image.device.logo.belkin";
			
			router_text = MessageText.getString( "devices.router" );
			
		}else{
			router_text = MessageText.getString( "devices.od" );
		}
		
		if ( dev_image_key != null ){
			image_area.setImage(imageLoader.getImage( dev_image_key ));
		}
		
		Label text1 = new Label(main, SWT.WRAP);
		text1.setBackground( Colors.white );
		text1.setFont( textInputFont );
		text1.setText( MessageText.getString("devices.od.turnon.text1", new String[]{ (is_belkin?"Belkin":"Vuze" ) + " " + router_text }));

		Label text2 = new Label(main, SWT.WRAP);
		text2.setBackground( Colors.white );
		text2.setFont( textInputFont );
		text2.setText( MessageText.getString("devices.od.turnon.text2", new String[]{ router_text }));
		
		noSpaceWarning = new Label(main, SWT.WRAP);
		noSpaceWarning.setBackground( Colors.white );
		noSpaceWarning.setFont( textInputFont );
		noSpaceWarning.setText( MessageText.getString("devices.od.turnon.text3", new String[]{ router_text }));
		noSpaceWarning.setForeground( Colors.red );
		noSpaceWarning.setVisible( no_space_available );

		Label link = new Label(main, SWT.WRAP);
		link.setBackground( Colors.white );
		link.setFont( textInputFont );
		link.setText( MessageText.getString("devices.od.turnon.learn") );
		
		String url = URL_LEARN_MORE;
		
		try{
			url += "?man=" + URLEncoder.encode( manufacturer, "UTF-8" );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}

		url = ConstantsVuze.getDefaultContentNetwork().getExternalSiteRelativeURL(url, true);

		LinkLabel.makeLinkedLabel( link, url );
		
		
		FormLayout layout = new FormLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 50;
		layout.spacing = 5;
		
		main.setLayout(layout);
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0, 20);
		data.left = new FormAttachment(0);
		image_area.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(image_area,10);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		text1.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(text1, 10 );
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		text2.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(text2, 10 );
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		noSpaceWarning.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(noSpaceWarning, 10 );
		data.left = new FormAttachment(0);
		link.setLayoutData(data);
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
		Composite 				footer,
		final boolean			no_space_available )
	{
		final Button	dont_ask_again = new Button( footer, SWT.CHECK );
		dont_ask_again.setText(MessageText.getString("general.dont.ask.again"));
		dont_ask_again.setSelection( true );
		
		Button cancelButton = new Button(footer,SWT.PUSH);
		cancelButton.setText(MessageText.getString("button.nothanks"));
			
		turnOnButton = new Button(footer,SWT.PUSH);
		turnOnButton.setText(MessageText.getString("Button.turnon"));
		
		
		FormLayout layout = new FormLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.spacing = 5;
		
		footer.setLayout(layout);
		FormData data;
			
		data = new FormData();
		data.left = new FormAttachment(0,45);
		data.right = new FormAttachment(turnOnButton);
		dont_ask_again.setLayoutData(data);

		data = new FormData();
		data.right = new FormAttachment(100);
		data.width = 100;
		cancelButton.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment( cancelButton );
		data.width = 100;
		turnOnButton.setLayoutData(data);
		
		
		
		turnOnButton.addListener(
			SWT.Selection, 
			new Listener() 
			{	
				public void 
				handleEvent(
					Event arg0 ) 
				{
				
					device.setEnabled( true );
					
					device.setShownFTUX();
					
					shell.close();
				}
			});
			
		turnOnButton.setEnabled( !no_space_available );
					
		new AEThread2( "scanner", true )
		{
			private long	last_avail = no_space_available?0:Long.MAX_VALUE;
			
			public void
			run()
			{
				while( !shell.isDisposed()){
					
					try{
						Thread.sleep(10*1000);
						
						final long avail = device.getSpaceAvailable( true );
						
						if ( avail != last_avail ){
							
							last_avail = avail;
							
							Utils.execSWTThread(
								new AERunnable() 
								{
									public void
									runSupport()
									{
										if ( !turnOnButton.isDisposed()){
											
											turnOnButton.setEnabled( avail > 0  );
										}
										
										if ( !noSpaceWarning.isDisposed()){
											
											noSpaceWarning.setVisible( avail <= 0 );
										}
									}
								});
						}
					}catch( Throwable e ){					
					}
				}
			}
		}.start();
		
		cancelButton.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(
					Event arg0 )
				{
					device.setEnabled( false );
					
					if ( dont_ask_again.getSelection()){
						
						device.setShownFTUX();
					}
					
					shell.close();
				}
			});
	}

	protected void close() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (shell != null && !shell.isDisposed()) {
					shell.dispose();
				}
			}
		});
	}
}
