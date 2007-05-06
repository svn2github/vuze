/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.skin;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public interface SWTSkinObjectListener
{
	public static int EVENT_SHOW = 0;

	public static int EVENT_HIDE = 1;

	public static int EVENT_SELECT = 2;
	
	public static int EVENT_DESTROY = 3;

	public static String[] NAMES = {
		"Show",
		"Hide",
		"Select",
		"Destroy",
	};

	/**
	 * Called when an event occurs
	 * 
	 * @param skinObject skin object the event occurred on
	 * @param eventType EVENT_* constant
	 * @param params Any parameters the event needs to send you
	 */
	public Object eventOccured(SWTSkinObject skinObject, int eventType,
			Object params);
}
