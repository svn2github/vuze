package org.gudy.azureus2.ui.swt.speedtest;

import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.config.StringListParameter;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterResult;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedLimitConfidence;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedLimitMonitor;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedManagerAlgorithmProviderV2;


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

public class SpeedTestSetLimitPanel extends AbstractWizardPanel {

    int measuredUploadKbps, measuredDownloadKbps;
    boolean downloadTestRan,uploadTestRan = true;
    boolean downloadHitLimit, uploadHitLimit;

    Label explain;

    Label downloadLabel;
    Label uploadLabel;
    Text uploadText;
    Button apply;

    StringListParameter downConfLevel;
    StringListParameter upConfLevel;



    public SpeedTestSetLimitPanel(Wizard wizard, IWizardPanel previousPanel, int upload, long maxup, int download, long maxdown) {
        super(wizard, previousPanel);
        
        downloadHitLimit 	= download > maxdown - 20*1024;
        uploadHitLimit 		= upload > maxup - 20*1024;
        
        measuredUploadKbps =upload/1024;
        if(measuredUploadKbps<5){
            uploadTestRan = false;
        }


        measuredDownloadKbps =download/1024;
        if(measuredDownloadKbps<5){
            downloadTestRan = false;
        }

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
        layout.numColumns = 4;
        panel.setLayout(layout);

        Label explain = new Label(panel, SWT.WRAP);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        explain.setLayoutData(gridData);
        Messages.setLanguageText(explain,"SpeedTestWizard.set.upload.panel.explain");

        //spacer line
        Label spacer = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 4;
        spacer.setLayoutData(gridData);

        Label spacer1 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        spacer1.setLayoutData(gridData);

        Label bytesCol = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        bytesCol.setLayoutData(gridData);
        Messages.setLanguageText(bytesCol,"SpeedTestWizard.set.upload.bytes.per.sec");

        Label bitsCol = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        bitsCol.setLayoutData(gridData);
        Messages.setLanguageText(bitsCol,"SpeedTestWizard.set.upload.bits.per.sec");

        Label confLevel = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        confLevel.setLayoutData(gridData);
        Messages.setLanguageText(confLevel,"SpeedTestWizard.set.limit.conf.level");


        //download limit label.
        Label dl = new Label( panel, SWT.NULL );
        gridData = new GridData();
        dl.setLayoutData(gridData);
        Messages.setLanguageText(
                dl,
                "SpeedTestWizard.set.download.label",
                new String[] { DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB)});

        final Text downloadLimitSetting = new Text(panel, SWT.BORDER);
        gridData = new GridData(GridData.BEGINNING);
        gridData.widthHint=80;
        downloadLimitSetting.setLayoutData(gridData);

        int bestDownloadSetting = determineRateSetting(measuredDownloadKbps,downloadTestRan,
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,
                SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING);

        downloadLimitSetting.setText( ""+bestDownloadSetting );
        downloadLimitSetting.addListener(SWT.Verify, new NumberListener(downloadLimitSetting) );

        //echo
        final Label downEcho = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.widthHint = 80;
        downEcho.setLayoutData(gridData);
        downEcho.setText( DisplayFormatters.formatByteCountToBitsPerSec(bestDownloadSetting*1024) );

        //convert bytes to bits on the fly for user.
        downloadLimitSetting.addListener(SWT.Modify, new ByteConversionListener(downEcho, downloadLimitSetting) );

        //download confidence setting.
        String[] confName = {
                SpeedLimitConfidence.ABSOLUTE.getInternationalizedString(),
                SpeedLimitConfidence.HIGH.getInternationalizedString(),
                SpeedLimitConfidence.MED.getInternationalizedString(),
                SpeedLimitConfidence.LOW.getInternationalizedString()
        };
        String[] confValue = {
                SpeedLimitConfidence.ABSOLUTE.getString(),
                SpeedLimitConfidence.HIGH.getString(),
                SpeedLimitConfidence.MED.getString(),
                SpeedLimitConfidence.LOW.getString()
        };

        String downDefaultConfidenceLevel = setDefaultConfidenceLevel(measuredDownloadKbps
                    ,SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,downloadTestRan);

