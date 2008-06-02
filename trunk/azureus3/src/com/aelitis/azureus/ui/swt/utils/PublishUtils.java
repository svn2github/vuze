package com.aelitis.azureus.ui.swt.utils;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.listener.publish.PublishTransaction;
import com.aelitis.azureus.ui.swt.browser.listener.publish.SeedingListener;

/**
 * Publish functions that are used by both the Publisher plugin and AZ3ui's publish window
 * 
 * @author TuxPaper
 *
 */
public class PublishUtils
{
	public static void setupContext(ClientMessageContext context) {
		context.registerTransactionType("publish", PublishTransaction.class);
		context.addMessageListener(new SeedingListener());
	}
}
