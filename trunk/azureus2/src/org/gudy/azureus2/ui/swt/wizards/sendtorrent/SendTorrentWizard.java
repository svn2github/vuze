/*
 * Created on Apr 2, 2006 11:05:58 AM
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

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author TuxPaper
 * @created Apr 2, 2006
 *
 */
public class SendTorrentWizard extends Wizard {

	public final static int SHARE_BY_EMAIL = 0;

	public final static int SHARE_BY_HTML = 1;

	private final TOTorrent[] torrents;

	private int shareByMode = -1;

	/**
	 * @param _azureus_core
	 * @param display
	 */
	public SendTorrentWizard(AzureusCore _azureus_core, Display display,
			TOTorrent[] torrents) {
		super(_azureus_core, display, "sendTorrent.title");
		this.torrents = torrents;

		setFirstPanel(new SendTorrentDestinationPanel(this, null));
	}

	public TOTorrent[] getTorrents() {
		return torrents;
	}

	public int getShareByMode() {
		return shareByMode;
	}

	public void setShareByMode(int shareByMode) {
		this.shareByMode = shareByMode;
	}

}
