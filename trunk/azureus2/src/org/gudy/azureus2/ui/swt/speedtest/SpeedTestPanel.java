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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminSpeedTestSchedulerImpl;

public class 
SpeedTestPanel
	extends AbstractWizardPanel 
	implements NetworkAdminSpeedTestListener
{	
	private SpeedTestWizard		wizard;
	
	private NetworkAdminSpeedTestScheduler nasts;
	
	private Text 		tasks;
	private ProgressBar progress;
	private Display 	display;

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
	  
	protected void
	runTest()
	{
		if ( !nasts.isRunning() ){
	
				// what's the contract here in terms of listener removal?
			
			nasts.addSpeedTestListener( this );

				//schedule a test
			
			boolean accepted = nasts.requestTestFromService( NetworkAdminSpeedTestSchedulerImpl.BIT_TORRENT_UPLOAD_AND_DOWNLOAD );
			
			if ( accepted ){
				
				nasts.start( NetworkAdminSpeedTestSchedulerImpl.BIT_TORRENT_UPLOAD_AND_DOWNLOAD );
				
			}else{
				
				nasts.removeSpeedTestListener( this );
				
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
	complete(
		final NetworkAdminSpeedTester.Result 	result )
	{
		nasts.removeSpeedTestListener( this );
		
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

	public void 
	stage(
		final String 		step )
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
		
		wizard.setFinishEnabled( false );
	}
	
	public boolean
	isFinishEnabled()
	{
		return( !switched_to_close );
	}
	
	public boolean
	isFinishSelectionOK()
	{
		return( !switched_to_close );
	}
	
	public IWizardPanel 
	getFinishPanel()
	{
		return( this );
	}
}
