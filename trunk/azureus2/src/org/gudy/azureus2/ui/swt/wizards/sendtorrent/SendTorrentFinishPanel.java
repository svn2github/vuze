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

import java.net.URLEncoder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.UrlUtils;
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
public class SendTorrentFinishPanel extends AbstractWizardPanel
{

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
		final TOTorrent[] torrents = sendWiz.getTorrents();
		final String[] names = new String[torrents.length];

		wizard.setTitle(MessageText.getString("sendTorrent.finish.title"));

		Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		rootPanel.setLayout(layout);

		Composite panel = new Composite(rootPanel, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = sendWiz.getShareByMode() == SendTorrentWizard.SHARE_BY_EMAIL
				? 2 : 1;
		panel.setLayout(layout);

		Label lblInstructions = new Label(panel, SWT.WRAP);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = layout.numColumns;
		lblInstructions.setLayoutData(gridData);
		if (sendWiz.getShareByMode() == SendTorrentWizard.SHARE_BY_EMAIL)
			Messages.setLanguageText(lblInstructions, "sendTorrent.finish.byEmail");
		else
			Messages.setLanguageText(lblInstructions, "sendTorrent.finish.byHTML");

		final Text txtCode = new Text(panel, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = layout.numColumns;
		txtCode.setLayoutData(gridData);
		txtCode.setBackground(panel.getBackground());

		Button btnToClipboard = new Button(panel, SWT.PUSH);
		if (sendWiz.getShareByMode() != SendTorrentWizard.SHARE_BY_EMAIL) {
			gridData.horizontalSpan = layout.numColumns;
		}
		Messages.setLanguageText(btnToClipboard,
				"sendTorrent.finish.button.toClipboard");
		btnToClipboard.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ClipboardCopy.copyToClipBoard(txtCode.getText());
			}
		});

		sendWiz.switchToClose();

		GlobalManager gm = sendWiz.getAzureusCore().getGlobalManager();

		for (int i = 0; i < torrents.length; i++) {
			TOTorrent torrent = torrents[i];

			names[i] = null;
			if (gm != null) {
				DownloadManager downloadManager = gm.getDownloadManager(torrent);
				if (downloadManager != null) {
					names[i] = downloadManager.getDisplayName();
				}
			}

			if (names[i] == null) {
				names[i] = new String(torrent.getName());
			}
		}

		if (sendWiz.getShareByMode() == SendTorrentWizard.SHARE_BY_EMAIL) {
			final String[] urls = new String[torrents.length];

			String s = "";
			for (int i = 0; i < torrents.length; i++) {
				TOTorrent torrent = torrents[i];

				if (i > 0) {
					s += "\n";
				}

				try {
					urls[i] = "http://getazureus.com/getContent.php?magnet=";
					urls[i] += Base32.encode(torrent.getHash());
					urls[i] += "&name=" + UrlUtils.encode(names[i]);
					s += urls[i];
				} catch (TOTorrentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			txtCode.setText(s);

			Button btnStartEMail = new Button(panel, SWT.PUSH);
			Messages.setLanguageText(btnStartEMail,
					"sendTorrent.finish.button.startEMail");
			btnStartEMail.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					String prefix = names.length == 1 ? "sendTorrent.one.email."
							: "sendTorrent.multi.email.";

					String allNames = names[0];
					for (int i = 1; i < names.length; i++) {
						allNames += ", " + names[i];
					}
					String subject = MessageText.getString(prefix + "subject",
							new String[] { allNames });

					String body;

					if (names.length == 1) {
						body = MessageText.getString(prefix + "body", new String[] {
								names[0],
								urls[0] });
					} else {

						if (MessageText.getString(prefix + "body.top", "") == "") {
							body = "";
						} else {
							body = MessageText.getString(prefix + "body.top", new String[] {
									allNames,
									txtCode.getText() });
						}

						if (MessageText.getString(prefix + "body.eachTorrent", "") != "") {
							for (int i = 0; i < names.length; i++) {
								body += MessageText.getString(prefix + "body.eachTorrent",
										new String[] { names[i], urls[i] });
							}
						}

						if (MessageText.getString(prefix + "body.bottom", "") != "") {
							body += MessageText.getString(prefix + "body.bottom",
									new String[] { allNames, txtCode.getText() });
						}
					}
					//ClipboardCopy.copyToClipBoard("mailto:?subject="
					//		+ UrlUtils.encode(subject) + "&body=" + UrlUtils.encode(body));

					Program.launch("mailto:?subject=" + UrlUtils.encode(subject)
							+ "&body=" + UrlUtils.encode(body));
				}
			});

		} else {
			// web

			String s = "";
			for (int i = 0; i < torrents.length; i++) {
				TOTorrent torrent = torrents[i];

				if (i > 0) {
					s += "<br>\n";
				}

				try {
					s += "<script language=\"javascript\" src=\"http://getazureus.com/js/azdetect.js\"></script>\n";
					s += "<a href=\"javascript:detectAndLoadMagnet('"
							+ Base32.encode(torrent.getHash()) + "');\">\n";
					s += "<img src=\"http://getazureus.com/js/az.png\" border=\"0\">\n";
					s += "Download " + names[i] + "</a>\n";
				} catch (TOTorrentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				txtCode.setText(s);
			}
		}
	}

	public void finish() {
		wizard.getWizardWindow().close();
	}

}
