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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
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
import org.gudy.azureus2.ui.swt.components.LinkLabel;
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

		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL + GridData.VERTICAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		GridLayout advanced_layout = new GridLayout();
		cSection.setLayout(advanced_layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			Label label = new Label(cSection, SWT.WRAP);
			gridData = new GridData();
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
		
		LinkLabel linkLabel = new LinkLabel(cSection, gridData, CFG_PREFIX
				+ "encrypt.info.link", MessageText.getString(CFG_PREFIX + "url"));

		///////////////////////   ADVANCED SOCKET SETTINGS GROUP //////////
		
		Group gSocket = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(gSocket, CFG_PREFIX + "socket.group");
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gSocket.setLayoutData(gridData);
		GridLayout glayout = new GridLayout();
		glayout.numColumns = 3;
		gSocket.setLayout(glayout);

		
		Label lmaxout = new Label(gSocket, SWT.NULL);
    Messages.setLanguageText(lmaxout, "ConfigView.section.connection.network.max.simultaneous.connect.attempts");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    lmaxout.setLayoutData( gridData );

    IntParameter max_connects = new IntParameter(gSocket, "network.max.simultaneous.connect.attempts", 1, 100, false, false );    
    gridData = new GridData();
    gridData.widthHint = 30;
		max_connects.setLayoutData(gridData);
    
    
    Label lbind = new Label(gSocket, SWT.NULL);
    Messages.setLanguageText(lbind, "ConfigView.label.bindip");
    StringParameter bindip = new StringParameter(gSocket, "Bind IP", "");
    gridData = new GridData();
    gridData.widthHint = 100;
    gridData.horizontalSpan = 2;
    bindip.setLayoutData(gridData);
	
    
    Label lpbind = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lpbind, CFG_PREFIX + "bind_port");
		final IntParameter port_bind = new IntParameter(gSocket, "network.bind.local.port", 0, 65535, true, false );
		gridData = new GridData();
		gridData.widthHint = 40;
		gridData.horizontalSpan = 2;
		port_bind.setLayoutData(gridData);
		
		
		Label lmtu = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lmtu, CFG_PREFIX + "mtu");
		final IntParameter mtu_size = new IntParameter(gSocket,"network.tcp.mtu.size");
		mtu_size.setMaximumValue(512 * 1024);
		gridData = new GridData();
		gridData.widthHint = 40;
		gridData.horizontalSpan = 2;
		mtu_size.setLayoutData(gridData);


		Label lsend = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lsend, CFG_PREFIX + "SO_SNDBUF");
		final IntParameter SO_SNDBUF = new IntParameter(gSocket,	"network.tcp.socket.SO_SNDBUF");
		gridData = new GridData();
		gridData.widthHint = 40;
		gridData.horizontalSpan = 2;
		SO_SNDBUF.setLayoutData(gridData);


		Label lreceiv = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lreceiv, CFG_PREFIX + "SO_RCVBUF");
		final IntParameter SO_RCVBUF = new IntParameter(gSocket,	"network.tcp.socket.SO_RCVBUF");
		gridData = new GridData();
		gridData.widthHint = 40;
		gridData.horizontalSpan = 2;
		SO_RCVBUF.setLayoutData(gridData);
		

		Label ltos = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(ltos, CFG_PREFIX + "IPTOS");
		final StringParameter IPTOS = new StringParameter(gSocket,	"network.tcp.socket.IPTOS");
		gridData = new GridData();
		gridData.widthHint = 30;
		gridData.horizontalSpan = 2;
		IPTOS.setLayoutData(gridData);


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
					ConfigurationManager.getInstance().removeParameter(	"network.tcp.socket.IPTOS");

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
		
		//////////////////////////////////////////////////////////////////////////
		
		
		Group gCrypto = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(gCrypto, CFG_PREFIX + "encrypt.group");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gCrypto.setLayoutData(gridData);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		gCrypto.setLayout(layout);
		
		Label lcrypto = new Label(gCrypto, SWT.WRAP);
		Messages.setLanguageText(lcrypto, CFG_PREFIX + "encrypt.info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.widthHint = 200;  // needed for wrap
		lcrypto.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		linkLabel = new LinkLabel(gCrypto, gridData, CFG_PREFIX
				+ "encrypt.info.link",
				"http://azureus.aelitis.com/wiki/index.php/Message_Stream_Encryption");
		
		final BooleanParameter require = new BooleanParameter(gCrypto,	"network.transport.encrypted.require", false, CFG_PREFIX + "require_encrypted_transport");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		require.setLayoutData(gridData);
		
		String[] encryption_types = { "Plain", "RC4" };
		String dropLabels[] = new String[encryption_types.length];
		String dropValues[] = new String[encryption_types.length];
		for (int i = 0; i < encryption_types.length; i++) {
			dropLabels[i] = encryption_types[i];
			dropValues[i] = encryption_types[i];
		}
		
		Composite cEncryptLevel = new Composite(gCrypto, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		cEncryptLevel.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cEncryptLevel.setLayout(layout);
		
		Label lmin = new Label(cEncryptLevel, SWT.NULL);
		Messages.setLanguageText(lmin, CFG_PREFIX + "min_encryption_level");
		final StringListParameter min_level = new StringListParameter(cEncryptLevel,	"network.transport.encrypted.min_level", encryption_types[1], dropLabels, dropValues);
		
		Label lcryptofb = new Label(gCrypto, SWT.WRAP);
		Messages.setLanguageText(lcryptofb, CFG_PREFIX + "encrypt.fallback_info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.widthHint = 200;  // needed for wrap
		lcryptofb.setLayoutData(gridData);

		BooleanParameter fallback_outgoing = new BooleanParameter(gCrypto, "network.transport.encrypted.fallback.outgoing", false, CFG_PREFIX + "encrypt.fallback_outgoing");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		fallback_outgoing.setLayoutData(gridData);
		
		BooleanParameter fallback_incoming = new BooleanParameter(gCrypto, "network.transport.encrypted.fallback.incoming", false, CFG_PREFIX + "encrypt.fallback_incoming");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		fallback_incoming.setLayoutData(gridData);
		
		Control[] encryption_controls = {	min_level.getControl(), lmin, lcryptofb, fallback_outgoing.getControl(), fallback_incoming.getControl() };
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
