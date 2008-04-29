package com.aelitis.azureus.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
	private static LoginInfoManager INSTANCE;

	/*
	 * DO NOT initialize userName and userID to null because null is a valid value for these variables
	 */
	private String userName = "no.user.name.has.been.set";

	private String userID = "no.user.id.has.been.set";

	private String pk = null;

	private List listeners = new ArrayList();

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

	public void setUserInfo(String userName, String userID, String pk) {
		boolean changed = false;
		boolean isNewLoginID = false;
		if (!("" + userName).equals("" + this.userName)) {
			System.out.println("name changed from " + this.userName + " to " + userName);
			this.userName = userName;
			changed = true;
		}
		if (!("" + userID).equals("" + this.userID)) {
			System.out.println("uid changed from " + this.userID + " to " + userID);
			this.userID = userID;
			changed = true;
			isNewLoginID = true;
		}
		if (!("" + pk).equals("" + this.pk)) {
			System.out.println("pk changed from " + this.pk + " to " + pk);
			this.pk = pk;
			changed = true;
		}

		if (changed) {
			notifyListeners(isNewLoginID);
		}
	}

	private void notifyListeners(boolean isNewLoginID) {

		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			((ILoginInfoListener) iterator.next()).loginUpdate(new LoginInfo(), isNewLoginID);
		}

	}

	public class LoginInfo
	{
		public final String userName = LoginInfoManager.this.userName;

		public final String userID = LoginInfoManager.this.userID;

		/**
		 * The public key that the webapp thinks we have
		 */
		public final String pk = LoginInfoManager.this.pk;
	}

}
