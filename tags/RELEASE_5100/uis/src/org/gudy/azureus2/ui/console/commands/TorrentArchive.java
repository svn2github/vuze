/*
 * Created on 04/12/2004
 * Created by Paul Duran
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.console.ConsoleInput;

public class TorrentArchive extends TorrentCommand {

	public TorrentArchive()
	{
		super("torrent_archive", "tar", "Archiving");
	}

	protected boolean performCommand(ConsoleInput ci, DownloadManager dm, List args) {
		try {
			Download download = PluginCoreUtils.wrap( dm );
			
			if ( !download.canStubbify()){
				
				ci.out.println( "> Can't archive as torrent is not in archiveable state" );
				
			}else{
			
				download.stubbify();
				
				return( true );
			}
		} catch (DownloadRemovalVetoException e) {
			ci.out.println("> Veto when archiving torrent (" + e.getMessage() + ")");
			
		} catch (Exception e) {
			e.printStackTrace(ci.out);
			
		}
		return false;
	}

	public String getCommandDescriptions() {
		return("torrent_archive (<torrentoptions>)\tr\tArchive torrent(s).");
	}

}
