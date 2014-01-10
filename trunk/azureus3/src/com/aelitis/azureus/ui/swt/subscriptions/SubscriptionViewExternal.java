/**
 * 
 */
package com.aelitis.azureus.ui.swt.subscriptions;


import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.util.ConstantsVuze;



public class
SubscriptionViewExternal
	implements SubscriptionsViewBase
{
	private Subscription	subs;

	private Composite		parent_composite;
	private Composite		composite;


	private SubscriptionMDIEntry 	mdiInfo;

	private UISWTView swtView;

	public
	SubscriptionViewExternal()
	{
	}

	public void
	updateBrowser(
		boolean	is_auto )
	{
		System.out.println( "updateBrowser" );
		
		// Utils.launch( getURL());
	}

	public void 
	refreshView() 
	{
		System.out.println( "refreshView" );
	}

	private void 
	initialize(
		Composite _parent_composite )
	{  
		parent_composite	= _parent_composite;

		composite = new Composite( parent_composite, SWT.NULL );

		composite.setLayout(new GridLayout());
		
		Label label = new Label( composite, SWT.NULL );
		
		label.setText( "You have selected to use an external browser to view subscriptions - not supported yet!" );
	}

	private String
	getURL()
	{
		
		//http://search.vuze.com/xsearch/?q=aers&mode=plus&search_source=http%3A%2F%2F127.0.0.1%3A9091%2F
			
		ContentNetwork contentNetwork = ContentNetworkManagerFactory.getSingleton().getContentNetwork(
				ConstantsVuze.DEFAULT_CONTENT_NETWORK_ID );
		
		String url = contentNetwork.getSubscriptionURL(subs.getID());
			
		Boolean	edit_mode = (Boolean)subs.getUserData( SubscriptionManagerUI.SUB_EDIT_MODE_KEY );
		
		if ( edit_mode != null ){
		
			if ( edit_mode.booleanValue()){
				
				url += SubscriptionManagerUI.EDIT_MODE_MARKER;
			}
			
			subs.setUserData( SubscriptionManagerUI.SUB_EDIT_MODE_KEY, null );
		}
		
		return( url );
	}
	
	private Composite 
	getComposite()
	{ 
		return( composite );
	}
	
	private String 
	getFullTitle() 
	{
		if ( subs == null ){
			
			return "";
		}
		
		return( subs.getName());
	}
	
	private void 
	viewActivated()
	{
		if ( subs != null && mdiInfo == null ){
			
			mdiInfo = (SubscriptionMDIEntry)subs.getUserData(SubscriptionManagerUI.SUB_ENTRYINFO_KEY);
		}
	}

	private void 
	viewDeactivated()
	{
		if ( mdiInfo != null && mdiInfo.spinnerImage != null ){
			
			mdiInfo.spinnerImage.setVisible(false);
		}
	}
	
	private void 
	dataSourceChanged(
		Object data ) 
	{
		if ( data instanceof Subscription ){
			
			subs = (Subscription) data;
			
			mdiInfo = (SubscriptionMDIEntry) subs.getUserData(SubscriptionManagerUI.SUB_ENTRYINFO_KEY);
		}
		
		if ( subs != null && swtView != null ){
			
			swtView.setTitle(getFullTitle());
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
		case UISWTViewEvent.TYPE_CREATE:
			swtView = (UISWTView)event.getData();
			swtView.setTitle(getFullTitle());
			break;

		case UISWTViewEvent.TYPE_DESTROY:
			break;

		case UISWTViewEvent.TYPE_INITIALIZE:
			initialize((Composite)event.getData());
			break;

		case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
			Messages.updateLanguageForControl(getComposite());
			swtView.setTitle(getFullTitle());
			break;

		case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
			dataSourceChanged(event.getData());
			break;

		case UISWTViewEvent.TYPE_FOCUSGAINED:
			viewActivated();
			break;

		case UISWTViewEvent.TYPE_FOCUSLOST:
			viewDeactivated();
			break;

		case UISWTViewEvent.TYPE_REFRESH:
			break;
		}

		return true;
	}
}