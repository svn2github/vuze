package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderV2;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerImpl;
import com.aelitis.azureus.core.speedmanager.impl.SpeedLimitMonitor;
import com.aelitis.azureus.core.speedmanager.impl.SpeedLimitConfidence;

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

    SpeedManagerChangeListener speedManagerListener = new SpeedManagerChangeListener();

    //The labels that change based on SpeedManagerChangeListener
    Label currDownCapacitySetting;
    Label currDownMinSetting;
    Label currDownConfSetting;
    Label currUpCapacitySetting;
    Label currUpMinSetting;
    Label currUpConfSetting;


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

        if( speedManagerListener==null ){
            //ToDo: log this condition.
            return;
        }

        //Need to remove all the listeners attached at the end of the createSection method.
        COConfigurationManager.removeParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,
                speedManagerListener);
        COConfigurationManager.removeParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,
                speedManagerListener);
        COConfigurationManager.removeParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT,
                speedManagerListener);
        COConfigurationManager.removeParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT,
                speedManagerListener);
        COConfigurationManager.removeParameterListener(
                SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,
                speedManagerListener);
        COConfigurationManager.removeParameterListener(
                SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,
                speedManagerListener);
        
        COConfigurationManager.removeParameterListener(
                TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY,
                speedManagerListener);
        COConfigurationManager.removeParameterListener(
                "Auto Upload Speed Seeding Enabled",
                speedManagerListener);

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
                SpeedLimitConfidence.LOW.getInternationalizedString()
        };

        String[] confLevelValues = {
                SpeedLimitConfidence.ABSOLUTE.getString(),
                SpeedLimitConfidence.HIGH.getString(),
                SpeedLimitConfidence.MED.getString(),
                SpeedLimitConfidence.LOW.getString()
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

        //spacer row with label.
        Label settingsLabel = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        settingsLabel.setLayoutData(gridData);
        settingsLabel.setText("Current Settings:");

        Label settingSpace = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        settingSpace.setLayoutData(gridData);

        //current download value row.
        Label downCurrVal = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        downCurrVal.setLayoutData(gridData);
        downCurrVal.setText("Download:");
        //max
        //currDownCapacitySetting = new Label(modeGroup, SWT.NULL);
        currDownCapacitySetting = createCurrentValueLabel(modeGroup, SWT.NULL,
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
        //min
        currDownMinSetting = createCurrentValueLabel(modeGroup, SWT.NULL,
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);
        //confidence
        currDownConfSetting = createCurrentValueLabel(modeGroup, SWT.NULL,
                SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING);


        //current download value row.
        Label upCurrVal = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        upCurrVal.setLayoutData(gridData);
        upCurrVal.setText("Upload:");
        //max
        currUpCapacitySetting = createCurrentValueLabel(modeGroup, SWT.NULL,
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
        //min
        currUpMinSetting = createCurrentValueLabel(modeGroup, SWT.NULL,
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT);
        //confidence
        currUpConfSetting = createCurrentValueLabel(modeGroup, SWT.NULL,
                SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING);
        

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=4;
        spacer.setLayoutData(gridData);


        //////////////////////////
        //DHT Ping Group
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


        //Add ChangeListeners to specific parameters.
        COConfigurationManager.addParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,
                speedManagerListener);
        COConfigurationManager.addParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,
                speedManagerListener);
        COConfigurationManager.addParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT,
                speedManagerListener);
        COConfigurationManager.addParameterListener(
                SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT,
                speedManagerListener);
        COConfigurationManager.addParameterListener(
                SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,
                speedManagerListener);
        COConfigurationManager.addParameterListener(
                SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,
                speedManagerListener);

        //These parameters enable and disable the "Add Comment" group.
        COConfigurationManager.addParameterListener(
                TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY,
                speedManagerListener);
        COConfigurationManager.addParameterListener(
                "Auto Upload Speed Seeding Enabled",
                speedManagerListener);


        return cSection;
    }

    /**
     *
     * @param group - Group this label belongs too.
     * @param SWTSetting - SWT setting for label.  ex. SWT.NULL
     * @param paramName - parameter name it shadows. Only works with the following params.
     * @return - Label
     */
    private Label createCurrentValueLabel(Group group, int SWTSetting, String paramName){

        Label retVal = new Label(group, SWT.NULL);
        String value="";

        try{
            if( paramName.startsWith("SpeedLimitMonitor.setting.") ){
                //read one of two confidence level settings which are Strings.
                String s = COConfigurationManager.getStringParameter(paramName);
                value = SpeedLimitConfidence.parseString(s).getInternationalizedString();
            }else{
                //read one of four limit settings which are Longs.
                long val = COConfigurationManager.getLongParameter(paramName);
                value = ""+val;
            }
        }catch(Throwable t){
            //ToDo: log this condition.
        }

        retVal.setText(value);
        GridData gd = new GridData();
        gd.horizontalIndent = 8;
        retVal.setLayoutData(gd);

        return retVal;
    }//createCurrentValueLabel

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

                //Need to also set "Auto Upload Speed Enabled" for DHT Pings and "Auto Speed Upload Version" to 2
                //ToDo: verify these parameters really need to be set. Recent changes might deprecate them!!
                //COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, true );
                //COConfigurationManager.setParameter( SpeedManagerImpl.CONFIG_VERSION, 2 );

            }else{
                //make it invisible.
                commentGroup.setEnabled(false);
                commentGroup.setVisible(false);

                //Set to V1, then set "Auto Upload Speed Enabled" to false.
                //ToDo: V1 will need a different set of parameters, to decoule from the global parameter.
                //ToDo: verify these parameters really need to be set. Recent changes might deprecate them!!
                //COConfigurationManager.setParameter( SpeedManagerImpl.CONFIG_VERSION, 1 );
                //COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, false );
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


    class SpeedManagerChangeListener implements ParameterListener {


        /**
         * Called, when a parameter has changed.
         * The listener could only react if the parameter name is relevant.
         * Or the listener can just read all parameters again.
         *
         * @param parameterName the name of the parameter that has changed
         */
        public void parameterChanged(String parameterName) {


            if(parameterName==null){
                return;
            }


            //check the parameters.
            if( parameterName.equalsIgnoreCase(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT ) )
            {
                updateLabel(currDownCapacitySetting, parameterName);

            }else if( parameterName.equalsIgnoreCase(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT ) )
            {
                updateLabel(currUpCapacitySetting, parameterName);

            }else if( parameterName.equalsIgnoreCase(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT ) )
            {
                updateLabel(currDownMinSetting, parameterName);

            }else if( parameterName.equalsIgnoreCase(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT ) )
            {
                updateLabel(currUpMinSetting, parameterName);

            }else if( parameterName.equalsIgnoreCase(
                    SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING ) )
            {
                updateLabelConfLimit( currDownConfSetting, parameterName);

            }else if( parameterName.equalsIgnoreCase(
                    SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING ) )
            {
                updateLabelConfLimit( currUpConfSetting, parameterName );
            }else if( parameterName.equalsIgnoreCase(
                    TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY ) )
            {
                updateAddCommentGroup();
            }else if( parameterName.equalsIgnoreCase(
                    "Auto Upload Speed Seeding Enabled" ) )
            {
                updateAddCommentGroup();
            }

        }//parameterChanged

        private void updateLabel(final Label lbl, final String param ){
            Utils.execSWTThread(new AERunnable(){
                public void runSupport(){
                    long setting = COConfigurationManager.getLongParameter(param);
                    lbl.setText(""+setting);
                }
            });
        }

        private void updateLabelConfLimit(final Label label, final String param ){
            Utils.execSWTThread(new AERunnable(){
                public void runSupport(){
                    String value = COConfigurationManager.getStringParameter(param);
                    String setting = SpeedLimitConfidence.parseString(value).getInternationalizedString();
                    label.setText(""+setting);
                }
            });
        }

        private void updateAddCommentGroup() {
            Utils.execSWTThread(new AERunnable(){
                public void runSupport(){
                    boolean enabled = COConfigurationManager.getBooleanParameter(
                            TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY);
                    boolean seedEnabled = COConfigurationManager.getBooleanParameter(
                            "Auto Upload Speed Seeding Enabled");

                    if( enabled || seedEnabled ){
                        commentGroup.setVisible(true);
                    }else{
                        commentGroup.setVisible(false);
                    }
                }//runSupport
            });
        }

    }//class SpeedManagerChangeListener

}
