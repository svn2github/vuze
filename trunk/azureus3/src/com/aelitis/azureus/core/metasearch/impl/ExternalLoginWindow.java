package com.aelitis.azureus.core.metasearch.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.CookieParser;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.listener.ExternalLoginCookieListener;

public class ExternalLoginWindow {
	
	Display display;
	Shell shell;
	
	String cookies;
	
	public ExternalLoginWindow(final ExternalLoginListener listener,final String loginUrl,boolean captureMode) {
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
		if(captureMode) {
			explain.setText(MessageText.getString("externalLogin.explanation.capture"));
		} else {
			explain.setText(MessageText.getString("externalLogin.explanation"));
		}
		
		Browser browser = new Browser(shell,Utils.getInitialBrowserStyle(SWT.BORDER));
		final ExternalLoginCookieListener cookieListener = new ExternalLoginCookieListener(new CookiesListener() {
			public void cookiesFound(String cookies) {
				if(listener != null) {
					ExternalLoginWindow.this.cookies = cookies;
					listener.cookiesFound(cookies);
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
		
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if(listener != null) {
					listener.canceled();
				}
			}
		});
		
		done.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if(listener != null) {
					listener.done(cookies);
				}
			}
		});
		
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
	
	public void close() {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				shell.close();
			}
		});
	}
	
	public static void main(String[] args) {
		Display display = new Display();
		ImageRepository.loadImages(display);
		ExternalLoginWindow slw = new ExternalLoginWindow(null,"http://hdbits.org/login.php",false);
		while(!slw.shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
	}

}
