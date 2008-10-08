package com.aelitis.azureus.core.metasearch.impl;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;

import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.util.http.HTTPAuthHelper;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.listener.ExternalLoginCookieListener;

public class ExternalLoginWindow {
	
	Display display;
	Shell shell;
	
	String originalLoginUrl;
	
	String cookies;
	
	HTTPAuthHelper	sniffer;
	
	public 
	ExternalLoginWindow(
		final ExternalLoginListener listener,
		String name, 
		final String _loginUrl,
		boolean captureMode,
		String	authMode,
		boolean isMine ) 
	{
		originalLoginUrl = _loginUrl;

		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if(functionsSWT != null) {
			Shell mainShell = functionsSWT.getMainShell();
			shell = new Shell(mainShell,SWT.TITLE | SWT.CLOSE);
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
		
		shell.addDisposeListener(
			new DisposeListener()
			{
				public void 
				widgetDisposed(
					DisposeEvent arg0) 
				{
					if ( sniffer != null ){
						
						sniffer.destroy();
					}
				}
			});
		
		
		Label explain = new Label(shell,SWT.WRAP);
		if(captureMode) {
			explain.setText(MessageText.getString("externalLogin.explanation.capture", new String[]{ name }));
		} else {
			explain.setText(MessageText.getString("externalLogin.explanation", new String[]{ name }));
		}
		
		final Browser browser = new Browser(shell,Utils.getInitialBrowserStyle(SWT.BORDER));
		final ExternalLoginCookieListener cookieListener = new ExternalLoginCookieListener(new CookiesListener() {
			public void cookiesFound(String cookies) {
				if(listener != null) {
					ExternalLoginWindow.this.cookies = cookies;
					listener.cookiesFound(ExternalLoginWindow.this,cookies);
				}
			}
		},browser);
		
		cookieListener.hook();
				
		Label separator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		
		Button alt_method = null;
		
		if ( isMine ){
			
			alt_method = new Button(shell,SWT.CHECK);
			
			final Button f_alt_method = alt_method;
			
			alt_method.setText(MessageText.getString("externalLogin.auth_method_proxy"));
			
			alt_method.setSelection( authMode == WebEngine.AM_PROXY );
			
			alt_method.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event arg0) {
	
					setCaptureMethod( browser, !f_alt_method.getSelection(), true );
				}
			});
		}
		
		setCaptureMethod( browser, authMode == WebEngine.AM_TRANSPARENT, true );
				
		Button cancel = new Button(shell,SWT.PUSH);
		cancel.setText(MessageText.getString("Button.cancel"));
		
		Button done = new Button(shell,SWT.PUSH);
		done.setText(MessageText.getString("Button.done"));
		
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if(listener != null) {
					listener.canceled(ExternalLoginWindow.this);
				}
				shell.dispose();
			}
		});
		
		done.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if(listener != null) {
					listener.done(ExternalLoginWindow.this,cookies);
				}
				shell.dispose();
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
		
		if ( isMine ){
			
			data =  new FormData();
			data.width = 100;
			data.left = new FormAttachment(0,5);
			data.right = new FormAttachment(cancel,-5);
			data.bottom = new FormAttachment(100,-5);
			alt_method.setLayoutData(data);
		}
		
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
	
	protected void
	setCaptureMethod(
		final Browser		browser,
		boolean				transparent,
		boolean				show_progress )
	{
		if ( sniffer != null ){
			
			sniffer.destroy();
			
			sniffer = null;
		}
		
		if ( show_progress ){
			
			final ProgressWindow prog_wind = 
				new ProgressWindow( shell, "externalLogin.wait", SWT.DIALOG_TRIM, 500 );
			
			browser.addProgressListener(
				new ProgressListener()
				{
					public void 
					changed(
						ProgressEvent arg0 )
					{
					}
					
					public void 
					completed(
						ProgressEvent arg0 ) 
					{
						browser.removeProgressListener( this );
						
						prog_wind.destroy();
					}
				});
		}
		
		if ( transparent ){
			
			browser.setUrl( originalLoginUrl );

		}else{
				
			try{
				sniffer = new HTTPAuthHelper( new URL( originalLoginUrl ));
					
				String str = originalLoginUrl.toString();
				
				int	pos = str.indexOf( "://" );
				
				str = str.substring( pos+3 );
				
				pos = str.indexOf( "/" );
				
				if ( pos != -1 ){
					
					str = str.substring( pos );
				}
				
				if ( !str.startsWith( "/" )){
					
					str  = "/" + str;
				}
				
				browser.setUrl( "http://localhost:" + sniffer.getPort() + str );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public boolean
	proxyCaptureModeRequired()
	{
		return( sniffer != null && sniffer.wasHTTPOnlyCookieDetected());
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
		ExternalLoginWindow slw = 
			new ExternalLoginWindow(
				new ExternalLoginListener()
				{
					public void 
					cookiesFound(
						ExternalLoginWindow window,String cookies)
					{
						System.out.println( "Cookies found: " + cookies );
					}
					
					public void 
					canceled(
						ExternalLoginWindow window)
					{
						System.out.println( "Cancelled" );
					}
					
					public void 
					done(
						ExternalLoginWindow window,String cookies)
					{
						System.out.println( "Done" );
					}
				},
				"test",
				"http://waffles.fm/",
				false,
				WebEngine.AM_PROXY,
				true );
		
		while(!slw.shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		System.out.println( "Found httponly cookies=" + slw.proxyCaptureModeRequired());
	}

}
