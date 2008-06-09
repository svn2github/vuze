package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Point;

public interface IBuddyPageListener
{
	public static final String OP_CLOSE = "close";

	public static final String OP_CANCEL = "cancel";

	public static final String OP_INVITEES = "buddy-invitees";

	public static final String OP_INVITEES_PARAM_BUDDIES = "buddies";

	public static final String OP_INVITEES_PARAM_EMAILS = "emails";

	public static final String OP_INVITE_CONFIRM = "invite-confirm";

	public static final String OP_INVITE_CONFIRM_PARAM_MSG = "message";

	public static final String OP_RESIZE = "resize";

	public static final String OP_RESIZE_PARAM_WIDTH = "width";

	public static final String OP_RESIZE_PARAM_HEIGHT = "height";

	public static final String OP_RESIZE_PARAM_WINDOW_STATE = "window_state";

	public static final String OP_RESIZE_PARAM_STATE_MAXIMIZE = "maximize";

	public static final String OP_RESIZE_PARAM_STATE_MINIMIZE = "minimize";

	public static final String OP_RESIZE_PARAM_STATE_RESTORE = "restore";

	public void handleClose();

	public void handleCancel();

	public void handleBuddyInvites();

	public void handleEmailInvites();

	public void handleInviteConfirm();

	public void handleResize();

	/**
	 * Returns a list of <code>VuzeBuddy</code>; the list may be empty but never <code>null</code>
	 * @return
	 */
	public List getInvitedBuddies();

	public List getInvitedEmails();

	public Map getConfirmationResponse();

	public List getConfirmationMessages();

	public Point getSize();

	public String getWindowState();

	public int getInvitationsSent();
}
