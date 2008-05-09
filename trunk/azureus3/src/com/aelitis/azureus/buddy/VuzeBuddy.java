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

package com.aelitis.azureus.buddy;

import java.util.Map;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;

/**
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public interface VuzeBuddy
{

	public String getDisplayName();

	public void setDisplayName(String displayName);

	public String getLoginID();

	public void setLoginID(String loginName);

	public long getLastUpdated();

	public void setLastUpdated(long lastUpdated);

	public byte[] getAvatar();

	public void setAvatar(byte[] image);

	public boolean isOnline();

	public String[] getPublicKeys();
	
	public void addPublicKey(String pk);
	
	public void removePublicKey(String pk);
	
	public void sendActivity(VuzeActivitiesEntry entry) throws NotLoggedInException;

	public void loadFromMap(Map mapNewBuddy);

	public void shareDownload(SelectedContent content, String message) throws NotLoggedInException;

	public void sendPayloadMap(Map map) throws NotLoggedInException;

	public Map toMap();

	public String getCode();

	public void setCode(String code);

	/**
	 * Get the URL of the user's profile.
	 * 
	 * @param referer referer id passed to webpage when profile link is loaded
	 *                so the webpage knows where the load came from
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	String getProfileUrl(String referer);
}
