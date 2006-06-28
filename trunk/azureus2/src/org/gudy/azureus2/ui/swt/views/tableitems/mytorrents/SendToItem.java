/*
 * Created on Jun 28, 2006 2:18:34 PM
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
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.wizards.sendtorrent.SendTorrentWizard;

import com.aelitis.azureus.core.AzureusCoreFactory;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;

/**
 * @author TuxPaper
 * @created Jun 28, 2006
 *
 */
public class SendToItem extends CoreTableColumn implements
		TableCellAddedListener, TableCellMouseListener
{
	/** Default Constructor */
	public SendToItem(String sTableID) {
		super("sendto", sTableID);

		// Position cheat: move to before name column
		String sItemPrefix = "Table." + sTableID + ".name";
		int iPosition = COConfigurationManager.getIntParameter(sItemPrefix
				+ ".position", 3);
		if (iPosition == -1) {
			iPosition = POSITION_LAST;
		}

		initializeAsGraphic(iPosition, 18);
	}

	public void cellAdded(TableCell cell) {
		((TableCellCore) cell).setGraphic(ImageRepository.getImage("sendto-small"));
	}

	public void cellMouseTrigger(TableCellMouseEvent event) {
		DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		if (dm == null) {
			return;
		}

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
			new SendTorrentWizard(AzureusCoreFactory.getSingleton(),
					Display.getCurrent(), new TOTorrent[] { dm.getTorrent() });
		}
	}
}
