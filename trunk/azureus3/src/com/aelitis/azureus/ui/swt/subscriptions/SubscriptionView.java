/**
 * 
 */
package com.aelitis.azureus.ui.swt.subscriptions;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionListener;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.BrowserWrapper;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.OpenCloseSearchDetailsListener;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.azureus.util.UrlFilter;

public class
SubscriptionView
	implements OpenCloseSearchDetailsListener, UIPluginViewToolBarListener,
	UISWTViewCoreEventListener
{
	private Subscription	subs;
	
	private Composite		parent_composite;
	private Composite		composite;
			
	//private Label			info_lab;
	//private Label			info_lab2;
	//private StyledText	json_area;
	//private Composite 		controls;
	
	private BrowserWrapper			mainBrowser;
	private BrowserWrapper			detailsBrowser;
	private SubscriptionMDIEntry 	mdiInfo;

	private UISWTView swtView;

	public
	SubscriptionView()
	{
	}
	

	protected void refreshView() {
		if (subs == null) {
			return;
		}
		String key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getEntrySWT(key);
			if (entry != null) {
				UISWTViewEventListener eventListener = entry.getEventListener();
				if (eventListener instanceof SubscriptionView) {
					SubscriptionView subsView = (SubscriptionView) eventListener;
					subsView.updateBrowser( false );
				}
			}
		}
	}


	private void 
	initialize(
		Composite _parent_composite )
	{  
		parent_composite	= _parent_composite;
		
		composite = new Composite( parent_composite, SWT.NULL );
		
		composite.setLayout(new FormLayout());
		
		//GridData grid_data = new GridData(GridData.FILL_BOTH );
		//composite.setLayoutData(grid_data);
		//FormData data;

			// control area
		
		/*
		controls = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		controls.setLayout(layout);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.top = new FormAttachment(0,0);
		controls.setLayoutData(data);
		
		GridData grid_data;

		info_lab = new Label( controls, SWT.NULL );
		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		info_lab.setLayoutData(grid_data);
	
			
		info_lab2 = new Label( controls, SWT.NULL );
		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		info_lab2.setLayoutData(grid_data);
		
		json_area = new StyledText(controls,SWT.BORDER);
		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.heightHint = 50;
		json_area.setLayoutData(grid_data);
		json_area.setWordWrap(true);
		*/
		
		subs.addListener(
			new SubscriptionListener()
			{
				public void 
				subscriptionChanged(
					Subscription subs ) 
				{
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								updateInfo();
							}
						});
				}
				
				public void
				subscriptionDownloaded(
					Subscription		subs,
					boolean				auto )
				{
					if ( auto ){
						
						updateBrowser( true );
					}
				}
			});
					
		updateInfo();
	}
	  
	
	protected void
	createBrowsers()
	{
		if (mainBrowser != null && !mainBrowser.isDisposed()) {
			return;
		}
		try{
			mainBrowser = new BrowserWrapper(composite,Utils.getInitialBrowserStyle(SWT.NONE));
			mainBrowser.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					((Browser)e.widget).setUrl("about:blank");
					((Browser)e.widget).setVisible(false);
				}
			});
			BrowserContext context = 
				new BrowserContext("browser-window"	+ Math.random(), mainBrowser, null, true);
			
			context.addListener(new BrowserContext.loadingListener(){
				public void browserLoadingChanged(boolean loading, String url) {
					if (mdiInfo.spinnerImage != null) {
						mdiInfo.spinnerImage.setVisible(loading);
					}
				}
			});
			
			context.addMessageListener(new TorrentListener());
			context.addMessageListener(new VuzeListener());
			context.addMessageListener(new DisplayListener(mainBrowser));
			context.addMessageListener(new ConfigListener(mainBrowser));
			context.addMessageListener(
					new MetaSearchListener( this ));
			
			ContentNetwork contentNetwork = ContentNetworkManagerFactory.getSingleton().getContentNetwork(
					context.getContentNetworkID());
			// contentNetwork won't be null because a new browser context
			// has the default content network
			
			String url = contentNetwork.getSubscriptionURL(subs.getID());
				
			Boolean	edit_mode = (Boolean)subs.getUserData( SubscriptionManagerUI.SUB_EDIT_MODE_KEY );
			
			if ( edit_mode != null ){
			
				if ( edit_mode.booleanValue()){
					
					url += SubscriptionManagerUI.EDIT_MODE_MARKER;
				}
				
				subs.setUserData( SubscriptionManagerUI.SUB_EDIT_MODE_KEY, null );
			}
							
			mainBrowser.setUrl(url);
			mainBrowser.setData("StartURL", url);
			
			FormData data = new FormData();
			data.left = new FormAttachment(0,0);
			data.right = new FormAttachment(100,0);
			data.top = new FormAttachment(composite,0);
			data.bottom = new FormAttachment(100,0);
			mainBrowser.setLayoutData(data);
			
			detailsBrowser = new BrowserWrapper(composite,Utils.getInitialBrowserStyle(SWT.NONE));
			detailsBrowser.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					((Browser)e.widget).setUrl("about:blank");
					((Browser)e.widget).setVisible(false);
				}
			});
			BrowserContext detailsContext = 
				new BrowserContext("browser-window"	+ Math.random(), detailsBrowser, null, false);
			detailsContext.addListener(new BrowserContext.loadingListener(){
				public void browserLoadingChanged(boolean loading, String url) {
					if (mdiInfo.spinnerImage != null) {
						mdiInfo.spinnerImage.setVisible(loading);
					}
				}
			});
			
			ClientMessageContext.torrentURLHandler url_handler =
				new ClientMessageContext.torrentURLHandler()
				{
					public void 
					handleTorrentURL(
						final String url ) 
					{
						Utils.execSWTThreadWithObject(
							"SMUI",
							new AERunnableObject()
							{
								public Object
								runSupport()
								{
									String subscriptionId 		= (String)detailsBrowser.getData("subscription_id");
									String subscriptionResultId = (String)detailsBrowser.getData("subscription_result_id");
			
									if ( subscriptionId != null && subscriptionResultId != null ){
										
										Subscription subs = SubscriptionManagerFactory.getSingleton().getSubscriptionByID( subscriptionId );
									
										if ( subs != null ){
											
											subs.addPotentialAssociation( subscriptionResultId, url );
										}
									}
									
									return( null );
								}
							},
							10*1000 );
					}
				};
				
			detailsContext.setTorrentURLHandler( url_handler );
			
			TorrentListener torrent_listener = new TorrentListener();
			
			torrent_listener.setTorrentURLHandler( url_handler );
			
			detailsContext.addMessageListener( torrent_listener );
			detailsContext.addMessageListener(new VuzeListener());
			detailsContext.addMessageListener(new DisplayListener(detailsBrowser));
			detailsContext.addMessageListener(new ConfigListener(detailsBrowser));
			url = "about:blank";
			detailsBrowser.setUrl(url);
			detailsBrowser.setData("StartURL", url);
			
			final ExternalLoginCookieListener cookieListener = new ExternalLoginCookieListener(new CookiesListener() {
				public void cookiesFound(String cookies) {
					if (detailsBrowser != null) {
						detailsBrowser.setData("current-cookies", cookies);
					}
				}
			},detailsBrowser);
			
			cookieListener.hook();
			
			data = new FormData();
			data.left = new FormAttachment(0,0);
			data.right = new FormAttachment(100,0);
			data.top = new FormAttachment(mainBrowser.getBrowser(),0);
			data.bottom = new FormAttachment(100,0);
			detailsBrowser.setLayoutData(data);
							
			mainBrowser.setVisible( true );
			detailsBrowser.setVisible( false );
			//detailsBrowser.set
			mainBrowser.getParent().layout(true,true);
			
			
		}catch( Throwable e ){
		
			Debug.printStackTrace(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		list.put("share", UIToolBarItem.STATE_ENABLED);
		list.put("remove", UIToolBarItem.STATE_ENABLED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if (item.getID().equals("remove")) {
	  	mdiInfo.removeWithConfirm();
		}
		return false;
	}

	protected void
	destroyBrowsers()
	{
		if ( mainBrowser != null ){
		
			mainBrowser.dispose();
			
			mainBrowser = null;
		}
		
		if ( detailsBrowser != null ){
			
			detailsBrowser.dispose();

			detailsBrowser = null;
		}
	}
	
	public void closeSearchResults(final Map params) {
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				detailsBrowser.setVisible(false);
				
				FormData gd = (FormData) mainBrowser.getLayoutData();
				gd.bottom = new FormAttachment(100, 0);
				mainBrowser.setLayoutData(gd);
	
				mainBrowser.getParent().layout(true);
				detailsBrowser.setUrl("about:blank");
				//mainBrowser.setUrl( (String)mainBrowser.getData( "StartURL" ));
			}
		});
	}
	
	public void openSearchResults(final Map params) {
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				String url = MapUtils.getMapString(params, "url",
						"http://google.com/search?q=" + Math.random());
				if (UrlFilter.getInstance().urlCanRPC(url)) {
					url = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(url, false, true);
				}
				
				//Gudy, Not Tux, Listener Added
				String listenerAdded = (String) detailsBrowser.getData("g.nt.la");
				if(listenerAdded == null) {
					final BrowserWrapper browser = detailsBrowser;
					detailsBrowser.setData("g.nt.la","");
					detailsBrowser.addProgressListener(new ProgressListener() {
						public void changed(ProgressEvent event) {}
						
						public void completed(ProgressEvent event) {
								String execAfterLoad = (String) browser.getData("execAfterLoad");
							//Erase it, so that it's only used once after the page loads
								browser.setData("execAfterLoad",null);
							if(execAfterLoad != null && ! execAfterLoad.equals("")) {
								//String execAfterLoadDisplay = execAfterLoad.replaceAll("'","\\\\'");
								//search.execute("alert('injecting script : " + execAfterLoadDisplay + "');");
								boolean result = browser.execute(execAfterLoad);
								//System.out.println("Injection : " + execAfterLoad + " (" + result + ")");
							}
	
						}
					});
				}
				
				
				//Store the "css" match string in the search cdp browser object
				String execAfterLoad = MapUtils.getMapString(params, "execAfterLoad", null);
				
				detailsBrowser.setData("execAfterLoad",execAfterLoad);
				
				
				detailsBrowser.setData("subscription_id", MapUtils.getMapString(params, "subs_id", null));
				detailsBrowser.setData("subscription_result_id", MapUtils.getMapString(params, "subs_rid", null));
							
				detailsBrowser.setUrl(url);
				detailsBrowser.setData("StartURL", url);
				detailsBrowser.setVisible(true);
	
				FormData data = (FormData) mainBrowser.getLayoutData();
				data.bottom = null;
				data.height = MapUtils.getMapInt(params, "top-height", 120);
				//mainBrowser.setLayoutData(data);
	
				mainBrowser.getParent().layout(true,true);
			}
		});
			
	}
	
	protected void
	updateBrowser(
		final boolean	is_auto )
	{
		if ( mainBrowser != null && !mainBrowser.isDisposed() ){
			
			Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						if ( mainBrowser != null && !mainBrowser.isDisposed() && mainBrowser.isVisible()){
						
							String url = (String)mainBrowser.getData( "StartURL" );

								// see if end of edit process indicated by the subscription being
								// re-downloaded on auto-mode
							
							if ( is_auto && url.endsWith( SubscriptionManagerUI.EDIT_MODE_MARKER )){
								
								url = url.substring(0,url.lastIndexOf( SubscriptionManagerUI.EDIT_MODE_MARKER ));
							
								mainBrowser.setData( "StartURL", url );
							}
							
							mainBrowser.setUrl( url );
						}
					}
				});
		}
	}
	
	protected void
	updateInfo()
	{
		/*
		String	engine_str = "";
		
		try{
			Engine engine = subs.getEngine();
			
			engine_str = engine.getString();
			
		}catch( Throwable e ){
			
			engine_str = Debug.getNestedExceptionMessage(e);
			
			Debug.out(e);
		}
		
		info_lab.setText( 
				"ID=" + subs.getID() +
				", version=" + subs.getVersion() +
				", subscribed=" + subs.isSubscribed() +
				", public=" + subs.isPublic() +
				", mine=" + subs.isMine() +
				", popularity=" + subs.getCachedPopularity() +
				", associations=" + subs.getAssociationCount() +
				", engine=" + engine_str );
		
		SubscriptionHistory history = subs.getHistory();
		
		info_lab2.setText( 
				"History: " + 
				"enabled=" + history.isEnabled() +
				", auto=" + history.isAutoDownload() +
				", last_scan=" + new SimpleDateFormat().format(new Date( history.getLastScanTime())) +
				", next_scan=" + new SimpleDateFormat().format(new Date( history.getNextScanTime())) +
				", last_new=" + new SimpleDateFormat().format(new Date( history.getLastNewResultTime())) +
				", read=" + history.getNumRead() +
				" ,unread=" + history.getNumUnread() +
				", error=" + history.getLastError() + " [af=" + history.isAuthFail() + "]" );
				
		try{
		
			json_area.setText( subs.getJSON());
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		*/
	}
	
	private Composite 
	getComposite()
	{ 
		return( composite );
	}
	
	private String 
	getFullTitle() 
	{
		if (subs == null) {
			return "";
		}
		return( subs.getName());
	}
	
	public void resizeMainBrowser() {
		if ( mainBrowser != null ){
			
			Utils.execSWTThreadLater(0,
				new Runnable()
				{
					public void
					run()
					{
						if ( mainBrowser != null && ! mainBrowser.isDisposed() && mainBrowser.isVisible()){
						
							FormData data = (FormData) mainBrowser.getLayoutData();
							data.bottom = new FormAttachment(100,-1);
							mainBrowser.getParent().layout(true);
							Utils.execSWTThreadLater(0,
									new Runnable() {
								public void run() {
									if ( mainBrowser != null && ! mainBrowser.isDisposed() && mainBrowser.isVisible()){
										
										FormData data = (FormData) mainBrowser.getLayoutData();
										data.bottom = new FormAttachment(100,0);
										mainBrowser.getParent().layout(true);
									}
								}
							}
							);
						}
					}
				});
		}
		
	}
	
	public void resizeSecondaryBrowser() {
		// TODO Auto-generated method stub
		
	}


	private void viewActivated() {
		if (subs != null && mdiInfo == null) {
			mdiInfo = (SubscriptionMDIEntry) subs.getUserData(SubscriptionManagerUI.SUB_ENTRYINFO_KEY);
		}
		createBrowsers();
	}

	private void viewDeactivated() {
		if (mdiInfo.spinnerImage != null) {
			mdiInfo.spinnerImage.setVisible(false);
		}
		destroyBrowsers();
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


	private void dataSourceChanged(Object data) {
		if (data instanceof Subscription) {
			subs = (Subscription) data;
			mdiInfo = (SubscriptionMDIEntry) subs.getUserData(SubscriptionManagerUI.SUB_ENTRYINFO_KEY);
		}
		if (subs != null && swtView != null) {
    	swtView.setTitle(getFullTitle());
		}
	}

}