package com.aelitis.azureus.ui.swt.browser.listener;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;

import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;

public class ExternalLoginCookieListener
implements StatusTextListener
{
	
	private static final String AZCOOKIEMSG = "AZCOOKIEMSG;";
	
	private CookiesListener listener;
	
	private Browser browser;
	
	private final static String getCookiesCode = 
		//"{" +
		"try {" +
		"var cookies = encodeURIComponent(document.cookie);" +
		"window.status = '" + AZCOOKIEMSG + "' + cookies;" +
		"//alert(window.status);\n" +
		"window.status = '';" +
		"} catch(e) {" +
		"alert(e);" +
		"}" ;
		//"}";
	
	public ExternalLoginCookieListener(CookiesListener _listener,Browser browser) {
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
			boolean result = browser.execute(getCookiesCode);
			System.out.println(result);
		}
	}
	
	public void stopListening() {
		browser.removeStatusTextListener(this);
	}

}
