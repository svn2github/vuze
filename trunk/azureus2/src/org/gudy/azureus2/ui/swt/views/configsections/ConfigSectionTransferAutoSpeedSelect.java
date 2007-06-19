package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerImpl;


/**
 * Created on Jun 13, 2007
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

public class ConfigSectionTransferAutoSpeedSelect
    implements UISWTConfigSection
{

    private final String CFG_PREFIX = "ConfigView.section.transfer.autospeed.";

    StringListParameter versionList;

    BooleanParameter enableAutoSpeed;
    BooleanParameter enableAutoSpeedWhileSeeding;

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
        return "transfer.select";
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

    /**
     * Create your own configuration panel here.  It can be anything that inherits
     * from SWT's Composite class.
     * Please be mindfull of small screen resolutions
     *
     * @param parent The parent of your configuration panel
     * @return your configuration panel
     */

    public Composite configSectionCreate(final Composite parent) {

        GridData gridData;

        Composite cSection = new Composite(parent, SWT.NULL);

        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL|GridData.HORIZONTAL_ALIGN_FILL);
        cSection.setLayoutData(gridData);
        GridLayout subPanel = new GridLayout();
        subPanel.numColumns = 3;
        cSection.setLayout(subPanel);

        //V1, V2 ... drop down.

        //enable auto-speed beta
        ///////////////////////////////////
        // AutoSpeed Beta mode group
        ///////////////////////////////////
        //Beta-mode grouping.
        Group modeGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        modeGroup.setText("Auto-Speed selector");
        GridLayout modeLayout = new GridLayout();
        modeLayout.numColumns = 3;
        modeGroup.setLayout(modeLayout);
        gridData = new GridData();
        gridData.widthHint = 350;
        modeGroup.setLayoutData(gridData);

        //Need a drop down to select which method will be used.
        Label label = new Label(modeGroup, SWT.NULL);
        label.setText("Algorithm: ");
        gridData = new GridData();
        gridData.widthHint = 50;
        label.setLayoutData(gridData);

        //ToDo: get from the message bundle.
        String[] modeNames = {
                "Auto-Speed (classic)",
                "Auto-Speed (beta)"
        };
        
        String[] modes = {
                "1",
                "2"
        };

        //versionList = new StringListParameter(modeGroup,
        //        SpeedManagerImpl.CONFIG_VERSION_STR,
        //        "1",
        //        modeNames,modes,true);
        versionList = new StringListParameter(modeGroup,SpeedManagerImpl.CONFIG_VERSION_STR, modeNames, modes);
        long verNum = COConfigurationManager.getLongParameter( SpeedManagerImpl.CONFIG_VERSION );
        if( verNum==1 ){
            //SpeedManagerAlgorithmProviderV1
            versionList.setValue(modes[0]);
        }else if( verNum==2 ){
            //SpeedManagerAlgorithmProviderV2
            versionList.setValue(modes[1]);
        }else{
            //Default is currently version ...V1.
            versionList.setValue(modes[0]);
            //ToDo: log this condition.
        }

        versionList.addChangeListener( new ConvertToLongChangeListener() );


        //spacer
        Label spacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //To enable the beta.
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        gridData.horizontalSpan = 2;
        enableAutoSpeed = new BooleanParameter(modeGroup,
                TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY,CFG_PREFIX+"enableauto");
        enableAutoSpeed.setLayoutData(gridData);

        //enableAutoSpeed.addChangeListener( new GroupModeChangeListener() );

        Label spacerGroup = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        spacerGroup.setLayoutData(gridData);

        //AutoSpeed while seeding enabled.
        enableAutoSpeedWhileSeeding = new BooleanParameter(modeGroup,
                "Auto Upload Speed Seeding Enabled",CFG_PREFIX+"enableautoseeding");
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        gridData.horizontalSpan = 2;
        enableAutoSpeedWhileSeeding.setLayoutData(gridData);

		enableAutoSpeed.setAdditionalActionPerformer(
	    		new ChangeSelectionActionPerformer( enableAutoSpeedWhileSeeding.getControls(), true ));       


        //Add listeners to disable setting when needed.
                

        //spacer
        Label spacer2 = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer2.setLayoutData(gridData);

        /////////////////////////////////////////
        //Add group to link to Azureus Wiki page.
        /////////////////////////////////////////
        Group azWiki = new Group(cSection, SWT.WRAP);
        gridData = new GridData();
        gridData.widthHint = 350;
        azWiki.setLayoutData(gridData);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 1;
        azWiki.setLayout(layout);

        azWiki.setText(MessageText.getString("Utils.link.visit"));

        final Label linkLabel = new Label(azWiki, SWT.NULL);
        linkLabel.setText( "Azureus Wiki AutoSpeed (beta)" );
        linkLabel.setData("http://azureus.aelitis.com/wiki/index.php/Auto_Speed");
        linkLabel.setCursor(Cursors.handCursor);
        linkLabel.setForeground(Colors.blue);
        gridData = new GridData();
        gridData.horizontalIndent = 10;
        linkLabel.setLayoutData( gridData );
	    linkLabel.addMouseListener(new MouseAdapter() {
	      public void mouseDoubleClick(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	      public void mouseUp(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	    });


        return cSection;
    }//configSectionCreate


    class ConvertToLongChangeListener implements ParameterChangeListener{

        public void parameterChanged(Parameter p, boolean caused_internally) {

            try{
                //StringList doesn't work with Long parameters, so need to convert here.
                String str = COConfigurationManager.getStringParameter(SpeedManagerImpl.CONFIG_VERSION_STR);
                long asLong = Long.parseLong( str );
                COConfigurationManager.setParameter(SpeedManagerImpl.CONFIG_VERSION, asLong );
            }catch(Throwable t){
                //ToDo: log an error.
                COConfigurationManager.setParameter(SpeedManagerImpl.CONFIG_VERSION, 1);
            }

        }

        /**
         * An int parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param p -
         * @param toValue -
         */
        public void intParameterChanging(Parameter p, int toValue) {

        }

        /**
         * A boolean parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param p -
         * @param toValue -
         */
        public void booleanParameterChanging(Parameter p, boolean toValue) {

        }

        /**
         * A String parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param p -
         * @param toValue -
         */
        public void stringParameterChanging(Parameter p, String toValue) {

        }

        /**
         * A double/float parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param owner -
         * @param toValue -
         */
        public void floatParameterChanging(Parameter owner, double toValue) {
        }
    }

}
