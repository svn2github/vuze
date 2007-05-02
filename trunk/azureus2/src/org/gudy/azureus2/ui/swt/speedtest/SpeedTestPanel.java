/*
 * Created on Apr 30, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.ui.swt.speedtest;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.WizardListener;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduledTest;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduledTestListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterResult;
import com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminSpeedTestSchedulerImpl;

public class 
SpeedTestPanel
	extends AbstractWizardPanel 
	implements NetworkAdminSpeedTestScheduledTestListener, NetworkAdminSpeedTesterListener
{	
	private SpeedTestWizard		wizard;
	
	private NetworkAdminSpeedTestScheduler nasts;
	private NetworkAdminSpeedTestScheduledTest	scheduled_test;
	
	private Text 		tasks;
	private ProgressBar progress;
	private Display 	display;

	private boolean		test_running;
	private boolean		switched_to_close;
	
	public 
	SpeedTestPanel(
		SpeedTestWizard _wizard, 
		IWizardPanel 	_previousPanel) 
	{
	    super( _wizard, _previousPanel );
	    
	    wizard	= _wizard;
	    
		nasts = NetworkAdminSpeedTestSchedulerImpl.getInstance();
	}
	
	public void 
	show() 
	{
		display = wizard.getDisplay();
		wizard.setTitle(MessageText.getString( SpeedTestWizard.CFG_PREFIX + "run" ));
		wizard.setCurrentInfo("");
		wizard.setPreviousEnabled(false);
		
		Composite panel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);

		tasks = new Text(panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL );
		tasks.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		gridData = new GridData(GridData.FILL_BOTH);
		tasks.setLayoutData(gridData);

		progress = new ProgressBar(panel, SWT.NULL);
		progress.setMinimum(0);
		progress.setMaximum(0);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		progress.setLayoutData(gridData);
	}

	public void 
	finish() 
	{
		test_running	= true;
		
		wizard.addListener(
			new WizardListener()
			{
				public void
				closed()
				{
					cancel();
				}
			});
		
		wizard.setFinishEnabled( false );
		
		Thread t = 
			new AEThread("SpeedTest Performer") 
			{
				public void 
				runSupport() 
				{
					runTest();
				}
			};
		
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
	}
	  
	public void
	cancel()
	{
		if ( scheduled_test != null ){
		
			scheduled_test.abort();
		}
	}
	
	protected void
	runTest()
	{
		test_running	= true;
		
		if ( nasts.getCurrentTest() !=  null ){
	
			reportStage( "Test already running!" );

		}else{
				// what's the contract here in terms of listener removal?
			
			try{
				scheduled_test = nasts.scheduleTest( NetworkAdminSpeedTestScheduler.TEST_TYPE_BITTORRENT );
	
				scheduled_test.addListener( this );
				
				scheduled_test.getTester().addListener( this );
				
				scheduled_test.start();				
				
			}catch( Throwable e ){
				
				reportStage( "Test request not accepted" );
								
			    if (display != null && !display.isDisposed()) {
				      display.asyncExec(new AERunnable(){
				        public void runSupport() {
				        	switchToClose();
				        }
				      });
			    }
				
				//ToDo: the test request failed, need to indicate this back to the UI!!
			}
		}
	}
	
	public void 
	stage(
		NetworkAdminSpeedTestScheduledTest 	test, 
		String 								step )
	{
		reportStage( step );
	}

	public void 
	complete(
		NetworkAdminSpeedTestScheduledTest test )
	{
	}
	
	public void 
	stage(
		NetworkAdminSpeedTester 	tester, 
		String 						step )
	{
		reportStage( step );
	}
	
	public void 
	complete(
		NetworkAdminSpeedTester			tester,
		NetworkAdminSpeedTesterResult 	result )
	{
		reportComplete( result );
	}
	
	protected void 
	reportComplete(
		final NetworkAdminSpeedTesterResult 	result )
	{
	    if (display != null && !display.isDisposed()) {
		      display.asyncExec(new AERunnable(){
		        public void runSupport() {
		          if (tasks != null && !tasks.isDisposed()) {
			            tasks.append("Upload speed = " + DisplayFormatters.formatByteCountToKiBEtcPerSec(result.getUploadSpeed()) + Text.DELIMITER);
			            tasks.append("Download speed = " + DisplayFormatters.formatByteCountToKiBEtcPerSec(result.getDownloadSpeed()) + Text.DELIMITER);
		          }
		          
		          switchToClose();
		        }
		      });
		    }
	}

	protected void 
	reportStage(
		final String 						step )
	{
	    if (display != null && !display.isDisposed()) {
		      display.asyncExec(new AERunnable(){
		        public void runSupport() {
		          if (tasks != null && !tasks.isDisposed()) {
			            tasks.append( step + Text.DELIMITER);
		          }
		        }
		      });
		    }	
	}
	
	protected void
	switchToClose()
	{
		switched_to_close	= true;
		
		wizard.switchToClose();
	}
	
	public boolean
	isFinishEnabled()
	{
		return( !( switched_to_close || test_running ));
	}
	
	public boolean
	isFinishSelectionOK()
	{
		return( !( switched_to_close || test_running ));
	}
	
	public IWizardPanel 
	getFinishPanel()
	{
		return( this );
	}
}
