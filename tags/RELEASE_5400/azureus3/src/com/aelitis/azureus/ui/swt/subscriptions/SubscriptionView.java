/**
 * 
 */
package com.aelitis.azureus.ui.swt.subscriptions;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;


public class
SubscriptionView
	implements SubscriptionsViewBase
{
	private SubscriptionsViewBase		impl;
	
	public
	SubscriptionView()
	{
		boolean	internal_subs = !COConfigurationManager.getBooleanParameter( "browser.external.subs" );
		
		impl = internal_subs?new SubscriptionViewInternal():new SubscriptionViewExternal();
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