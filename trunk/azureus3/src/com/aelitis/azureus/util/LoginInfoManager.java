package com.aelitis.azureus.util;

import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.util.UrlUtils;

import com.aelitis.azureus.core.crypto.VuzeCryptoException;
import com.aelitis.azureus.core.crypto.VuzeCryptoManager;
import com.aelitis.azureus.core.util.CopyOnWriteList;

/**
 * A manager class to store login info
 * <P>
 * Users can add <code>ILoginInfoListener</code>'s to be notified when any login event occurs
 * 
 * @author knguyen
 *
 */
public class LoginInfoManager
{
	private static final String NAME_NOT_SET_VALUE = "no.user.name.has.been.set";

	private static LoginInfoManager INSTANCE;

	/*
	 * DO NOT initialize userName and userID to null because null is a valid value for these variables
	 */
	private String userName = NAME_NOT_SET_VALUE;

	private String displayName = null;

	private String pk = null;

	private boolean isRegistrationStillOpen = false;
	
	private CopyOnWriteList listeners = new CopyOnWriteList();

	private String avatarURL;

	private LoginInfoManager() {
		//Making this private
	}

	public static LoginInfoManager getInstance() {
		if (null == INSTANCE) {
			INSTANCE = new LoginInfoManager();
		}
		return INSTANCE;
	}

	/**
	 * Adds the given <code>IRuntimeInfoListener</code> to the list of listeners for login info
	 * @param listener
	 */
	public void addListener(ILoginInfoListener listener) {
		if (false == listeners.contains(listener)) {
			listeners.add(listener);
			
			if (isLoggedIn()) {
				listener.loginUpdate(new LoginInfo(), true);

				if (avatarURL != null) {
					listener.avatarURLUpdated(avatarURL);
				}
			}
		}

	}

	/**
	 * Removes the given <code>IRuntimeInfoListener</code> from the list of listeners for login info
	 * @param infoType
	 * @param listener
	 */
	public void removeListener(ILoginInfoListener listener) {
		listeners.remove(listener);
	}

	public LoginInfo getUserInfo() {
		return new LoginInfo();
	}

	public void setUserInfo(String userName, String displayName, String pk, String avatarURL) {
		boolean changed = false;
		boolean isNewLoginID = false;
		if (!("" + userName).equals("" + this.userName)) {
			this.userName = userName;
			changed = true;
			isNewLoginID = true;
		}
		if (!("" + displayName).equals("" + this.displayName)) {
			this.displayName = displayName;
			changed = true;
		}
		if (!("" + pk).equals("" + this.pk)) {
			this.pk = pk;
			changed = true;
		}
		if (!("" + avatarURL).equals("" + this.avatarURL)) {
			setAvatarURL(avatarURL);
			changed = true;
		}

		if (changed) {
			notifyListeners(isNewLoginID);
		}
	}
	
	public void setAvatarURL(String avatarURL) {
		if (avatarURL == null) {
			return;
		}
		if (avatarURL.equals(this.avatarURL)) {
			return;
		}
		this.avatarURL = avatarURL;
		
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			((ILoginInfoListener) iterator.next()).avatarURLUpdated(avatarURL);
		}
	}

	public boolean isLoggedIn() {
		return this.userName != null && !this.userName.equals(NAME_NOT_SET_VALUE);
	}

	private void notifyListeners(boolean isNewLoginID) {

		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			((ILoginInfoListener) iterator.next()).loginUpdate(new LoginInfo(),
					isNewLoginID);
		}

	}

	public class LoginInfo
	{
		public final String userName = LoginInfoManager.this.userName;

		public final String displayName = LoginInfoManager.this.displayName == null
				? LoginInfoManager.this.userName : LoginInfoManager.this.displayName;

		public final boolean isRegistrationStillOpen = LoginInfoManager.this.isRegistrationStillOpen;
		
		/**
		 * The public key that the webapp thinks we have
		 */
		public final String pk = LoginInfoManager.this.pk;
		
		public final String avatarUrl = LoginInfoManager.this.avatarURL;

		public String getProfileAHREF(String referer) {
			StringBuffer buf = new StringBuffer();
			buf.append("<A HREF=\"");
			buf.append( ConstantsVuze.getDefaultContentNetwork().getProfileService(userName, referer ));
			buf.append("\" TITLE=\"");
			buf.append(displayName);
			if (!displayName.equals(userName)) {
				buf.append(" (");
				buf.append(userName);
				buf.append(")");
			}
			buf.append("\">");
			buf.append(displayName);
			buf.append("</A>");
			return buf.toString();
		}
	}

	/**
	 * @param mapUserInfo
	 *
	 * @since 3.1.0.1
	 */
	public void setUserInfo(Map mapUserInfo) {
		try {
			setUserInfo(MapUtils.getMapString(mapUserInfo, "login-id", null),
					MapUtils.getMapString(mapUserInfo, "display-name", null),
					VuzeCryptoManager.getSingleton().getPublicKey(""),
					MapUtils.getMapString(mapUserInfo, "avatar.url", null));
		} catch (VuzeCryptoException e) {
		}
	}

	/**
	 * @param b
	 *
	 * @since 4.0.0.5
	 */
	public void logout() {
		this.userName = null;
		this.displayName = null;
		this.avatarURL = null;
		this.pk = null;
		notifyListeners(false);
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			((ILoginInfoListener) iterator.next()).avatarURLUpdated(avatarURL);
		}
	}

}
