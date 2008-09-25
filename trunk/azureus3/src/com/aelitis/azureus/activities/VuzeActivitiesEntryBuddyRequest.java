/**
 * Created on Apr 15, 2008
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

import org.gudy.azureus2.core3.internat.MessageText;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created Apr 15, 2008
 *
 */
public class VuzeActivitiesEntryBuddyRequest
	extends VuzeActivitiesEntryBuddy
{
	private String urlAccept;

	public String getUrlAccept() {
		return urlAccept;
	}

	public VuzeActivitiesEntryBuddyRequest() {
		super();
	}

	public void init(VuzeBuddy buddy, String acceptURL, long attempNumber) {
		this.buddy = buddy;

		urlAccept = Constants.appendURLSuffix(acceptURL);

		String textID = "v3.activity.buddy-request";
		if (attempNumber > 1) {
			textID += ".multi";
		}
		String text = MessageText.getString(textID, new String[] {
			buddy.getProfileAHREF(VuzeActivitiesConstants.TYPEID_BUDDYREQUEST),
			urlAccept,
			"" + attempNumber
		});
		
		setText(text);
		setTypeID(VuzeActivitiesConstants.TYPEID_BUDDYREQUEST, true);
		setID(buildID(buddy.getCode()));
	}

	public static String buildID(String code) {
		return VuzeActivitiesConstants.TYPEID_BUDDYREQUEST + "-" + code;
	}

}
