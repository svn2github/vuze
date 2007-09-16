/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.utils;

import java.util.ArrayList;

import org.gudy.azureus2.core3.util.HashWrapper;

/**
 * @author TuxPaper
 * @created Sep 15, 2007
 *
 */
public class PlayNowList
{
	private static ArrayList playNowList = new ArrayList();

	/**
	 * @param hw
	 *
	 * @since 3.0.1.7
	 */
	public static void add(HashWrapper hw) {
		playNowList.add(hw);
	}

	public static void remove(HashWrapper hw) {
		playNowList.remove(hw);
	}
	
	public static boolean contains(HashWrapper hw) {
		return playNowList.contains(hw);
	}
}
