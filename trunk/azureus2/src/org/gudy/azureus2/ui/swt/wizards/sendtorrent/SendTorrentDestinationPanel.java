/*
 * Created on Apr 2, 2006 11:10:59 AM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.wizards.sendtorrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author TuxPaper
 * @created Apr 2, 2006
 *
 */
public class SendTorrentDestinationPanel extends AbstractWizardPanel
{

	/**
	 * @param wizard
	 * @param previousPanel
	 */
	public SendTorrentDestinationPanel(Wizard wizard, IWizardPanel previousPanel) {
		super(wizard, previousPanel);
	}

	public void show() {
		final SendTorrentWizard sendWiz = (SendTorrentWizard) wizard;

		TOTorrent[] torrents = sendWiz.getTorrents();

		wizard.setTitle(MessageText.getString("sendTorrent.destination.title",
				new String[] { "" + torrents.length }));

		Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

		Composite panel = new Composite(rootPanel, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);
		layout = new GridLayout();
		panel.setLayout(layout);

		Button btnByEmail = new Button(panel, SWT.RADIO);
		Messages.setLanguageText(btnByEmail, "sendTorrent.destination.byEmail");
		btnByEmail.setData("mode", new Integer(SendTorrentWizard.SHARE_BY_EMAIL));
		btnByEmail.setSelection(true);

		Button btnByHTML = new Button(panel, SWT.RADIO);
		Messages.setLanguageText(btnByHTML, "sendTorrent.destination.byHTML");
		btnByHTML.setData("mode", new Integer(SendTorrentWizard.SHARE_BY_HTML));

		Listener modeListener = new Listener() {
			public void handleEvent(Event e) {
				int mode = ((Integer) e.widget.getData("mode")).intValue();
				sendWiz.setShareByMode(mode);
			}
		};
		sendWiz.setShareByMode(SendTorrentWizard.SHARE_BY_EMAIL);

		btnByEmail.addListener(SWT.Selection, modeListener);
		btnByHTML.addListener(SWT.Selection, modeListener);

		wizard.setFinishEnabled(true);
	}

	public boolean isNextEnabled() {
		return true;
	}

	public IWizardPanel getNextPanel() {
		return new SendTorrentFinishPanel(wizard, this);
	}

	public IWizardPanel getFinishPanel() {
		return new SendTorrentFinishPanel(wizard, this);
	}
}
