/**
 * Created on Apr 23, 2008
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
 
package com.aelitis.azureus.buddy;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;

/**
 * @author TuxPaper
 * @created Apr 23, 2008
 *
 */
public interface VuzeBuddyListener
{
	/**
	 * A buddy has been removed from the {@link VuzeBuddyManager}
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	public void buddyRemoved(VuzeBuddy buddy);

	/**
	 * A buddy has been added to {@link VuzeBuddyManager}
	 * 
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	public void buddyAdded(VuzeBuddy buddy, int position);

	/**
	 * A buddy's information has changed (not including position)
	 * 
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	public void buddyChanged(VuzeBuddy buddy);

	/**
	 * The order of the Buddy List has changed 
	 *
	 * @since 3.0.5.3
	 */
	public void buddyOrderChanged();
}
