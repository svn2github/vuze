/*
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionConnectionAdvanced implements UISWTConfigSection {

	private final String CFG_PREFIX = "ConfigView.section.connection.advanced.";
	
	private final int REQUIRED_MODE = 2;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_CONNECTION;
	}

	public String configSectionGetName() {
		return "connection.advanced";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;

		Composite cSection = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		GridLayout advanced_layout = new GridLayout();
		advanced_layout.numColumns = 2;
		cSection.setLayout(advanced_layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			Label label = new Label(cSection, SWT.WRAP);
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			label.setLayoutData(gridData);

			final String[] modeKeys = { "ConfigView.section.mode.beginner",
					"ConfigView.section.mode.intermediate",
					"ConfigView.section.mode.advanced" };

			String param1, param2;
			if (REQUIRED_MODE < modeKeys.length)
				param1 = MessageText.getString(modeKeys[REQUIRED_MODE]);
			else
				param1 = String.valueOf(REQUIRED_MODE);
					
			if (userMode < modeKeys.length)
				param2 = MessageText.getString(modeKeys[userMode]);
			else
				param2 = String.valueOf(userMode);

			label.setText(MessageText.getString("ConfigView.notAvailableForMode",
					new String[] { param1, param2 } ));

			return cSection;
		}

		///////////////////////   ADVANCED NETWORK SETTINGS GROUP //////////

		final BooleanParameter enable_advanced = new BooleanParameter(cSection,
				"config.connection.show_advanced", false);
		Messages.setLanguageText(enable_advanced, "ConfigView.section."
				+ configSectionGetName());
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		enable_advanced.setLayoutData(gridData);

		///////////////////////

		GridData advanced_grid_data;

		final IntParameter mtu_size = new IntParameter(cSection,
				"network.tcp.mtu.size");
		mtu_size.setMaximumValue(512 * 1024);
		advanced_grid_data = new GridData();
		advanced_grid_data.widthHint = 40;
		mtu_size.setLayoutData(advanced_grid_data);
		Label lmtu = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(lmtu, CFG_PREFIX + "mtu");

		final IntParameter SO_SNDBUF = new IntParameter(cSection,
				"network.tcp.socket.SO_SNDBUF");
		advanced_grid_data = new GridData();
		advanced_grid_data.widthHint = 40;
		SO_SNDBUF.setLayoutData(advanced_grid_data);
		Label lsend = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(lsend, CFG_PREFIX + "SO_SNDBUF");

		final IntParameter SO_RCVBUF = new IntParameter(cSection,
				"network.tcp.socket.SO_RCVBUF");
		advanced_grid_data = new GridData();
		advanced_grid_data.widthHint = 40;
		SO_RCVBUF.setLayoutData(advanced_grid_data);
		Label lreceiv = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(lreceiv, CFG_PREFIX + "SO_RCVBUF");

		final StringParameter IPTOS = new StringParameter(cSection,
				"network.tcp.socket.IPTOS");
		gridData = new GridData();
		gridData.widthHint = 30;
		IPTOS.setLayoutData(gridData);
		Label ltos = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(ltos, CFG_PREFIX + "IPTOS");

		//do simple input verification, and registry key setting for TOS field
		IPTOS.addChangeListener(new ParameterChangeListener() {

			final Color obg = IPTOS.getControl().getBackground();

			final Color ofg = IPTOS.getControl().getForeground();

			public void parameterChanged(Parameter p, boolean caused_internally) {
				String raw = IPTOS.getValue();
				int value = -1;

				try {
					value = Integer.decode(raw).intValue();
				} catch (Throwable t) {
				}

				if (value < 0 || value > 255) { //invalid or no value entered
					ConfigurationManager.getInstance().removeParameter(
							"network.tcp.socket.IPTOS");

					if (raw != null && raw.length() > 0) { //error state
						IPTOS.getControl().setBackground(Colors.red);
						IPTOS.getControl().setForeground(Colors.white);
					} else { //no value state
						IPTOS.getControl().setBackground(obg);
						IPTOS.getControl().setForeground(ofg);
					}

					enableTOSRegistrySetting(false); //disable registry setting if necessary
				} else { //passes test
					IPTOS.getControl().setBackground(obg);
					IPTOS.getControl().setForeground(ofg);

					enableTOSRegistrySetting(true); //enable registry setting if necessary
				}
			}
		});

		Control[] advanced_controls = { mtu_size.getControl(), lmtu,
				SO_SNDBUF.getControl(), lsend, SO_RCVBUF.getControl(), lreceiv,
				IPTOS.getControl(), ltos };

		enable_advanced.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(advanced_controls));
		enable_advanced.setAdditionalActionPerformer(new IAdditionalActionPerformer() {
					boolean checked;

					public void performAction() {
						if (!checked) { //revert all advanced options back to defaults
							ConfigurationManager.getInstance().removeParameter(
									"network.tcp.mtu.size");
							ConfigurationManager.getInstance().removeParameter(
									"network.tcp.socket.SO_SNDBUF");
							ConfigurationManager.getInstance().removeParameter(
									"network.tcp.socket.SO_RCVBUF");
							ConfigurationManager.getInstance().removeParameter(
									"network.tcp.socket.IPTOS");
						}
					}

					public void setSelected(boolean selected) {
						checked = selected;
					}

					public void setIntValue(int value) {
					}

					public void setStringValue(String value) {
					}
				});

		
		
		final BooleanParameter require = new BooleanParameter(cSection,	"network.transport.encrypted.require", false, CFG_PREFIX + "require_encrypted_transport");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		require.setLayoutData(gridData);
		
		
		String[] encryption_types = { "XOR", "RC4", "AES" };
		String dropLabels[] = new String[encryption_types.length];
		String dropValues[] = new String[encryption_types.length];
		for (int i = 0; i < encryption_types.length; i++) {
			dropLabels[i] = encryption_types[i];
			dropValues[i] = encryption_types[i];
		}
		
		final StringListParameter min_level = new StringListParameter(cSection,	"network.transport.encrypted.min_level", encryption_types[0], dropLabels, dropValues);
		Label lmin = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(lmin, CFG_PREFIX + "min_encryption_level");
		
		
		Control[] encryption_controls = {	min_level.getControl(), lmin };
		require.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(encryption_controls));
		
		
		///////////////////////   

		return cSection;

	}

	private void enableTOSRegistrySetting(boolean enable) {
		PlatformManager mgr = PlatformManagerFactory.getPlatformManager();

		if (mgr.hasCapability(PlatformManagerCapabilities.SetTCPTOSEnabled)) { 
			//see http://azureus.aelitis.com/wiki/index.php/AdvancedNetworkSettings
			try {
				mgr.setTCPTOSEnabled(enable);
			} catch (PlatformManagerException pe) {
				Debug.printStackTrace(pe);
			}
		}
	}

}
