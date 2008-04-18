package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.HashMap;
import java.util.Map;

import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.MapUtils;

public abstract class AbstractStatusListener
	extends AbstractMessageListener
	implements IStatusMessageListener
{
	protected Map decodedMap = new HashMap(0);

	public AbstractStatusListener(String listenerID) {
		super(listenerID);
	}

	public void handleMessage(BrowserMessage message) {
		String opID = message.getOperationId();

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
		}

	}

	public String getUserName() {
		if (true == decodedMap.containsKey(OP_LOGIN_UPDATE_PARAM_USER_NAME)) {
			return MapUtils.getMapString(decodedMap, OP_LOGIN_UPDATE_PARAM_USER_NAME,
					"");
		}
		return null;

	}

	public String getUserID() {
		if (true == decodedMap.containsKey(OP_LOGIN_UPDATE_PARAM_USER_ID)) {
			return MapUtils.getMapString(decodedMap, OP_LOGIN_UPDATE_PARAM_USER_ID,
					"");
		}

		return null;
	}

	public String getPK() {
		if (true == decodedMap.containsKey(OP_PK)) {
			return MapUtils.getMapString(decodedMap, OP_PK, "");
		}

		return null;
	}
}
