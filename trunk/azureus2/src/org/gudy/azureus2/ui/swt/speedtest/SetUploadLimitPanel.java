package org.gudy.azureus2.ui.swt.speedtest;

import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterResult;


/**
 * Created on May 1, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class SetUploadLimitPanel extends AbstractWizardPanel {

    int measuredUploadKbps, measuredDownloadKbps;

    Label explain;

    Label downloadLabel;
    Label uploadLabel;
    Text uploadText;
    Button apply;



    public SetUploadLimitPanel(Wizard wizard, IWizardPanel previousPanel, int upload, int download) {
        super(wizard, previousPanel);
        measuredUploadKbps =upload/1024;
        measuredDownloadKbps =download/1024;
    }

    /**
     * Panel has text at the top explaining the result.
     * Then under that it has a label the measured upload value and the recommended value.
     * Then a button with apply.
     */
    public void show() {

        wizard.setTitle(MessageText.getString("SpeedTestWizard.set.upload.title"));
        wizard.setCurrentInfo(MessageText.getString("SpeedTestWizard.set.upload.hint"));

        Composite rootPanel = wizard.getPanel();
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        rootPanel.setLayout(layout);

        Composite panel = new Composite(rootPanel, SWT.NULL);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        panel.setLayoutData(gridData);

        layout = new GridLayout();
        layout.numColumns = 3;
        panel.setLayout(layout);

        Label explain = new Label(panel, SWT.WRAP);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        explain.setLayoutData(gridData);
        Messages.setLanguageText(explain,"SpeedTestWizard.set.upload.panel.explain");

        //spacer line
        Label spacer = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        spacer.setLayoutData(gridData);

        //upload limit label.
        Label ul = new Label(panel, SWT.NULL );
        gridData = new GridData();
        ul.setLayoutData(gridData);
        Messages.setLanguageText(
        		ul, 
        		"SpeedTestWizard.set.upload.label", 
        		new String[] { DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB)});

        final Text uploadLimitSetting = new Text(panel, SWT.BORDER );
        gridData = new GridData(GridData.BEGINNING);
        gridData.widthHint=80;
        uploadLimitSetting.setLayoutData(gridData);
        int eightyPercent = calculatePercent(measuredUploadKbps,80);

        //don't accept any value less the 20 kb/s
        if(eightyPercent<20)
            eightyPercent=20;

        uploadLimitSetting.setText( ""+eightyPercent );
        uploadLimitSetting.addListener(SWT.Verify, new Listener() {
          public void handleEvent(Event e) {
            String text = e.text;
            char[] chars = new char[text.length()];
            text.getChars(0, chars.length, chars, 0);
            for (int i = 0; i < chars.length; i++) {
              if (!('0' <= chars[i] && chars[i] <= '9')) {
                e.doit = false;
                return;
              }
            }
          }
        });

        apply = new Button(panel, SWT.PUSH);
        Messages.setLanguageText(apply, "SpeedTestWizard.set.upload.button.apply" );
        gridData = new GridData();
        gridData.widthHint = 70;
        apply.setLayoutData(gridData);
        apply.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event){

                //Turn the string into an int and make it kbps.
                int uploadLimitKBPS = Integer.parseInt( uploadLimitSetting.getText() );
                //No value less then 20 kpbs should be allowed.
                if(uploadLimitKBPS<20)
                    uploadLimitKBPS=20;

                //set upload limits
                COConfigurationManager.setParameter( "AutoSpeed Max Upload KBs", uploadLimitKBPS ); //ToDo: does this go in TransferSpeedValidator?
                COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, uploadLimitKBPS );
                COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY , uploadLimitKBPS );

                wizard.setFinishEnabled(true);
                wizard.setPreviousEnabled(false);
            }
        });


