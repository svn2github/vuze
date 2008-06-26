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

import java.util.Comparator;
import java.util.Map;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;

/**
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public interface VuzeBuddy
	extends Comparator, Comparable
{
		/**
		 * These constants are ordered so, for example, to test if chat supported do a
		 *     getVersion() >= VERSION_CHAT
		 */
	
	public static final int	VERSION_INITIAL	= BuddyPlugin.VERSION_INITIAL;
	public static final int	VERSION_CHAT	= BuddyPlugin.VERSION_CHAT;
	
	public String getDisplayName();

	public void setDisplayName(String displayName);

	public String getLoginID();

	public void setLoginID(String loginName);

	public long getLastUpdated();

	public void setLastUpdated(long lastUpdated);

	public byte[] getAvatar();

	public void setAvatar(byte[] image);

	public boolean isOnline( boolean is_connected );

		// returns a VERSION constant from above list. This is the maximum version of
		// any of the buddies identities
	
	public int getVersion();
	
	public String[] getPublicKeys();
	
	public void addPublicKey(String pk);
	
	public void removePublicKey(String pk);
	
	public void sendActivity(VuzeActivitiesEntry entry) throws NotLoggedInException;

	public void loadFromMap(Map mapNewBuddy);

	public void shareDownload(SelectedContentV3 content, String message) throws NotLoggedInException;

	public void sendPayloadMap(Map map) throws NotLoggedInException;

	public void sendBuddyMessage(String namespace, Map map) throws NotLoggedInException;
	
	public Map toMap();

	public String getCode();

	public void setCode(String code);

	String getProfileUrl(String referer);

	/**
	 * @param createdOn
	 *
	 * @since 3.0.5.3
	 */
	void setCreatedOn(long createdOn);

	/**
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	long getCreatedOn();

	/**
	 * @throws NotLoggedInException
	 *
	 * @since 3.0.5.3
	 */
	void tellBuddyToSyncUp()
			throws NotLoggedInException;

	/**
	 * @param referer
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	String getProfileAHREF(String referer);

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	String toDebugString();

	/**
	 * @param referer
	 * @param useImage
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	String getProfileAHREF(String referer, boolean useImage);

	/**
	 * @param l
	 *
	 * @since 3.1.0.1
	 */
	void addListener(VuzeBuddyListener l);

	/**
	 * @param l
	 *
	 * @since 3.1.0.1
	 */
	void removeListener(VuzeBuddyListener l);

	/**
	 * @return
	 *
	 * @since 3.1.0.1
	 */
	VuzeBuddyListener[] getListeners();
}
