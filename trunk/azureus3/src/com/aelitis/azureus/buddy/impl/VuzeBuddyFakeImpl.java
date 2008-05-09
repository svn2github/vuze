/**
 * Created on May 9, 2008
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

package com.aelitis.azureus.buddy.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Buddy Object not linked to an buddypluginbuddy
 * 
 * @author TuxPaper
 * @created May 9, 2008
 *
 */
public class VuzeBuddyFakeImpl
	extends VuzeBuddyImpl
{
	private List pks = new ArrayList();

	public void addPublicKey(String pk) {
		if (!pks.contains(pk)) {
			pks.add(pk);
		}
	}
	
	// @see com.aelitis.azureus.buddy.impl.VuzeBuddyImpl#removePublicKey(java.lang.String)
	public void removePublicKey(String pk) {
		pks.remove(pk);
	}
	
	// @see com.aelitis.azureus.buddy.impl.VuzeBuddyImpl#getPublicKeys()
	public String[] getPublicKeys() {
		return (String[]) pks.toArray(new String[pks.size()]);
	}
}
