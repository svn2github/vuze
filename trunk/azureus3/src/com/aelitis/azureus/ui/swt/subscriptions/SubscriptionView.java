/**
 * 
 */
package com.aelitis.azureus.ui.swt.subscriptions;

import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;


public class
SubscriptionView
	implements SubscriptionsViewBase
{
	private SubscriptionsViewBase		impl;
	
	public
	SubscriptionView()
	{
			// we need a webui view for subscriptions before we can support an external browser view
		
		impl = new SubscriptionViewInternal();
	}
	
	public void
	updateBrowser(
		boolean	is_auto )
	{
		impl.updateBrowser(is_auto);
	}
	
	public void 
	refreshView() 
	{
		impl.refreshView();
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event ) 
	{
		return( impl.eventOccurred( event ));
	}
}