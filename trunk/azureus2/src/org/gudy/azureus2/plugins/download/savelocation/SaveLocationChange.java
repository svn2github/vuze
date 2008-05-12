/*
 * Created on 12 May 2008
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.plugins.download.savelocation;

import java.io.File;

/**
 * Used by {@link SaveLocationManager} - you create an instance, set the
 * attributes here and return the value.
 * 
 * @since 3.0.5.3
 */
public class SaveLocationChange {
	public File download_location = null;
	public String download_name = null;
	public File torrent_location = null;
	public String torrent_name = null;
	
	public String toString() {
		StringBuffer res = new StringBuffer("SaveLocationChange: ");
		res.append("DL-LOC=");
		res.append(download_location);
		res.append(", DL-NAME=");
		res.append(download_name);
		res.append(", TOR-LOC=");
		res.append(torrent_location);
		res.append(", TOR-NAME=");
		res.append(torrent_name);
		return res.toString();
	}
}
