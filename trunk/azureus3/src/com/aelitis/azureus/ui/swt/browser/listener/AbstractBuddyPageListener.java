package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.MapUtils;

/**
 * This is a convenience abstract base class for listeners to the add buddy page;
 * it will automatically parse the <code>BrowserMessage</code> and call the appropriate methods.
 *
 * @author knguyen
 *
 */
public abstract class AbstractBuddyPageListener
	extends AbstractMessageListener
	implements IBuddyPageListener
{

	public static final String LISTENER_ID = "buddy-page";

	protected Map decodedMap = new HashMap(0);

	protected Browser browser;

	private Map confirmationResponse = null;

	private String confirmationMessage = null;

	private String invitationMessage = "";

	public AbstractBuddyPageListener(Browser browser) {
		super(LISTENER_ID);
		this.browser = browser;
	}

	public void handleMessage(final BrowserMessage message) {

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
							confirmationMessage = MapUtils.getMapString(confirmationResponse,
									"message", null);
						} else if (getmessageObj instanceof String) {
							confirmationMessage = getmessageObj.toString();
						} else {
							confirmationResponse = null;
							confirmationMessage = null;
						}
						handleInviteConfirm();
					}
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

	public String getConfirmationMessage() {
		return confirmationMessage;
	}

	public void setConfirmationMessage(String confirmationMessage) {
		this.confirmationMessage = confirmationMessage;
	}
}
