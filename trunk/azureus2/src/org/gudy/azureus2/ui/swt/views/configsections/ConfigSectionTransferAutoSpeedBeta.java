package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.ConfigurationParameterNotFoundException;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerImpl;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedLimitConfidence;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedLimitMonitor;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedManagerAlgorithmProviderV2;

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

    BooleanParameter enableV2AutoSpeedBeta;

    StringListParameter strategyList;

    //upload/download limits
    IntParameter downMaxLim;
    IntParameter downMinLim;
    IntParameter uploadMaxLim;
    IntParameter uploadMinLim;

    StringListParameter confDownload;
    StringListParameter confUpload;

    //add a comment to the auto-speed debug logs.
    Group commentGroup;

    Group uploadCapGroup;

    //vivaldi set-points
    Group vivaldiGroup;
    IntParameter vGood;
    IntParameter vGoodTol;
    IntParameter vBad;
    IntParameter vBadTol;

    //DHT ping set-points
    Group dhtGroup;
    IntParameter dGood;
    IntParameter dGoodTol;
    IntParameter dBad;
    IntParameter dBadTol;
    //general ping set-points.
    IntParameter adjustmentInterval;
    BooleanParameter skipAfterAdjustment;

    Button reset;

    IntListParameter downloadModeUsedCap;
    IntListParameter seedModeUsedCap;

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
        return "transfer.select";
    }

    /**
     * In order for the plugin to display its section correctly, a key in the
     * Plugin language file will need to contain
     * <TT>ConfigView.section.<i>&lt;configSectionGetName() result&gt;</i>=The Section name.</TT><br>
     *
     * @return The name of the configuration section
     */
    public String configSectionGetName() {
        return "transfer.select.v2";
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

        //ToDo: for now we are NOT going to internationalize this panel. Wait until the panel is in its final format.

        GridData gridData;

        Composite cSection = new Composite(parent, SWT.NULL);

        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL|GridData.HORIZONTAL_ALIGN_FILL);
        cSection.setLayoutData(gridData);
        GridLayout subPanel = new GridLayout();
        subPanel.numColumns = 3;
        cSection.setLayout(subPanel);

        //add a comment to the debug log.
        ///////////////////////////////////
        // Comment group
        ///////////////////////////////////
        //comment grouping.
        commentGroup = new Group(cSection, SWT.NULL);
        commentGroup.setText("Add comment to debug log");
        GridLayout commentLayout = new GridLayout();
        commentLayout.numColumns = 3;
        commentGroup.setLayout(commentLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        commentGroup.setLayoutData(gridData);

        //Label
        Label commentLabel = new Label(commentGroup,SWT.NULL);
        commentLabel.setText("Add Comment: ");
        gridData = new GridData();
        gridData.widthHint = 70;
        gridData.horizontalSpan=1;
        commentLabel.setLayoutData(gridData);

        //Text-Box
        final Text commentBox = new Text(commentGroup, SWT.BORDER);
        gridData = new GridData(GridData.BEGINNING);
        gridData.widthHint = 200;
        gridData.horizontalSpan=1;
        commentBox.setText("");
        commentBox.setLayoutData(gridData);


        //button
        Button commentButton = new Button( commentGroup, SWT.PUSH);
        //Messages.
        gridData = new GridData();
        gridData.widthHint = 70;
        gridData.horizontalSpan=1;
        commentButton.setLayoutData(gridData);
        commentButton.setText("Log");
        commentButton.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event){
                //Add a file to the log.
                AEDiagnosticsLogger dLog = AEDiagnostics.getLogger("v3.AutoSpeed_Beta_Debug");
                String comment = commentBox.getText();
                if(comment!=null){
                    if( comment.length()>0){
                        dLog.log( "user-comment:"+comment );
                        commentBox.setText("");
                    }
                }
            }
        });

        //spacer
        Label commentSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        commentSpacer.setLayoutData(gridData);

                
        ///////////////////////////////////
        // AutoSpeed Beta mode group
        ///////////////////////////////////
        //Beta-mode grouping.
        Group modeGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        modeGroup.setText("AutoSpeed-Beta settings");
        GridLayout modeLayout = new GridLayout();
        modeLayout.numColumns = 4;
        modeGroup.setLayout(modeLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        modeGroup.setLayoutData(gridData);

        //Need a drop down to select which method will be used.
        Label label = new Label(modeGroup, SWT.NULL);
        label.setText("Input Data: ");
        gridData = new GridData();
        gridData.widthHint = 60;
        label.setLayoutData(gridData);

        //Set DHT as the default 
        String[] modeNames = {
                "Vivaldi Distance",
                "DHT Ping time"
        };
        String[] modes = {
                SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_VIVALDI,
                SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_DHT
        };
        strategyList = new StringListParameter(modeGroup,
                SpeedManagerAlgorithmProviderV2.SETTING_DATA_SOURCE_INPUT,
                SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_DHT,
                modeNames,modes,true);

        strategyList.addChangeListener( new GroupModeChangeListener() );


        //ToDo: switch layout managers here.

        //spacer
        Label spacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=4;
        spacer.setLayoutData(gridData);

        //label column for speed test results
        Label limits = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        limits.setText("Line Speed Limits: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label limMax = new Label(modeGroup,SWT.NULL);
        gridData = new GridData();
        limMax.setLayoutData(gridData);
        limMax.setText("max - capacity");
        //Messages.setLanguageText //ToDo: internationalize

        Label limMin = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        limMin.setLayoutData(gridData);
        limMin.setText("min - setting");
        //Messages.setLanguageText //ToDo: internationalize

        Label confLevel = new Label(modeGroup, SWT.NULL);
        gridData =  new GridData();
        confLevel.setLayoutData(gridData);
        confLevel.setText("confidence level");
        //Messages.setLanguageText //ToDo: internationalize

        //download settings
        Label setDown = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        setDown.setLayoutData(gridData);
        setDown.setText("Download: ");
        //Messages.setLanguageText //ToDo: internationalize

        gridData = new GridData();
        gridData.widthHint = 80;
        downMaxLim = new IntParameter(modeGroup,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
        downMaxLim.setLayoutData( gridData );

        gridData = new GridData();
        gridData.widthHint = 80;
        downMinLim = new IntParameter(modeGroup,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);
        downMinLim.setLayoutData( gridData );

        String[] confLevelNames = {
                SpeedLimitConfidence.ABSOLUTE.getInternationalizedString(),
                SpeedLimitConfidence.HIGH.getInternationalizedString(),
                SpeedLimitConfidence.MED.getInternationalizedString(),
                SpeedLimitConfidence.LOW.getInternationalizedString(),
                SpeedLimitConfidence.NONE.getInternationalizedString()
        };

        String[] confLevelValues = {
                SpeedLimitConfidence.ABSOLUTE.getString(),
                SpeedLimitConfidence.HIGH.getString(),
                SpeedLimitConfidence.MED.getString(),
                SpeedLimitConfidence.LOW.getString(),
                SpeedLimitConfidence.NONE.getString()
        };

        gridData = new GridData();
        gridData.widthHint = 80;
        confDownload = new StringListParameter(modeGroup, SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING, confLevelNames, confLevelValues);
        confDownload.setLayoutData( gridData );


        //upload settings
        Label setUp = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        setUp.setLayoutData(gridData);
        setUp.setText("Upload: ");
        //Messages.setLanguageText //ToDo: internationalize

        gridData = new GridData();
        gridData.widthHint = 80;
        uploadMaxLim = new IntParameter(modeGroup, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
        uploadMaxLim.setLayoutData( gridData );

        gridData = new GridData();
        gridData.widthHint = 80;
        uploadMinLim = new IntParameter(modeGroup, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT);
        uploadMinLim.setLayoutData( gridData );

        gridData = new GridData();
        gridData.widthHint = 80;
        confUpload = new StringListParameter(modeGroup, SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,confLevelNames,confLevelValues);
        confUpload.setLayoutData( gridData );

        //spacer
        spacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=4;
        spacer.setLayoutData(gridData);

        //Restore Defaults:
        Label restorDef = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        restorDef.setLayoutData(gridData);
        restorDef.setText("Restore Defaults:");

        //Button and listener here.
        reset = new Button(modeGroup, SWT.PUSH);
        reset.setText("Reset");  //ToDo: internationalize.
        gridData = new GridData();
        gridData.widthHint = 70;
        reset.setLayoutData(gridData);
        reset.addListener(SWT.Selection, new RestoreDefaultsListener());
        //

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=4;
        spacer.setLayoutData(gridData);

        ///////////////////////////
        // Upload Capacity used settings.
        ///////////////////////////
        uploadCapGroup = new Group(cSection, SWT.NULL);
        uploadCapGroup.setText("Upload Capacity Usage");
        //uploadCapGroup.setLayout(subPanel);

        GridLayout uCapLayout = new GridLayout();
        uCapLayout.numColumns = 2;
        uploadCapGroup.setLayout(uCapLayout);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        uploadCapGroup.setLayoutData(gridData);

        //Label column
        Label upCapModeLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        upCapModeLbl.setText("Mode:");
        upCapModeLbl.setLayoutData(gridData);

        Label ucSetLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        gridData.horizontalSpan = 2;
        ucSetLbl.setText("% Capacity Used");

        Label dlModeLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        dlModeLbl.setText("Downloading:");

        //add a drop down.
        String[] downloadModeNames = {
                " 80%",
                " 70%",
                " 60%",
                " 50%"
        };

        int[] downloadModeValues = {
                80,
                70,
                60,
                50
        };

        downloadModeUsedCap = new IntListParameter(uploadCapGroup,
                SpeedLimitMonitor.USED_UPLOAD_CAPACITY_DOWNLOAD_MODE,
                downloadModeNames, downloadModeValues);

        Label sdModeLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        sdModeLbl.setText("Seeding:");

        //add a drop down.
        String[] seedModeNames = {
                "100%",
                " 90%",
                " 80%"
        };

        int[] seedModeValues = {
                100,
                90,
                80
        };

        seedModeUsedCap = new IntListParameter(uploadCapGroup,
                SpeedLimitMonitor.USED_UPLOAD_CAPACITY_SEEDING_MODE,
                seedModeNames,seedModeValues);

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=4;
        spacer.setLayoutData(gridData);

        //////////////////////////
        // DHT Ping Group
        //////////////////////////

        dhtGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        dhtGroup.setText("Data: DHT Pings");
        dhtGroup.setLayout(subPanel);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        dhtGroup.setLayoutData(gridData);

        //label column for Vivaldi limits
        Label dhtSetting = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        dhtSetting.setText("DHT Ping Settings: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label dSet = new Label(dhtGroup,SWT.NULL);
        gridData = new GridData();
        dSet.setLayoutData(gridData);
        dSet.setText("set point (ms)");
        //Messages.setLanguageText //ToDo: internationalize

        Label dTol = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dTol.setLayoutData(gridData);
        dTol.setText("tolerance (ms)");
        //Messages.setLanguageText //ToDo: internationalize

        //good
        Label dGoodLbl = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dGoodLbl.setLayoutData(gridData);
        dGoodLbl.setText("Good: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        dGood = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_SET_POINT);
        dGood.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        dGoodTol = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_TOLERANCE);
        dGoodTol.setLayoutData( gridData );

        //bad
        Label dBadLbl = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dBadLbl.setLayoutData(gridData);
        dBadLbl.setText("Bad: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        dBad = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_SET_POINT);
        dBad.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        dBadTol = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_TOLERANCE);
        dBadTol.setLayoutData( gridData );

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        spacer.setLayoutData(gridData);

        //how much data to accumulate before making an adjustment.
        Label iCount = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=2;
        gridData.horizontalAlignment=GridData.BEGINNING;
        iCount.setLayoutData(gridData);
        iCount.setText("Adjustment interval: ");

        adjustmentInterval = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_INTERVALS_BETWEEN_ADJUST);
        gridData = new GridData();
        gridData.widthHint = 50;
        adjustmentInterval.setLayoutData(gridData);

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        spacer.setLayoutData(gridData);

        //how much data to accumulate before making an adjustment.
        Label skip = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=2;
        gridData.horizontalAlignment=GridData.BEGINNING;
        skip.setLayoutData(gridData);
        skip.setText("Skip after adjustment: ");

        skipAfterAdjustment = new BooleanParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_WAIT_AFTER_ADJUST);
        gridData = new GridData();
        gridData.widthHint = 50;
        skipAfterAdjustment.setLayoutData(gridData);


        //////////////////////////
        //Vivaldi Median Distance Group
        //////////////////////////

        //Vivaldi grouping.
        vivaldiGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        vivaldiGroup.setText("Data: Vivaldi");
        vivaldiGroup.setLayout(subPanel);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        vivaldiGroup.setLayoutData(gridData);

        //label column for Vivaldi limits
        Label vivaldiSetting = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        vivaldiSetting.setText("Vivaldi Settings: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label vSet = new Label(vivaldiGroup,SWT.NULL);
        gridData = new GridData();
        vSet.setLayoutData(gridData);
        vSet.setText("set point (ms)");
        //Messages.setLanguageText //ToDo: internationalize

        Label vTol = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        vTol.setLayoutData(gridData);
        vTol.setText("tolerance (ms)");
        //Messages.setLanguageText //ToDo: internationalize

        //good
        Label vGoodLbl = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        vGoodLbl.setLayoutData(gridData);
        vGoodLbl.setText("Good: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        vGood = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_SET_POINT);
        vGood.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        vGoodTol = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_TOLERANCE);
        vGoodTol.setLayoutData( gridData );

        //bad
        Label vBadLbl = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        vBadLbl.setLayoutData(gridData);
        vBadLbl.setText("Bad: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        vBad = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_SET_POINT);
        vBad.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        vBadTol = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_TOLERANCE);
        vBadTol.setLayoutData( gridData );

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //Hide the group that is not selected.
        String value = strategyList.getValue();
        enableGroups(value);

        return cSection;
    }


    void enableGroups(String strategyListValue){
        if(strategyListValue==null){
            return;
        }
            if( SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_VIVALDI.equals(strategyListValue) ){
                //enable the Vivaldi median distance group.
                if( vivaldiGroup!=null ){
                    vivaldiGroup.setEnabled(true);
                    vivaldiGroup.setVisible(true);
                }
                if( dhtGroup!=null ){
                    dhtGroup.setEnabled(false);
                    dhtGroup.setVisible(false);
                }

            }
            else if( SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_DHT.equals(strategyListValue) ){
                //enable the DHT group
                if( vivaldiGroup!=null){
                    vivaldiGroup.setEnabled(false);
                    vivaldiGroup.setVisible(false);
                }
                if( dhtGroup!=null ){
                    dhtGroup.setEnabled(true);
                    dhtGroup.setVisible(true);
                }
            }


        //only enable the comment section if the beta is enabled.
        boolean isBothEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
        boolean isSeedingEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
        long version = COConfigurationManager.getLongParameter( SpeedManagerImpl.CONFIG_VERSION );

        boolean isV2Enabled = false;
        if( (isBothEnabled || isSeedingEnabled) && version==2 ){
            isV2Enabled = true;
        }

        if( commentGroup!=null){
            if( isV2Enabled ){
                //make this section visible.
                commentGroup.setEnabled(true);
                commentGroup.setVisible(true);
            }else{
                //make it invisible.
                commentGroup.setEnabled(false);
                commentGroup.setVisible(false);
            }
        }
    }//enableGroups

    /**
     * Listen for changes in the drop down, then enable/disable the appropriate
     * group mode.
     */
    class GroupModeChangeListener implements ParameterChangeListener
    {
        /**
         * Enable/Disable approriate group.
         * @param p -
         * @param caused_internally  -
         */
        public void parameterChanged(Parameter p, boolean caused_internally) {
            String value = strategyList.getValue();
            enableGroups(value);
        }


        public void intParameterChanging(Parameter p, int toValue) {
            //nothing to do here.
        }


        public void booleanParameterChanging(Parameter p, boolean toValue) {
            //nothing to do here.
        }


        public void stringParameterChanging(Parameter p, String toValue) {
            //nothing to do here.
        }


        public void floatParameterChanging(Parameter owner, double toValue) {
            //nothing to do here.
        }
    }//class GroupModeChangeListener


    class RestoreDefaultsListener implements Listener {

        public void handleEvent(Event event) {

            ConfigurationDefaults configDefs = ConfigurationDefaults.getInstance();
            try{
                long downMax = configDefs.getLongParameter( SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT );
                long downMin = configDefs.getLongParameter( SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT );
                String downConf = configDefs.getStringParameter( SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING );
                long upMax = configDefs.getLongParameter( SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT );
                long upMin = configDefs.getLongParameter( SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT );
                String upConf = configDefs.getStringParameter( SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING );

                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,downMax);
                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT,downMin);
                COConfigurationManager.setParameter(SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,downConf);
                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,upMax);
                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT,upMin);
                COConfigurationManager.setParameter(SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,upConf);

            }catch(ConfigurationParameterNotFoundException cpnfe){
                //ToDo: log this.    
            }


        }//handleEvent

    }//class

}
