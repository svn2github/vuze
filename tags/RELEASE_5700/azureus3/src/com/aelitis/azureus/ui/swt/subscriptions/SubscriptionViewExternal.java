/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.subscriptions;


import java.util.Locale;

import org.eclipse.swt.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
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
	}

	public void 
	refreshView() 
	{
	}

	private void
	launchView()
	{
		PluginInterface xmweb_ui = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "xmwebui" );
		
		if (xmweb_ui == null || !xmweb_ui.getPluginState().isOperational()){
			
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			
			MessageBoxShell mb = new MessageBoxShell( 
    				SWT.ICON_ERROR | SWT.OK,
    				MessageText.getString( "external.browser.failed" ),
    				MessageText.getString( "xmwebui.required" ));
			
			mb.setParent(uiFunctions.getMainShell());
			
    		mb.open(null);
    		
		}else{
		
			WebPlugin wp = (WebPlugin)xmweb_ui.getPlugin();
			
			String remui = wp.getProtocol().toLowerCase( Locale.US ) + "://127.0.0.1:" + wp.getPort() + "/";
			
			String test_url = ConstantsVuze.getDefaultContentNetwork().getServiceURL( ContentNetwork.SERVICE_XSEARCH, new Object[]{ "", false });
			
			int	pos = test_url.indexOf( '?' );
			
			String mode = xmweb_ui.getUtilities().getFeatureManager().isFeatureInstalled( "core" )?"plus":"trial";
				
				// gotta escape the name on top of overall escaping below as unfortunately it gets un-escaped somewhere and then can confuse parsing of the URL in
				// the remote search plugin
			
			String query = "Subscription: " + UrlUtils.encode(subs.getName()) + " ("+subs.getID() + ")";
			
			String search_url = 
					test_url.substring( 0, pos+1 ) + 
						"q=" + UrlUtils.encode( query ) + "&" +
						"mode=" + mode + "&" +
						"search_source=" + UrlUtils.encode( remui );
			
			Utils.launch( search_url );
		}
	}
	
	private void 
	initialize(
		Composite _parent_composite )
	{  
		parent_composite	= _parent_composite;

		composite = new Composite( parent_composite, SWT.NULL );

		GridLayout layout = new GridLayout(3, false);
		
		layout.marginHeight = 32;
		layout.marginWidth	= 32;
		
		composite.setLayout( layout );
		
		Label label = new Label( composite, SWT.NULL );
		GridData gd = new GridData( GridData.FILL_HORIZONTAL );
		gd.horizontalSpan = 3;
		label.setLayoutData( gd );
		Messages.setLanguageText( label, "subs.ext.view.info" );
		
		label = new Label( composite, SWT.NULL );
		Messages.setLanguageText( label, "subs.ext.view.launch.info" );
		
		Button button = new Button( composite, SWT.PUSH );
		Messages.setLanguageText( button, "iconBar.run" );

		button.addSelectionListener(
			new SelectionAdapter() {
			
				public void 
				widgetSelected(
					SelectionEvent e) 
				{
					launchView();
				}
			});
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