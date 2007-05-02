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
import org.eclipse.swt.widgets.*;
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

    private NetworkAdminSpeedTestScheduler nasts;
	private NetworkAdminSpeedTestScheduledTest	scheduled_test;

    private Label       explain;
    private Label       testType;
    private Button      test;
    private Button      abort;
    private Label       testCountDown1;
    private Label       testCountDown2;

    private Text textMessages;
	private ProgressBar progress;
	private Display 	display;

	private boolean		test_running;
	private boolean		switched_to_close;

    //measured upload and download results.
    int uploadTest, downloadTest;

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
		wizard.setCurrentInfo("BitTorrent bandwidth testing.");
		wizard.setPreviousEnabled(false);
		
		Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

        Composite panel = new Composite(rootPanel, SWT.NULL);
        GridData gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);

        //label explain section.
        layout = new GridLayout();
        layout.numColumns = 4;
        panel.setLayout(layout);

        Label explain = new Label(panel, SWT.WRAP);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        explain.setLayoutData(gridData);
        StringBuffer sb = new StringBuffer("This test will measure the speed at which data can be simultaneously uploaded");
        sb.append("and downloaded in your network. The test will first request a testing slot from our service. If a ");
        sb.append("testing slot is available it will then get a virtual torrent, half for uploading and half for downloading.");
        sb.append("Once the test start it has 2 minutes to complete. It will first pause all the downloads, then set a ");
        sb.append("very high global limit, so it doesn't consume too much server bandwidth. It lets the download rate ");
        explain.setText( sb.toString() );

        //space line
        Label spacer = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 4;
        spacer.setLayoutData(gridData);

        //label type and button section.
        Label ul = new Label(panel, SWT.NULL );
        gridData = new GridData();
        ul.setLayoutData(gridData);
        ul.setText("Azureus speed test: ");

        Label ulType = new Label(panel, SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        ulType.setLayoutData(gridData);
        ulType.setText("BT upload/download");

        test = new Button(panel, SWT.PUSH);
        test.setText("run");
        gridData = new GridData();
        gridData.widthHint = 70;
        test.setLayoutData(gridData);

        abort = new Button(panel, SWT.PUSH);
        abort.setText("abort");
        gridData = new GridData();
        gridData.widthHint = 70;
        abort.setLayoutData(gridData);

        //test count down section.


        //test progress bar
        //layout = new GridLayout();
        //layout.numColumns = 1;
        //panel.setLayout(layout);

        progress = new ProgressBar(panel, SWT.SMOOTH);
		progress.setMinimum(0);
		progress.setMaximum(100);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        progress.setLayoutData(gridData);

        //message text section
        layout = new GridLayout();
        layout.numColumns = 1;
        panel.setLayout(layout);

        textMessages = new Text(panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL );
		textMessages.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 4;
        textMessages.setLayoutData(gridData);

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
		          if (textMessages != null && !textMessages.isDisposed()) {
                        uploadTest = result.getUploadSpeed();
                        downloadTest = result.getDownloadSpeed();
                        textMessages.append("Upload speed = " + DisplayFormatters.formatByteCountToKiBEtcPerSec(result.getUploadSpeed()) + Text.DELIMITER);
			            textMessages.append("Download speed = " + DisplayFormatters.formatByteCountToKiBEtcPerSec(result.getDownloadSpeed()) + Text.DELIMITER);
                        wizard.setNextEnabled(true);
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

                  if(step==null)
                    return;

                  //intercept progress indications.
                  if( step.startsWith("progress:")){
                      //expect format of string to be "progress: # : ..." where # is 0-100
                      int progressAmount = getProgressValueFromString(step);
                      progress.setSelection(progressAmount);
                  }

                  //print everything including progress indications.
                  if (textMessages != null && !textMessages.isDisposed()) {
			            textMessages.append( step + Text.DELIMITER);
		          }
		        }
		      });
		    }	
	}

    /**
     *
     * @param step - String with the expected format.  "progress: #" where # is 0 - 100.
     * @return The number as an integer, if the result is not known return 0.
     */
    private static int getProgressValueFromString(String step){
        if(step==null)
            return 0;
        
        if( !step.startsWith("progress:") )
            return 0;

        String[] value = step.split(":");
        if(value.length<2)
            return 0;

        int progress;
        try{
            progress = Integer.parseInt(value[1].trim());
        }catch(Exception e){
            return 0;
        }

        if( progress<0 || progress>100 )
            return 0;

        return progress;
    }//getProgressValueFromString

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


    public boolean isNextEnabled(){
        //only enable after the test completes correctly.
        return uploadTest>0;
    }//isNextEnabled

    public IWizardPanel getNextPanel() {
        return new SetUploadLimitPanel( wizard, this, uploadTest, downloadTest);
    }

}
