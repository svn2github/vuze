package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.List;
import java.util.Map;

public interface IBuddyPageListener
{
	public static final String OP_CLOSE = "close";

	public static final String OP_CANCEL = "cancel";

	public static final String OP_INVITEES = "buddy-invitees";

	public static final String OP_INVITEES_PARAM_BUDDIES = "buddies";

	public static final String OP_INVITEES_PARAM_EMAILS = "emails";

	public static final String OP_INVITE_CONFIRM = "invite-confirm";
	
	public static final String OP_INVITE_CONFIRM_PARAM_MSG = "message";
	
	public void handleClose();

	public void handleCancel();

	public void handleBuddyInvites();

	public void handleEmailInvites();
	
	public void handleInviteConfirm();

	/**
	 * Returns a list of <code>VuzeBuddy</code>; the list may be empty but never <code>null</code>
	 * @return
	 */
	public List getInvitedBuddies();

	public List getInvitedEmails();
	
	public Map getConfirmationResponse();
	
	public String getInvitationMessage();
	
}
