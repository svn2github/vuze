package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.config.StringListParameter;
import org.gudy.azureus2.ui.swt.config.ParameterChangeListener;
import org.gudy.azureus2.ui.swt.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderV2;
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
        modeGroup.setText("AutoSpeed selector");
        GridLayout modeLayout = new GridLayout();
        modeLayout.numColumns = 3;
        modeGroup.setLayout(modeLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        modeGroup.setLayoutData(gridData);

        //Need a drop down to select which method will be used.
        Label label = new Label(modeGroup, SWT.NULL);
        label.setText("algorithm: ");
        gridData = new GridData();
        gridData.widthHint = 50;
        label.setLayoutData(gridData);

        //ToDo: get from the message bundle.
        String[] modeNames = {
                "AutoSpeed (classic)",
                "Azureus SpeedSense (beta)"
        };
        String[] modes = {
                "1",
                "2"
        };

        versionList = new StringListParameter(modeGroup,
                SpeedManagerImpl.CONFIG_VERSION_STR,
                "1",
                modeNames,modes,true);

        versionList.addChangeListener( new ConvertToLongChangeListener() );


        //spacer
        Label spacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);


        //To enable the beta.
        gridData = new GridData();
        gridData.widthHint = 50;
        gridData.horizontalAlignment = GridData.END;
        enableAutoSpeed = new BooleanParameter(modeGroup,
                TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY);
        enableAutoSpeed.setLayoutData(gridData);

        //enableAutoSpeed.addChangeListener( new GroupModeChangeListener() );

        Label enableLabel = new Label(modeGroup, SWT.NULL);
        enableLabel.setText("Enable Auto Speed Adjustments");
        gridData = new GridData();
        gridData.widthHint = 40;
        cSection.setLayoutData(gridData);

        BooleanParameter enable_au_seeding = new BooleanParameter(
				modeGroup, "Auto Upload Speed Seeding Enabled",
				CFG_PREFIX + "enableautoseeding" );
		gridData = new GridData();
		gridData.horizontalSpan = 2;
        //gridData.horizontalAlignment
        enable_au_seeding.setLayoutData(gridData);

//		enableAutoSpeed.setAdditionalActionPerformer(
//	    		new ChangeSelectionActionPerformer( enable_au_seeding.getControls(), true ));       


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
