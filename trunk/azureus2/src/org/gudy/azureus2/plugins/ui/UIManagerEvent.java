/*
 * Created on 11-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.plugins.ui;

public interface 
UIManagerEvent 
{
	public static final int ET_SHOW_TEXT_MESSAGE				= 1;		// data is String[] - title, message, text
	public static final int ET_OPEN_TORRENT_VIA_FILE			= 2;		// data is File 
	public static final int ET_OPEN_TORRENT_VIA_URL				= 3;		// data is URL[] - { torrent_url, referrer url}  
	public static final int ET_PLUGIN_VIEW_MODEL_CREATED		= 4;		// data is PluginViewModel (or subtype)
	public static final int ET_PLUGIN_CONFIG_MODEL_CREATED		= 5;		// data is PluginConfigModel (or subtype)
	public static final int ET_COPY_TO_CLIPBOARD				= 6;		// data is String
	public static final int ET_PLUGIN_VIEW_MODEL_DESTROYED		= 7;		// data is PluginViewModel (or subtype)
	public static final int ET_PLUGIN_CONFIG_MODEL_DESTROYED	= 8;		// data is PluginConfigModel (or subtype)
	public static final int ET_OPEN_URL							= 9;		// data is URL

	public int
	getType();
	
	public Object
	getData();
}