        downConfLevel = new StringListParameter(panel,
                SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,
                downDefaultConfidenceLevel,
                confName, confValue,true);
        downConfLevel.setValue( downDefaultConfidenceLevel );

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

        int uploadCapacity = determineRateSetting(measuredUploadKbps,uploadTestRan,
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,
                SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING);

        //don't accept any value less the 20 kb/s
        if(uploadCapacity<20)
            uploadCapacity=20;

        uploadLimitSetting.setText( ""+uploadCapacity );
        uploadLimitSetting.addListener(SWT.Verify, new NumberListener(uploadLimitSetting));


        //echo
        final Label echo = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.widthHint = 80;
        echo.setLayoutData(gridData);
        echo.setText( DisplayFormatters.formatByteCountToBitsPerSec(uploadCapacity*1024) );
        //This space has a change listener the updates in bits/sec.

        //want a change listener to update the echo label which has the value in bits/sec.
        uploadLimitSetting.addListener(SWT.Modify, new ByteConversionListener(echo,uploadLimitSetting));

        //upload confidence setting.
        String upDefaultConfidenceLevel = setDefaultConfidenceLevel(measuredUploadKbps
                    ,SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,uploadTestRan);

        upConfLevel = new StringListParameter(panel,
                SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,
                upDefaultConfidenceLevel,
                confName, confValue, true);
        upConfLevel.setValue( upDefaultConfidenceLevel );


        //spacer col
        Label c1 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        gridData.widthHint = 80;
        c1.setLayoutData(gridData);


        SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();

        if ( uploadTestRan ){

            //ToDo: cable modem might over-estimate speed. Might need to drop result accordingly.

            sm.setEstimatedUploadCapacityBytesPerSec(
        			measuredUploadKbps*1024,
        			uploadHitLimit?
        				SpeedManagerLimitEstimate.TYPE_MEASURED_MIN :SpeedManagerLimitEstimate.TYPE_MEASURED);
        }

        if ( downloadTestRan ){
        	
        	sm.setEstimatedDownloadCapacityBytesPerSec( 
        			measuredDownloadKbps*1024,
        			downloadHitLimit?
        				SpeedManagerLimitEstimate.TYPE_MEASURED_MIN :SpeedManagerLimitEstimate.TYPE_MEASURED);
        }

