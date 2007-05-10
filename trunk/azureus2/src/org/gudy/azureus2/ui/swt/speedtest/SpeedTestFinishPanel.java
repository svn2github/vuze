package org.gudy.azureus2.ui.swt.speedtest;

import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;

import java.text.NumberFormat;

/**
 * Created on May 3, 2007
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

public class SpeedTestFinishPanel extends AbstractWizardPanel
{
    public SpeedTestFinishPanel(Wizard wizard, IWizardPanel previousPanel) {
        super(wizard, previousPanel);
    }

//    /**
//     *
//     */
//    public void show() {
//
//        String title = MessageText.getString("SpeedTestWizard.finish.panel.title");
//        wizard.setTitle(title);
//
//        Composite rootPanel = wizard.getPanel();
//        GridLayout layout = new GridLayout();
//        layout.numColumns = 1;
//        rootPanel.setLayout(layout);
//
//        Composite panel = new Composite(rootPanel, SWT.NULL);
//        GridData gridData = new GridData( GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL );
//        panel.setLayoutData(gridData);
//        layout = new GridLayout();
//        layout.numColumns = 3;
//        panel.setLayout(layout);
//
//        Label label = new Label(panel, SWT.WRAP);
//        gridData = new GridData();
//        gridData.horizontalSpan = 3;
//        gridData.widthHint = 380;
//        label.setLayoutData(gridData);
//        Messages.setLanguageText(label,"SpeedTestWizard.finish.panel.click.close");
//
//        //show the setting for upload speed.
//        int maxUploadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
//        int maxUploadSeedingKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
//        int maxDownloadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
//        //boolean setting.
//        boolean autoSpeedEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
//        boolean autoSpeedSeedingEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
//
//        StringBuffer sb = new StringBuffer("\n\n");
//        String maxUpload = MessageText.getString("SpeedTestWizard.finish.panel.max.upload");
//        String maxUploadVal = DisplayFormatters.formatByteCountToKiBEtcPerSec( maxUploadKbs*1024 );
//        sb.append(maxUpload).append(" ").append(maxUploadVal).append("\n");
//
//        String maxSeedingUpload = MessageText.getString("SpeedTestWizard.finish.panel.max.seeding.upload");
//        String maxSeedingUploadVal = DisplayFormatters.formatByteCountToKiBEtcPerSec( maxUploadSeedingKbs*1024 );
//        sb.append(maxSeedingUpload).append(" ").append(maxSeedingUploadVal).append("\n");
//
//        String maxDownload = MessageText.getString("SpeedTestWizard.finish.panel.max.download");
//        if(maxDownloadKbs==0){
//            String unlimited = MessageText.getString("ConfigView.unlimited");
//            sb.append(maxDownload).append(" ").append(unlimited).append("\n");
//        }else{
//            String maxDownloadVal = DisplayFormatters.formatByteCountToKiBEtcPerSec( maxDownloadKbs*1024 );
//            sb.append(maxDownload).append(" ").append(maxDownloadVal).append("\n");
//        }
//
//
//        String autoSpeed = MessageText.getString("SpeedTestWizard.finish.panel.auto.speed");
//        sb.append(autoSpeed);
//
//        String enabled = MessageText.getString("SpeedTestWizard.finish.panel.enabled","enabled");
//        String disabled = MessageText.getString("SpeedTestWizard.finish.panel.disabled","disabled");
//
//        if( autoSpeedEnabled ){
//            sb.append(enabled);
//        }else{
//            sb.append(disabled);
//        }
//        sb.append("\n");
//        String autoSpeedWhileSeeding = MessageText.getString("SpeedTestWizard.finish.panel.auto.speed.seeding");
//        sb.append(autoSpeedWhileSeeding).append(" ");
//        if(autoSpeedSeedingEnabled){
//            sb.append(enabled);
//        }else{
//            sb.append(disabled);
//        }
//        sb.append("\n");
//
//        //print out configuration data, so they know what the current values are.
//        Label lblSettings = new Label(panel, SWT.WRAP);
//        gridData = new GridData();
//        lblSettings.setLayoutData(gridData);
//        lblSettings.setText( sb.toString() );
//
//    }//show

    /**
     *
     */
    public void show() {

        String title = MessageText.getString("SpeedTestWizard.finish.panel.title");
        wizard.setTitle(title);

        Composite rootPanel = wizard.getPanel();
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        rootPanel.setLayout(layout);

        Composite panel = new Composite(rootPanel, SWT.NULL);
        GridData gridData = new GridData( GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL );
        panel.setLayoutData(gridData);
        layout = new GridLayout();
        layout.numColumns = 3;
        panel.setLayout(layout);

        Label label = new Label(panel, SWT.WRAP);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        gridData.widthHint = 380;
        label.setLayoutData(gridData);
        Messages.setLanguageText(label,"SpeedTestWizard.finish.panel.click.close");

        //show the setting for upload speed.
        int maxUploadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
        int maxUploadSeedingKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
        int maxDownloadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
        //boolean setting.
        boolean autoSpeedEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
        boolean autoSpeedSeedingEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );

        //spacer 1
        Label s1 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        s1.setLayoutData(gridData);

        //displays a bytes/sec column and a bits/sec column
        createHeaderLine(panel);

        //
        String maxUpload = MessageText.getString("SpeedTestWizard.finish.panel.max.upload");
        String maxUploadVal = DisplayFormatters.formatByteCountToKiBEtcPerSec( maxUploadKbs*1024 );

        createDataLine(panel, maxUpload, maxUploadVal, maxUploadKbs);


        String maxSeedingUpload = MessageText.getString("SpeedTestWizard.finish.panel.max.seeding.upload");
        String maxSeedingUploadVal = DisplayFormatters.formatByteCountToKiBEtcPerSec( maxUploadSeedingKbs*1024 );

        createDataLine(panel, maxSeedingUpload, maxSeedingUploadVal, maxUploadSeedingKbs);

        String maxDownload = MessageText.getString("SpeedTestWizard.finish.panel.max.download");
        String maxDownloadVal;
        if(maxDownloadKbs==0){
            maxDownloadVal = MessageText.getString("ConfigView.unlimited");
        }else{
            maxDownloadVal = DisplayFormatters.formatByteCountToKiBEtcPerSec( maxDownloadKbs*1024 );
        }

        createDataLine(panel, maxDownload, maxDownloadVal, maxDownloadKbs);

        //spacer 2
        Label s2 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        s2.setLayoutData(gridData);

        String autoSpeed = MessageText.getString("SpeedTestWizard.finish.panel.auto.speed");
        createStatusLine(panel, autoSpeed, autoSpeedEnabled);

        String autoSpeedWhileSeeding = MessageText.getString("SpeedTestWizard.finish.panel.auto.speed.seeding");
        createStatusLine(panel, autoSpeedWhileSeeding, autoSpeedSeedingEnabled);

    }//show

    private static final String colSpace = "  ";

    private void createHeaderLine(Composite panel){
        GridData gridData;
        Label c1 = new Label(panel, SWT.NULL);//label
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c1.setLayoutData(gridData);
        c1.setText(" ");



        Label c2 = new Label(panel,SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c2.setLayoutData(gridData);
        c2.setText("bytes/sec"+colSpace);//ToDo: internationalize.



        Label c3 = new Label(panel,SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c3.setLayoutData(gridData);
        c3.setText(colSpace+"bits/sec");//ToDo: internationalize.
    }

    /**
     *
     * @param panel -
     * @param label - label
     * @param enabled - is enabled
     */
    private void createStatusLine(Composite panel, String label, boolean enabled){
        GridData gridData;
        Label r3c1 = new Label(panel, SWT.NULL);//label
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        r3c1.setLayoutData(gridData);
        r3c1.setText(label);

        Label c2 = new Label(panel,SWT.NULL);//space.
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c2.setLayoutData(gridData);
        String maxUploadBitsSec = "       ";
        c2.setText(maxUploadBitsSec);

        Label c3 = new Label(panel,SWT.NULL);//enabled or disabled
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        c3.setLayoutData(gridData);
        if(enabled){
            c3.setText( MessageText.getString("SpeedTestWizard.finish.panel.enabled","enabled") );
        }else{
            c3.setText( MessageText.getString("SpeedTestWizard.finish.panel.disabled","disabled") );
        }

    }//createStatusLine

    /**
     * One line of data in the UI
     * @param panel -
     * @param label - label
     * @param value - bytes/sec
     * @param maxKbps - bits/sec
     */
    private void createDataLine(Composite panel, String label, String value, int maxKbps) {
        GridData gridData;
        Label r3c1 = new Label(panel, SWT.NULL);//max upload
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        r3c1.setLayoutData(gridData);
        r3c1.setText(label+"  ");

        Label r3c2 = new Label(panel,SWT.NULL);//kbytes/sec
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        r3c2.setLayoutData(gridData);
        r3c2.setText(value+colSpace);

        Label r3c3 = new Label(panel,SWT.NULL);//kbits/sec
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        r3c3.setLayoutData(gridData);
        //String maxUploadBitsSec = "      ( "+maxKbps*8+" )";  DisplayFormatters.formatDecimal(maxKbps*8,3,true,true);
        //String maxUploadBitsSec = "      ( "+DisplayFormatters.formatDecimal(maxKbps*8,3,true,true)+" )";
        String maxUploadBitsSec = NumberFormat.getInstance().format(maxKbps*8);
        r3c3.setText(colSpace+maxUploadBitsSec);
    }


    public boolean isPreviousEnabled(){
        return false;
    }
    
}
