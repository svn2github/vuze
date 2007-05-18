package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.IntParameter;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderV2;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderVivaldi;

/**
 * Created on May 15, 2007
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

public class ConfigSectionTransferAutoSpeedBeta
        implements UISWTConfigSection
{

    Combo strategyCombo;

    //upload/download limits
    IntParameter downMaxLim;
    IntParameter downMinLim;
    IntParameter uploadMaxLim;
    IntParameter uploadMinLim;

    IntParameter vGood;
    IntParameter vGoodTol;
    IntParameter vBad;
    IntParameter vBadTol;

    BooleanParameter enableV2AutoSpeedBeta;

    /**
     * Create your own configuration panel here.  It can be anything that inherits
     * from SWT's Composite class.
     * Please be mindfull of small screen resolutions
     *
     * @param parent The parent of your configuration panel
     * @return your configuration panel
     */

    /**
     * Returns section you want your configuration panel to be under.
     * See SECTION_* constants.  To add a subsection to your own ConfigSection,
     * return the configSectionGetName result of your parent.<br>
     */
    public String configSectionGetParentSection() {
        return ConfigSection.SECTION_TRANSFER;
    }

    /**
     * In order for the plugin to display its section correctly, a key in the
     * Plugin language file will need to contain
     * <TT>ConfigView.section.<i>&lt;configSectionGetName() result&gt;</i>=The Section name.</TT><br>
     *
     * @return The name of the configuration section
     */
    public String configSectionGetName() {
        return "transfer.autospeedbeta";
    }

    /**
     * User selected Save.
     * All saving of non-plugin tabs have been completed, as well as
     * saving of plugins that implement org.gudy.azureus2.plugins.ui.config
     * parameters.
     */
    public void configSectionSave() {
    }

    /**
     * Config view is closing
     */
    public void configSectionDelete() {
    }


    public Composite configSectionCreate(final Composite parent) {

        //ToDo: for new we are NOT going to internationalize this panel. Wait until the panel is in its final format.

        GridData gridData;

        Composite cSection = new Composite(parent, SWT.NULL);

        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL|GridData.HORIZONTAL_ALIGN_FILL);
        cSection.setLayoutData(gridData);
        GridLayout subPanel = new GridLayout();
        subPanel.numColumns = 3;
        cSection.setLayout(subPanel);


        //To enable the beta.
        Label enableLabel = new Label(cSection, SWT.NULL);
        enableLabel.setText("Enable AutoSpeed Beta: ");
        gridData = new GridData();
        gridData.widthHint = 40;
        cSection.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.widthHint = 50;
        enableV2AutoSpeedBeta = new BooleanParameter(cSection,SpeedManagerAlgorithmProviderV2.SETTING_V2_BETA_ENABLED);
        enableV2AutoSpeedBeta.setLayoutData(gridData);

        //spacer
        Label enableSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        enableSpacer.setLayoutData(gridData);

        //Need a drop down to select which method will be used.
        Label label = new Label(cSection, SWT.NULL);
        label.setText("algorithm: ");
        gridData = new GridData();
        gridData.widthHint = 40;
        cSection.setLayoutData(gridData);

        strategyCombo = new Combo(cSection, SWT.READ_ONLY);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        strategyCombo.add("SpeedSense",0);
        strategyCombo.add("Vivaldi",1);
        strategyCombo.add("PingTrends",2);
        cSection.setLayoutData(gridData);
        strategyCombo.select(1);

        //ToDo: for now we put in just the Vivaldi settings, but this WILL change.

        //spacer
        Label spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //label column for speed test results
        Label limits = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        limits.setText("Speed Test Limits: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label limMax = new Label(cSection,SWT.NULL);
        gridData = new GridData();
        limMax.setLayoutData(gridData);
        limMax.setText("max");
        //Messages.setLanguageText //ToDo: internationalize

        Label limMin = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        limMin.setLayoutData(gridData);
        limMin.setText("min");
        //Messages.setLanguageText //ToDo: internationalize


        //download settings
        Label setDown = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        setDown.setLayoutData(gridData);
        setDown.setText("Download: ");
        //Messages.setLanguageText //ToDo: internationalize

        gridData = new GridData();
        gridData.widthHint = 50;
        downMaxLim = new IntParameter(cSection,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
        int setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,80000);
        downMaxLim.setValue( setting );
        downMaxLim.setLayoutData( gridData );


        gridData = new GridData();
        gridData.widthHint = 50;
        downMinLim = new IntParameter(cSection,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT,8000);
        downMinLim.setValue( setting );
        downMinLim.setLayoutData( gridData );

        //upload settings
        Label setUp = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        setUp.setLayoutData(gridData);
        setUp.setText("Upload: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        uploadMaxLim = new IntParameter(cSection, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,1300);
        uploadMaxLim.setValue( setting );
        uploadMaxLim.setLayoutData( gridData );


        gridData = new GridData();
        gridData.widthHint = 50;
        uploadMinLim = new IntParameter(cSection, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT, 800, 5000);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT,1300);
        uploadMinLim.setValue( setting );
        uploadMinLim.setLayoutData( gridData );

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //label column for Vivaldi limits
        Label vivaldiSetting = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        vivaldiSetting.setText("Vivaldi Settings: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label vSet = new Label(cSection,SWT.NULL);
        gridData = new GridData();
        vSet.setLayoutData(gridData);
        vSet.setText("set point");
        //Messages.setLanguageText //ToDo: internationalize

        Label vTol = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        vTol.setLayoutData(gridData);
        vTol.setText("tolerance");
        //Messages.setLanguageText //ToDo: internationalize

        //good
        Label vGoodLbl = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        vGoodLbl.setLayoutData(gridData);
        vGoodLbl.setText("Good: ");
        //Messages.setLanguageText //ToDo: internationalize

        
        gridData = new GridData();
        gridData.widthHint = 50;
        vGood = new IntParameter(cSection, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_SET_POINT);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_SET_POINT,100);
        vGood.setValue( setting );
        vGood.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        vGoodTol = new IntParameter(cSection, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_TOLERANCE);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_TOLERANCE,300);
        vGoodTol.setValue( setting );
        vGoodTol.setLayoutData( gridData );

        //bad
        Label vBadLbl = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        vBadLbl.setLayoutData(gridData);
        vBadLbl.setText("Bad: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        vBad = new IntParameter(cSection, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_SET_POINT);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_SET_POINT,1300);
        vBad.setValue( setting );
        vBad.setLayoutData( gridData );

        
        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        vBadTol = new IntParameter(cSection, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_TOLERANCE);
        setting = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_TOLERANCE,300);
        vBadTol.setValue( setting );
        vBadTol.setLayoutData( gridData );

        return cSection;
    }

    /**
     * Generic verify listener to make sure only numbers are entered into text field.
     */
    class GenericNumbersOnlyListener implements Listener {

        public void handleEvent(Event event){
            String text = event.text;
            char[] chars = new char[text.length()];
            text.getChars(0, chars.length, chars, 0);
            for (int i=0; i<chars.length; i++) {
                if( !('0' <= chars[i] && chars[i] <= '9')){
                    event.doit = false;
                    return;
                }
            }
        }//handleEvent
    }

}
