package com.aelitis.azureus.core.metasearch.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.CookieParser;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.listener.ExternalLoginCookieListener;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionListWindow;

public class ExternalLoginWindow {
	
	Display display;
	Shell shell;
	
	public ExternalLoginWindow(final WebEngine engine,final String loginUrl, final String[] requiredCookies) {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if(functionsSWT != null) {
			Shell mainShell = functionsSWT.getMainShell();
			shell = new Shell(mainShell,SWT.TITLE);
			shell.setSize(800,600);
			Utils.centerWindowRelativeTo(shell, mainShell);
			
		} else {
			shell = new Shell(SWT.TITLE | SWT.CLOSE);
			shell.setSize(800,600);
			Utils.centreWindow(shell);
		}
		
		display = shell.getDisplay();
		shell.setText(MessageText.getString("externalLogin.title"));
		
		shell.setLayout(new FormLayout());
		
		Label explain = new Label(shell,SWT.WRAP);
		explain.setText(MessageText.getString("externalLogin.explanation"));
		
		Browser browser = new Browser(shell,Utils.getInitialBrowserStyle(SWT.BORDER));
		final ExternalLoginCookieListener cookieListener = new ExternalLoginCookieListener(new CookiesListener() {
			public void cookiesFound(String cookies) {
				System.out.println(cookies);
				if(engine != null) {
					engine.setCookies(cookies);
				}
				if(CookieParser.cookiesContain(requiredCookies, cookies)) {
					//shell.dispose();
				}
			}
		},browser);
		
		cookieListener.hook();
		
		browser.setUrl(loginUrl);
		
		Label separator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		
		Button cancel = new Button(shell,SWT.PUSH);
		cancel.setText(MessageText.getString("Button.cancel"));
		
		Button done = new Button(shell,SWT.PUSH);
		done.setText(MessageText.getString("Button.done"));
		
		FormData data;
		
		data =  new FormData();
		data.left = new FormAttachment(0,5);
		data.right = new FormAttachment(100,-5);
		data.top = new FormAttachment(0,5);
		explain.setLayoutData(data);
		
		data =  new FormData();
		data.left = new FormAttachment(0,5);
		data.right = new FormAttachment(100,-5);
		data.top = new FormAttachment(explain,5);
		data.bottom = new FormAttachment(separator,-5);
		browser.setLayoutData(data);
		
		data =  new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(cancel,-5);
		separator.setLayoutData(data);
		
		data =  new FormData();
		data.width = 100;
		data.right = new FormAttachment(done,-5);
		data.bottom = new FormAttachment(100,-5);
		cancel.setLayoutData(data);
		
		data =  new FormData();
		data.width = 100;
		data.right = new FormAttachment(100,-5);
		data.bottom = new FormAttachment(100,-5);
		done.setLayoutData(data);
		
		shell.layout();
		shell.open();
	}
	
	public static void main(String[] args) {
		Display display = new Display();
		ImageRepository.loadImages(display);
		ExternalLoginWindow slw = new ExternalLoginWindow(null,"http://hdbits.org/login.php",new String[] {"pass"});
		while(!slw.shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
	}

}
