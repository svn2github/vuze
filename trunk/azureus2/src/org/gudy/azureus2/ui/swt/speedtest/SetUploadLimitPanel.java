package org.gudy.azureus2.ui.swt.speedtest;

import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;


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
        GridData gridData = new GridData(GridData.FILL_BOTH);
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
        Messages.setLanguageText(ul, "SpeedTestWizard.set.upload.label");

        final Text uploadLimitSetting = new Text(panel, SWT.BORDER );
        gridData = new GridData(GridData.FILL_HORIZONTAL);
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
                //COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY , uploadLimitKBPS );
                COConfigurationManager.setParameter( "AutoSpeed Max Upload KBs", uploadLimitKBPS ); //ToDo: does this go in TransferSpeedValidator?
                COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, uploadLimitKBPS );
                COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY , uploadLimitKBPS );

                wizard.setFinishEnabled(true);
                wizard.setPreviousEnabled(false);
            }
        });

    }//show

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

    //here finish just closes the wizard.
}
