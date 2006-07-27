/*
 * Created on 27 Jul 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.progress;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationListener;

public class 
ProgressWindow 
{
	public static void
	register(
		AzureusCore		core )
	{
		core.addOperationListener(
			new AzureusCoreOperationListener()
			{
				public boolean
				operationCreated(
					AzureusCoreOperation	operation )
				{
					if ( 	operation.getOperationType() == AzureusCoreOperation.OP_FILE_MOVE &&
							Utils.isThisThreadSWT()){
												
						if ( operation.getTask() != null ){
							
							new ProgressWindow( operation );
														
							return( true );
						}
					}
					
					return( false );
				}
			});
	}
	
	private volatile Shell 		shell;
	private volatile boolean 	task_complete;
	
	public 
	ProgressWindow(
		final AzureusCoreOperation	operation )
	{
		final RuntimeException[] error = {null};
		
		new DelayedEvent( 
				1000,
				new AERunnable()
				{
					public void
					runSupport()
					{
						synchronized( this ){
							
							if ( !task_complete ){
								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											synchronized( this ){
												
												if ( !task_complete ){
											
													showDialog();
												}
											}
										}
									},
									false );
							}
						}
					}
				});
		
		new AEThread( "ProgressWindow", true )
		{
			public void 
			runSupport()
			{
				try{					
					operation.getTask().run( operation );
					
				}catch( RuntimeException e ){
					
					error[0] = e;
					
				}catch( Throwable e ){
		
					error[0] = new RuntimeException( e );
					
				}finally{
					
					synchronized( this ){
						
						task_complete = true;
						
						Utils.execSWTThread( new Runnable(){public void run(){}}, true );
					}			
				}
			}
		}.start();
			
		try{
			final Display display = SWTThread.getInstance().getDisplay();
	
			while( !( task_complete || display.isDisposed())){
				
				if (!display.readAndDispatch()) display.sleep();
			}
		}finally{
			
				// bit of boiler plate in case something fails in the dispatch loop
			
			synchronized( this ){
				
				task_complete = true;
			}
			
			try{
				if ( shell != null && !shell.isDisposed()){
				
					shell.dispose();
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		if ( error[0] != null ){
			
			throw( error[0] );
		}
	}
	
	protected void
	showDialog()
	{
		final Display display = SWTThread.getInstance().getDisplay();
		
		shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(
				display, 
				( SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL ) & ~SWT.CLOSE );

		shell.setText( MessageText.getString( "progress.window.title" ));

		if(! Constants.isOSX) {
			shell.setImage(ImageRepository.getImage("azureus"));
		}

		GridLayout layout = new GridLayout();
		shell.setLayout(layout);

		Label label = new Label(shell, SWT.NONE);
		label.setText(MessageText.getString( "progress.window.msg.filemove" ));
		GridData gridData = new GridData();
		label.setLayoutData(gridData);

		shell.pack();
		
		Utils.centreWindow( shell );

		shell.open();
	}
}
