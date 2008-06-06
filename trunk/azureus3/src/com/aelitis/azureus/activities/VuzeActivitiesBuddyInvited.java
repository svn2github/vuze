/**
 * Created on Jun 6, 2008
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

package com.aelitis.azureus.activities;

import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * @author TuxPaper
 * @created Jun 6, 2008
 *
 */
public class VuzeActivitiesBuddyInvited
	extends VuzeActivitiesEntry
{

	public VuzeActivitiesBuddyInvited(List displayNames) {
		String names = "";
		for (Iterator iter = displayNames.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			if (names.length() > 0) {
				names += ", ";
			}
			names += name;
		}
		String id = "v3.activity.buddy-invited";
		if (displayNames.size() > 1) {
			id += ".multi";
		}

		String text = MessageText.getString(id, new String[] {
			names
		});

		setText(text);
		setTypeID(VuzeActivitiesConstants.TYPEID_BUDDYINVITED, true);
		setID(VuzeActivitiesConstants.TYPEID_BUDDYINVITED + "-"
				+ SystemTime.getCurrentTime());
	}
}
