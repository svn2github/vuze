package com.aelitis.azureus.ui.swt.shells.main;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.views.skin.Browse;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.util.JSONUtils;

/**
 * A convenience class for creating the Debug menu
 * <p>
 * This has been extracted out into its own class since it does not really belong to production code
 * @author knguyen
 *
 */
public class DebugMenuHelper
{
	/**
	 * Creates the Debug menu and its children
	 * NOTE: This is a development only menu and so it's not modularized into separate menu items
	 * because this menu is always rendered in its entirety
	 * @param menu
	 * @param mainWindow
	 * @return
	 */
	public static MenuItem createDebugMenuItem(final Menu menuDebug) {
		MenuItem item;

		final UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null == uiFunctions) {
			throw new IllegalStateException(
					"UIFunctionsManagerSWT.getUIFunctionsSWT() is returning null");
		}

		item = new MenuItem(menuDebug, SWT.CASCADE);
		item.setText("Subscriptions");
		Menu menuSubscriptions = new Menu(menuDebug.getParent(), SWT.DROP_DOWN);
		item.setMenu(menuSubscriptions);

		item = new MenuItem(menuSubscriptions, SWT.NONE);
		item.setText("Create RSS Feed");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final Shell shell = new Shell(uiFunctions.getMainShell());
				shell.setLayout(new FormLayout());
				
				Label label = new Label(shell,SWT.NONE);
				label.setText("RSS Feed URL :");
				final Text urlText = new Text(shell,SWT.BORDER);
				urlText.setText(Utils.getLinkFromClipboard(shell.getDisplay(),false));
				Label separator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
				Button cancel = new Button(shell,SWT.PUSH);
				cancel.setText("Cancel");
				Button ok = new Button(shell,SWT.PUSH);
				ok.setText("Ok");
				
				FormData data;
				
				data = new FormData();
				data.left = new FormAttachment(0,5);
				data.right = new FormAttachment(100,-5);
				data.top = new FormAttachment(0,5);
				label.setLayoutData(data);
				
				data = new FormData();
				data.left = new FormAttachment(0,5);
				data.right = new FormAttachment(100,-5);
				data.top = new FormAttachment(label);
				data.width = 400;
				urlText.setLayoutData(data);
				
				data = new FormData();
				data.left = new FormAttachment(0,5);
				data.right = new FormAttachment(100,-5);
				data.top = new FormAttachment(urlText);
				separator.setLayoutData(data);
				
				data = new FormData();
				data.right = new FormAttachment(ok);
				data.width = 100;
				data.top = new FormAttachment(separator);
				cancel.setLayoutData(data);
				
				data = new FormData();
				data.right = new FormAttachment(100,-5);
				data.width = 100;
				data.top = new FormAttachment(separator);
				ok.setLayoutData(data);
				
				cancel.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event arg0) {
						shell.dispose();
					}
				});
				
				ok.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event arg0) {
						String url_str = urlText.getText();
						shell.dispose();
						
						try{
							URL	url = new URL( url_str );
							
							SubscriptionManagerFactory.getSingleton().createRSS( url_str, url );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				});
				
				shell.pack();
				
				
				Utils.centerWindowRelativeTo(shell, uiFunctions.getMainShell());
				
				shell.open();
				shell.setFocus();
				urlText.setFocus();
				
				
			}
		});


		item = new MenuItem(menuDebug, SWT.CASCADE);
		item.setText("BrowserTB");
		Menu menuBrowserTB = new Menu(menuDebug.getParent(), SWT.DROP_DOWN);
		item.setMenu(menuBrowserTB);

		item = new MenuItem(menuBrowserTB, SWT.NONE);
		item.setText("browser TB control ext");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Browse browse = (Browse) SkinViewManager.getByClass(Browse.class);
				Map map = new HashMap();
				map.put("download-url",
						"http://beta.legaltorrents.com/get/98-story-of-stuff");
				map.put("display-name", "control ext");
				map.put("referer", "cvs");
				map.put("is-vuze-content", new Boolean(false));
				BrowserMessage message = new BrowserMessage(
						"AZMSG;0;display;set-selected-content;"
								+ JSONUtils.encodeToJSON(map));
				browse.getBrowserSkinObject().getContext().getDispatcher().resetSequence();
				browse.getBrowserSkinObject().getContext().getDispatcher().dispatch(
						message);
			}
		});
		item = new MenuItem(menuBrowserTB, SWT.NONE);
		item.setText("browser TB control int");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Browse browse = (Browse) SkinViewManager.getByClass(Browse.class);
				Map map = new HashMap();
				map.put("torrent-hash", "6EF6QUWI6IGNLX4MV37JGXQH7HOONEJL");
				map.put("display-name", "control int");
				map.put("referer", "cvs");
				BrowserMessage message = new BrowserMessage(
						"AZMSG;0;display;set-selected-content;"
								+ JSONUtils.encodeToJSON(map));
				browse.getBrowserSkinObject().getContext().getDispatcher().resetSequence();
				browse.getBrowserSkinObject().getContext().getDispatcher().dispatch(
						message);
			}
		});
		item = new MenuItem(menuBrowserTB, SWT.NONE);
		item.setText("browser TB control int canplay");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Browse browse = (Browse) SkinViewManager.getByClass(Browse.class);
				Map map = new HashMap();
				map.put("torrent-hash", "6EF6QUWI6IGNLX4MV37JGXQH7HOONEJL");
				map.put("display-name", "control int play");
				map.put("referer", "cvs");
				map.put("can-play", new Boolean(true));
				BrowserMessage message = new BrowserMessage(
						"AZMSG;0;display;set-selected-content;"
								+ JSONUtils.encodeToJSON(map));
				browse.getBrowserSkinObject().getContext().getDispatcher().resetSequence();
				browse.getBrowserSkinObject().getContext().getDispatcher().dispatch(
						message);
			}
		});

		return item;
	}
}
