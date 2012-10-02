package com.aelitis.azureus.ui.swt.browser.listener;

import java.net.URLDecoder;


import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;

import com.aelitis.azureus.ui.swt.browser.BrowserWrapper;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;

public class ExternalLoginCookieListener
implements StatusTextListener,LocationListener,ProgressListener
{
	
	private static final String AZCOOKIEMSG = "AZCOOKIEMSG;";
	
	private CookiesListener listener;
	
	private BrowserWrapper browser;
	
	private final static String getCookiesCode = 
		//"{" +
		"try {" +
		"var cookies = encodeURIComponent(document.cookie);" +
		"window.status = '" + AZCOOKIEMSG + "' + cookies;" +
		"//alert(window.status);\n" +
		"window.status = '';" +
		"} catch(e) {" +
		"}" ;
		//"}";
	
	public ExternalLoginCookieListener(CookiesListener _listener,BrowserWrapper browser) {
		this.listener = _listener;
		this.browser = browser;
		browser.addStatusTextListener(this);
	}
	
	
	public void changed(StatusTextEvent event) {
		if(event.text.startsWith(AZCOOKIEMSG)) {
			String uriEncodedCookies =event.text.substring(AZCOOKIEMSG.length());
			try {
				String cookies = URLDecoder.decode(uriEncodedCookies, "UTF-8");
				
				if(listener != null) {
					listener.cookiesFound(cookies);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void getCookies() {
		if(browser != null) {
			browser.execute(getCookiesCode);
		}
	}
	
	public void stopListening() {
		browser.removeStatusTextListener(this);
	}
	
	public void hookOnPageLoaded() {
		browser.addProgressListener(this);
	}
	
	public void hookOnPageChanged() {
		browser.addLocationListener(this);
	}
	
	public void hook() {
		hookOnPageChanged();
		hookOnPageLoaded();
	}
	
	public void unHook() {
		
	}
	
	public void changed(ProgressEvent arg0) {
		
	}
	
	public void completed(ProgressEvent arg0) {
		getCookies();
	}
	
	public void changed(LocationEvent arg0) {
		getCookies();
	}
	
	public void changing(LocationEvent arg0) {
		
	}

}
