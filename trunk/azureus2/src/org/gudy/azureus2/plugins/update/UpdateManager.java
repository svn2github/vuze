/*
 * Created on 07-May-2004
 * Created by Paul Gardner
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.plugins.update;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public interface 
UpdateManager 
{
		/**
		 * Add an update with a single downloader
		 * @param name
		 * @param downloader
		 * @param restart_required
		 */
	
	public void
	addUpdate(
		String				name,
		ResourceDownloader	downloader,
		int					restart_required );
	
		/**
		 * Add an update with a number of downloaders
		 * @param name
		 * @param downloaders
		 * @param restart_required
		 */
	
	public void
	addUpdate(
		String					name,
		ResourceDownloader[]	downloaders,
		int						restart_required );
	
		/**
		 * get the currently defined updates
		 * @return
		 */
	
	public Update[]
	getUpdates();
	
		/**
		 * add a listener that will be informed when new updates are added
		 * @param l
		 */
	
	public void
	addListener(
		UpdateManagerListener	l );
	
		/**
		 * remove the afore mentioned listener
		 * @param l
		 */
	
	public void
	removeListener(
		UpdateManagerListener	l );
}
