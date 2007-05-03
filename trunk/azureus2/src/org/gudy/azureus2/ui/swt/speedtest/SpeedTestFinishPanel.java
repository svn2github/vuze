package org.gudy.azureus2.ui.swt.speedtest;

import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;

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

    /**
     *
     */
    public void show() {

        wizard.setTitle("Speed Test Finished!");
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
        label.setText("You have finished the speed test wizard. click close.");

        //show the setting for upload speed.
        int maxUploadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
        int maxUploadSeedingKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
        int maxDownloadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
        //boolean setting.
        boolean autoSpeedEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY );
        boolean autoSpeedSeedingEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_CONFIGKEY );

        StringBuffer sb = new StringBuffer("\n\n");
        sb.append("Max upload : ").append(maxUploadKbs).append(" kb/s\n");
        sb.append("Max upload while seeding : ").append(maxUploadSeedingKbs).append(" kb/s\n");
        sb.append("Max download : ").append(maxDownloadKbs).append(" kb/s\n");
        sb.append("Auto speed is : ");
        if( autoSpeedEnabled )
            sb.append("enabled");
        else
            sb.append("disabled");
        sb.append("\n");
        sb.append("Auto speed while seeding is : ");
        if(autoSpeedSeedingEnabled)
            sb.append("enabled");
        else
            sb.append("disabled");
        sb.append("\n");

        //print out configuration data, so they know what the current values are.
        Label lblSettings = new Label(panel, SWT.WRAP);
        gridData = new GridData();
        lblSettings.setLayoutData(gridData);
        lblSettings.setText( sb.toString() );

    }//show

    public boolean isPreviousEnabled(){
        return false;
    }
    
}