//        //spacer col
//        Label c1 = new Label(panel, SWT.NULL);
//        gridData = new GridData();
//        gridData.horizontalSpan = 1;
//        c1.setLayoutData(c1);
//
//        //echo
//        Label c2 = new Label(panel, SWT.NULL);
//        gridData = new GridData();
//        gridData.horizontalSpan = 1;
//        c2.setLayoutData(c2);
//        c2.setText( DisplayFormatters.formatByteCountToKiBEtcPerSec(eightyPercent*8) );
//        //This space has a change listener the updates in bits/sec.
//
//        //spacer col
//        Label c3 = new Label(panel, SWT.NULL);
//        gridData = new GridData();
//        gridData.horizontalSpan = 1;
//        c3.setLayoutData(c3);

        //spacer line
        Label spacer2 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        spacer2.setLayoutData(gridData);

        //switch column width to 5 columns.
        Composite resultsPanel = new Composite(rootPanel, SWT.NULL);
        gridData = new GridData( GridData.VERTICAL_ALIGN_END | GridData.FILL_HORIZONTAL );
        resultsPanel.setLayoutData(gridData);

        layout = new GridLayout();
        layout.numColumns = 5;
        layout.makeColumnsEqualWidth=true;
        resultsPanel.setLayout(layout);


        //display last test result
        NetworkAdminSpeedTesterResult result = SpeedTestData.getInstance().getLastResult();
        if( result.hadError() ){
            //error
            String error = result.getLastError();
            createResultLabels(resultsPanel,true);
            createErrorDesc(resultsPanel,error);
            createTestDesc(resultsPanel);
            
        }else{
            //no error
            //print out the last result format.
            int upload = result.getUploadSpeed();
            int download = result.getDownloadSpeed();

            createResultLabels(resultsPanel,false);
            createResultData(resultsPanel, MessageText.getString("GeneralView.label.uploadspeed") ,upload);
            createResultData(resultsPanel, MessageText.getString("GeneralView.label.downloadspeed"), download);
            createTestDesc(resultsPanel);
        }

    }//show

    /**
     * Create a label for the test. The layout is assumed to be five across. If an error
     * occured in the test then the units are not printed out.
     * @param panel -
     * @param hadError - true if the test had an error.
     */
    private void createResultLabels(Composite panel,boolean hadError){
        GridData gridData;

        //spacer column
        Label c1 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c1.setLayoutData(gridData);

        //label
        Label c2 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.END;
        c2.setLayoutData(gridData);
        c2.setText( MessageText.getString("SpeedTestWizard.set.upload.result") );


        //bytes
        Label c3 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.CENTER;
        c3.setLayoutData(gridData);
        if(!hadError){
            c3.setText( MessageText.getString("SpeedTestWizard.set.upload.bytes.per.sec") );
        }

        //bits
        Label c4 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.CENTER;
        c4.setLayoutData(gridData);
        if(!hadError){
            c4.setText( MessageText.getString("SpeedTestWizard.set.upload.bits.per.sec") );
        }

        //spacer column
        Label c5 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c5.setLayoutData(gridData);

    }

    private void createResultData(Composite panel,String label, int rate){
        GridData gridData;

        //spacer column
        Label c1 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c1.setLayoutData(gridData);

        //label
        Label c2 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.END;
        c2.setLayoutData(gridData);
        c2.setText( label );


        //bytes
        Label c3 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.CENTER;
        c3.setLayoutData(gridData);
        c3.setText( DisplayFormatters.formatByteCountToKiBEtcPerSec(rate) );

        //bits
        Label c4 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.CENTER;
        c4.setLayoutData(gridData);
        c4.setText( DisplayFormatters.formatByteCountToBitsPerSec(rate) );

        //spacer column
        Label c5 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c5.setLayoutData(gridData);
    }

    private void createTestDesc(Composite panel){

    }

    private void createErrorDesc(Composite panel,String error){

    }

    public int calculatePercent(int value, int percent){
        return  Math.round( ((float)value/100.0f)*(percent) );
    }

    public void finish(){
        wizard.switchToClose();
    }//finish

    public IWizardPanel getFinishPanel(){

        return new SpeedTestFinishPanel(wizard,this);
    }

    public boolean isNextEnabled(){
        //This is the final step for now.
        return false;
    }
    
}