        apply = new Button(panel, SWT.PUSH);
        Messages.setLanguageText(apply, "SpeedTestWizard.set.upload.button.apply" );
        gridData = new GridData();
        gridData.widthHint = 70;
        apply.setLayoutData(gridData);
        apply.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event){

                //Turn the string into an int and make it kbps.
                int uploadLimitKBPS = Integer.parseInt( uploadLimitSetting.getText() );
                int downlaodLimitKBPS = Integer.parseInt( downloadLimitSetting.getText() );
                //No value less then 20 kpbs should be allowed.
                if(uploadLimitKBPS<20){
                    uploadLimitKBPS=20;
                }

                //download value can never be less then upload.
                if( downlaodLimitKBPS < uploadLimitKBPS ){
                    downlaodLimitKBPS = uploadLimitKBPS;
                }

                //set upload limits
                COConfigurationManager.setParameter( "AutoSpeed Max Upload KBs", uploadLimitKBPS ); //ToDo: does this go in TransferSpeedValidator?
                COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, uploadLimitKBPS );
                COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY , uploadLimitKBPS );

                //provide the linkage to Auto-Speed V2 configuration settings.
                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT, uploadLimitKBPS*1024);
                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT, downlaodLimitKBPS*1024);


                String downConfValue = downConfLevel.getValue();
                String upConfValue = upConfLevel.getValue();
                COConfigurationManager.setParameter( SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING, downConfValue );
                COConfigurationManager.setParameter( SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING, upConfValue );
                		
                wizard.setFinishEnabled(true);
                wizard.setPreviousEnabled(false);
            }
        });


        //spacer col
        Label c3 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c3.setLayoutData(gridData);

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
            createResultData(resultsPanel, MessageText.getString("GeneralView.label.downloadspeed"), download);
            createResultData(resultsPanel, MessageText.getString("GeneralView.label.uploadspeed") ,upload);            
            createTestDesc(resultsPanel);
        }

    }//show

    /**
     * Set the default Confidence setting to medium, unless the value is close to 500 KBytes/sec.
     *
     * Note: This had to drop confidence down to medium, since upload rates can be over-estimated.
     *
     * In this case it is very possible that the real limit is higher.
     * @param transferRateKBPS - in kBytes/sec
     * @param paramName - configuration param to set.
     * @param testRan - Was this type of test ran?
     * @return - String -  Absolute | High | Med | Low | None
     */
    private static String setDefaultConfidenceLevel(int transferRateKBPS, String paramName, boolean testRan){

        //Need to over-ride the parameter.
        String prevSetting = COConfigurationManager.getStringParameter(paramName,
                SpeedLimitConfidence.LOW.getString() );

        //if it was previous set to ABSOLUTE then leave it alone.
        if( prevSetting.equalsIgnoreCase( SpeedLimitConfidence.ABSOLUTE.getString() ) )
        {
            return prevSetting;
        }

        //if no test was run then leave the confidence level alone.
        if( !testRan ){
            return prevSetting;
        }

        //if the transfer rate is near limit it is less likely to be the true limit.
        //decide here if we should have low setting.
        //ToDo: these limits shouldn't be hard coded since the Service can change at any time.
        if( transferRateKBPS < 550 && transferRateKBPS > 450 ){

            //set to low, when near limit and previous not set to ABSOLUTE
            COConfigurationManager.setParameter( paramName, SpeedLimitConfidence.LOW.getString() );
            return SpeedLimitConfidence.LOW.getString();
        }

        //In all other cases set to MED. This will allow for lowering of the limits if a chocking ping is found.
        COConfigurationManager.setParameter( paramName, SpeedLimitConfidence.MED.getString() );
        return SpeedLimitConfidence.MED.getString();
    }

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

    
    /**
     *
     * @param measuredRate - upload or download rate measured by speed test.
     * @param testRan - was this test upload/download ran?
     * @param maxSettingParamName - ex. SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT
     * @param confSettingParamName - ex. SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING
     * @return - recommended rate.
     */
    public static int determineRateSetting(int measuredRate, boolean testRan,
                                String maxSettingParamName , String confSettingParamName)
    {
        int retVal = measuredRate;

        //get AutoSpeedV2 download setting.
        int autoSpeedV2Limit = COConfigurationManager.getIntParameter(
                maxSettingParamName )/1024;

        //Use a low value to indicate the download test was NOT run.
        if( !testRan ){
            retVal = autoSpeedV2Limit;
        }

        //Need to over-ride the parameter.
        String downConf = COConfigurationManager.getStringParameter(confSettingParamName,
                SpeedLimitConfidence.LOW.getString() );

        //if it was previous set to ABSOLUTE then leave it alone.
        if( downConf.equalsIgnoreCase( SpeedLimitConfidence.ABSOLUTE.getString() ) )
        {
            retVal= autoSpeedV2Limit;
        }

        //The result cannot be less then 20 kbytes/sec.
        if(retVal < 20 ){
            retVal = 20;
        }

        return retVal;
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

    /**
     * Convert the bytes into bit.
     */
    class ByteConversionListener implements Listener
    {
        final Label echoLbl;
        final Text setting;

        public ByteConversionListener(final Label _echoLbl, final Text _setting){
            echoLbl = _echoLbl;
            setting = _setting;
        }

        public void handleEvent(Event e){
            String newVal = setting.getText();
            try{
                int newValInt = Integer.parseInt(newVal);
                if( echoLbl!=null ){
                    echoLbl.setText( DisplayFormatters.formatByteCountToBitsPerSec(newValInt*1024) );
                }
            }catch(Throwable t){
                //echo.setText(" - ");
            }
        }
    }//class

    /**
     * Only numbers are allowed.
     */
    class NumberListener implements Listener
    {
        final Text setting;
        public NumberListener(final Text _setting){
            setting = _setting;
        }

        public void handleEvent(Event e){
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
    }//class

}
