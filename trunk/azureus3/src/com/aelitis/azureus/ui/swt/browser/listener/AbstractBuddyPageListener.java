package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.progress.ProgressReportMessage;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.util.MapUtils;

/**
 * This is a convenience abstract base class for listeners to the add buddy page;
 * it will automatically parse the <code>BrowserMessage</code> and call the appropriate methods.
 *
 * @author knguyen
 *
 */
public abstract class AbstractBuddyPageListener
	extends AbstractBrowserMessageListener
	implements IBuddyPageListener
{

	public static final String LISTENER_ID = "buddy-page";

	protected Map decodedMap = new HashMap(0);

	protected Browser browser;

	private Map confirmationResponse = null;

	private String invitationMessage = "";

	private Point size = new Point(-1, -1);

	private String windowState = null;

	private int invitationsSent = 0;

	private String message_success = MessageText.getString("message.status.success");

	public AbstractBuddyPageListener(Browser browser) {
		super(LISTENER_ID);
		this.browser = browser;
	}

	public void handleMessage(final BrowserMessage message) {
		if (true == Constants.isCVSVersion()) {
			System.out.println(message.getFullMessage());//KN: sysout
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				String opID = message.getOperationId();
				decodedMap = message.getDecodedMap();
				if (true == OP_CLOSE.equals(opID)) {
					handleClose();
				} else if (true == OP_CANCEL.equals(opID)) {
					handleCancel();
				} else if (true == OP_INVITEES.equals(opID)) {

					if (true == decodedMap.containsKey(OP_INVITEES_PARAM_BUDDIES)) {
						handleBuddyInvites();
					}
					if (true == decodedMap.containsKey(OP_INVITEES_PARAM_EMAILS)) {
						handleEmailInvites();
					}

				} else if (true == OP_INVITE_CONFIRM.equals(opID)) {
					if (true == decodedMap.containsKey(OP_INVITE_CONFIRM_PARAM_MSG)) {
						Object getmessageObj = decodedMap.get(OP_INVITE_CONFIRM_PARAM_MSG);
						if (getmessageObj instanceof Map) {
							confirmationResponse = (Map) getmessageObj;

							List sentInvitations = MapUtils.getMapList(confirmationResponse,
									"sentInvitations", Collections.EMPTY_LIST);
							invitationsSent = sentInvitations.size();

						} else {
							confirmationResponse = null;
						}
						handleInviteConfirm();
					}
				}

				else if (true == OP_RESIZE.equals(opID)) {

					if (true == decodedMap.containsKey(OP_RESIZE_PARAM_WINDOW_STATE)) {
						windowState = decodedMap.get(OP_RESIZE_PARAM_WINDOW_STATE).toString();
						if (null != windowState) {
							if (false == windowState.equals(OP_RESIZE_PARAM_STATE_MAXIMIZE)
									&& false == windowState.equals(OP_RESIZE_PARAM_STATE_MINIMIZE)
									&& false == windowState.equals(OP_RESIZE_PARAM_STATE_RESTORE)) {
								windowState = null;
							}
						}
					} else {
						if (true == decodedMap.containsKey(OP_RESIZE_PARAM_WIDTH)) {
							size.x = Integer.parseInt(decodedMap.get(OP_RESIZE_PARAM_WIDTH).toString());
						}
						if (true == decodedMap.containsKey(OP_RESIZE_PARAM_HEIGHT)) {
							size.y = Integer.parseInt(decodedMap.get(OP_RESIZE_PARAM_HEIGHT).toString());
						}
					}
					handleResize();
				}
			}
		});

	}

	public List getInvitedBuddies() {

		if (true == decodedMap.containsKey(OP_INVITEES_PARAM_BUDDIES)) {

			List invitedBuddies = new ArrayList();

			List invitedBuddyMaps = MapUtils.getMapList(decodedMap,
					OP_INVITEES_PARAM_BUDDIES, new ArrayList());

			for (Iterator iterator = invitedBuddyMaps.iterator(); iterator.hasNext();) {
				Map map = (HashMap) iterator.next();
				VuzeBuddy vBuddy = VuzeBuddyManager.createPotentialBuddy();
				vBuddy.setDisplayName(map.get("displayName").toString());
				vBuddy.setLoginID(map.get("name").toString());
				invitedBuddies.add(vBuddy);
			}

			return invitedBuddies;
		}

		return Collections.EMPTY_LIST;
	}

	public List getInvitedEmails() {
		if (true == decodedMap.containsKey(OP_INVITEES_PARAM_EMAILS)) {
			return MapUtils.getMapList(decodedMap, OP_INVITEES_PARAM_EMAILS,
					new ArrayList());
		}
		return Collections.EMPTY_LIST;
	}

	public Map getConfirmationResponse() {
		return confirmationResponse;
	}

	public String getInvitationMessage() {
		return invitationMessage;
	}

	public List getConfirmationMessages() {
		Map response = getConfirmationResponse();
		List message = new ArrayList();

		if (null != response && false == response.isEmpty()) {

			List sentInvitations = MapUtils.getMapList(response, "sentInvitations",
					Collections.EMPTY_LIST);
			if (null != sentInvitations && false == sentInvitations.isEmpty()) {
				for (Iterator iterator = sentInvitations.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					if (object instanceof Map) {
						Map invitation = (Map) object;
						String msg = MapUtils.getMapString(invitation, "value", "");
						msg += " : ";

						if (true == MapUtils.getMapBoolean(invitation, "success", false)) {
							msg += message_success;
							message.add(new ProgressReportMessage(msg,
									ProgressReportMessage.MSG_TYPE_INFO));
						} else {
							msg += MapUtils.getMapString(invitation, "cause", "");
							message.add(new ProgressReportMessage(msg,
									ProgressReportMessage.MSG_TYPE_ERROR));
						}
					}
				}
			}

		}

		return message;
	}

	public String getFormattedInviteMessage() {
		String message;
		int successMessages = 0;
		int errorMessages = 0;

		List messages = getConfirmationMessages();
		for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
			ProgressReportMessage msg = (ProgressReportMessage) iterator.next();
			if (true == msg.isInfo()) {
				successMessages++;
			} else {
				errorMessages++;
			}

		}

		if (errorMessages == 0) {
			if (successMessages == 1) {
				message = MessageText.getString("message.confirm.invite.singular");
			} else if (successMessages > 1) {
				message = MessageText.getString("message.confirm.invite.plural");
			} else {
				message = "DEBUG: confirmation with no error and no success???";
			}
		} else {
			message = MessageText.getString("message.confirm.invite.error");
		}

		return message;
	}

	public Point getSize() {
		return size;
	}

	public String getWindowState() {
		return windowState;
	}

	public int getInvitationsSent() {
		return invitationsSent;
	}

}
