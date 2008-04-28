package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.browser.Browser;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyImpl;
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

	private List invitedBuddies = new ArrayList();

	private List invitedEmails = new ArrayList();

	public AbstractBuddyPageListener(Browser browser) {
		super(LISTENER_ID);
		this.browser = browser;
	}

	public void handleMessage(BrowserMessage message) {
		System.out.println(message.getFullMessage());//KN: sysout
		String opID = message.getOperationId();
		decodedMap = message.getDecodedMap();
		if (true == OP_CLOSE.equals(opID)) {
			handleClose();
		} else if (true == OP_CANCEL.equals(opID)) {
			handleCancel();
		} else if (true == OP_INVITEES.equals(opID)) {

			if (true == decodedMap.containsKey(OP_INVITEES_PARAM_BUDDIES)) {

				invitedBuddies.clear();

				List invitedBuddyMaps = MapUtils.getMapList(decodedMap,
						OP_INVITEES_PARAM_BUDDIES, new ArrayList());

				for (Iterator iterator = invitedBuddyMaps.iterator(); iterator.hasNext();) {
					Map map = (HashMap) iterator.next();
					System.out.println("Invited budy:" + map);//KN:

					for (Iterator iterator2 = map.entrySet().iterator(); iterator2.hasNext();) {
						Map.Entry entry = (Map.Entry) iterator2.next();
						System.out.println("\t" + entry.getKey() + " = " + entry.getValue());//KN: sysout

					}

					VuzeBuddy vBuddy = new VuzeBuddyImpl();
					vBuddy.setDisplayName(map.get("displayName").toString());
					vBuddy.setLoginID(map.get("name").toString());
					//					vBuddy.addPublicKey(pk)
					invitedBuddies.add(vBuddy);
				}

				handleBuddyInvites();
			}

			if (true == decodedMap.containsKey(OP_INVITEES_PARAM_EMAILS)) {
				invitedEmails = MapUtils.getMapList(decodedMap,
						OP_INVITEES_PARAM_EMAILS, new ArrayList());

				handleEmailInvites();
			}

		}
	}

	public List getInvitedBuddies() {
		return invitedBuddies;
	}

	public List getInvitedEmails() {
		return invitedEmails;
	}

}
