package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.util.ConstantsV3;
import com.aelitis.azureus.util.MapUtils;

public abstract class AbstractStatusListener
	extends AbstractBrowserMessageListener
	implements IStatusMessageListener
{
	protected Map decodedMap = new HashMap(0);

	public AbstractStatusListener(String listenerID) {
		super(listenerID);
	}

	public void handleMessage(BrowserMessage message) {
		if (context != null
				&& context.getContentNetwork() != ConstantsV3.DEFAULT_CONTENT_NETWORK) {
			context.debug("ERROR: Got Login JS RPC from non default network.  Ignoring");
			Debug.out("ERROR: Got Login JS RPC from non default network.  Ignoring");
			return;
		}

		String opID = message.getOperationId();
		if (true == Constants.isCVSVersion()) {
			System.out.println("\tLogin status message: " + message.getFullMessage());//KN: sysout
		}
		/*
		 * When no parameter is supplied the BrowserMessage throws an exception;
		 * it really should be returning a null.
		 */
		try {
			decodedMap = message.getDecodedMap();
		} catch (Exception e) {
			decodedMap = new HashMap(0);
		}
		if (true == OP_LOGIN_UPDATE.equals(opID)) {
			handleLoginUpdate();
		} else if (true == OP_LOGIN_STATUS.equals(opID)) {
			handleLoginStatus();
		} else if (true == OP_PAGE_LOAD_COMPLETED.equals(opID)) {
			handlePageLoadCompleted();
		}

	}

	public String getUserName() {
		if (true == decodedMap.containsKey(OP_LOGIN_UPDATE_PARAM_USER_NAME)) {
			return MapUtils.getMapString(decodedMap, OP_LOGIN_UPDATE_PARAM_USER_NAME,
					"");
		}
		return null;

	}

	public String getAvatar() {
		if (true == decodedMap.containsKey(OP_LOGIN_UPDATE_PARAM_AVATAR)) {
			return MapUtils.getMapString(decodedMap, OP_LOGIN_UPDATE_PARAM_AVATAR,
					"");
		}
		return null;

	}

	public String getDisplayName() {
		if (true == decodedMap.containsKey(OP_LOGIN_UPDATE_PARAM_DISPLAY_NAME)) {
			return MapUtils.getMapString(decodedMap,
					OP_LOGIN_UPDATE_PARAM_DISPLAY_NAME, "");
		}

		return null;
	}

	public String getPK() {
		if (true == decodedMap.containsKey(OP_PK)) {
			return MapUtils.getMapString(decodedMap, OP_PK, "");
		}

		return null;
	}

	/**
	 * Called when a login state has changed
	 * Default implementation does nothing; subclasses may override
	 */
	public void handleLoginUpdate() {
	}

	/**
	 * Called to report the current login status
	 * Default implementation does nothing; subclasses may override
	 */
	public void handleLoginStatus() {
	}

	/**
	 * Called when a browser page has completed loading it's content
	 * Default implementation does nothing; subclasses may override
	 */
	public void handlePageLoadCompleted() {
	}

	public boolean isRegistrationStillOpen() {
		if (true == decodedMap.containsKey(OP_LOGIN_UPDATE_PARAM_REGISTRATION_OPEN)) {
			return MapUtils.getMapBoolean(decodedMap,
					OP_LOGIN_UPDATE_PARAM_REGISTRATION_OPEN, true);
		}
		return true;
	}

}
