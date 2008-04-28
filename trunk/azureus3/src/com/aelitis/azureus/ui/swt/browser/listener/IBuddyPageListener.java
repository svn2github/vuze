package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.List;

public interface IBuddyPageListener
{
	public static final String OP_CLOSE = "close";

	public static final String OP_CANCEL = "cancel";

	public static final String OP_INVITEES = "buddy-invitees";

	public static final String OP_INVITEES_PARAM_BUDDIES = "buddies";

	public static final String OP_INVITEES_PARAM_EMAILS = "emails";

	public void handleClose();

	public void handleCancel();

	public void handleBuddyInvites();

	public void handleEmailInvites();

	/**
	 * Returns a list of <code>VuzeBuddy</code>; the list may be empty but never <code>null</code>
	 * @return
	 */
	public List getInvitedBuddies();

	public List getInvitedEmails();
}
