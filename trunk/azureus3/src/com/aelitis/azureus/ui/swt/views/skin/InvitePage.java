package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractStatusListener;
import com.aelitis.azureus.ui.swt.views.skin.IDetailPage.RefreshListener;
import com.aelitis.azureus.util.Constants;

public class InvitePage
	extends AbstractDetailPage

{

	public static final String PAGE_ID = "invite.page";

	private Composite content;

	private Browser browser = null;

	private ClientMessageContext context = null;

	private RefreshListener refreshListener = null;

	public InvitePage(DetailPanel detailPanel) {
		super(detailPanel, PAGE_ID);
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		init();
	}

	private void init() {
		FillLayout fLayout = new FillLayout();
		fLayout.marginHeight = 0;
		fLayout.marginWidth = 0;
		content.setLayout(fLayout);

	}

	private Browser getBrowser() {
		if (null == browser) {
			browser = new Browser(content, SWT.NONE);
			String url = Constants.URL_PREFIX + "share.start?ts=" + Math.random();
			browser.setUrl(url);

			/*
			 * Calling to initialize the listeners
			 */
			getMessageContext();

		}
		return browser;
	}

	public Control getControl() {
		return content;
	}

	public ClientMessageContext getMessageContext() {
		if (null == context) {
			context = new BrowserContext(
					"buddy-page-listener-invite" + Math.random(), getBrowser(), null,
					true);

			/*
			 * Add listener to call the 'inviteFromShare' script; this listener is only called
			 * once whenever a web page is loaded the first time or when it's refreshed
			 */
			context.addMessageListener(new AbstractStatusListener("status") {
				public void handlePageLoadCompleted() {
					/*
					 * Setting inviteFromShare to false in the browser
					 */
					context.executeInBrowser("inviteFromShare(" + false + ")");
					
					if(null != refreshListener){
						refreshListener.refreshCompleted();
					}
				}
			});

			/*
			 * Add the appropriate messaging listeners
			 */
			getMessageContext().addMessageListener(
					new AbstractBuddyPageListener(getBrowser()) {

						public void handleCancel() {
							ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
							if (null != buttonBar) {
								buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
							}

							getDetailPanel().show(false);

						}

						public void handleClose() {
							ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
							if (null != buttonBar) {
								buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
							}

							getDetailPanel().show(false);

						}

						public void handleBuddyInvites() {
						}

						public void handleEmailInvites() {
						}

						public void handleInviteConfirm() {
							try {
								VuzeBuddyManager.inviteWithShare(getConfirmationResponse(),
										null, null, null);
							} catch (NotLoggedInException e) {
								// XXX Handle me!
								e.printStackTrace();
							}
						}

						public void handleResize() {
							if (null != getWindowState()) {
								System.out.println("Resizing standalone Add Friends: "
										+ getWindowState());//KN: sysout
							}
						}

					}

			);
		}
		return context;
	}

	public void refresh(RefreshListener refreshListener) {
		this.refreshListener = refreshListener;
		/*
		 * Calling to init the browser if it's not been done already
		 */
		if (null == browser) {
			getBrowser();
		}
		browser.refresh();
	}

}
