/*
 * File    : ConfigPanelServer.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionConnection implements UISWTConfigSection {
	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_CONNECTION;
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public Composite configSectionCreate(final Composite parent) {
		Label label;
		GridData gridData;
		GridLayout layout;
		Composite cMiniArea;

		int userMode = COConfigurationManager.getIntParameter("User Mode");

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		cSection.setLayout(layout);

		///////////////////////

		cMiniArea = new Composite(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		cMiniArea.setLayout(layout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cMiniArea.setLayoutData(gridData);
		
		
		label = new Label(cMiniArea, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.label.serverport");
		gridData = new GridData();
		label.setLayoutData(gridData);

		final IntParameter tcplisten = new IntParameter(cMiniArea,
				"TCP.Listen.Port", 1, 65535, false, false);
		gridData = new GridData();
		gridData.widthHint = 40;
		tcplisten.setLayoutData(gridData);

		tcplisten.addChangeListener(new ParameterChangeListener() {
			public void parameterChanged(Parameter p, boolean caused_internally) {
				int val = tcplisten.getValue();

				if (val == 6880 || val == 6881) {
					tcplisten.setValue(6881);
				}
			}
		});

		if (userMode < 2) {
			// wiki link
			label = new Label(cSection, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData(gridData);
			label.setText(MessageText.getString("Utils.link.visit") + ":");

			final Label linkLabel = new Label(cSection, SWT.NULL);
			linkLabel.setText(MessageText
					.getString("ConfigView.section.connection.serverport.wiki"));
			linkLabel
					.setData("http://azureus.aelitis.com/wiki/index.php?title=Why_ports_like_6881_are_no_good_choice");
			linkLabel.setCursor(Cursors.handCursor);
			linkLabel.setForeground(Colors.blue);
			gridData = new GridData();
			linkLabel.setLayoutData(gridData);
			linkLabel.addMouseListener(new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent arg0) {
					Program.launch((String) ((Label) arg0.widget).getData());
				}

				public void mouseDown(MouseEvent arg0) {
					Program.launch((String) ((Label) arg0.widget).getData());
				}
			});
		}

		if (userMode > 0) {
			/////////////////////// PEER SOURCES GROUP ///////////////////

			Group peer_sources_group = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(peer_sources_group,
					"ConfigView.section.connection.group.peersources");
			GridLayout peer_sources_layout = new GridLayout();
			peer_sources_group.setLayout(peer_sources_layout);

			gridData = new GridData();
			peer_sources_group.setLayoutData(gridData);

			label = new Label(peer_sources_group, SWT.WRAP);
			Messages.setLanguageText(label,
					"ConfigView.section.connection.group.peersources.info");
			gridData = new GridData();
			label.setLayoutData(gridData);

			for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {

				String p = PEPeerSource.PS_SOURCES[i];

				String config_name = "Peer Source Selection Default." + p;
				String msg_text = "ConfigView.section.connection.peersource." + p;

				BooleanParameter peer_source = new BooleanParameter(peer_sources_group,
						config_name, msg_text);

				gridData = new GridData();
				peer_source.setLayoutData(gridData);
			}


			//////////////////////

			if (userMode > 1) {

				/////////////////////// NETWORKS GROUP ///////////////////

				Group networks_group = new Group(cSection, SWT.NULL);
				Messages.setLanguageText(networks_group,
						"ConfigView.section.connection.group.networks");
				GridLayout networks_layout = new GridLayout();
				networks_group.setLayout(networks_layout);

				gridData = new GridData();
				networks_group.setLayoutData(gridData);

				label = new Label(networks_group, SWT.NULL);
				Messages.setLanguageText(label,
						"ConfigView.section.connection.group.networks.info");
				gridData = new GridData();
				label.setLayoutData(gridData);

				for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {

					String nn = AENetworkClassifier.AT_NETWORKS[i];

					String config_name = "Network Selection Default." + nn;
					String msg_text = "ConfigView.section.connection.networks." + nn;

					BooleanParameter network = new BooleanParameter(networks_group,
							config_name, msg_text);

					gridData = new GridData();
					network.setLayoutData(gridData);
				}

				label = new Label(networks_group, SWT.NULL);
				gridData = new GridData();
				label.setLayoutData(gridData);

				BooleanParameter network_prompt = new BooleanParameter(networks_group,
						"Network Selection Prompt",
						"ConfigView.section.connection.networks.prompt");

				gridData = new GridData();
				network_prompt.setLayoutData(gridData);

			} // end userMode>1
		} // end userMode>0

		///////////////////////   

		return cSection;

	}
}
