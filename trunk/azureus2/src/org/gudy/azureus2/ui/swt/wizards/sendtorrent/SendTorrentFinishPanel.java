/*
 * Created on Apr 4, 2006 11:24:30 AM
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author TuxPaper
 * @created Apr 4, 2006
 *
 */
public class SendTorrentFinishPanel extends AbstractWizardPanel {

	/**
	 * @param wizard
	 * @param previousPanel
	 */
	public SendTorrentFinishPanel(Wizard wizard, IWizardPanel previousPanel) {
		super(wizard, previousPanel);
	}

	public void show() {
		GridData gridData;
		final SendTorrentWizard sendWiz = (SendTorrentWizard) wizard;

		wizard.setTitle(MessageText.getString("sendTorrent.finish.title"));

		Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

		Composite panel = new Composite(rootPanel, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);
		layout = new GridLayout();
		panel.setLayout(layout);

		Label lblInstructions = new Label(panel, SWT.WRAP);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		lblInstructions.setLayoutData(gridData);
		if (sendWiz.getShareByMode() == SendTorrentWizard.SHARE_BY_EMAIL)
			Messages.setLanguageText(lblInstructions, "sendTorrent.finish.byEmail");
		else
			Messages.setLanguageText(lblInstructions, "sendTorrent.finish.byHTML");

		final Text txtCode = new Text(panel, SWT.READ_ONLY + SWT.MULTI);
		gridData = new GridData(GridData.FILL_BOTH);
		txtCode.setLayoutData(gridData);
		txtCode.setText("Silly beta-tester, this option isn't complete!\n\n"
				+ "Gudy will tell me what to put here");
		txtCode.setBackground(panel.getBackground());

		Button btnToClipboard = new Button(panel, SWT.PUSH);
		Messages.setLanguageText(btnToClipboard,
				"sendTorrent.finish.button.toClipboard");
		btnToClipboard.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ClipboardCopy.copyToClipBoard(txtCode.getText());
			}
		});

		sendWiz.switchToClose();
	}

	public void finish() {
		wizard.getWizardWindow().close();
	}

}
