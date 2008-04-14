/**
 * Created on Apr 14, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.aelitis.azureus.ui.swt.buddy.impl;

import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.buddy.VuzeBuddy;

/**
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public class VuzeBuddyUtils
{
	/**
	 * Creates a random buddy.  Not actually stored anywhere..
	 * 
	 * 
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	
	public static VuzeBuddy createRandomBuddy() {
		int x = (int) (Math.random() * 10000);
		VuzeBuddySWTImpl buddy = new VuzeBuddySWTImpl("StupidKey" + x);
		buddy.setLoginID("Login" + x);
		buddy.setDisplayName("Mr Random " + x);
		buddy.setAvatarImage(ImageRepository.getImage("azureus128"));
		
		return buddy;
	}
}
