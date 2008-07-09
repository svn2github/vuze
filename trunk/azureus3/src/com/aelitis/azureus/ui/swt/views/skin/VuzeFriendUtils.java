package com.aelitis.azureus.ui.swt.views.skin;

import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;

public class VuzeFriendUtils
{

	private static VuzeFriendUtils instance;

	private InvitePage invitePage = null;

	public static VuzeFriendUtils getInstance() {
		if (null == instance) {
			instance = new VuzeFriendUtils();
		}
		return instance;
	}

	/**
	 * Initialize the Invite Friends flow; passing the given message to the 'preselect()' method
	 * in the Invite web page
	 * @param message
	 */
	public void invite(final String message) {
		if (null != invitePage) {
			SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
				public void loginComplete() {
					try {
						invitePage.preSelect(message);
					} catch (Exception e) {
					}
				}
			});

		}
	}

	public InvitePage getInvitePage() {
		return invitePage;
	}

	public void setInvitePage(InvitePage invitePage) {
		this.invitePage = invitePage;
	}

}
