/*
 * Created on Jan 30, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.net.netstatus.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

public class 
NetStatusPluginView 
	implements UISWTViewEventListener
{
	private boolean		created = false;	

	private Composite	composite;
	private Button		start_button;
	private Button		cancel_button;
	private StyledText 	log;
	
	private boolean		test_running;
	
	public
	NetStatusPluginView(
		PluginInterface		_plugin_interface )
	{
		
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		switch( event.getType() ){

			case UISWTViewEvent.TYPE_CREATE:{
				
				if ( created ){
					
					return( false );
				}
				
				created = true;
				
				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
				
				initialise((Composite)event.getData());
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					destroy();
					
				}finally{
					
					created = false;
				}
				
				break;
			}
		}
		
		return true;
	}
	
	protected void
	initialise(
		Composite	_composite )
	{
		composite	= _composite;
		
		Composite main = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);
		
			// control
		
		Composite control = new Composite(main, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		control.setLayout(layout);

		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.horizontalSpan = 3;
		control.setLayoutData(grid_data);

				// start
		
			start_button = new Button( control, SWT.PUSH );
				
		 	Messages.setLanguageText( start_button, "ConfigView.section.start");
		 	
		 	start_button.addSelectionListener(
		 		new SelectionAdapter()
		 		{
		 			public void
		 			widgetSelected(
		 				SelectionEvent e )
		 			{
		 				start_button.setEnabled( false );
		 				
		 				cancel_button.setEnabled( true );
		 				
		 				startTest();
		 			}
		 		});
		 	
		 		// cancel
		 	
		 	cancel_button = new Button( control, SWT.PUSH );
		 	
		 	Messages.setLanguageText( cancel_button, "UpdateWindow.cancel");
		 	
		 	cancel_button.addSelectionListener(
		 		new SelectionAdapter()
		 		{
		 			public void
		 			widgetSelected(
		 				SelectionEvent e )
		 			{
		 				cancel_button.setEnabled( false );
		 						 				
		 				cancelTest();
		 			}
		 		});
		
		 	cancel_button.setEnabled( false );
		 	
			// log area
		
		log = new StyledText(main,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		grid_data = new GridData(GridData.FILL_BOTH);
		grid_data.horizontalSpan = 2;
		log.setLayoutData(grid_data);
	}
	
	protected void
	startTest()
	{
		new AEThread2( "NetStatus:start", true )
			{
				public void
				run()
				{
					startTestSupport();
				}
			}.start();
	}
	
	protected void
	cancelTest()
	{
		new AEThread2( "NetStatus:cancel", true )
			{
				public void
				run()
				{
					cancelTestSupport();
				}
			}.start();
	}
	
	protected void
	startTestSupport()
	{
		try{
			synchronized( this ){
				
				if ( test_running ){
					
					Debug.out( "Test already running!!!!" );
					
					return;
				}
				
				test_running = true;
			}
			
			println( "Test starting" );
			
			Thread.sleep(2000);
			
			println( "Test complete" );
			
		}catch( Throwable e ){
			
		}finally{
			
			try{
				Composite c = composite;
				
				if ( c != null && !c.isDisposed()){
					
					try{
						c.getDisplay().asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									if ( !start_button.isDisposed()){
										
										start_button.setEnabled( true );
									}
									
									if ( !cancel_button.isDisposed()){
										
										cancel_button.setEnabled( false );
									}
								}
							});
						
					}catch( Throwable e ){
					}
				}
			}finally{
				
				synchronized( this ){

					test_running = false;
				}
			}
		}
	}
	
	protected void
	println(
		String		str )
	{
		print( str + "\n" );
	}
	
	protected void
	print(
		final String		str )
	{
		if ( !log.isDisposed()){
			
			log.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							log.append( str );
						}
					});
		}
	}
	
	protected void
	cancelTestSupport()
	{
		println( "Cancelling test..." );
	}
	
	protected void
	destroy()
	{
		cancelTest();
		
		composite = null;
	}
}
