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

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;

/**
 * Plugins which want to control the logic of where the default save location
 * for downloads (including for <tt>on-completion</tt> and <tt>on-removal</tt>
 * behaviour) can implement this class and register it through the
 * {@link DownloadManager#setSaveLocationManager(SaveLocationManager)}.
 * 
 * <p>
 * 
 * Each method here returns a {@link SaveLocationChange} object, which contains
 * instructions which allows both the download and the torrent to be moved and
 * renamed.
 * 
 * @since 3.0.5.3
 *
 * <p><b>Note:</b> This interface is intended to be implemented by plugins.</p>
 */
public interface SaveLocationManager {

	/**
	 * Return the location to move the download to when it first started (or
	 * return <tt>null</tt> to keep the download and torrent in the same
	 * location). 
	 * 
	 * @param download Download to handle.
	 * @return The new save location instructions.
	 */
	public SaveLocationChange onInitialization(Download download);

	/**
	 * Return the location to move the download to when it is completed (or
	 * return <tt>null</tt> to keep the download and torrent in the same
	 * location).
	 * 
	 * @param download Download to handle.
	 * @return The new save location instructions.
	 */
	public SaveLocationChange onCompletion(Download download);

	/**
	 * Return the location to move the download to when it is removed (or
	 * return <tt>null</tt> to keep the download and torrent in the same
	 * location).
	 * 
	 * @param download Download to handle.
	 * @return The new save location instructions.
	 */
	public SaveLocationChange onRemoval(Download download);
	
	/**
	 * Determines the location where the download should be stored at any
	 * arbitrary point. This might be used to manually force a download to
	 * be stored at an appropriate location.
	 * 
	 * <p>
	 * 
	 * @param download Download to handle.
	 * @return The new save location instructions.
	 */
	public SaveLocationChange recalculatePath(Download download);

}
