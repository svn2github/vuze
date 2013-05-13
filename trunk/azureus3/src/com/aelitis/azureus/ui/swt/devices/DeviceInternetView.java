/*
 * Created on Feb 2, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.plugins.net.netstatus.NetStatusPlugin;
import com.aelitis.azureus.plugins.net.netstatus.swt.NetStatusPluginTester;

public class 
DeviceInternetView 
	extends DeviceManagerUI.categoryView
{
	private DeviceManagerUI		device_manager_ui;
	
	private NetStatusPlugin		plugin;
	
	private Composite		main;

	private Button			start_button;
	private Button			cancel_button;
	private StyledText 		log;
	
	private static final int				
		selected_tests = 	NetStatusPluginTester.TEST_INBOUND | 
							NetStatusPluginTester.TEST_OUTBOUND |
							NetStatusPluginTester.TEST_NAT_PROXIES;
	
	private NetStatusPluginTester		current_test;
	
	private static final int LOG_NORMAL 	= 1;
	private static final int LOG_SUCCESS 	= 2;
	private static final int LOG_ERROR 		= 3;
	private static final int LOG_INFO 		= 4;
	
	private int	log_type	= LOG_NORMAL;

	protected
	DeviceInternetView(
		DeviceManagerUI	dm_ui,
		String			title )
	{
		super( dm_ui, Device.DT_INTERNET, title );
		
		device_manager_ui	= dm_ui;
	}
	
	public void 
	initialize(
		Composite parent )
	{  
		PluginInterface pi = device_manager_ui.getPluginInterface().getPluginManager().getPluginInterfaceByClass( NetStatusPlugin.class  );
		
		plugin = (NetStatusPlugin)pi.getPlugin();
		
		main = new Composite( parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginTop = 4;
		layout.marginBottom = 4;
		layout.marginHeight = 4;
		layout.marginWidth = 4;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);
		
		Label info_lab = new Label( main, SWT.NONE );
		
		info_lab.setText( "Test your internet connection" );
		
			// control
		
		Composite control = new Composite(main, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 4;
		layout.marginWidth = 4;
		control.setLayout(layout);

		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.horizontalSpan = 1;
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
		grid_data.horizontalSpan = 1;
		grid_data.horizontalIndent = 4;
		log.setLayoutData(grid_data);
		log.setIndent( 4 );
	}
	
	protected void
	startTest()
	{
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						startTestSupport(core);
					}
				});
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
	startTestSupport(
			AzureusCore core)
	{
		try{
			synchronized( this ){
				
				if ( current_test != null ){
					
					Debug.out( "Test already running!!!!" );
					
					return;
				}
				
				int tests = selected_tests;
				
				if ( NetworkAdmin.getSingleton().isIPV6Enabled()){
					
					tests |= NetStatusPluginTester.TEST_IPV6;
				}
				
				current_test = 
					new NetStatusPluginTester(
						plugin,
						tests,
						new NetStatusPluginTester.loggerProvider()
						{
							public void 
							log(
								String str) 
							{
								println( str );
							}
							
							public void 
							logSuccess(
								String str) 
							{
								try{
									log_type = LOG_SUCCESS;
									
									println( str );
									
								}finally{
									
									log_type = LOG_NORMAL;
								}
							}
							
							public void 
							logInfo(
								String str) 
							{
								try{
									log_type = LOG_INFO;
									
									println( str );
									
								}finally{
									
									log_type = LOG_NORMAL;
								}
							}
							
							public void 
							logFailure(
								String str) 
							{
								try{
									log_type = LOG_ERROR;
									
									println( str );
									
								}finally{
									
									log_type = LOG_NORMAL;
								}
							}
						});
			}
			
			println( "Test starting", true );
			
			current_test.run(core);
			
			println( current_test.isCancelled()?"Test Cancelled":"Test complete" );
			
		}catch( Throwable e ){
			
		}finally{
			
			try{
				Composite c = main;
				
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

					current_test.cancel();
					
					current_test = null;
				}
			}
		}
	}
	
	protected void
	println(
		String		str )
	{
		print( str + "\n", false );
	}
	
	protected void
	println(
		String		str,
		boolean		clear_first )
	{
		print( str + "\n", clear_first );
	}
	
	protected void
	print(
		final String		str,
		final boolean		clear_first )
	{		
		if ( !( log.isDisposed() || log.getDisplay().isDisposed())){
			
			final int f_log_type = log_type;
			
			log.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( log.isDisposed()){
								
								return;
							}
							
							int	start;
							
							if ( clear_first ){
							
								start	= 0;
								
								log.setText( str );
								
							}else{
							
								start = log.getText().length();
								
								log.append( str );
							}
							
							Color 	color;
							
							if ( f_log_type == LOG_NORMAL ){
								
								color = Colors.black;
								
							}else if ( f_log_type == LOG_SUCCESS ){
								
								color = Colors.green;
															
							}else if ( f_log_type == LOG_INFO ){
								
								color = Colors.blue;
								
							}else{
								
								color = Colors.red;
							}
							
							StyleRange styleRange = new StyleRange();
							styleRange.start = start;
							styleRange.length = str.length();
							styleRange.foreground = color;
							log.setStyleRange(styleRange);
							
							log.setSelection( log.getText().length());
						}
					});
		}
	}
	
	protected void
	cancelTestSupport()
	{
		synchronized( this ){
			
			if ( current_test != null ){
				
				println( "Cancelling test..." );
				
				current_test.cancel();
			}
		}
	}
	
	public Composite 
	getComposite()
	{
		return( main );
	}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	//swtView = (UISWTView)event.getData();
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
        break;
        
      case UISWTViewEvent.TYPE_FOCUSGAINED:
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        break;
    }

    return true;
  }
}
